package com.example.claudeapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Issue(
    @DocumentId
    val issueId: String = "",
    val reportedBy: String = "", // userId
    val category: String = "",
    val severity: String = "medium", // low, medium, high
    val description: String = "",
    val status: String = "reported", // reported, verified, notified, in_progress, resolved, rejected
    val location: IssueLocation = IssueLocation(),
    val images: List<String> = emptyList(), // Cloudinary URLs
    val cloudinaryPublicIds: List<String> = emptyList(), // Cloudinary public IDs for image management
    val metadata: IssueMetadata = IssueMetadata(),
    val upvotes: Int = 0,
    val upvotedBy: List<String> = emptyList(), // userId array
    val downvotes: Int = 0,
    val downvotedBy: List<String> = emptyList(), // userId array
    val comments: List<Comment> = emptyList(),
    val commentsCount: Int = 0,
    val shares: Int = 0,
    val sharedBy: List<String> = emptyList(), // userId array
    val verifications: Int = 0,
    val verifiedBy: List<String> = emptyList(), // userId array
    val priorityScore: Double = 0.0,
    val notifiedAt: Timestamp? = null,
    val resolvedAt: Timestamp? = null,
    val adminNotes: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

data class IssueLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val area: String = "",
    val city: String = "",
    val state: String = "",
    val country: String = ""
)

data class IssueMetadata(
    val timestamp: Timestamp = Timestamp.now(),
    val device: String = "",
    val appVersion: String = ""
)

enum class IssueCategory(val displayName: String) {
    POTHOLES("Potholes"),
    BROKEN_STREET_LIGHTS("Broken Street Lights"),
    GARBAGE("Garbage"),
    WATER_LOGGING("Water Logging")
}

enum class IssueSeverity(val displayName: String, val multiplier: Int) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3)
}

enum class IssueStatus(val displayName: String) {
    REPORTED("Reported"),
    VERIFIED("Verified"),
    NOTIFIED("Notified"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved"),
    REJECTED("Rejected")
}

data class Comment(
    val commentId: String = "",
    val issueId: String = "",
    val userId: String = "",
    val userDisplayName: String = "",
    val content: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

