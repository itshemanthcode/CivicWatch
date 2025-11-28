package com.example.claudeapp.data.camera

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraService {
    companion object {
        fun createImageFile(context: Context): File {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = File(context.getExternalFilesDir(null), "CivicWatch")
            if (!storageDir.exists()) {
                val created = storageDir.mkdirs()
                println("Created directory: $created, path: ${storageDir.absolutePath}")
            }
            val imageFile = File(storageDir, "CIVICWATCH_${timeStamp}.jpg")
            println("Created image file: ${imageFile.absolutePath}")
            return imageFile
        }
        
        fun getImageUri(context: Context, imageFile: File): Uri {
            return try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
                println("Created URI: $uri")
                uri
            } catch (e: Exception) {
                println("Error creating URI: ${e.message}")
                throw e
            }
        }
    }
}

@Composable
fun rememberCameraLauncher(
    onImageCaptured: (Uri) -> Unit,
    onError: (Exception) -> Unit
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture()
) { success ->
    if (success) {
        // The image was captured successfully
        // The URI will be passed to the launcher
    } else {
        onError(Exception("Failed to capture image"))
    }
}
