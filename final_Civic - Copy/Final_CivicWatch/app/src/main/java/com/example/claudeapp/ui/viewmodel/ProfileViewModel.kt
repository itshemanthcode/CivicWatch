package com.example.claudeapp.ui.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.claudeapp.data.repository.AuthRepository
import com.example.claudeapp.data.repository.IssueRepository
import com.example.claudeapp.ui.screens.profile.Badge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val issueRepository: IssueRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    fun loadProfileData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val currentUser = authRepository.currentUser
                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No user signed in"
                    )
                    return@launch
                }
                
                // Sync reports count from database to ensure accuracy
                issueRepository.syncUserReportsCount(currentUser.uid)
                
                // Get updated user data
                val user = authRepository.getCurrentUserData()
                if (user == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load user data"
                    )
                    return@launch
                }
                
                val computedBadges = buildList {
                    if (user.reportsCount >= 5) add(
                        Badge(
                            title = "Active Reporter",
                            description = "Reported 5+ issues",
                            icon = Icons.Default.Add,
                            iconColor = Color(0xFFFF9800)
                        )
                    )
                    if (user.resolvedCount >= 10) add(
                        Badge(
                            title = "Community Verifier",
                            description = "Verified 10+ issues",
                            icon = Icons.Default.CheckCircle,
                            iconColor = Color(0xFF2196F3)
                        )
                    )
                }
                // Fetch user's issues
                val userIssues = issueRepository.getIssues(userId = currentUser.uid, limit = 50)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userName = user.displayName.ifBlank { user.email },
                    userEmail = user.email,
                    points = user.points,
                    reportsCount = user.reportsCount,
                    verificationsCount = user.resolvedCount,
                    badges = computedBadges,
                    totalIssuesReported = user.reportsCount,
                    userIssues = userIssues
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
                _uiState.value = _uiState.value.copy(
                    isSignedOut = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message
                )
            }
        }
    }
    
}

data class ProfileUiState(
    val isLoading: Boolean = false,
    val userName: String = "",
    val userEmail: String = "",
    val points: Int = 0,
    val reportsCount: Int = 0,
    val verificationsCount: Int = 0,
    val badges: List<Badge> = emptyList(),
    val totalIssuesReported: Int = 0,
    val userIssues: List<com.example.claudeapp.data.model.Issue> = emptyList(),
    val isSignedOut: Boolean = false,
    val error: String? = null
)
