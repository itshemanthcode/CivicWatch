package com.example.claudeapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class AppNotification(
    @DocumentId
    val notificationId: String = "",
    val userId: String = "",
    val type: String = "", // status_change, new_nearby, resolved, etc.
    val title: String = "",
    val body: String = "",
    val data: Map<String, Any> = emptyMap(), // additional metadata
    val read: Boolean = false,
    val createdAt: Timestamp = Timestamp.now()
)

enum class NotificationType(val displayName: String) {
    STATUS_CHANGE("Status Change"),
    NEW_NEARBY("New Nearby Issue"),
    RESOLVED("Issue Resolved"),
    VERIFIED("Issue Verified"),
    UPVOTED("Issue Upvoted"),
    COMMENT("New Comment"),
    GENERAL("General")
}

