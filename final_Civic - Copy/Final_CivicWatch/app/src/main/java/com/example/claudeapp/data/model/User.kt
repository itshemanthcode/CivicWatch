package com.example.claudeapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoURL: String = "",
    val points: Int = 0,
    val badges: List<String> = emptyList(),
    val reportsCount: Int = 0,
    val resolvedCount: Int = 0,
    val reputation: Int = 100, // 0-100
    val createdAt: Timestamp = Timestamp.now(),
    val lastLoginAt: Timestamp = Timestamp.now(),
    val notificationSettings: NotificationSettings = NotificationSettings()
)

data class NotificationSettings(
    val pushEnabled: Boolean = true,
    val emailEnabled: Boolean = true,
    val weeklyDigest: Boolean = true
)

