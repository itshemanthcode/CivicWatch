package com.example.claudeapp.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.claudeapp.R
import com.example.claudeapp.data.model.Issue
import com.example.claudeapp.ui.components.IssueCard
import com.example.claudeapp.ui.screens.comments.CommentsBottomSheet
import com.example.claudeapp.ui.viewmodel.IssueViewModel
import com.example.claudeapp.utils.shareIssue
import com.google.firebase.auth.FirebaseAuth

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
    
    // Filter State
    var showLocationDialog by remember { mutableStateOf(false) }
    var showCityInputDialog by remember { mutableStateOf(false) }
    var filterMode by remember { mutableStateOf("Global") } // "Global" or "Local"
    var localCity by remember { mutableStateOf("") }
    var tempCityInput by remember { mutableStateOf("") }

    // Filter issues based on mode and sort by upvotes
    val filteredIssues = remember(uiState.issues, filterMode, localCity) {
        val issues = if (filterMode == "Global") {
            uiState.issues
        } else {
            uiState.issues.filter {
                it.location.city.trim().equals(localCity.trim(), ignoreCase = true) ||
                it.location.address.contains(localCity, ignoreCase = true)
            }
        }
        // Sort by upvotes descending (highest upvotes first)
        issues.sortedByDescending { it.upvotes }
    }

    // Optimize for smooth scrolling
    val listState = rememberLazyListState()
    
    // Memoize derived values based on filteredIssues
    val issuesCount = remember(filteredIssues) { filteredIssues.size }
    val isLoading = remember(uiState.isLoading) { uiState.isLoading }
    val isEmpty = remember(filteredIssues) { filteredIssues.isEmpty() }
    
    // Calculate notified and resolved counts
    val notifiedCount = remember(filteredIssues) {
        filteredIssues.count { issue ->
            issue.status == com.example.claudeapp.data.model.IssueStatus.NOTIFIED.name.lowercase()
        }
    }
    val resolvedCount = remember(filteredIssues) {
        filteredIssues.count { issue ->
            issue.status == com.example.claudeapp.data.model.IssueStatus.RESOLVED.name.lowercase()
        }
    }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadIssues()
    }

    // Scroll to highlighted issue if present
    LaunchedEffect(highlightIssueId, filteredIssues) {
        if (highlightIssueId != null && filteredIssues.isNotEmpty()) {
            val index = filteredIssues.indexOfFirst { it.issueId == highlightIssueId }
            if (index != -1) {
                listState.animateScrollToItem(index + 2)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToReport,
                modifier = Modifier.padding(16.dp),
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_report_issue),
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
                                contentDescription = stringResource(R.string.cd_civicwatch_logo),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.app_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // Action Buttons - Globe Icon
                        IconButton(
                            onClick = { showLocationDialog = true },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Location Filter",
                            )
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
                            label = stringResource(R.string.stat_issues),
                            color = MaterialTheme.colorScheme.onSurface,
                            accentColor = MaterialTheme.colorScheme.primary,
                            backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        StatItem(
                            icon = Icons.Default.Schedule,
                            value = notifiedCount.toString(),
                            label = stringResource(R.string.stat_notified),
                            color = MaterialTheme.colorScheme.onSurface,
                            accentColor = MaterialTheme.colorScheme.secondary,
                            backgroundColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                        )
                        StatItem(
                            icon = Icons.Default.CheckCircle,
                            value = resolvedCount.toString(),
                            label = stringResource(R.string.stat_resolved),
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
                            text = if (filterMode == "Local") "No issues found in $localCity" else stringResource(R.string.empty_issues_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (filterMode == "Local") "Try searching for a different area or switch to Global view." else stringResource(R.string.empty_issues_message),
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
                            Text(stringResource(R.string.btn_report_first))
                        }
                    }
                }
            }
            
            // --- ISSUES LIST ---
            if (!isLoading && !isEmpty) {
                items(
                    items = filteredIssues,
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
                            shareIssue(context, issue)
                            
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            val userId = currentUser?.uid ?: "anonymous"
                            viewModel.incrementShareCount(issueId, userId)
                        },
                        onVerify = { /* TODO: Navigate to verification screen */ },
                        onLocationClick = { issue ->
                            navController.navigate("map/${issue.issueId}")
                        }
                    )
                }
            }
        }
    }
    
    // --- IMPROVED LOCATION FILTER DIALOG ---
    if (showLocationDialog) {
        Dialog(onDismissRequest = { showLocationDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select View Mode",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Global Option
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                filterMode = "Global"
                                showLocationDialog = false
                            },
                        color = if (filterMode == "Global") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Global View",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "See issues from everywhere",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            if (filterMode == "Global") {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Local Option
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showLocationDialog = false
                                tempCityInput = localCity
                                showCityInputDialog = true
                            },
                        color = if (filterMode == "Local") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Local View",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Focus on a specific city",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            if (filterMode == "Local") {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- IMPROVED CITY INPUT DIALOG ---
    if (showCityInputDialog) {
        Dialog(onDismissRequest = { showCityInputDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Enter Location",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Which city's issues do you want to see?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                    )
                    
                    OutlinedTextField(
                        value = tempCityInput,
                        onValueChange = { tempCityInput = it },
                        label = { Text("City Name") },
                        placeholder = { Text("e.g. New York, London") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (tempCityInput.isNotBlank()) {
                                    localCity = tempCityInput
                                    filterMode = "Local"
                                    showCityInputDialog = false
                                }
                            }
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showCityInputDialog = false },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                if (tempCityInput.isNotBlank()) {
                                    localCity = tempCityInput
                                    filterMode = "Local"
                                    showCityInputDialog = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            enabled = tempCityInput.isNotBlank()
                        ) {
                            Text("Apply Filter")
                        }
                    }
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

// --- STAT ITEM COMPOSABLE ---
@Composable
fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    accentColor: Color,
    backgroundColor: Color
) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val titleMediumStyle = MaterialTheme.typography.titleMedium
    val bodySmallStyle = MaterialTheme.typography.bodySmall
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(min = 80.dp)
    ) {
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
                tint = accentColor
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