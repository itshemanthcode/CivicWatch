package com.example.claudeapp.data.repository

import com.example.claudeapp.data.model.Issue
import com.example.claudeapp.data.model.Authority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZapierRepository @Inject constructor() {
    
    // TODO: Replace with your actual Zapier webhook URL from the AI agent
    // Get this from: https://agents.zapier.com/copy/21835688-3b77-4412-b95e-f309fa5e2bbb
    private val zapierWebhookUrl = "https://hooks.zapier.com/hooks/catch/YOUR_WEBHOOK_ID/"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * Send issue notification to authorities via Zapier AI agent
     * The AI agent will handle composing and sending emails to relevant authorities
     */
    suspend fun notifyAuthoritiesAboutIssue(
        issue: Issue,
        authorities: List<Authority> = emptyList()
    ): kotlin.Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Prepare the payload for Zapier webhook
            val payload = JSONObject().apply {
                put("issueId", issue.issueId)
                put("category", issue.category)
                put("severity", issue.severity)
                put("description", issue.description)
                put("status", issue.status)
                put("upvotes", issue.upvotes)
                put("verifications", issue.verifications)
                put("reportedBy", issue.reportedBy)
                put("createdAt", issue.createdAt.toDate().toString())
                
                // Location information
                val location = JSONObject().apply {
                    put("address", issue.location.address)
                    put("area", issue.location.area)
                    put("city", issue.location.city)
                    put("state", issue.location.state)
                    put("country", issue.location.country)
                    put("latitude", issue.location.latitude)
                    put("longitude", issue.location.longitude)
                }
                put("location", location)
                
                // Image URLs
                put("images", issue.images)
                
                // Authority information (if available)
                if (authorities.isNotEmpty()) {
                    val authorityList = authorities.map { authority ->
                        JSONObject().apply {
                            put("organizationName", authority.organizationName)
                            put("email", authority.email)
                            put("city", authority.jurisdiction.city)
                            put("state", authority.jurisdiction.state)
                            put("departmentType", authority.departmentType)
                            put("handledCategories", authority.handledCategories)
                        }
                    }
                    put("authorities", authorityList)
                }
                
                // Additional context for AI agent
                put("priorityScore", issue.priorityScore)
                put("commentsCount", issue.commentsCount)
                put("notifiedAt", issue.notifiedAt?.toDate()?.toString() ?: "")
            }
            
            // Create HTTP request
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = payload.toString().toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url(zapierWebhookUrl)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()
            
            // Execute request
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                kotlin.Result.success(Unit)
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                kotlin.Result.failure(Exception("Zapier webhook failed: ${response.code} - $errorBody"))
            }
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }
    
    /**
     * Send a simple notification to Zapier (for testing or simple cases)
     */
    suspend fun sendSimpleNotification(
        issueId: String,
        category: String,
        location: String,
        description: String
    ): kotlin.Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("issueId", issueId)
                put("category", category)
                put("location", location)
                put("description", description)
                put("action", "notify_authorities")
            }
            
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = payload.toString().toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url(zapierWebhookUrl)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                kotlin.Result.success(Unit)
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                kotlin.Result.failure(Exception("Zapier webhook failed: ${response.code} - $errorBody"))
            }
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }
}


