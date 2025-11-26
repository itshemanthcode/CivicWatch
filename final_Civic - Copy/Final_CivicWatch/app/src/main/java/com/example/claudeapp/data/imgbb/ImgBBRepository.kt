package com.example.claudeapp.data.imgbb

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import com.example.claudeapp.R
import com.example.claudeapp.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImgBBRepository @Inject constructor(
    private val imageUtils: ImageUtils
) {
    
    data class UploadResult(
        val id: String,
        val url: String,
        val displayUrl: String,
        val width: Int,
        val height: Int,
        val size: Long,
        val deleteUrl: String
    )
    
    private fun getApiKey(context: Context): String {
        val apiKey = context.getString(R.string.imgbb_api_key)
        if (apiKey.isEmpty() || apiKey == "your_imgbb_api_key_here") {
            throw Exception("ImgBB API key is not configured. Please check your configuration.")
        }
        return apiKey
    }
    
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
    
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.imgbb.com/")
        .client(
            OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val apiService = retrofit.create(ImgBBApiService::class.java)
    
    suspend fun uploadImage(
        context: Context,
        imageUri: Uri,
        name: String? = null
    ): Result<UploadResult> = withContext(Dispatchers.IO) {
        try {
            // Check network connectivity first
            if (!isNetworkAvailable(context)) {
                return@withContext Result.failure(Exception("No internet connection. Please check your network and try again."))
            }
            
            // Validate image format first
            if (!imageUtils.isValidImageFormat(imageUri)) {
                return@withContext Result.failure(Exception("Invalid image format. Please select a valid image file (JPG, PNG, WebP, GIF)."))
            }
            
            // Compress image before upload (optimized for faster uploads)
            val compressedResult = try {
                imageUtils.compressImage(
                    context = context,
                    imageUri = imageUri,
                    maxWidth = 1280,
                    maxHeight = 720,
                    quality = 75,
                    maxFileSizeKB = 512
                )
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Failed to compress image: ${e.message}"))
            }
            
            // Convert image to base64
            val base64Image = try {
                convertImageToBase64(context, compressedResult.compressedUri)
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Failed to convert image to base64: ${e.message}"))
            }
            
            // Validate base64 data
            if (base64Image.isEmpty()) {
                return@withContext Result.failure(Exception("Image conversion resulted in empty data"))
            }
            
            // Upload to ImgBB
            val response = try {
                apiService.uploadImage(
                    apiKey = getApiKey(context),
                    base64Image = base64Image,
                    name = name ?: "civicwatch_issue_${System.currentTimeMillis()}"
                )
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Network error during upload: ${e.message}"))
            }
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.success == true) {
                    val data = responseBody.data
                    if (data != null) {
                        val result = UploadResult(
                            id = data.id,
                            url = data.url,
                            displayUrl = data.display_url,
                            width = data.width.toIntOrNull() ?: 0,
                            height = data.height.toIntOrNull() ?: 0,
                            size = data.size.toLongOrNull() ?: 0L,
                            deleteUrl = data.delete_url
                        )
                        Result.success(result)
                    } else {
                        Result.failure(Exception("Invalid response data: data field is null"))
                    }
                } else {
                    val errorMessage = responseBody?.let { 
                        "ImgBB API Error: success=${it.success}, status=${it.status}" 
                    } ?: "ImgBB API Error: No response body"
                    Result.failure(Exception(errorMessage))
                }
            } else {
                val errorMessage = when (response.code()) {
                    400 -> "Bad Request: Invalid image data or API key"
                    401 -> "Unauthorized: Invalid API key"
                    403 -> "Forbidden: API key doesn't have permission"
                    413 -> "File Too Large: Image exceeds size limit"
                    429 -> "Rate Limited: Too many requests"
                    500 -> "Server Error: ImgBB service unavailable"
                    else -> "Upload failed: HTTP ${response.code()} - ${response.message()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun convertImageToBase64(context: Context, imageUri: Uri): String {
        val inputStream: InputStream? = try {
            context.contentResolver.openInputStream(imageUri)
        } catch (e: Exception) {
            throw Exception("Failed to open image stream: ${e.message}")
        }
        
        if (inputStream == null) {
            throw Exception("Could not open input stream for image")
        }
        
        return try {
            val bytes = inputStream.readBytes()
            inputStream.close()
            if (bytes.isEmpty()) {
                throw Exception("Image file is empty")
            }
            Base64.getEncoder().encodeToString(bytes)
        } catch (e: Exception) {
            inputStream.close()
            throw Exception("Failed to read image data: ${e.message}")
        }
    }
    
    fun generateThumbnailUrl(imageUrl: String, size: Int = 200): String {
        // ImgBB doesn't have built-in thumbnail generation, so we return the original URL
        // You could use a service like Cloudinary or implement your own thumbnail generation
        return imageUrl
    }
    
    fun generateOptimizedImageUrl(imageUrl: String, width: Int? = null, height: Int? = null): String {
        // ImgBB doesn't have built-in image transformations, so we return the original URL
        return imageUrl
    }
}
