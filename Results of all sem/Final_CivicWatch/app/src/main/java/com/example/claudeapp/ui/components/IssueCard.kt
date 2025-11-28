package com.example.claudeapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import coil.compose.AsyncImage
import com.example.claudeapp.R
import com.example.claudeapp.data.model.Issue
import com.example.claudeapp.data.model.IssueCategory
import com.example.claudeapp.data.model.IssueSeverity
import com.example.claudeapp.data.model.IssueStatus
import com.example.claudeapp.ui.theme.CivicBlue
import com.example.claudeapp.ui.theme.CivicRed
import com.example.claudeapp.ui.components.ImageViewerDialog
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@Composable
private fun AnimatedThumbsUp(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "thumbs_up_scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "thumbs_up_alpha"
    )

    if (isVisible) {
        Box(
            modifier = modifier
                .scale(scale)
                .graphicsLayer { this.alpha = alpha },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ThumbUp,
                contentDescription = "Thumbs Up",
                tint = CivicBlue,
                modifier = Modifier.size(60.dp)
            )
        }
    }
}

@Composable
fun IssueCard(
    issue: Issue,
    onUpvote: (String) -> Unit,
    onDownvote: (String) -> Unit,
    onComment: (String) -> Unit,
    onShare: (String) -> Unit,
    onVerify: (String) -> Unit,
    onLocationClick: (Issue) -> Unit = {},
    onDelete: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid ?: "anonymous"
    
    // Ensure we have a valid user ID for voting
    val canVote = userId != "anonymous" && userId.isNotEmpty()
    
    // Determine user's voting state
    val hasUpvoted = issue.upvotedBy.contains(userId)
    val hasDownvoted = issue.downvotedBy.contains(userId)
    
    // Debug logging for voting state
    println("IssueCard Debug - issueId: ${issue.issueId}, userId: $userId, hasUpvoted: $hasUpvoted, upvotedBy: ${issue.upvotedBy}")
    println("IssueCard Debug - currentUser: ${FirebaseAuth.getInstance().currentUser?.uid}, isAnonymous: ${currentUser?.isAnonymous}")
    
    // Add LaunchedEffect to monitor state changes
    LaunchedEffect(issue.upvotedBy, issue.downvotedBy) {
        println("IssueCard State Change - issueId: ${issue.issueId}, upvotedBy: ${issue.upvotedBy}, downvotedBy: ${issue.downvotedBy}")
    }
    
    // State for double-tap animation
    var showThumbsUp by remember { mutableStateOf(false) }
    
    // State for image viewer
    var showImageViewer by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableStateOf(0) }
    
    // State for options menu
    var showMenu by remember { mutableStateOf(false) }
    
    // Handle double-tap animation
    LaunchedEffect(showThumbsUp) {
        if (showThumbsUp) {
            delay(1000) // Show for 1 second
            showThumbsUp = false
        }
    }
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Main Issue Card with Instagram-style layout
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* TODO: Navigate to issue details */ },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column {
                // Top section with location and category/status chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Location on the left - clickable
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onLocationClick(issue) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "View on Map",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatLocationDisplay(issue.location),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Category and status chips on the right
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category chip
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = getCategoryColor(issue.category).copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = getCategoryDisplayName(issue.category),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = getCategoryColor(issue.category)
                            )
                        }
                        
                        // Status chip
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = getStatusColor(issue.status).copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = getStatusDisplayName(issue.status),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = getStatusColor(issue.status)
                            )
                        }
                        
                        // More Options Menu (only if onDelete is provided)
                        if (onDelete != null) {
                            Box {
                                IconButton(
                                    onClick = { showMenu = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.MoreVert,
                                        contentDescription = androidx.compose.ui.res.stringResource(R.string.cd_more_options),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(androidx.compose.ui.res.stringResource(R.string.menu_delete_issue), color = CivicRed) },
                                        onClick = {
                                            showMenu = false
                                            onDelete(issue.issueId)
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                                                contentDescription = androidx.compose.ui.res.stringResource(R.string.btn_delete),
                                                tint = CivicRed
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Image section - Instagram-style swipeable gallery with pagination
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (issue.images.isNotEmpty()) {
                        // Instagram-style image pager
                        val pageCount = issue.images.size
                        val pagerState = rememberPagerState()
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(350.dp)
                                .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp))
                        ) {
                            // Swipeable image container
                            HorizontalPager(
                                count = pageCount,
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                itemSpacing = 0.dp
                            ) { page ->
                                val imageUri = issue.images[page]
                                
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = androidx.compose.ui.res.stringResource(R.string.cd_issue_image),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = {
                                                    selectedImageIndex = page
                                                    showImageViewer = true
                                                },
                                                onDoubleTap = {
                                                    if (!canVote) {
                                                        showThumbsUp = true
                                                    } else if (hasUpvoted) {
                                                        // If already upvoted, just show animation without calling onUpvote
                                                        // This prevents the toggle behavior that would remove the upvote
                                                        showThumbsUp = true
                                                    } else {
                                                        // If not upvoted, trigger upvote and show animation
                                                        onUpvote(issue.issueId)
                                                        showThumbsUp = true
                                                    }
                                                }
                                            )
                                        },
                                    contentScale = ContentScale.Crop
                                )
                            }
                            
                            // Image count indicator (like Instagram) - Top Right
                            if (pageCount > 1) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.Black.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = "${pagerState.currentPage + 1}/${pageCount}",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        // Animated thumbs-up overlay
                        AnimatedThumbsUp(
                            isVisible = showThumbsUp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(350.dp)
                                .align(Alignment.Center)
                        )
                    } else {
                        // Placeholder when no image
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            if (!canVote) {
                                                showThumbsUp = true
                                            } else if (hasUpvoted) {
                                                // If already upvoted, just show animation without calling onUpvote
                                                // This prevents the toggle behavior that would remove the upvote
                                                showThumbsUp = true
                                            } else {
                                                // If not upvoted, trigger upvote and show animation
                                                onUpvote(issue.issueId)
                                                showThumbsUp = true
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(R.string.no_image_available),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Animated thumbs-up overlay for placeholder
                        AnimatedThumbsUp(
                            isVisible = showThumbsUp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
                
                // Interaction buttons section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side - Action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Enhanced Upvote button with state indication
                        VoteButton(
                            icon = Icons.Default.ThumbUp,
                            count = issue.upvotes,
                            isActive = hasUpvoted,
                            isDisabled = false, // Always enabled for automatic switching
                            activeColor = CivicBlue,
                            inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            onClick = { 
                                onUpvote(issue.issueId) 
                            },
                            contentDescription = androidx.compose.ui.res.stringResource(R.string.cd_upvote)
                        )
                        
                        // Enhanced Downvote button with state indication
                        VoteButton(
                            icon = Icons.Default.ThumbDown,
                            count = issue.downvotes,
                            isActive = hasDownvoted,
                            isDisabled = false, // Always enabled for automatic switching
                            activeColor = CivicRed,
                            inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            onClick = { 
                                onDownvote(issue.issueId) 
                            },
                            contentDescription = androidx.compose.ui.res.stringResource(R.string.cd_downvote)
                        )
                        
                        // Comments button
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onComment(issue.issueId) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Comment,
                                    contentDescription = androidx.compose.ui.res.stringResource(R.string.cd_comments),
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = issue.commentsCount.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Right side - Share button with count
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onShare(issue.issueId) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = androidx.compose.ui.res.stringResource(R.string.cd_share),
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = issue.shares.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Description section - below the interaction buttons
                if (issue.description.isNotEmpty()) {
                    Text(
                        text = issue.description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Timestamp
                Text(
                    text = formatTimestamp(issue.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        
        // Image viewer for zooming issue images
        ImageViewerDialog(
            imageUrls = issue.images,
            initialIndex = selectedImageIndex,
            isVisible = showImageViewer,
            onDismiss = { showImageViewer = false }
        )
    }
}

private fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "potholes" -> Color(0xFF4CAF50)
        "streetlights" -> Color(0xFFFF9800)
        "garbage" -> Color(0xFF795548)
        "waterlogging" -> Color(0xFF2196F3)
        "damaged_roads" -> Color(0xFF9C27B0)
        "broken_sidewalks" -> Color(0xFF607D8B)
        "vandalism" -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }
}

private fun getCategoryDisplayName(category: String): String {
    return when (category.lowercase()) {
        "potholes" -> "Potholes"
        "streetlights" -> "Streetlights"
        "garbage" -> "Garbage"
        "waterlogging" -> "Waterlogging"
        "damaged_roads" -> "Damaged Roads"
        "broken_sidewalks" -> "Sidewalks"
        "vandalism" -> "Vandalism"
        else -> "Other"
    }
}

private fun getStatusColor(status: String): Color {
    return when (status.lowercase()) {
        "reported" -> Color(0xFF2196F3)
        "verified" -> Color(0xFF4CAF50)
        "notified" -> Color(0xFFFF9800)
        "in_progress" -> Color(0xFF9C27B0)
        "resolved" -> Color(0xFF4CAF50)
        "rejected" -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }
}

private fun getStatusDisplayName(status: String): String {
    return when (status.lowercase()) {
        "reported" -> "Reported"
        "verified" -> "Verified"
        "notified" -> "Notified"
        "in_progress" -> "In Progress"
        "resolved" -> "Resolved"
        "rejected" -> "Rejected"
        else -> "Unknown"
    }
}

private fun formatLocationDisplay(location: com.example.claudeapp.data.model.IssueLocation): String {
    return when {
        location.address.isNotEmpty() -> location.address
        location.city.isNotEmpty() && location.state.isNotEmpty() -> "${location.city}, ${location.state}"
        location.city.isNotEmpty() -> location.city
        location.state.isNotEmpty() -> location.state
        location.country.isNotEmpty() -> location.country
        else -> "Unknown location"
    }
}

private fun hasLocationDetails(location: com.example.claudeapp.data.model.IssueLocation): Boolean {
    return location.area.isNotEmpty() || location.city.isNotEmpty() || location.state.isNotEmpty() || location.country.isNotEmpty()
}

private fun formatLocationDetails(location: com.example.claudeapp.data.model.IssueLocation): String {
    return buildString {
        // Show area first if available
        if (location.area.isNotEmpty()) {
            append(location.area)
        }
        
        // Then add city if available
        if (location.city.isNotEmpty()) {
            if (isNotEmpty()) append(", ")
            append(location.city)
        }
        
        // Then add state if available
        if (location.state.isNotEmpty()) {
            if (isNotEmpty()) append(", ")
            append(location.state)
        }
        
        // Finally add country if available
        if (location.country.isNotEmpty()) {
            if (isNotEmpty()) append(", ")
            append(location.country)
        }
    }
}

@Composable
private fun formatTimestamp(timestamp: com.google.firebase.Timestamp): String {
    val date = timestamp.toDate()
    val now = Date()
    val diff = now.time - date.time
    
    return when {
        diff < 60 * 1000 -> androidx.compose.ui.res.stringResource(R.string.time_just_now)
        diff < 60 * 60 * 1000 -> androidx.compose.ui.res.stringResource(R.string.time_minutes_ago, diff / (60 * 1000))
        diff < 24 * 60 * 60 * 1000 -> androidx.compose.ui.res.stringResource(R.string.time_hours_ago, diff / (60 * 60 * 1000))
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
    }
}

@Composable
private fun VoteButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    isActive: Boolean,
    isDisabled: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    disabledColor: Color,
    onClick: () -> Unit,
    contentDescription: String
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = tween(200),
        label = "vote_button_scale"
    )
    
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(32.dp)
                .scale(scale),
            enabled = !isDisabled
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
                tint = when {
                    isActive -> activeColor
                    isDisabled -> disabledColor
                    else -> inactiveColor
                }
            )
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = when {
                isActive -> activeColor
                isDisabled -> disabledColor
                else -> inactiveColor
            }
        )
    }
}

