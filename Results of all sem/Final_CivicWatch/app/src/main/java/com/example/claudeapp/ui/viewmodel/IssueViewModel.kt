package com.example.claudeapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.claudeapp.data.model.Issue
import com.example.claudeapp.data.model.IssueCategory
import com.example.claudeapp.data.model.IssueStatus
import com.example.claudeapp.data.repository.IssueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IssueViewModel @Inject constructor(
    private val issueRepository: IssueRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(IssueUiState())
    val uiState: StateFlow<IssueUiState> = _uiState.asStateFlow()
    
    fun loadIssues(
        category: IssueCategory? = null,
        status: IssueStatus? = null,
        userId: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val issues = issueRepository.getIssues(
                    category = category,
                    status = status,
                    userId = userId
                )
                
                _uiState.value = _uiState.value.copy(
                    issues = issues,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    issues = emptyList(),
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    fun loadNearbyIssues(latitude: Double, longitude: Double, radiusKm: Double = 5.0) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val nearbyIssues = issueRepository.getNearbyIssues(
                    latitude = latitude,
                    longitude = longitude,
                    radiusKm = radiusKm
                )
                
                _uiState.value = _uiState.value.copy(
                    nearbyIssues = nearbyIssues,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    nearbyIssues = emptyList(),
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    fun upvoteIssue(issueId: String, userId: String) {
        
        // Validate user ID
        if (userId == "anonymous" || userId.isEmpty()) {
            return
        }
        
        viewModelScope.launch {
            try {
                // Check if user has already downvoted this issue
                val currentIssue = _uiState.value.issues.find { it.issueId == issueId }
                if (currentIssue != null) {
                    // Optimistic UI update - update the UI immediately
                    val updatedIssues = _uiState.value.issues.map { issue ->
                        if (issue.issueId == issueId) {
                            when {
                                issue.downvotedBy.contains(userId) -> {
                                    // Switch from downvote to upvote
                                    issue.copy(
                                        upvotes = issue.upvotes + 1,
                                        downvotes = maxOf(0, issue.downvotes - 1),
                                        upvotedBy = issue.upvotedBy + userId,
                                        downvotedBy = issue.downvotedBy.filter { it != userId }
                                    )
                                }
                                !issue.upvotedBy.contains(userId) -> {
                                    // Regular upvote
                                    issue.copy(
                                        upvotes = issue.upvotes + 1,
                                        upvotedBy = issue.upvotedBy + userId
                                    )
                                }
                                else -> {
                                    // Remove upvote
                                    issue.copy(
                                        upvotes = maxOf(0, issue.upvotes - 1),
                                        upvotedBy = issue.upvotedBy.filter { it != userId }
                                    )
                                }
                            }
                        } else issue
                    }
                    
                    // Update UI state immediately
                    _uiState.value = _uiState.value.copy(issues = updatedIssues)
                    val updatedIssue = updatedIssues.find { it.issueId == issueId }
                    
                    // Perform the actual database operation
                    if (currentIssue.downvotedBy.contains(userId)) {
                        issueRepository.switchFromDownvoteToUpvote(issueId, userId)
                    } else if (!currentIssue.upvotedBy.contains(userId)) {
                        issueRepository.upvoteIssue(issueId, userId)
                    } else {
                        issueRepository.removeUpvote(issueId, userId)
                    }
                }
            } catch (e: Exception) {
                // Revert optimistic update on error
                loadIssues()
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun removeUpvote(issueId: String, userId: String) {
        viewModelScope.launch {
            try {
                // Optimistic UI update
                val updatedIssues = _uiState.value.issues.map { issue ->
                    if (issue.issueId == issueId) {
                        issue.copy(
                            upvotes = maxOf(0, issue.upvotes - 1),
                            upvotedBy = issue.upvotedBy.filter { it != userId }
                        )
                    } else issue
                }
                _uiState.value = _uiState.value.copy(issues = updatedIssues)
                
                issueRepository.removeUpvote(issueId, userId)
            } catch (e: Exception) {
                // Revert optimistic update on error
                loadIssues()
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun downvoteIssue(issueId: String, userId: String) {
        viewModelScope.launch {
            try {
                // Check if user has already upvoted this issue
                val currentIssue = _uiState.value.issues.find { it.issueId == issueId }
                if (currentIssue != null) {
                    // Optimistic UI update - update the UI immediately
                    val updatedIssues = _uiState.value.issues.map { issue ->
                        if (issue.issueId == issueId) {
                            when {
                                issue.upvotedBy.contains(userId) -> {
                                    // Switch from upvote to downvote
                                    issue.copy(
                                        upvotes = maxOf(0, issue.upvotes - 1),
                                        downvotes = issue.downvotes + 1,
                                        upvotedBy = issue.upvotedBy.filter { it != userId },
                                        downvotedBy = issue.downvotedBy + userId
                                    )
                                }
                                !issue.downvotedBy.contains(userId) -> {
                                    // Regular downvote
                                    issue.copy(
                                        downvotes = issue.downvotes + 1,
                                        downvotedBy = issue.downvotedBy + userId
                                    )
                                }
                                else -> {
                                    // Remove downvote
                                    issue.copy(
                                        downvotes = maxOf(0, issue.downvotes - 1),
                                        downvotedBy = issue.downvotedBy.filter { it != userId }
                                    )
                                }
                            }
                        } else issue
                    }
                    
                    // Update UI state immediately
                    _uiState.value = _uiState.value.copy(issues = updatedIssues)
                    
                    // Perform the actual database operation
                    if (currentIssue.upvotedBy.contains(userId)) {
                        issueRepository.switchFromUpvoteToDownvote(issueId, userId)
                    } else if (!currentIssue.downvotedBy.contains(userId)) {
                        issueRepository.downvoteIssue(issueId, userId)
                    } else {
                        issueRepository.removeDownvote(issueId, userId)
                    }
                }
            } catch (e: Exception) {
                // Revert optimistic update on error
                loadIssues()
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun removeDownvote(issueId: String, userId: String) {
        viewModelScope.launch {
            try {
                // Optimistic UI update
                val updatedIssues = _uiState.value.issues.map { issue ->
                    if (issue.issueId == issueId) {
                        issue.copy(
                            downvotes = maxOf(0, issue.downvotes - 1),
                            downvotedBy = issue.downvotedBy.filter { it != userId }
                        )
                    } else issue
                }
                _uiState.value = _uiState.value.copy(issues = updatedIssues)
                
                issueRepository.removeDownvote(issueId, userId)
            } catch (e: Exception) {
                // Revert optimistic update on error
                loadIssues()
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun addComment(issueId: String, userId: String, content: String) {
        viewModelScope.launch {
            try {
                issueRepository.addComment(issueId, userId, content)
                // Refresh the issues list
                loadIssues()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun shareIssue(issueId: String) {
        // This would typically open the system share dialog
        // For now, we'll just log it
        println("Sharing issue: $issueId")
    }
    
    fun incrementShareCount(issueId: String, userId: String) {
        viewModelScope.launch {
            try {
                issueRepository.incrementShareCount(issueId, userId)
                // Refresh the issues list
                loadIssues()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    suspend fun getComments(issueId: String): List<com.example.claudeapp.data.model.Comment> {
        return try {
            issueRepository.getComments(issueId)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = e.message)
            emptyList()
        }
    }
    
    fun createIssue(issue: Issue) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val issueId = issueRepository.createIssue(issue)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastCreatedIssueId = issueId
                )
                // Refresh the issues list to show the new report
                loadIssues()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearLastCreatedIssueId() {
        _uiState.value = _uiState.value.copy(lastCreatedIssueId = null)
    }
    fun deleteIssue(issueId: String, userId: String) {
        viewModelScope.launch {
            try {
                // Optimistic UI update - remove from list immediately
                val updatedIssues = _uiState.value.issues.filter { it.issueId != issueId }
                _uiState.value = _uiState.value.copy(issues = updatedIssues)
                
                issueRepository.deleteIssue(issueId, userId)
            } catch (e: Exception) {
                // Revert on error
                loadIssues()
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}

data class IssueUiState(
    val issues: List<Issue> = emptyList(),
    val nearbyIssues: List<Issue> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastCreatedIssueId: String? = null
)
