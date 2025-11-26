package com.example.claudeapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.claudeapp.data.preferences.ThemeManager
import com.example.claudeapp.data.preferences.rememberThemeState
import com.example.claudeapp.ui.navigation.CivicWatchNavigation
import com.example.claudeapp.ui.theme.ClaudeAppTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var googleSignInClient: GoogleSignInClient
    
    @Inject
    lateinit var themeManager: ThemeManager
    private var googleSignInResult: ((com.google.android.gms.auth.api.signin.GoogleSignInAccount?) -> Unit)? = null
    
    // Permission request state
    private var permissionsGranted = mutableStateOf(false)
    private var permissionRequestCallback: ((Boolean) -> Unit)? = null
    
    // Required permissions
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        println("MainActivity: Google Sign-In result received")
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            println("MainActivity: Google Sign-In successful, account: ${account?.email}")
            googleSignInResult?.invoke(account)
        } catch (e: ApiException) {
            println("MainActivity: Google Sign-In failed with error: ${e.message}")
            googleSignInResult?.invoke(null)
        }
    }
    
    private val permissionRequestLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        println("MainActivity: Permission request result - all granted: $allGranted")
        permissionsGranted.value = allGranted
        permissionRequestCallback?.invoke(allGranted)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        println("MainActivity: onCreate called")
        
        // Check permissions on startup
        checkPermissions()
        
        setContent {
            println("MainActivity: Setting content with ClaudeAppTheme")
            val themeState = rememberThemeState(themeManager)
            
            ClaudeAppTheme(
                darkTheme = themeState.isDarkTheme
            ) {
                CivicWatchNavigation(
                    onGoogleSignInClick = { launchGoogleSignIn() },
                    onGoogleSignInResult = { result ->
                        googleSignInResult = result
                    },
                    onRequestPermissions = { callback ->
                        permissionRequestCallback = callback
                        requestPermissions()
                    },
                    permissionsGranted = permissionsGranted.value
                )
            }
        }
    }
    
    private fun checkPermissions() {
        val allGranted = requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
        permissionsGranted.value = allGranted
        println("MainActivity: Permission check - all granted: $allGranted")
    }
    
    private fun requestPermissions() {
        println("MainActivity: Requesting permissions")
        permissionRequestLauncher.launch(requiredPermissions)
    }
    
    private fun launchGoogleSignIn() {
        println("MainActivity: Launching Google Sign-In")
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }
}


