package com.example.claudeapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageUtils @Inject constructor() {
    
    data class ImageProcessingResult(
        val compressedUri: Uri,
        val originalSize: Long,
        val compressedSize: Long,
        val width: Int,
        val height: Int
    )
    
    suspend fun compressImage(
        context: Context,
        imageUri: Uri,
        maxWidth: Int = 1280,
        maxHeight: Int = 720,
        quality: Int = 75,
        maxFileSizeKB: Int = 512
    ): ImageProcessingResult = withContext(Dispatchers.IO) {
        
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        
        if (originalBitmap == null) {
            throw IllegalArgumentException("Could not decode image from URI: $imageUri")
        }
        
        // Get original file size
        val originalSize = getFileSize(context, imageUri)
        
        // Fix orientation based on EXIF data
        val orientedBitmap = fixImageOrientation(context, imageUri, originalBitmap)
        
        // Calculate new dimensions
        val (newWidth, newHeight) = calculateDimensions(
            orientedBitmap.width,
            orientedBitmap.height,
            maxWidth,
            maxHeight
        )
        
        // Resize bitmap
        val resizedBitmap = Bitmap.createScaledBitmap(orientedBitmap, newWidth, newHeight, true)
        
        // Compress bitmap
        val compressedUri = compressBitmapToUri(
            context,
            resizedBitmap,
            quality,
            maxFileSizeKB
        )
        
        val compressedSize = getFileSize(context, compressedUri)
        
        // Clean up
        originalBitmap.recycle()
        orientedBitmap.recycle()
        resizedBitmap.recycle()
        
        ImageProcessingResult(
            compressedUri = compressedUri,
            originalSize = originalSize,
            compressedSize = compressedSize,
            width = newWidth,
            height = newHeight
        )
    }
    
    private fun fixImageOrientation(context: Context, imageUri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val exif = ExifInterface(inputStream!!)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
            inputStream.close()
            
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap
            }
            
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }
    
    private fun calculateDimensions(
        originalWidth: Int,
        originalHeight: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Pair<Int, Int> {
        val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()
        
        return when {
            originalWidth <= maxWidth && originalHeight <= maxHeight -> {
                Pair(originalWidth, originalHeight)
            }
            aspectRatio > 1 -> {
                // Landscape
                val newWidth = maxWidth
                val newHeight = (maxWidth / aspectRatio).toInt()
                Pair(newWidth, newHeight)
            }
            else -> {
                // Portrait
                val newHeight = maxHeight
                val newWidth = (maxHeight * aspectRatio).toInt()
                Pair(newWidth, newHeight)
            }
        }
    }
    
    private fun compressBitmapToUri(
        context: Context,
        bitmap: Bitmap,
        quality: Int,
        maxFileSizeKB: Int
    ): Uri {
        val tempFile = File.createTempFile("compressed_image", ".jpg", context.cacheDir)
        var currentQuality = quality
        
        do {
            val outputStream = FileOutputStream(tempFile)
            // Use JPEG format for better compression and compatibility
            bitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, outputStream)
            outputStream.close()
            
            val fileSizeKB = tempFile.length() / 1024
            if (fileSizeKB <= maxFileSizeKB || currentQuality <= 20) {
                break
            }
            currentQuality -= 10
        } while (currentQuality > 20)
        
        return Uri.fromFile(tempFile)
    }
    
    private fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val size = inputStream?.available()?.toLong() ?: 0L
            inputStream?.close()
            size
        } catch (e: Exception) {
            0L
        }
    }
    
    fun generateUniqueFileName(prefix: String = "image", extension: String = "jpg"): String {
        val timestamp = System.currentTimeMillis()
        return "${prefix}_${timestamp}.${extension}"
    }
    
    fun isValidImageFormat(uri: Uri): Boolean {
        return try {
            // For content URIs, we need to check the MIME type
            val mimeType = uri.toString()
            when {
                mimeType.startsWith("content://") -> {
                    // For content URIs, we'll assume they're valid if they come from the image picker
                    // The actual validation will happen during compression
                    true
                }
                mimeType.startsWith("file://") -> {
                    // For file URIs, check the extension
                    val extension = mimeType.substringAfterLast(".").lowercase()
                    extension in listOf("jpg", "jpeg", "png", "webp", "gif")
                }
                else -> {
                    // Fallback to extension check
                    val extension = mimeType.substringAfterLast(".").lowercase()
                    extension in listOf("jpg", "jpeg", "png", "webp", "gif")
                }
            }
        } catch (e: Exception) {
            // If we can't determine the format, assume it's valid and let compression handle it
            true
        }
    }
    
    suspend fun getImageDimensions(context: Context, uri: Uri): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()
        
        Pair(options.outWidth, options.outHeight)
    }
}
