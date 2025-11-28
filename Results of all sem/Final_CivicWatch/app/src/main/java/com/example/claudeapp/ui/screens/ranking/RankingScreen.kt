package com.example.claudeapp.ui.screens.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.claudeapp.ui.viewmodel.RankingViewModel
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.draw.shadow

// Use MaterialTheme colors for proper dark mode support


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    viewModel: RankingViewModel = hiltViewModel(),
    navController: NavController? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Back navigation handler
    val handleBackNavigation = {
        navController?.popBackStack()
    }

    LaunchedEffect(Unit) {
        viewModel.loadRankings()
    }

    // Use the custom dark background for a clean look matching the image
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- 1. Top App Bar (Matches "Leaderboard" style) ---
        TopAppBar(
            modifier = Modifier.statusBarsPadding(),
            title = {
                Text(
                    text = "Leaderboard",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            navigationIcon = {
                IconButton(onClick = { handleBackNavigation() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background // Matches image header background
            )
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val errorMessage = uiState.error
            if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Error loading rankings",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (uiState.topPerformers.isEmpty() && uiState.allRankings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No rankings yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Be the first to report an issue and earn points!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // --- 2. Top Performers Section (Vertical, Modern Design) ---
                item {
                    Text(
                        text = "Top Performers",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                    )
                }

                itemsIndexed(uiState.topPerformers) { index, performer ->
                    TopPerformerCardVertical(
                        performer = performer, 
                        rank = index + 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // --- 3. All Rankings Section (Clean, Detailed List) ---
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "All Rankings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                itemsIndexed(uiState.allRankings) { index, ranking ->
                    EnhancedRankingItemCard(ranking = ranking, rank = index + 1)
                }

            // --- 4. How to Earn Points Section ---
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "How to Earn Points",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Contribute to the community to climb the ranks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Report Issue Card
                    PointEarningCard(
                        title = "Report",
                        points = "+10",
                        icon = Icons.Default.Camera,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Verify Issue Card
                    PointEarningCard(
                        title = "Verify",
                        points = "+5",
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF4CAF50), // Green
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Upvote Card
                    PointEarningCard(
                        title = "Upvote",
                        points = "+1",
                        icon = Icons.Default.ThumbUp,
                        color = Color(0xFFFF9800), // Orange
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
            }
            }
        }
    }
}

// ====================================================================
// --- NEW/MODIFIED COMPOSABLES (Rest of the structure remains the same) ---
// ====================================================================

@Composable
fun PointActionRow(icon: ImageVector, title: String, points: String, color: Color) {
    // This is now redundant but kept to not break potential external calls.
    // The explicit PointActionRow implementation has been moved inline above for the exact image match.
    // ... (content omitted for brevity)
}

@Composable
fun TopPerformerCardVertical(
    performer: TopPerformer,
    rank: Int,
    modifier: Modifier = Modifier
) {
    val medalColor = when (rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> MaterialTheme.colorScheme.primary
    }
    
    val rankLabel = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "#$rank"
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (rank <= 3) 8.dp else 4.dp
        ),
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
            // Rank Badge
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(medalColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (rank <= 3) {
                    Text(
                        text = rankLabel,
                        fontSize = 28.sp,
                        modifier = Modifier.padding(4.dp)
                    )
                } else {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = medalColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))
            
            // User Icon for ranks 4+
            if (rank > 3) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(medalColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = performer.icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = medalColor
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Name and Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = performer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${performer.points} points",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Points Display (Right-aligned)
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${performer.points}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = medalColor
                )
                Text(
                    text = "pts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EnhancedTopPerformerCard(performer: TopPerformer, rank: Int) {
    val iconTint = when (rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .width(110.dp)
            .height(130.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Performer Icon/Medal
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.8f))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = performer.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }

            // Name
            Text(
                text = performer.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Points
            Text(
                text = "${performer.points}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EnhancedRankingItemCard(ranking: RankingItem, rank: Int) {
    val cardColor = MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Icon/Number
            Icon(
                imageVector = ranking.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = ranking.iconColor
            )

            Spacer(modifier = Modifier.width(16.dp))

            // User Details (Name and Achievements)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = ranking.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Reports Count
                    IconText(
                        icon = Icons.Default.Info, // Used as a flag/report icon in the image
                        text = "${ranking.reports} reports",
                        color = MaterialTheme.colorScheme.secondary
                    )

                    // Verifications Count
                    IconText(
                        icon = Icons.Default.CheckCircle,
                        text = "${ranking.verifications} verifications",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Points (Large and prominent, right-aligned)
            Text(
                text = "${ranking.points}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun EnhancedPointActionCard(action: PointAction) {
    // This is now redundant
}

@Composable
fun IconText(icon: ImageVector, text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = color
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

// Data classes (Unchanged functionality)
data class TopPerformer(
    val name: String,
    val points: Int,
    val icon: ImageVector,
    val iconColor: Color
)

data class RankingItem(
    val name: String,
    val points: Int,
    val reports: Int,
    val verifications: Int,
    val icon: ImageVector,
    val iconColor: Color
)

data class PointAction(
    val title: String,
    val points: String,
    val icon: ImageVector,
    val iconColor: Color
)