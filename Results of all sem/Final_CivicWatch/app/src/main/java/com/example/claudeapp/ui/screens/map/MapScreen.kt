package com.example.claudeapp.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.claudeapp.data.model.Issue
import com.example.claudeapp.data.model.IssueCategory
import com.example.claudeapp.data.model.IssueStatus
import com.example.claudeapp.ui.viewmodel.IssueViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import androidx.compose.ui.viewinterop.AndroidView
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.animation.*
import androidx.compose.animation.core.*

// Use MaterialTheme colors for proper dark mode support

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    selectedIssueId: String? = null,
    viewModel: IssueViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var categoryExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<IssueCategory?>(null) }
    

    val displayValue = selectedCategory?.displayName ?: "All Issue Types"
    val categories = remember { IssueCategory.values().toList() }

    // Default location (San Francisco)
    val defaultCenter = Point.fromLngLat(-122.4194, 37.7749)

    LaunchedEffect(Unit) {
        viewModel.loadIssues()
    }

    // Handle selected issue
    val selectedIssue = remember(selectedIssueId, uiState.issues) {
        selectedIssueId?.let { issueId ->
            uiState.issues.find { it.issueId == issueId }
        }
    }

    val filteredIssues = remember(selectedCategory, uiState.issues) {
        if (selectedCategory == null) {
            uiState.issues
        } else {
            uiState.issues.filter { issue ->
                issue.category.equals(selectedCategory!!.name, ignoreCase = true)
            }
        }
    }
    

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- 1. Modern Top App Bar ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = "CivicWatch",
                                modifier = Modifier.padding(8.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "CivicWatch",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Issue Map View",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.loadIssues() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // --- 2. Filter Card ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(20.dp),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FilterList,
                            contentDescription = "Filter",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Filter Issues",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Modern Dropdown
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            readOnly = true,
                            value = displayValue,
                            onValueChange = {},
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Category,
                                    contentDescription = "Category",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = categoryExpanded
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.SelectAll,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text("All Issues", fontWeight = FontWeight.Medium)
                                    }
                                },
                                onClick = {
                                    selectedCategory = null
                                    categoryExpanded = false
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outline
                            )
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                modifier = Modifier.size(8.dp),
                                                shape = CircleShape,
                                                color = getCategoryColor(category.name)
                                            ) {}
                                            Spacer(Modifier.width(12.dp))
                                            Text(category.displayName)
                                        }
                                    },
                                    onClick = {
                                        selectedCategory = category
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // --- 3. Map Container ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(24.dp),
                            spotColor = Color.Black.copy(alpha = 0.1f)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (uiState.isLoading) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Loading Map...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (selectedCategory != null && filteredIssues.isEmpty()) {
                            NoIssuesCard(selectedCategory!!)
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(24.dp))
                            ) {
                                AndroidView(
                                    modifier = Modifier.fillMaxSize(),
                                    factory = { ctx ->
                                        val mapView = MapView(ctx)
                                        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) {
                                            val initialCenter = if (filteredIssues.isNotEmpty()) {
                                                Point.fromLngLat(
                                                    filteredIssues.first().location.longitude,
                                                    filteredIssues.first().location.latitude
                                                )
                                            } else defaultCenter
                                            mapView.getMapboxMap().setCamera(
                                                CameraOptions.Builder()
                                                    .center(initialCenter)
                                                    .zoom(12.0)
                                                    .build()
                                            )
                                        }
                                        mapView
                                    },
                                update = { mapView ->
                                    val annotationApi = mapView.annotations
                                    val pointManager = annotationApi.createPointAnnotationManager()
                                    pointManager.deleteAll()
                                    filteredIssues.forEach { issue ->
                                        val point = Point.fromLngLat(
                                            issue.location.longitude,
                                            issue.location.latitude
                                        )
                                        val opts = PointAnnotationOptions()
                                            .withPoint(point)
                                            .withTextField(issue.category)
                                            .withIconImage(
                                                createMarkerBitmap(issue.category, issue.status)
                                            )
                                        pointManager.create(opts)
                                    }
                                    
                                }
                                )
                                
                            }
                        }

                        // ✅ Floating Legend (Inside the Map) - Glassmorphism Effect
                        if (!uiState.isLoading && filteredIssues.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .widthIn(min = 140.dp, max = 160.dp)
                            ) {
                                // Glass effect background with blur
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.5.dp,
                                            color = Color.White.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .shadow(
                                            elevation = 8.dp,
                                            shape = RoundedCornerShape(16.dp),
                                            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            spotColor = Color.White.copy(alpha = 0.3f)
                                        ),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Legend",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        )
                                        
                                        // Glass divider
                                        HorizontalDivider(
                                            color = Color.White.copy(alpha = 0.3f),
                                            thickness = 1.dp
                                        )
                                        
                                        LegendItem("Potholes", Color(0xFFE91E63))
                                        LegendItem("Street Lights", Color(0xFFFF9800))
                                        LegendItem("Garbage", Color(0xFF4CAF50))
                                        LegendItem("Water Logging", Color(0xFF2196F3))
                                    }
                                }
                                
                                // Subtle shine effect at the top
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .align(Alignment.TopCenter)
                                        .clip(RoundedCornerShape(16.dp)),
                                    color = Color.White.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                ) {}
                            }
                        }
                        
                        
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            modifier = Modifier.size(10.dp),
            shape = CircleShape,
            color = color
        ) {}
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun NoIssuesCard(selectedCategory: IssueCategory) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Icon(
                imageVector = Icons.Outlined.SearchOff,
                contentDescription = "No Issues",
                modifier = Modifier.padding(32.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No ${selectedCategory.displayName} Issues",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Great news! No issues of this type\nhave been reported in this area.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

private fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "potholes" -> Color(0xFFE91E63)
        "broken_street_lights" -> Color(0xFFFF9800)
        "garbage" -> Color(0xFF4CAF50)
        "water_logging" -> Color(0xFF2196F3)
        else -> Color(0xFF9E9E9E)
    }
}

private fun createMarkerBitmap(category: String, status: String): Bitmap {
    val size = 64
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val categoryColor = when (category.lowercase()) {
        "potholes" -> 0xFFE91E63.toInt()
        "broken_street_lights" -> 0xFFFF9800.toInt()
        "garbage" -> 0xFF4CAF50.toInt()
        "water_logging" -> 0xFF2196F3.toInt()
        else -> 0xFF9E9E9E.toInt()
    }

    val finalColor = when (status.lowercase()) {
        "resolved" -> darkenColor(categoryColor)
        "verified" -> categoryColor
        "in_progress" -> lightenColor(categoryColor)
        else -> categoryColor
    }

    paint.color = finalColor
    val rect = RectF(4f, 4f, size - 4f, size - 4f)
    canvas.drawOval(rect, paint)
    return bmp
}

private fun darkenColor(color: Int): Int {
    val factor = 0.7f
    val a = (color shr 24 and 0xFF)
    val r = ((color shr 16 and 0xFF) * factor).toInt()
    val g = ((color shr 8 and 0xFF) * factor).toInt()
    val b = ((color and 0xFF) * factor).toInt()
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

private fun lightenColor(color: Int): Int {
    val factor = 0.3f
    val a = (color shr 24 and 0xFF)
    val r = ((color shr 16 and 0xFF) + (255 - (color shr 16 and 0xFF)) * factor).toInt()
    val g = ((color shr 8 and 0xFF) + (255 - (color shr 8 and 0xFF)) * factor).toInt()
    val b = ((color and 0xFF) + (255 - (color and 0xFF)) * factor).toInt()
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

