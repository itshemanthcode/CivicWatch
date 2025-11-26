package com.example.claudeapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Authority(
    @DocumentId
    val authorityId: String = "",
    val organizationName: String = "",
    val email: String = "",
    val jurisdiction: AuthorityJurisdiction = AuthorityJurisdiction(),
    val role: String = "viewer", // superadmin, admin, viewer
    val notificationPreferences: AuthorityNotificationPreferences = AuthorityNotificationPreferences(),
    val handledCategories: List<String> = emptyList(), // Issue categories this authority handles (e.g., ["potholes", "water_logging"])
    val departmentType: String = "", // e.g., "road_department", "utilities", "sanitation", "public_works"
    val createdAt: Timestamp = Timestamp.now()
)

data class AuthorityJurisdiction(
    val city: String = "",
    val state: String = "",
    val areas: List<String> = emptyList() // specific neighborhoods/zones
)

data class AuthorityNotificationPreferences(
    val email: Boolean = true,
    val threshold: Int = 10 // notify after X upvotes
)

enum class AuthorityRole(val displayName: String) {
    SUPER_ADMIN("Super Admin"),
    ADMIN("Admin"),
    VIEWER("Viewer")
}

