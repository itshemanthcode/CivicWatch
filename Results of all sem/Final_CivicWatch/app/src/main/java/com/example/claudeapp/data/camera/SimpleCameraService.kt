package com.example.claudeapp.data.camera

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SimpleCameraService {
    private const val TAG = "SimpleCameraService"
    
    fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        
        // Try external storage first, fall back to internal storage
        val storageDir = try {
            val externalDir = context.getExternalFilesDir(null)
            if (externalDir != null && externalDir.exists()) {
                File(externalDir, "CivicWatch").also { 
                    if (!it.exists()) it.mkdirs()
                }
            } else {
                // Fall back to internal storage
                File(context.filesDir, "CivicWatch").also { 
                    if (!it.exists()) it.mkdirs()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing storage directory", e)
            // Use internal storage as fallback
            File(context.filesDir, "CivicWatch").also { 
                if (!it.exists()) it.mkdirs()
            }
        }
        
        Log.d(TAG, "Creating image file in directory: ${storageDir.absolutePath}")
        
        val imageFile = File(storageDir, "CIVICWATCH_${timeStamp}.jpg")
        Log.d(TAG, "Image file path: ${imageFile.absolutePath}")
        
        return imageFile
    }
    
    fun getImageUri(context: Context, imageFile: File): Uri {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
            Log.d(TAG, "FileProvider URI: $uri")
            uri
        } catch (e: Exception) {
            Log.e(TAG, "Error creating FileProvider URI", e)
            throw e
        }
    }
    
    fun createImageFileAndUri(context: Context): Pair<File, Uri> {
        return try {
            val file = createImageFile(context)
            Log.d(TAG, "Created image file: ${file.absolutePath}")
            Log.d(TAG, "File exists: ${file.exists()}")
            Log.d(TAG, "File can write: ${file.canWrite()}")
            
            val uri = getImageUri(context, file)
            Log.d(TAG, "Created URI: $uri")
            
            Pair(file, uri)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating image file and URI", e)
            throw e
        }
    }
}
