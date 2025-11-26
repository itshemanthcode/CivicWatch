package com.example.claudeapp.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.claudeapp.data.model.Issue
import com.example.claudeapp.ui.components.IssueCard
import com.example.claudeapp.ui.viewmodel.IssueViewModel
import com.example.claudeapp.ui.screens.comments.CommentsBottomSheet
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.claudeapp.R
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext

// Use MaterialTheme colors for proper dark mode support


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleModernHomeScreen(
    navController: NavHostController,
    highlightIssueId: String? = null,
    onNavigateToReport: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToRanking: () -> Unit = {},
    viewModel: IssueViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedIssueForComments by remember { mutableStateOf<Issue?>(null) }
    
    // Optimize for smooth scrolling
    val listState = rememberLazyListState()
    
    // Memoize derived values
    val issuesCount = remember(uiState.issues) { uiState.issues.size }
    val isLoading = remember(uiState.isLoading) { uiState.isLoading }
    val isEmpty = remember(uiState.issues) { uiState.issues.isEmpty() }
    
    // Calculate notified and resolved counts
    // Notified count includes issues with status "NOTIFIED"
    val notifiedCount = remember(uiState.issues) {
        uiState.issues.count { issue ->
            issue.status == com.example.claudeapp.data.model.IssueStatus.NOTIFIED.name.lowercase()
        }
    }
    val resolvedCount = remember(uiState.issues) {
        uiState.issues.count { issue ->
            issue.status == com.example.claudeapp.data.model.IssueStatus.RESOLVED.name.lowercase()
        }
    }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadIssues()
    }

    // Scroll to highlighted issue if present
    LaunchedEffect(highlightIssueId, uiState.issues) {
        if (highlightIssueId != null && uiState.issues.isNotEmpty()) {
            val index = uiState.issues.indexOfFirst { it.issueId == highlightIssueId }
            if (index != -1) {
                // Add offset for header items (Header + Stats = 2 items)
                // Note: If loading or empty state is showing, this might be off, but 
                // issues.isNotEmpty() check handles the empty case.
                listState.animateScrollToItem(index + 2)
            }
        }
    }

    Scaffold(
        // FIX 1: Set Scaffold background to the dark background color
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToReport,
                modifier = Modifier.padding(16.dp),
                containerColor = MaterialTheme.colorScheme.secondary // Use Brand Green for the CTA
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = androidx.compose.ui.res.stringResource(R.string.cd_report_issue),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- SCROLLABLE TOP NAVBAR ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Logo and Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.civicwatch_full_logo),
                                contentDescription = androidx.compose.ui.res.stringResource(R.string.cd_civicwatch_logo),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = androidx.compose.ui.res.stringResource(R.string.app_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // Action Buttons
                        Row {
                            IconButton(onClick = onNavigateToMap) {
                                Icon(Icons.Default.LocationOn, contentDescription = androidx.compose.ui.res.stringResource(R.string.cd_map), tint = MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(onClick = onNavigateToRanking) {
                                Icon(Icons.Default.Star, contentDescription = androidx.compose.ui.res.stringResource(R.string.cd_rankings), tint = MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(onClick = onNavigateToProfile) {
                                Icon(Icons.Default.Person, contentDescription = androidx.compose.ui.res.stringResource(R.string.cd_profile), tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
            
            // --- SCROLLABLE STATISTICS SECTION ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            icon = Icons.Default.List,
                            value = issuesCount.toString(),
                            label = androidx.compose.ui.res.stringResource(R.string.stat_issues),
                            color = MaterialTheme.colorScheme.onSurface,
                            accentColor = MaterialTheme.colorScheme.primary,
                            backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        StatItem(
                            icon = Icons.Default.Schedule,
                            value = notifiedCount.toString(),
                            label = androidx.compose.ui.res.stringResource(R.string.stat_notified),
                            color = MaterialTheme.colorScheme.onSurface,
                            accentColor = MaterialTheme.colorScheme.secondary,
                            backgroundColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                        )
                        StatItem(
                            icon = Icons.Default.CheckCircle,
                            value = resolvedCount.toString(),
                            label = androidx.compose.ui.res.stringResource(R.string.stat_resolved),
                            color = MaterialTheme.colorScheme.onSurface,
                            accentColor = MaterialTheme.colorScheme.primary,
                            backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    }
                }
            }
            
            // --- LOADING STATE ---
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            
            // --- EMPTY STATE ---
            if (!isLoading && isEmpty) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.empty_issues_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.empty_issues_message),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onNavigateToReport,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(androidx.compose.ui.res.stringResource(R.string.btn_report_first))
                        }
                    }
                }
            }
            
            // --- ISSUES LIST ---
            if (!isLoading && !isEmpty) {
                items(
                    items = uiState.issues,
                    key = { issue -> issue.issueId ?: issue.toString() }
                ) { issue ->
                    IssueCard(
                        issue = issue,
                        onUpvote = { issueId ->
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
                            selectedIssueForComments = uiState.issues.find { it.issueId == issueId }
                        },
                        onShare = { issueId ->
                            com.example.claudeapp.utils.shareIssue(context, issue)
                            
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            val userId = currentUser?.uid ?: "anonymous"
                            viewModel.incrementShareCount(issueId, userId)
                        },
                        onVerify = { /* TODO: Navigate to verification screen */ },
                        onLocationClick = { issue ->
                            // Navigate to map with specific issue
                            navController.navigate("map/${issue.issueId}")
                        }
                    )
                }
            }
        }
    }
    
    // Comments Bottom Sheet
    selectedIssueForComments?.let { issue ->
        CommentsBottomSheet(
            issue = issue,
            onDismiss = { selectedIssueForComments = null }
        )
    }
}

// --- UPDATED STAT ITEM COMPOSABLE ---
@Composable
fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color, // Text color for value
    accentColor: Color, // Icon and accent background color
    backgroundColor: Color // Box background color
) {
    // Memoize colors to prevent unnecessary recomposition
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val titleMediumStyle = MaterialTheme.typography.titleMedium
    val bodySmallStyle = MaterialTheme.typography.bodySmall
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(min = 80.dp) // Ensures consistent spacing
    ) {
        // Icon with colored background for visual punch
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .padding(vertical = 8.dp, horizontal = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = accentColor // Use brand color for icon tint
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = value,
            style = titleMediumStyle,
            fontWeight = FontWeight.ExtraBold,
            color = onSurfaceColor
        )
        Text(
            text = label,
            style = bodySmallStyle,
            color = onSurfaceVariantColor,
            textAlign = TextAlign.Center
        )
    }
}