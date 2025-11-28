package com.example.claudeapp.data.repository

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.AuthResult
import com.google.firebase.firestore.FirebaseFirestore
import com.example.claudeapp.data.model.User
import com.example.claudeapp.data.model.NotificationSettings
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val googleSignInClient: GoogleSignInClient
) {
    
    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser
    
    val isUserSignedIn: Boolean
        get() {
            val isSignedIn = currentUser != null
            println("AuthRepository: isUserSignedIn - currentUser: $currentUser, isSignedIn: $isSignedIn")
            return isSignedIn
        }
    
    suspend fun signInWithGoogle(account: GoogleSignInAccount): User {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        val authResult = firebaseAuth.signInWithCredential(credential).await()
        
        val firebaseUser = authResult.user
        if (firebaseUser != null) {
            return createOrUpdateUser(firebaseUser)
        } else {
            throw Exception("Authentication failed")
        }
    }
    
    suspend fun signInWithEmail(email: String, password: String): User {
        val result: AuthResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: throw Exception("Authentication failed")
        return createOrUpdateUser(firebaseUser)
    }
    
    suspend fun signUpWithEmail(displayName: String, email: String, password: String): User {
        val result: AuthResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: throw Exception("Account creation failed")
        
        // Send email verification for new users
        firebaseUser.sendEmailVerification().await()
        
        // Create user object but DON'T store in Firestore until email is verified
        val user = createNewUser(firebaseUser).copy(
            displayName = displayName,
            email = email
        )
        
        // Store user data temporarily in a "pending_verification" collection
        // This allows us to track the user but not give them full access
        firestore.collection("pending_verification").document(firebaseUser.uid).set(user).await()
        
        return user
    }
    
    suspend fun sendEmailVerification(): Boolean {
        val user = currentUser ?: throw Exception("No user signed in")
        return try {
            user.sendEmailVerification().await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun checkEmailVerification(): Boolean {
        val user = currentUser ?: return false
        try {
            user.reload().await()
            return user.isEmailVerified
        } catch (e: Exception) {
            println("AuthRepository: Error reloading user: ${e.message}")
            return false
        }
    }
    
    suspend fun moveVerifiedUserToMainCollection(): User? {
        val currentUser = firebaseAuth.currentUser ?: return null
        
        // Reload user to get latest verification status
        try {
            currentUser.reload().await()
        } catch (e: Exception) {
            println("AuthRepository: Error reloading user for verification: ${e.message}")
        }
        
        if (!currentUser.isEmailVerified) {
            throw Exception("Email not verified")
        }
        
        // Check if user already exists in main collection
        val existingUserDoc = firestore.collection("users").document(currentUser.uid).get().await()
        if (existingUserDoc.exists()) {
            // User already exists in main collection, just return the existing user
            println("AuthRepository: User already exists in main collection")
            return existingUserDoc.toObject(User::class.java)
        }
        
        // Get user data from pending_verification collection
        val pendingUserDoc = firestore.collection("pending_verification").document(currentUser.uid).get().await()
        
        if (!pendingUserDoc.exists()) {
            // No pending data found, create a new user with current Firebase Auth data
            println("AuthRepository: No pending user data found, creating new user from Firebase Auth data")
            val user = createNewUser(currentUser)
            firestore.collection("users").document(currentUser.uid).set(user).await()
            return user
        }
        
        val user = pendingUserDoc.toObject(User::class.java) ?: throw Exception("Failed to parse user data")
        
        // Move user to main users collection
        firestore.collection("users").document(currentUser.uid).set(user).await()
        
        // Delete from pending_verification collection
        firestore.collection("pending_verification").document(currentUser.uid).delete().await()
        
        return user
    }
    
    suspend fun cleanupUnverifiedUser() {
        val currentUser = firebaseAuth.currentUser ?: return
        
        // Delete user from pending_verification collection
        firestore.collection("pending_verification").document(currentUser.uid).delete().await()
        
        // Delete the Firebase Auth user account
        currentUser.delete().await()
    }
    
    suspend fun signInWithEmailAndVerify(email: String, password: String): User {
        val result: AuthResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: throw Exception("Authentication failed")
        
        if (!firebaseUser.isEmailVerified) {
            throw Exception("Please verify your email before signing in")
        }
        
        return createOrUpdateUser(firebaseUser)
    }
    
    suspend fun signOut() {
        // Sign out from Firebase Auth
        firebaseAuth.signOut()
        
        // Sign out from Google Sign-In to ensure account selection on next sign-in
        googleSignInClient.signOut().await()
    }
    
    private suspend fun createOrUpdateUser(firebaseUser: FirebaseUser): User {
        val userRef = firestore.collection("users").document(firebaseUser.uid)
        val userDoc = userRef.get().await()
        
        val user = if (userDoc.exists()) {
            // Update existing user
            val existingUser = userDoc.toObject(User::class.java) ?: createNewUser(firebaseUser)
            existingUser.copy(
                lastLoginAt = com.google.firebase.Timestamp.now()
            )
        } else {
            // Create new user
            createNewUser(firebaseUser)
        }
        
        userRef.set(user).await()
        return user
    }
    
    private fun createNewUser(firebaseUser: FirebaseUser): User {
        return User(
            userId = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            displayName = firebaseUser.displayName ?: "",
            photoURL = firebaseUser.photoUrl?.toString() ?: "",
            points = 0,
            badges = emptyList(),
            reportsCount = 0,
            resolvedCount = 0,
            reputation = 100,
            createdAt = com.google.firebase.Timestamp.now(),
            lastLoginAt = com.google.firebase.Timestamp.now(),
            notificationSettings = NotificationSettings()
        )
    }
    
    suspend fun getCurrentUserData(): User? {
        val currentUser = firebaseAuth.currentUser ?: return null
        val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
        return userDoc.toObject(User::class.java)
    }
    
    suspend fun updateUserProfile(user: User) {
        val userRef = firestore.collection("users").document(user.userId)
        userRef.set(user).await()
    }
    
    suspend fun checkEmailExists(email: String): Boolean {
        return try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            !querySnapshot.isEmpty
        } catch (e: Exception) {
            // If there's an error checking, assume email doesn't exist to be safe
            false
        }
    }
}
