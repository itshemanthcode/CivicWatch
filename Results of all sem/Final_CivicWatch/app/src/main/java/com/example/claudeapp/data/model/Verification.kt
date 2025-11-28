package com.example.claudeapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Verification(
    @DocumentId
    val verificationId: String = "",
    val issueId: String = "",
    val verifiedBy: String = "", // userId
    val images: List<String> = emptyList(), // Cloudinary URLs
    val timestamp: Timestamp = Timestamp.now(),
    val location: VerificationLocation = VerificationLocation()
)

data class VerificationLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

