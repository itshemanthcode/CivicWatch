package com.civicwatch.claudeapp.ai

import android.graphics.Bitmap
import com.example.claudeapp.data.model.IssueCategory
import com.example.claudeapp.data.model.IssueSeverity
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class TriageResult(
    val category: IssueCategory?,
    val severity: IssueSeverity,
    val description: String
)

suspend fun analyzeIssueImage(bitmap: Bitmap): TriageResult {
    return withContext(Dispatchers.IO) {
        val apiKey = "AIzaSyB_Mk4Hwvwp9HtOcsVqQi5ck8iorhVxlXI" // Using the new key

        val model = GenerativeModel(
            modelName = "gemini-2.0-flash-001",
            apiKey = apiKey
        )

        val prompt = """
            Analyze this image of a civic issue. Provide the following details in JSON format:
            1. category: One of [POTHOLES, GARBAGE, WATER_LOGGING, BROKEN_STREET_LIGHTS]. If it doesn't fit well, choose the closest or null.
            2. severity: One of [LOW, MEDIUM, HIGH]. Based on the potential danger or inconvenience.
            3. description: A professional, concise description of the issue observed (max 2 sentences).
            
            Respond ONLY with the JSON object. Do not include markdown formatting like ```json.
            Example:
            {
              "category": "POTHOLES",
              "severity": "HIGH",
              "description": "A large pothole in the middle of the road causing traffic obstruction."
            }
        """.trimIndent()

        val input = content {
            text(prompt)
            image(bitmap)
        }

        try {
            val response = model.generateContent(input)
            val text = response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: "{}"
            
            val json = JSONObject(text)
            
            val categoryStr = json.optString("category")
            val severityStr = json.optString("severity")
            val description = json.optString("description")

            val category = try {
                IssueCategory.valueOf(categoryStr.uppercase())
            } catch (e: Exception) {
                null
            }

            val severity = try {
                IssueSeverity.valueOf(severityStr.uppercase())
            } catch (e: Exception) {
                IssueSeverity.MEDIUM
            }

            TriageResult(category, severity, description)
        } catch (e: Exception) {
            e.printStackTrace()
            // Return a default/fallback result in case of error
            TriageResult(null, IssueSeverity.MEDIUM, "")
        }
    }
}
