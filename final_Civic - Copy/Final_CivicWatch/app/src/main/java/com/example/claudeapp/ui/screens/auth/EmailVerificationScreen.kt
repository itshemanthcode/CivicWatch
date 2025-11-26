package com.example.claudeapp.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.claudeapp.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationScreen(
    onVerificationSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    // Define CivicWatch brand colors as local constants
    val BrandPrimary = Color(0xFF1E3A8A) // Dark Blue
    val BrandSecondary = Color(0xFF4CAF50) // Green
    
    val uiState by viewModel.uiState.collectAsState()
    var verificationRequested by remember { mutableStateOf(false) }
    
    // Check verification status when screen loads
    LaunchedEffect(Unit) {
        viewModel.sendEmailVerification()
    }
    
    // Navigate to main screen when verification is successful
    LaunchedEffect(uiState.isSignedIn) {
        if (uiState.isSignedIn) {
            onVerificationSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Professional email icon with brand colors
        Icon(
            imageVector = Icons.Default.Mail,
            contentDescription = "Email Verification",
            modifier = Modifier.size(80.dp),
            tint = BrandPrimary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Verify Your Email",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = BrandPrimary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (uiState.verificationEmailSent && uiState.error == null) {
                "We've sent a verification link to your email address. Please check your inbox and click the link to verify your account."
            } else {
                "Please ensure you entered a valid email address. We'll send a verification link once confirmed."
            },
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Check verification button - Primary action with brand colors
        Button(
            onClick = {
                verificationRequested = true
                viewModel.checkEmailVerification()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !uiState.isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandPrimary,
                contentColor = Color.White
            )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("I've Verified My Email", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Resend verification email button - Secondary action with brand colors
        OutlinedButton(
            onClick = {
                viewModel.resendVerificationEmail()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !uiState.isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = BrandPrimary
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = BrandPrimary
            )
        ) {
            Text("Resend Verification Email", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Back to login button - Styled with brand colors
        TextButton(
            onClick = onBackToLogin,
            colors = ButtonDefaults.textButtonColors(
                contentColor = BrandPrimary
            )
        ) {
            Text("Back to Login", fontWeight = FontWeight.Medium)
        }
        
        // Success message - Enhanced with brand colors and high contrast
        if (uiState.verificationEmailSent && uiState.error == null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = BrandSecondary
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Text(
                    text = "Verification email sent! Please check your inbox.",
                    modifier = Modifier.padding(16.dp),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Error message - simple text without buttons
        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
