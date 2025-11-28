package com.example.claudeapp.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.claudeapp.R
import com.example.claudeapp.data.model.Issue
import com.example.claudeapp.data.model.IssueCategory
import com.example.claudeapp.data.model.IssueStatus
import com.example.claudeapp.ui.components.IssueCard
import com.example.claudeapp.ui.viewmodel.IssueViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToReport: () -> Unit = {},
    onNavigateToMapWithIssue: (String) -> Unit = {},
    viewModel: IssueViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        // Load issues from Firebase
        viewModel.loadIssues()
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.app_name),
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(onClick = { /* TODO: Show filter dialog */ }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Filter"
                    )
                }
            }
        )
        
        // Content
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.issues.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No issues reported yet",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Be the first to report an issue in your area",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.issues) { issue ->
                        IssueCard(
                            issue = issue,
                            onUpvote = { issueId ->
                                // Get current user ID
                                val currentUser = FirebaseAuth.getInstance().currentUser
                                val userId = currentUser?.uid ?: "anonymous"
                                viewModel.upvoteIssue(issueId, userId)
                            },
                            onDownvote = { issueId ->
                                val currentUser = FirebaseAuth.getInstance().currentUser
                                val userId = currentUser?.uid ?: "anonymous"
                                viewModel.downvoteIssue(issueId, userId)
                            },
                            onComment = { issueId ->
                                // TODO: Navigate to comments screen or show comment dialog
                                println("Opening comments for issue: $issueId")
                            },
                            onShare = { issueId ->
                                // TODO: Show share dialog
                                println("Opening share dialog for issue: $issueId")
                            },
                            onVerify = { issueId ->
                                // TODO: Navigate to verification screen
                            },
                            onLocationClick = { issue ->
                                // Navigate to map with specific issue
                                onNavigateToMapWithIssue(issue.issueId)
                            }
                        )
                    }
                }
            }
            
            // Floating Action Button
            FloatingActionButton(
                onClick = onNavigateToReport,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Report Issue"
                )
            }
            
            // Temporary Sign Out Button (for testing)
            Button(
                onClick = {
                    // Sign out the current user
                    FirebaseAuth.getInstance().signOut()
                    // Restart the app to go back to splash screen
                    android.os.Process.killProcess(android.os.Process.myPid())
                },
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomStart)
            ) {
                Text("Sign Out (Test)")
            }
        }
    }
}

