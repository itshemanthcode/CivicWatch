package com.example.claudeapp.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.claudeapp.ui.components.IssueCard
import com.example.claudeapp.ui.viewmodel.IssueViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.claudeapp.ui.screens.comments.CommentsBottomSheet
import com.example.claudeapp.data.model.Issue
import androidx.compose.ui.platform.LocalContext
import com.example.claudeapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserIssuesScreen(
    navController: NavController,
    userId: String,
    initialIssueId: String? = null,
    viewModel: IssueViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedIssueForComments by remember { mutableStateOf<Issue?>(null) }
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(userId) {
        viewModel.loadIssues(userId = userId)
    }

    // Scroll to initial issue
    LaunchedEffect(uiState.issues, initialIssueId) {
        if (initialIssueId != null && uiState.issues.isNotEmpty()) {
            val index = uiState.issues.indexOfFirst { it.issueId == initialIssueId }
            if (index != -1) {
                listState.scrollToItem(index)
            }
        }
    }

    var showDeleteDialog by remember { mutableStateOf<String?>(null) }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(androidx.compose.ui.res.stringResource(R.string.dialog_delete_title)) },
            text = { Text(androidx.compose.ui.res.stringResource(R.string.dialog_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val issueId = showDeleteDialog!!
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        val currentUserId = currentUser?.uid ?: "anonymous"
                        viewModel.deleteIssue(issueId, currentUserId)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(androidx.compose.ui.res.stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(androidx.compose.ui.res.stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text(androidx.compose.ui.res.stringResource(R.string.title_your_reports)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.issues) { issue ->
                    IssueCard(
                        issue = issue,
                        onUpvote = { issueId ->
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            val currentUserId = currentUser?.uid ?: "anonymous"
                            viewModel.upvoteIssue(issueId, currentUserId)
                        },
                        onDownvote = { issueId ->
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            val currentUserId = currentUser?.uid ?: "anonymous"
                            viewModel.downvoteIssue(issueId, currentUserId)
                        },
                        onComment = { issueId ->
                            selectedIssueForComments = uiState.issues.find { it.issueId == issueId }
                        },
                        onShare = { issueId ->
                            com.example.claudeapp.utils.shareIssue(context, issue)
                            
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            val currentUserId = currentUser?.uid ?: "anonymous"
                            viewModel.incrementShareCount(issueId, currentUserId)
                        },
                        onVerify = { /* TODO */ },
                        onLocationClick = { /* Optional */ },
                        onDelete = { issueId ->
                            showDeleteDialog = issueId
                        }
                    )
                }
            }
        }
    }

    selectedIssueForComments?.let { issue ->
        CommentsBottomSheet(
            issue = issue,
            onDismiss = { selectedIssueForComments = null }
        )
    }
}
