package com.example.claudeapp.ui.screens.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.claudeapp.data.model.Issue

@Composable
fun ShareDialog(
    issue: Issue,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit
) {
    val context = LocalContext.current
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Share Issue",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Issue preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = issue.category.replace("_", " ").uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = issue.description.ifEmpty { "No description provided" },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "📍 ${issue.location.address}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Share this issue on:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Share options
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // WhatsApp
                    ShareOptionButton(
                        title = "WhatsApp",
                        subtitle = "Share with contacts",
                        onClick = {
                            shareToWhatsApp(context, issue)
                            onShare(issue.issueId)
                            onDismiss()
                        }
                    )
                    
                    // Instagram
                    ShareOptionButton(
                        title = "Instagram",
                        subtitle = "Share to stories or feed",
                        onClick = {
                            shareToInstagram(context, issue)
                            onShare(issue.issueId)
                            onDismiss()
                        }
                    )
                    
                    // Generic share
                    ShareOptionButton(
                        title = "Other Apps",
                        subtitle = "Share via other apps",
                        onClick = {
                            shareToOtherApps(context, issue)
                            onShare(issue.issueId)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ShareOptionButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun shareToWhatsApp(context: Context, issue: Issue) {
    val shareText = buildShareText(issue)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        setPackage("com.whatsapp")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // WhatsApp not installed, fallback to generic share
        shareToOtherApps(context, issue)
    }
}

private fun shareToInstagram(context: Context, issue: Issue) {
    val shareText = buildShareText(issue)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        setPackage("com.instagram.android")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // Instagram not installed, fallback to generic share
        shareToOtherApps(context, issue)
    }
}

private fun shareToOtherApps(context: Context, issue: Issue) {
    val shareText = buildShareText(issue)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, "CivicWatch Issue Report")
    }
    
    context.startActivity(Intent.createChooser(intent, "Share Issue"))
}

private fun buildShareText(issue: Issue): String {
    return """
        🚨 CivicWatch Issue Report
        
        📋 Type: ${issue.category.replace("_", " ").uppercase()}
        📍 Location: ${issue.location.address}
        📝 Description: ${issue.description.ifEmpty { "No description provided" }}
        
        👍 Upvotes: ${issue.upvotes}
        💬 Comments: ${issue.commentsCount}
        📤 Shares: ${issue.shares}
        
        Help us make our community better by reporting issues through CivicWatch!
        
        #CivicWatch #CommunityIssues #${issue.category.replace("_", "")}
    """.trimIndent()
}
