package com.example.claudeapp.ui.screens.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.claudeapp.R
import com.example.claudeapp.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    onNavigateToMain: () -> Unit,
    onRequestPermissions: ((Boolean) -> Unit) -> Unit = {},
    permissionsGranted: Boolean = false
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    
    var startAnimation by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionCallback: ((Boolean) -> Unit)? by remember { mutableStateOf(null) }

    // Animation for fade-in effect
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1500) // Fade in duration
    )

    // Check authentication state and navigate accordingly
    LaunchedEffect(key1 = true) {
        println("SplashScreen: Starting splash screen")
        startAnimation = true
        
        // Check if user is already signed in
        println("SplashScreen: Checking auth state...")
        authViewModel.checkAuthState()
        
        // Get initial auth state
        val initialAuthState = authViewModel.uiState.value
        println("SplashScreen: Initial auth state - isSignedIn: ${initialAuthState.isSignedIn}")
        
        delay(2000) // Show splash for 2 seconds (reduced from 3)
        
        // Check permissions first
        if (!permissionsGranted) {
            println("SplashScreen: Permissions not granted, showing permission dialog")
            showPermissionDialog = true
            return@LaunchedEffect
        }
        
        // Check auth state after delay
        val currentAuthState = authViewModel.uiState.value
        println("SplashScreen: Final auth state - isSignedIn: ${currentAuthState.isSignedIn}")
        
        if (currentAuthState.isSignedIn) {
            println("SplashScreen: User already signed in, navigating to main")
            onNavigateToMain()
        } else {
            println("SplashScreen: User not signed in, navigating to login")
            onSplashFinished()
        }
    }
    
    // Also watch for auth state changes reactively
    LaunchedEffect(authState.isSignedIn) {
        if (authState.isSignedIn) {
            println("SplashScreen: Auth state changed - user signed in, navigating to main")
            onNavigateToMain()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    // Using colors matching the logo's dark blue and blending them
                    colors = listOf(
                        Color(0xFF152A5A), // Dark Blue
                        Color(0xFF336699)  // Lighter Blue
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            // Apply fade-in animation to the entire content
            modifier = Modifier.alpha(alphaAnim.value)
        ) {
            // --- 1. Display the Full Logo Image ---
            // This replaces the entire manual shield drawing logic.
            Image(
                painter = painterResource(id = R.drawable.civicwatch_full_logo),
                contentDescription = "Civicwatch Brand Logo and Tagline",
                modifier = Modifier.size(300.dp) // Dominant size for splash screen
            )

            Spacer(modifier = Modifier.height(40.dp))

            // --- 2. Loading Indicator ---
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = Color.White,
                strokeWidth = 3.dp
            )
        }
    }
    
    // Permission Request Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { /* Don't allow dismissing */ },
            title = {
                Text(
                    text = "Permissions Required",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    text = "CivicWatch needs access to your camera and location to help you report issues and find nearby problems. These permissions are essential for the app to function properly.",
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        onRequestPermissions { granted ->
                            permissionCallback?.invoke(granted)
                            if (granted) {
                                // Continue with normal flow
                                val currentAuthState = authViewModel.uiState.value
                                if (currentAuthState.isSignedIn) {
                                    onNavigateToMain()
                                } else {
                                    onSplashFinished()
                                }
                            } else {
                                // Show error or retry
                                showPermissionDialog = true
                            }
                        }
                    }
                ) {
                    Text("Allow Permissions")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // User denied permissions, show dialog again
                        showPermissionDialog = true
                    }
                ) {
                    Text("Deny")
                }
            }
        )
    }
}