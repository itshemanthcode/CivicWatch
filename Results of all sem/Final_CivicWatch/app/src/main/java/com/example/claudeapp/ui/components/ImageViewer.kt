package com.example.claudeapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

@Composable
fun ImageViewer(
    imageUrl: String,
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            ImageViewerContent(
                imageUrl = imageUrl,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun ImageViewerContent(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(200),
        label = "scale"
    )

    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(200),
        label = "offsetX"
    )

    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = tween(200),
        label = "offsetY"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
    ) {
        // Close button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(
                    Color.Black.copy(alpha = 0.5f),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Image with zoom and pan gestures
        AsyncImage(
            model = imageUrl,
            contentDescription = "Zoomable Image",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = animatedScale,
                    scaleY = animatedScale,
                    translationX = animatedOffsetX,
                    translationY = animatedOffsetY
                )
                .pointerInput(Unit) {
                    detectTransformGestures(
                        onGesture = { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 5f)
                            
                            if (scale > 1f) {
                                val newOffsetX = offsetX + pan.x
                                val newOffsetY = offsetY + pan.y
                                
                                // Limit panning to prevent image from going too far off screen
                                val maxOffsetX = (size.width * (scale - 1f)) / 2f
                                val maxOffsetY = (size.height * (scale - 1f)) / 2f
                                
                                offsetX = newOffsetX.coerceIn(-maxOffsetX, maxOffsetX)
                                offsetY = newOffsetY.coerceIn(-maxOffsetY, maxOffsetY)
                            } else {
                                // Reset offset when zoomed out
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1f) {
                                // Reset zoom
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                // Zoom in
                                scale = 2f
                            }
                        }
                    )
                },
            contentScale = ContentScale.Fit
        )

        // Instructions overlay (only show when not zoomed)
        if (scale <= 1f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = "Pinch to zoom • Double tap to zoom in/out • Drag to pan",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun ImageViewerDialog(
    imageUrls: List<String>,
    initialIndex: Int = 0,
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    if (isVisible && imageUrls.isNotEmpty()) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            MultiImageViewerContent(
                imageUrls = imageUrls,
                initialIndex = initialIndex,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun MultiImageViewerContent(
    imageUrls: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(initialIndex) }
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(200),
        label = "scale"
    )

    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(200),
        label = "offsetX"
    )

    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = tween(200),
        label = "offsetY"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
    ) {
        // Close button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(
                    Color.Black.copy(alpha = 0.5f),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Image counter (if multiple images)
        if (imageUrls.size > 1) {
            Text(
                text = "${currentIndex + 1} / ${imageUrls.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Current image
        AsyncImage(
            model = imageUrls[currentIndex],
            contentDescription = "Zoomable Image",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = animatedScale,
                    scaleY = animatedScale,
                    translationX = animatedOffsetX,
                    translationY = animatedOffsetY
                )
                .pointerInput(Unit) {
                    detectTransformGestures(
                        onGesture = { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 5f)
                            
                            if (scale > 1f) {
                                val newOffsetX = offsetX + pan.x
                                val newOffsetY = offsetY + pan.y
                                
                                val maxOffsetX = (size.width * (scale - 1f)) / 2f
                                val maxOffsetY = (size.height * (scale - 1f)) / 2f
                                
                                offsetX = newOffsetX.coerceIn(-maxOffsetX, maxOffsetX)
                                offsetY = newOffsetY.coerceIn(-maxOffsetY, maxOffsetY)
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                scale = 2f
                            }
                        },
                        onTap = { offset ->
                            // Swipe to next/previous image
                            if (scale <= 1f) {
                                val centerX = size.width / 2f
                                if (offset.x < centerX && currentIndex > 0) {
                                    currentIndex--
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                } else if (offset.x > centerX && currentIndex < imageUrls.size - 1) {
                                    currentIndex++
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        }
                    )
                },
            contentScale = ContentScale.Fit
        )

        // Instructions overlay
        if (scale <= 1f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = if (imageUrls.size > 1) {
                        "Pinch to zoom • Double tap to zoom • Tap sides to navigate"
                    } else {
                        "Pinch to zoom • Double tap to zoom in/out • Drag to pan"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }
    }
}
