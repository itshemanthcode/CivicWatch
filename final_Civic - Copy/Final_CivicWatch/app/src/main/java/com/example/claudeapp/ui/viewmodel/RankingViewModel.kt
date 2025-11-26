package com.example.claudeapp.ui.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.claudeapp.data.model.Issue
import com.example.claudeapp.data.model.User
import com.example.claudeapp.data.repository.AuthRepository
import com.example.claudeapp.data.repository.IssueRepository
import com.example.claudeapp.ui.screens.ranking.PointAction
import com.example.claudeapp.ui.screens.ranking.RankingItem
import com.example.claudeapp.ui.screens.ranking.TopPerformer
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class RankingViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val issueRepository: IssueRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(RankingUiState())
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()
    
    fun loadRankings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Fetch all issues to calculate points from live data
                val allIssuesSnapshot = firestore.collection("issues")
                    .get()
                    .await()
                
                val allIssues = allIssuesSnapshot.documents.mapNotNull { 
                    it.toObject(Issue::class.java) 
                }
                
                // Calculate user stats from issues
                val userStatsMap = calculateUserStatsFromIssues(allIssues)
                
                // Get all users
                val allUsersSnapshot = firestore.collection("users")
                    .get()
                    .await()
                
                val allUsers = allUsersSnapshot.documents.mapNotNull { 
                    it.toObject(User::class.java) 
                }
                
                // Update user points and counts in database based on live data
                val updatedUsers = mutableListOf<User>()
                for (user in allUsers) {
                    val stats = userStatsMap[user.userId] ?: UserStats(0, 0, 0, 0)
                    
                    // Calculate points: reports * 10 + upvotes * 1 + verifications * 5
                    val calculatedPoints = (stats.reportsCount * 10) + (stats.upvotesCount * 1) + (stats.verificationsCount * 5)
                    
                    // Update user document if values differ
                    if (user.points != calculatedPoints || 
                        user.reportsCount != stats.reportsCount || 
                        user.resolvedCount != stats.verificationsCount) {
                        
                        try {
                            val userRef = firestore.collection("users").document(user.userId)
                            userRef.update(
                                mapOf(
                                    "points" to calculatedPoints,
                                    "reportsCount" to stats.reportsCount,
                                    "resolvedCount" to stats.verificationsCount
                                )
                            ).await()
                        } catch (e: Exception) {
                            println("Error updating user ${user.userId}: ${e.message}")
                        }
                    }
                    
                    // Create updated user object with calculated values
                    val updatedUser = user.copy(
                        points = calculatedPoints,
                        reportsCount = stats.reportsCount,
                        resolvedCount = stats.verificationsCount
                    )
                    updatedUsers.add(updatedUser)
                }
                
                // Sort users by points (descending)
                val sortedUsers = updatedUsers.sortedByDescending { it.points }
                
                // If no users, show empty state
                if (sortedUsers.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        topPerformers = emptyList(),
                        allRankings = emptyList(),
                        pointActions = getPointActions(),
                        error = null
                    )
                    return@launch
                }

                val topPerformers = sortedUsers.take(3).mapIndexed { index, user ->
                    TopPerformer(
                        name = user.displayName.ifBlank { user.email },
                        points = user.points,
                        icon = when (index) {
                            0 -> Icons.Default.Person
                            else -> Icons.Default.Star
                        },
                        iconColor = when (index) {
                            0 -> Color(0xFFFFD700) // Gold
                            1 -> Color(0xFFC0C0C0) // Silver
                            else -> Color(0xFFFF9800) // Bronze/Orange
                        }
                    )
                }

                val allRankings = sortedUsers.map { user ->
                    RankingItem(
                        name = user.displayName.ifBlank { user.email },
                        points = user.points,
                        reports = user.reportsCount,
                        verifications = user.resolvedCount,
                        icon = Icons.Default.Person,
                        iconColor = Color(0xFF2196F3)
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    topPerformers = topPerformers,
                    allRankings = allRankings,
                    pointActions = getPointActions(),
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load rankings"
                )
            }
        }
    }
    
    /**
     * Calculate user statistics from all issues in the database
     * Returns a map of userId -> UserStats
     */
    private fun calculateUserStatsFromIssues(issues: List<Issue>): Map<String, UserStats> {
        val statsMap = mutableMapOf<String, UserStats>()
        
        for (issue in issues) {
            // Count reports
            if (issue.reportedBy.isNotEmpty()) {
                val reporterStats = statsMap.getOrPut(issue.reportedBy) { UserStats(0, 0, 0, 0) }
                statsMap[issue.reportedBy] = reporterStats.copy(
                    reportsCount = reporterStats.reportsCount + 1
                )
            }
            
            // Count upvotes
            for (userId in issue.upvotedBy) {
                val upvoterStats = statsMap.getOrPut(userId) { UserStats(0, 0, 0, 0) }
                statsMap[userId] = upvoterStats.copy(
                    upvotesCount = upvoterStats.upvotesCount + 1
                )
            }
            
            // Count verifications
            for (userId in issue.verifiedBy) {
                val verifierStats = statsMap.getOrPut(userId) { UserStats(0, 0, 0, 0) }
                statsMap[userId] = verifierStats.copy(
                    verificationsCount = verifierStats.verificationsCount + 1
                )
            }
        }
        
        return statsMap
    }
    
    /**
     * Data class to hold calculated user statistics
     */
    private data class UserStats(
        val reportsCount: Int,
        val upvotesCount: Int,
        val verificationsCount: Int,
        val totalPoints: Int = 0 // Not used, calculated on the fly
    )
    
    private fun getPointActions(): List<PointAction> {
        return listOf(
            PointAction("Report Issue", "+10 pts", Icons.Default.Add, Color(0xFF4CAF50)),
            PointAction("Verify Issue", "+5 pts", Icons.Default.CheckCircle, Color(0xFF2196F3)),
            PointAction("Upvote", "+1 pt", Icons.Default.ThumbUp, Color(0xFFFF9800))
        )
    }
}

data class RankingUiState(
    val isLoading: Boolean = false,
    val topPerformers: List<TopPerformer> = emptyList(),
    val allRankings: List<RankingItem> = emptyList(),
    val pointActions: List<PointAction> = emptyList(),
    val error: String? = null
)
