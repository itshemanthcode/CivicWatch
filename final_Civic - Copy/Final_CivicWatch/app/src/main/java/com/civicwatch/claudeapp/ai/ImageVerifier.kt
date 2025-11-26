package com.civicwatch.claudeapp.ai
//import com.civicwatch.example.claudeapp.ai.verifyIssueWithGemini
import com.google.ai.client.generativeai.type.content



//package com.your.package.name.ai   // ⚠️ Change this to your actual package name!

import android.graphics.Bitmap
//import com.civicwatch.example.claudeapp.ai.verifyIssueWithGemini




import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import java.io.ByteArrayOutputStream

suspend fun verifyIssueWithGemini(bitmap: Bitmap): Pair<String, Float> {
    val apiKey = "AIzaSyAck_fPyysjiKzyO9yoNDHeLRPPDCIHLVk"  // 🔑 paste your Gemini API key here

    val model = GenerativeModel(
        modelName = "gemini-2.0-flash-001",  // Vision-capable model
        apiKey = apiKey
    )

    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
    val imageBytes = stream.toByteArray()

    val prompt = """
        You are a city infrastructure inspector.
        Classify this image into exactly ONE of these categories:
        - potholes (or pothole)
        - water_logging (or water logging)
        - broken_street_lights (or broken street light/lights)
        - garbage

        Important: Return the label in lowercase, using underscores for multi-word categories.
        For potholes, return "potholes" (plural preferred).
        For water logging, return "water_logging" (with underscore).
        For broken street lights, return "broken_street_lights" (with underscore, plural).
        For garbage, return "garbage".

        Respond ONLY in this format:
        <label>|<confidence in percentage>
        Examples: 
        potholes|92
        water_logging|85
        broken_street_lights|78
        garbage|90
    """.trimIndent()

    val input = content {
        text(prompt)
        image(bitmap)
    }

    val response = model.generateContent(input)
    val text = response.text ?: return Pair("unknown", 0f)

    // Try to parse the response in the expected format: label|confidence
    var label = "unknown"
    var confidence = 0f
    
    // First, try to split by pipe character
    val parts = text.trim().split("|")
    if (parts.size >= 2) {
        label = parts[0].trim().lowercase()
        // Extract confidence number (remove % if present)
        val confidenceStr = parts[1].trim().replace("%", "").replace(Regex("[^0-9.]"), "")
        confidence = confidenceStr.toFloatOrNull() ?: 0f
    } else {
        // If pipe format not found, try to extract label and confidence from text
        // Look for common patterns
        val lines = text.trim().lines()
        for (line in lines) {
            // Try to find label (one of the known categories)
            val lowerLine = line.lowercase()
            when {
                lowerLine.contains("pothole") -> label = "potholes"
                lowerLine.contains("water") && (lowerLine.contains("log") || lowerLine.contains("flood")) -> label = "water_logging"
                (lowerLine.contains("street") && lowerLine.contains("light")) || 
                    (lowerLine.contains("broken") && lowerLine.contains("light")) -> label = "broken_street_lights"
                lowerLine.contains("garbage") || lowerLine.contains("trash") || lowerLine.contains("waste") || lowerLine.contains("litter") -> label = "garbage"
            }
            
            // Try to extract confidence percentage
            val confidenceMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*%").find(line)
            if (confidenceMatch != null) {
                confidence = confidenceMatch.groupValues[1].toFloatOrNull() ?: 0f
            } else {
                // Try to find just a number that could be confidence
                val numberMatch = Regex("\\b(\\d{1,3})\\b").find(line)
                if (numberMatch != null) {
                    val num = numberMatch.groupValues[1].toIntOrNull() ?: 0
                    if (num in 0..100) {
                        confidence = num.toFloat()
                    }
                }
            }
        }
    }
    
    // Normalize label: replace spaces with underscores
    label = label.replace(" ", "_").replace("-", "_")
    
    // Handle common variations and normalize
    when {
        label.contains("pothole") -> label = "potholes"
        (label.contains("water") && (label.contains("log") || label.contains("flood"))) -> label = "water_logging"
        (label.contains("street") && label.contains("light")) || 
            (label.contains("broken") && label.contains("light")) -> label = "broken_street_lights"
        label.contains("garbage") || label.contains("trash") || label.contains("waste") || label.contains("litter") -> label = "garbage"
    }
    
    return Pair(label, confidence)
}
