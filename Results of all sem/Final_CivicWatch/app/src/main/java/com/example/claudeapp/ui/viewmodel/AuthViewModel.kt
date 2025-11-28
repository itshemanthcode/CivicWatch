package com.example.claudeapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.claudeapp.data.model.User
import com.example.claudeapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    init {
        // Initialize with proper state - not loading by default
        _uiState.value = _uiState.value.copy(
            isSignedIn = false,
            isLoading = false
        )
    }
    
    fun checkAuthState() {
        val isSignedIn = authRepository.isUserSignedIn
        println("AuthViewModel: checkAuthState - isUserSignedIn: $isSignedIn")
        
        _uiState.value = _uiState.value.copy(
            isSignedIn = isSignedIn,
            isLoading = false
        )
        
        if (isSignedIn) {
            println("AuthViewModel: User is signed in, loading current user data")
            loadCurrentUser()
        } else {
            println("AuthViewModel: User is not signed in")
        }
    }
    
    fun resetLoadingState() {
        _uiState.value = _uiState.value.copy(isLoading = false)
    }
    
    private fun loadCurrentUser() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val user = authRepository.getCurrentUserData()
                _uiState.value = _uiState.value.copy(
                    currentUser = user,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    fun signInWithGoogle() {
        // This method is no longer used - Google Sign-In is handled by MainActivity
        // The actual Google Sign-In flow will call signInWithGoogle(account) directly
    }
    
    fun signInWithGoogle(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        println("AuthViewModel: signInWithGoogle called with account: ${account.email}")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val user = authRepository.signInWithGoogle(account)
                println("AuthViewModel: Google Sign-In successful, user: ${user.displayName}")
                _uiState.value = _uiState.value.copy(
                    isSignedIn = true,
                    currentUser = user,
                    isLoading = false,
                    needsEmailVerification = false // Google accounts don't need email verification
                )
            } catch (e: Exception) {
                println("AuthViewModel: Google Sign-In failed with error: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            authRepository.signOut()
            _uiState.value = _uiState.value.copy(
                isSignedIn = false,
                currentUser = null,
                isLoading = false
            )
        }
    }
    
    fun updateEmail(value: String) {
        _uiState.value = _uiState.value.copy(email = value)
    }
    
    fun updatePassword(value: String) {
        _uiState.value = _uiState.value.copy(password = value)
    }
    
    fun updateDisplayName(value: String) {
        _uiState.value = _uiState.value.copy(displayName = value)
    }
    
    fun toggleMode() {
        _uiState.value = _uiState.value.copy(
            isSignUpMode = !_uiState.value.isSignUpMode,
            error = null
        )
    }
    
    fun signInWithEmail() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        println("AuthViewModel: signInWithEmail called with email: $email, password length: ${password.length}")
        
        if (email.isEmpty() || password.isEmpty()) {
            println("AuthViewModel: Email or password empty")
            _uiState.value = _uiState.value.copy(error = "Email and password required")
            return
        }
        
        viewModelScope.launch {
            println("AuthViewModel: Starting sign in process")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val user = authRepository.signInWithEmailAndVerify(email, password)
                println("AuthViewModel: Sign in successful, user: ${user.displayName}")
                _uiState.value = _uiState.value.copy(
                    isSignedIn = true,
                    currentUser = user,
                    isLoading = false
                )
            } catch (e: Exception) {
                println("AuthViewModel: Sign in failed with error: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    fun signUpWithEmail() {
        val name = _uiState.value.displayName.trim()
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        println("AuthViewModel: signUpWithEmail called with name: $name, email: $email, password length: ${password.length}")
        
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            println("AuthViewModel: Name, email or password empty")
            _uiState.value = _uiState.value.copy(error = "Name, email and password required")
            return
        }
        
        viewModelScope.launch {
            println("AuthViewModel: Starting sign up process")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Check if email already exists
                val emailExists = authRepository.checkEmailExists(email)
                println("AuthViewModel: Email exists check result: $emailExists")
                
                if (emailExists) {
                    // Email already exists, try to sign in instead
                    println("AuthViewModel: Email already exists, attempting sign in")
                    val user = authRepository.signInWithEmailAndVerify(email, password)
                    _uiState.value = _uiState.value.copy(
                        isSignedIn = true,
                        currentUser = user,
                        isLoading = false,
                        needsEmailVerification = false
                    )
                } else {
                    // Email is new, proceed with signup and verification
                    println("AuthViewModel: Email is new, proceeding with signup")
                    val user = authRepository.signUpWithEmail(name, email, password)
                    println("AuthViewModel: Sign up successful, user: ${user.displayName}")
                    _uiState.value = _uiState.value.copy(
                        isSignedIn = false, // Don't sign in yet, need verification
                        currentUser = user,
                        isLoading = false,
                        needsEmailVerification = true
                    )
                }
            } catch (e: Exception) {
                println("AuthViewModel: Sign up failed with error: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    fun sendEmailVerification() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val success = authRepository.sendEmailVerification()
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        verificationEmailSent = true,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to send verification email. Please try again.",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                println("AuthViewModel: sendEmailVerification failed with error: ${e.message}")
                val errorMessage = when {
                    e.message?.contains("invalid-email", ignoreCase = true) == true -> 
                        "Please enter a valid email address"
                    e.message?.contains("user-not-found", ignoreCase = true) == true -> 
                        "Please enter a valid email address"
                    e.message?.contains("network", ignoreCase = true) == true -> 
                        "Network error. Please check your internet connection and try again"
                    else -> "Failed to send verification email. Please enter a valid email address and try again"
                }
                _uiState.value = _uiState.value.copy(
                    error = errorMessage,
                    isLoading = false
                )
            }
        }
    }
    
    fun checkEmailVerification() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val isVerified = authRepository.checkEmailVerification()
                println("AuthViewModel: Email verification check result: $isVerified")
                
                if (isVerified) {
                    // Email is verified, move user from pending to main collection
                    val user = authRepository.moveVerifiedUserToMainCollection()
                    if (user != null) {
                        println("AuthViewModel: User successfully moved to main collection: ${user.displayName}")
                        _uiState.value = _uiState.value.copy(
                            isSignedIn = true,
                            currentUser = user,
                            isLoading = false,
                            needsEmailVerification = false,
                            verificationEmailSent = false,
                            error = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to complete verification. Please try again.",
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Email not verified yet. Please check your email and click the verification link.",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                println("AuthViewModel: checkEmailVerification failed with error: ${e.message}")
                val errorMessage = when {
                    e.message?.contains("Email not verified") == true -> 
                        "Email not verified yet. Please check your email and click the verification link."
                    e.message?.contains("No pending user data found") == true -> 
                        "Verification in progress. Please wait a moment and try again."
                    else -> e.message ?: "Verification failed. Please try again."
                }
                _uiState.value = _uiState.value.copy(
                    error = errorMessage,
                    isLoading = false
                )
            }
        }
    }
    
    fun resendVerificationEmail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val success = authRepository.sendEmailVerification()
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        verificationEmailSent = true,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to resend verification email. Please try again.",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                println("AuthViewModel: resendVerificationEmail failed with error: ${e.message}")
                val errorMessage = when {
                    e.message?.contains("invalid-email", ignoreCase = true) == true -> 
                        "Please enter a valid email address"
                    e.message?.contains("user-not-found", ignoreCase = true) == true -> 
                        "Please enter a valid email address"
                    e.message?.contains("network", ignoreCase = true) == true -> 
                        "Network error. Please check your internet connection and try again"
                    else -> "Failed to resend verification email. Please enter a valid email address and try again"
                }
                _uiState.value = _uiState.value.copy(
                    error = errorMessage,
                    isLoading = false
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun cleanupUnverifiedUser() {
        viewModelScope.launch {
            try {
                authRepository.cleanupUnverifiedUser()
                _uiState.value = _uiState.value.copy(
                    isSignedIn = false,
                    currentUser = null,
                    isLoading = false,
                    needsEmailVerification = false,
                    verificationEmailSent = false,
                    error = null
                )
            } catch (e: Exception) {
                println("AuthViewModel: cleanupUnverifiedUser failed with error: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to cleanup user: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
}

data class AuthUiState(
    val isSignedIn: Boolean = false,
    val currentUser: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSignUpMode: Boolean = false,
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val needsEmailVerification: Boolean = false,
    val verificationEmailSent: Boolean = false
)
