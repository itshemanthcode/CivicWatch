package com.example.claudeapp.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.claudeapp.data.imgbb.ImgBBRepository
import com.example.claudeapp.data.model.Issue
import com.example.claudeapp.data.model.IssueCategory
import com.example.claudeapp.data.model.IssueStatus
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IssueRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val imgbbRepository: ImgBBRepository,
    private val zapierRepository: ZapierRepository
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    suspend fun createIssue(issue: Issue): String {
        val issueId = UUID.randomUUID().toString()
        val issueWithId = issue.copy(issueId = issueId)
        
        // Save issue to Firestore
        firestore.collection("issues").document(issueId).set(issueWithId).await()
        
        // Update user points and reports count
        updateUserStatsAfterReport(issue.reportedBy)
        
        return issueId
    }
    
    private suspend fun updateUserStatsAfterReport(userId: String) {
        val userRef = firestore.collection("users").document(userId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val user = snapshot.toObject(com.example.claudeapp.data.model.User::class.java)
            
            if (user != null) {
                // User exists, update stats
                val updatedUser = user.copy(
                    points = user.points + 10, // 10 points for reporting
                    reportsCount = user.reportsCount + 1
                )
                transaction.set(userRef, updatedUser)
            } else {
                // User doesn't exist, create with initial stats
                // Get user email from Firebase Auth if possible
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val newUser = com.example.claudeapp.data.model.User(
                    userId = userId,
                    email = firebaseUser?.email ?: "",
                    displayName = firebaseUser?.displayName ?: "",
                    photoURL = firebaseUser?.photoUrl?.toString() ?: "",
                    points = 10, // 10 points for first report
                    reportsCount = 1,
                    resolvedCount = 0,
                    reputation = 100,
                    createdAt = com.google.firebase.Timestamp.now(),
                    lastLoginAt = com.google.firebase.Timestamp.now()
                )
                transaction.set(userRef, newUser)
            }
        }.await()
        
        // Also verify the count matches actual issues in database
        syncUserReportsCount(userId)
    }
    
    // Sync user's reports count with actual count from database
    suspend fun syncUserReportsCount(userId: String) {
        try {
            val actualCount = firestore.collection("issues")
                .whereEqualTo("reportedBy", userId)
                .get()
                .await()
                .size()
            
            val userRef = firestore.collection("users").document(userId)
            userRef.update("reportsCount", actualCount).await()
        } catch (e: Exception) {
            // Log error but don't fail the report creation
            println("Error syncing user reports count: ${e.message}")
        }
    }
    
    suspend fun getIssues(
        limit: Int = 20,
        lastDocument: com.google.firebase.firestore.DocumentSnapshot? = null,
        category: IssueCategory? = null,
        status: IssueStatus? = null,
        userId: String? = null
    ): List<Issue> {
        var query = firestore.collection("issues")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
        
        if (category != null) {
            query = query.whereEqualTo("category", category.name)
        }
        
        if (status != null) {
            query = query.whereEqualTo("status", status.name.lowercase())
        }
        
        if (userId != null) {
            query = query.whereEqualTo("reportedBy", userId)
        }
        
        if (lastDocument != null) {
            query = query.startAfter(lastDocument)
        }
        
        val snapshot = query.get().await()
        return snapshot.documents.mapNotNull { it.toObject(Issue::class.java) }
    }
    
    suspend fun getIssueById(issueId: String): Issue? {
        val document = firestore.collection("issues").document(issueId).get().await()
        return document.toObject(Issue::class.java)
    }
    
    suspend fun getNearbyIssues(
        latitude: Double,
        longitude: Double,
        radiusKm: Double = 5.0,
        limit: Int = 50
    ): List<Issue> {
        // Note: This is a simplified implementation
        // For production, consider using GeoFirestore or similar for efficient geo queries
        val snapshot = firestore.collection("issues")
            .limit(limit.toLong())
            .get().await()
        
        val issues = snapshot.documents.mapNotNull { it.toObject(Issue::class.java) }
            .filter { issue ->
                val distance = calculateDistance(
                    latitude, longitude,
                    issue.location.latitude, issue.location.longitude
                )
                distance <= radiusKm
            }
        
        return issues
    }
    
    suspend fun upvoteIssue(issueId: String, userId: String) {
        val issueRef = firestore.collection("issues").document(issueId)
        val userRef = firestore.collection("users").document(userId)
        
        var shouldNotify = false
        var updatedIssue: Issue? = null
        
        firestore.runTransaction { transaction ->
            val issueSnapshot = transaction.get(issueRef)
            val userSnapshot = transaction.get(userRef)
            val issue = issueSnapshot.toObject(Issue::class.java)
            val user = userSnapshot.toObject(com.example.claudeapp.data.model.User::class.java)
            
            if (issue != null && !issue.upvotedBy.contains(userId)) {
                // Update issue upvotes
                val updatedUpvotedBy = issue.upvotedBy + userId
                val newUpvotes = issue.upvotes + 1
                
                // Auto-change status to NOTIFIED when upvotes reach 5
                // This implements the "Civic Issue Notification Escalator" - monitors for 5+ upvotes
                val currentStatus = issue.status.lowercase()
                val newStatus = if (newUpvotes >= 5 && 
                    currentStatus != IssueStatus.NOTIFIED.name.lowercase() &&
                    currentStatus != IssueStatus.RESOLVED.name.lowercase() &&
                    currentStatus != IssueStatus.REJECTED.name.lowercase()) {
                    // Change from "verified" (or "reported") to "notified"
                    IssueStatus.NOTIFIED.name.lowercase()
                } else {
                    issue.status
                }
                
                val issueToSave = issue.copy(
                    upvotes = newUpvotes,
                    upvotedBy = updatedUpvotedBy,
                    status = newStatus,
                    updatedAt = com.google.firebase.Timestamp.now(),
                    notifiedAt = if (newStatus == IssueStatus.NOTIFIED.name.lowercase()) 
                        com.google.firebase.Timestamp.now() else issue.notifiedAt
                )
                transaction.set(issueRef, issueToSave)
                
                // Update user points (+1 for upvoting)
                if (user != null) {
                    val updatedUser = user.copy(
                        points = user.points + 1
                    )
                    transaction.set(userRef, updatedUser)
                }
                
                // Send notification to issue reporter
                if (issue.reportedBy != userId) {
                    val notificationId = UUID.randomUUID().toString()
                    val notification = com.example.claudeapp.data.model.AppNotification(
                        notificationId = notificationId,
                        userId = issue.reportedBy,
                        type = com.example.claudeapp.data.model.NotificationType.UPVOTED.name,
                        title = "Your Report was Upvoted",
                        body = "Someone upvoted your report on ${issue.category}",
                        data = mapOf("issueId" to issueId),
                        createdAt = com.google.firebase.Timestamp.now()
                    )
                    transaction.set(firestore.collection("notifications").document(notificationId), notification)
                }
                
                // Check if we need to notify authorities
                if (newStatus == IssueStatus.NOTIFIED.name.lowercase() && 
                    issue.status != IssueStatus.NOTIFIED.name.lowercase()) {
                    shouldNotify = true
                    updatedIssue = issueToSave
                }
            }
        }.await()
        
        // Trigger Zapier notification after transaction completes
        if (shouldNotify && updatedIssue != null) {
            coroutineScope.launch {
                notifyAuthoritiesForIssue(updatedIssue!!)
            }
        }
    }
    
    suspend fun removeUpvote(issueId: String, userId: String) {
        val issueRef = firestore.collection("issues").document(issueId)
        val userRef = firestore.collection("users").document(userId)
        
        firestore.runTransaction { transaction ->
            val issueSnapshot = transaction.get(issueRef)
            val userSnapshot = transaction.get(userRef)
            val issue = issueSnapshot.toObject(Issue::class.java)
            val user = userSnapshot.toObject(com.example.claudeapp.data.model.User::class.java)
            
            if (issue != null && issue.upvotedBy.contains(userId)) {
                // Update issue upvotes
                val updatedUpvotedBy = issue.upvotedBy.filter { it != userId }
                val updatedIssue = issue.copy(
                    upvotes = maxOf(0, issue.upvotes - 1),
                    upvotedBy = updatedUpvotedBy,
                    updatedAt = com.google.firebase.Timestamp.now()
                )
                transaction.set(issueRef, updatedIssue)
                
                // Update user points (-1 for removing upvote)
                if (user != null) {
                    val updatedUser = user.copy(
                        points = maxOf(0, user.points - 1)
                    )
                    transaction.set(userRef, updatedUser)
                }
            }
        }.await()
    }
    
    suspend fun downvoteIssue(issueId: String, userId: String) {
        val issueRef = firestore.collection("issues").document(issueId)
        val userRef = firestore.collection("users").document(userId)
        
        firestore.runTransaction { transaction ->
            val issueSnapshot = transaction.get(issueRef)
            val userSnapshot = transaction.get(userRef)
            val issue = issueSnapshot.toObject(Issue::class.java)
            val user = userSnapshot.toObject(com.example.claudeapp.data.model.User::class.java)
            
            if (issue != null && !issue.downvotedBy.contains(userId)) {
                // Update issue downvotes
                val updatedDownvotedBy = issue.downvotedBy + userId
                val updatedIssue = issue.copy(
                    downvotes = issue.downvotes + 1,
                    downvotedBy = updatedDownvotedBy,
                    updatedAt = com.google.firebase.Timestamp.now()
                )
                transaction.set(issueRef, updatedIssue)
                
                // Update user points (+1 for downvoting)
                if (user != null) {
                    val updatedUser = user.copy(
                        points = user.points + 1
                    )
                    transaction.set(userRef, updatedUser)
                }
            }
        }.await()
    }
    
    suspend fun removeDownvote(issueId: String, userId: String) {
        val issueRef = firestore.collection("issues").document(issueId)
        val userRef = firestore.collection("users").document(userId)
        
        firestore.runTransaction { transaction ->
            val issueSnapshot = transaction.get(issueRef)
            val userSnapshot = transaction.get(userRef)
            val issue = issueSnapshot.toObject(Issue::class.java)
            val user = userSnapshot.toObject(com.example.claudeapp.data.model.User::class.java)
            
            if (issue != null && issue.downvotedBy.contains(userId)) {
                // Update issue downvotes
                val updatedDownvotedBy = issue.downvotedBy.filter { it != userId }
                val updatedIssue = issue.copy(
                    downvotes = maxOf(0, issue.downvotes - 1),
                    downvotedBy = updatedDownvotedBy,
                    updatedAt = com.google.firebase.Timestamp.now()
                )
                transaction.set(issueRef, updatedIssue)
                
                // Update user points (-1 for removing downvote)
                if (user != null) {
                    val updatedUser = user.copy(
                        points = maxOf(0, user.points - 1)
                    )
                    transaction.set(userRef, updatedUser)
                }
            }
        }.await()
    }
    
    suspend fun switchFromDownvoteToUpvote(issueId: String, userId: String) {
        val issueRef = firestore.collection("issues").document(issueId)
        val userRef = firestore.collection("users").document(userId)
        
        firestore.runTransaction { transaction ->
            val issueSnapshot = transaction.get(issueRef)
            val userSnapshot = transaction.get(userRef)
            val issue = issueSnapshot.toObject(Issue::class.java)
            val user = userSnapshot.toObject(com.example.claudeapp.data.model.User::class.java)
            
            if (issue != null && issue.downvotedBy.contains(userId) && !issue.upvotedBy.contains(userId)) {
                // Remove from downvotes and add to upvotes
                val updatedDownvotedBy = issue.downvotedBy.filter { it != userId }
                val updatedUpvotedBy = issue.upvotedBy + userId
                val updatedIssue = issue.copy(
                    downvotes = maxOf(0, issue.downvotes - 1),
                    upvotes = issue.upvotes + 1,
                    downvotedBy = updatedDownvotedBy,
                    upvotedBy = updatedUpvotedBy,
                    updatedAt = com.google.firebase.Timestamp.now()
                )
                transaction.set(issueRef, updatedIssue)
                
                // User points remain the same (0 net change)
            }
        }.await()
    }
    
    suspend fun switchFromUpvoteToDownvote(issueId: String, userId: String) {
        val issueRef = firestore.collection("issues").document(issueId)
        val userRef = firestore.collection("users").document(userId)
        
        firestore.runTransaction { transaction ->
            val issueSnapshot = transaction.get(issueRef)
            val userSnapshot = transaction.get(userRef)
            val issue = issueSnapshot.toObject(Issue::class.java)
            val user = userSnapshot.toObject(com.example.claudeapp.data.model.User::class.java)
            
            if (issue != null && issue.upvotedBy.contains(userId) && !issue.downvotedBy.contains(userId)) {
                // Remove from upvotes and add to downvotes
                val updatedUpvotedBy = issue.upvotedBy.filter { it != userId }
                val updatedDownvotedBy = issue.downvotedBy + userId
                val updatedIssue = issue.copy(
                    upvotes = maxOf(0, issue.upvotes - 1),
                    downvotes = issue.downvotes + 1,
                    upvotedBy = updatedUpvotedBy,
                    downvotedBy = updatedDownvotedBy,
                    updatedAt = com.google.firebase.Timestamp.now()
                )
                transaction.set(issueRef, updatedIssue)
                
                // User points remain the same (0 net change)
            }
        }.await()
    }
    
    suspend fun addComment(issueId: String, userId: String, content: String) {
        val issueRef = firestore.collection("issues").document(issueId)
        val userRef = firestore.collection("users").document(userId)
        
        firestore.runTransaction { transaction ->
            val issueSnapshot = transaction.get(issueRef)
            val userSnapshot = transaction.get(userRef)
            val issue = issueSnapshot.toObject(Issue::class.java)
            val user = userSnapshot.toObject(com.example.claudeapp.data.model.User::class.java)
            
            if (issue != null && user != null) {
                val commentId = UUID.randomUUID().toString()
                val newComment = com.example.claudeapp.data.model.Comment(
                    commentId = commentId,
                    issueId = issueId,
                    userId = userId,
                    userDisplayName = user.displayName.ifBlank { user.email },
                    content = content,
                    createdAt = com.google.firebase.Timestamp.now(),
                    updatedAt = com.google.firebase.Timestamp.now()
                )
                
                val updatedComments = issue.comments + newComment
                val updatedIssue = issue.copy(
                    comments = updatedComments,
                    commentsCount = updatedComments.size,
                    updatedAt = com.google.firebase.Timestamp.now()
                )
                transaction.set(issueRef, updatedIssue)
                
                // Update user points (+2 for commenting)
                val updatedUser = user.copy(
                    points = user.points + 2
                )
                transaction.set(userRef, updatedUser)
                
                // Send notification to issue reporter
                if (issue.reportedBy != userId) {
                    val notificationId = UUID.randomUUID().toString()
                    val notification = com.example.claudeapp.data.model.AppNotification(
                        notificationId = notificationId,
                        userId = issue.reportedBy,
                        type = com.example.claudeapp.data.model.NotificationType.COMMENT.name,
                        title = "New Comment on your Report",
                        body = "${user.displayName} commented: ${content.take(50)}${if (content.length > 50) "..." else ""}",
                        data = mapOf("issueId" to issueId),
                        createdAt = com.google.firebase.Timestamp.now()
                    )
                    transaction.set(firestore.collection("notifications").document(notificationId), notification)
                }
            }
        }.await()
    }
    
    suspend fun incrementShareCount(issueId: String, userId: String) {
        val issueRef = firestore.collection("issues").document(issueId)
        val userRef = firestore.collection("users").document(userId)
        
        firestore.runTransaction { transaction ->
            val issueSnapshot = transaction.get(issueRef)
            val userSnapshot = transaction.get(userRef)
            val issue = issueSnapshot.toObject(Issue::class.java)
            val user = userSnapshot.toObject(com.example.claudeapp.data.model.User::class.java)
            
            if (issue != null && user != null) {
                val updatedSharedBy = issue.sharedBy + userId
                val updatedIssue = issue.copy(
                    shares = issue.shares + 1,
                    sharedBy = updatedSharedBy,
                    updatedAt = com.google.firebase.Timestamp.now()
                )
                transaction.set(issueRef, updatedIssue)
                
                // Update user points (+1 for sharing)
                val updatedUser = user.copy(
                    points = user.points + 1
                )
                transaction.set(userRef, updatedUser)
            }
        }.await()
    }
    
    suspend fun getComments(issueId: String): List<com.example.claudeapp.data.model.Comment> {
        val issue = getIssueById(issueId)
        return issue?.comments ?: emptyList()
    }
    
    suspend fun updateIssueStatus(issueId: String, status: IssueStatus, adminNotes: String = "") {
        val issueRef = firestore.collection("issues").document(issueId)
        
        // Get current issue to check if status is changing to NOTIFIED
        val currentIssue = issueRef.get().await().toObject(Issue::class.java)
        val wasNotified = currentIssue?.status == IssueStatus.NOTIFIED.name.lowercase()
        val isBecomingNotified = status == IssueStatus.NOTIFIED && !wasNotified
        
        val updates = mutableMapOf<String, Any>(
            "status" to status.name.lowercase(),
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
        
        if (adminNotes.isNotEmpty()) {
            updates["adminNotes"] = adminNotes
        }
        
        if (status == IssueStatus.RESOLVED) {
            updates["resolvedAt"] = com.google.firebase.Timestamp.now()
        }
        
        if (status == IssueStatus.NOTIFIED) {
            updates["notifiedAt"] = com.google.firebase.Timestamp.now()
        }
        
        issueRef.update(updates).await()
        
        // Trigger Zapier notification if status changed to NOTIFIED
        if (isBecomingNotified && currentIssue != null) {
            val updatedIssue = currentIssue.copy(
                status = status.name.lowercase(),
                notifiedAt = com.google.firebase.Timestamp.now(),
                updatedAt = com.google.firebase.Timestamp.now()
            )
            coroutineScope.launch {
                notifyAuthoritiesForIssue(updatedIssue)
            }
        }
    }
    
    /**
     * Civic Issue Notification Escalator
     * Monitors Firebase for issues with 5+ upvotes and automatically notifies appropriate authorities
     * Determines authority based on issue type and location (road dept for potholes, utilities for streetlights, etc.)
     */
    private suspend fun notifyAuthoritiesForIssue(issue: Issue) {
        try {
            // Step 1: Determine which authorities to contact based on issue category and location
            val authorities = determineRelevantAuthorities(issue)
            
            if (authorities.isEmpty()) {
                println("No relevant authorities found for issue ${issue.issueId} (category: ${issue.category}, location: ${issue.location.city}, ${issue.location.state})")
                return
            }
            
            println("Found ${authorities.size} relevant authority/authorities for issue ${issue.issueId}")
            
            // Step 2: Send comprehensive report via Zapier AI agent
            val result = zapierRepository.notifyAuthoritiesAboutIssue(issue, authorities)
            result.fold(
                onSuccess = {
                    println("✅ Successfully escalated issue ${issue.issueId} to authorities via Zapier")
                },
                onFailure = { error ->
                    println("❌ Failed to escalate issue ${issue.issueId} to authorities via Zapier: ${error.message}")
                    // Log error but don't fail the operation - issue status is already updated
                }
            )
        } catch (e: Exception) {
            println("Error in Civic Issue Notification Escalator: ${e.message}")
            e.printStackTrace()
            // Don't throw - this is a background operation
        }
    }
    
    /**
     * Determine relevant authorities based on:
     * 1. Issue category/type (road dept for potholes, utilities for streetlights, etc.)
     * 2. Location (city and state)
     */
    private suspend fun determineRelevantAuthorities(issue: Issue): List<com.example.claudeapp.data.model.Authority> {
        return try {
            // First, try to find authorities that handle this specific category
            val categoryBasedAuthorities = try {
                firestore.collection("authorities")
                    .whereEqualTo("jurisdiction.city", issue.location.city)
                    .whereEqualTo("jurisdiction.state", issue.location.state)
                    .whereArrayContains("handledCategories", issue.category.lowercase())
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.toObject(com.example.claudeapp.data.model.Authority::class.java) }
                    .filter { it.email.isNotEmpty() && it.notificationPreferences.email }
            } catch (e: Exception) {
                println("Error fetching category-based authorities: ${e.message}")
                emptyList()
            }
            
            // If we found category-specific authorities, use them
            if (categoryBasedAuthorities.isNotEmpty()) {
                return categoryBasedAuthorities
            }
            
            // Fallback: Find authorities by department type based on issue category
            val departmentType = mapCategoryToDepartmentType(issue.category)
            val departmentBasedAuthorities = try {
                firestore.collection("authorities")
                    .whereEqualTo("jurisdiction.city", issue.location.city)
                    .whereEqualTo("jurisdiction.state", issue.location.state)
                    .whereEqualTo("departmentType", departmentType)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.toObject(com.example.claudeapp.data.model.Authority::class.java) }
                    .filter { it.email.isNotEmpty() && it.notificationPreferences.email }
            } catch (e: Exception) {
                println("Error fetching department-based authorities: ${e.message}")
                emptyList()
            }
            
            // If we found department-based authorities, use them
            if (departmentBasedAuthorities.isNotEmpty()) {
                return departmentBasedAuthorities
            }
            
            // Final fallback: Get all authorities for the location
            firestore.collection("authorities")
                .whereEqualTo("jurisdiction.city", issue.location.city)
                .whereEqualTo("jurisdiction.state", issue.location.state)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(com.example.claudeapp.data.model.Authority::class.java) }
                .filter { it.email.isNotEmpty() && it.notificationPreferences.email }
        } catch (e: Exception) {
            println("Error determining relevant authorities: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Map issue category to department type
     * This helps determine which authority department should handle the issue
     */
    private fun mapCategoryToDepartmentType(category: String): String {
        val categoryLower = category.lowercase()
        return when {
            categoryLower.contains("pothole") || categoryLower.contains("road") || categoryLower.contains("street") -> "road_department"
            categoryLower.contains("light") -> "utilities"
            categoryLower.contains("garbage") || categoryLower.contains("trash") || categoryLower.contains("waste") -> "sanitation"
            categoryLower.contains("water") -> "public_works"
            else -> "public_works" // Default to public works
        }
    }
    
    suspend fun uploadIssueImage(context: Context, imageUri: String, issueId: String): String {
        val result = imgbbRepository.uploadImage(
            context = context,
            imageUri = android.net.Uri.parse(imageUri),
            name = "civicwatch_issue_${issueId}_${System.currentTimeMillis()}"
        )
        
        return when {
            result.isSuccess -> {
                val uploadResult = result.getOrNull()!!
                uploadResult.url
            }
            result.isFailure -> {
                throw Exception("Failed to upload image: ${result.exceptionOrNull()?.message}")
            }
            else -> {
                throw Exception("Unknown result type")
            }
        }
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        
        return earthRadius * c
    }

    suspend fun deleteIssue(issueId: String, userId: String) {
        val issueRef = firestore.collection("issues").document(issueId)
        val userRef = firestore.collection("users").document(userId)
        
        firestore.runTransaction { transaction ->
            val issueSnapshot = transaction.get(issueRef)
            val userSnapshot = transaction.get(userRef)
            val issue = issueSnapshot.toObject(Issue::class.java)
            val user = userSnapshot.toObject(com.example.claudeapp.data.model.User::class.java)
            
            if (issue != null && issue.reportedBy == userId) {
                // Delete the issue
                transaction.delete(issueRef)
                
                // Update user stats
                if (user != null) {
                    val updatedUser = user.copy(
                        reportsCount = maxOf(0, user.reportsCount - 1)
                    )
                    transaction.set(userRef, updatedUser)
                }
            } else {
                throw Exception("Issue not found or user not authorized to delete")
            }
        }.await()
    }
}
