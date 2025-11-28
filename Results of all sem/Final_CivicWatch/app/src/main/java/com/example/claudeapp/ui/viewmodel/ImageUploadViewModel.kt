package com.example.claudeapp.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.claudeapp.data.imgbb.ImgBBRepository
import com.example.claudeapp.utils.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageUploadViewModel @Inject constructor(
    private val imgbbRepository: ImgBBRepository,
    private val imageUtils: ImageUtils
) : ViewModel() {
    
    data class UploadState(
        val isUploading: Boolean = false,
        val uploadProgress: Float = 0f,
        val uploadedImages: List<ImgBBRepository.UploadResult> = emptyList(),
        val error: String? = null
    )
    
    private val _uploadState = MutableStateFlow(UploadState())
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()
    
    fun uploadImages(
        context: Context,
        imageUris: List<Uri>,
        folder: String = "civicwatch/issues"
    ) {
        viewModelScope.launch {
            _uploadState.value = _uploadState.value.copy(
                isUploading = true,
                error = null,
                uploadProgress = 0f
            )
            
            try {
                val results = mutableListOf<ImgBBRepository.UploadResult>()
                val totalImages = imageUris.size
                
                imageUris.forEachIndexed { index, uri ->
                    // Validate image format
                    if (!imageUtils.isValidImageFormat(uri)) {
                        throw IllegalArgumentException("Invalid image format: $uri")
                    }
                    
                    // Upload to ImgBB
                    val uploadResult = imgbbRepository.uploadImage(
                        context = context,
                        imageUri = uri,
                        name = "civicwatch_issue_${System.currentTimeMillis()}_$index"
                    )
                    
                    when {
                        uploadResult.isSuccess -> {
                            results.add(uploadResult.getOrNull()!!)
                            val progress = (index + 1).toFloat() / totalImages
                            _uploadState.value = _uploadState.value.copy(
                                uploadProgress = progress,
                                uploadedImages = results.toList()
                            )
                        }
                        uploadResult.isFailure -> {
                            throw uploadResult.exceptionOrNull() ?: Exception("Upload failed")
                        }
                    }
                }
                
                _uploadState.value = _uploadState.value.copy(
                    isUploading = false,
                    uploadProgress = 1f,
                    uploadedImages = results
                )
                
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("No internet connection") == true -> 
                        "No internet connection. Please check your network and try again."
                    e.message?.contains("Invalid image format") == true -> 
                        "Invalid image format. Please select a valid image file (JPG, PNG, WebP, GIF)."
                    e.message?.contains("Failed to compress image") == true -> 
                        "Failed to process image. Please try with a different image."
                    e.message?.contains("Failed to convert image") == true -> 
                        "Failed to process image. Please try with a different image."
                    e.message?.contains("Unauthorized") == true -> 
                        "Image upload service is temporarily unavailable. Please try again later."
                    e.message?.contains("Rate Limited") == true -> 
                        "Too many upload requests. Please wait a moment and try again."
                    e.message?.contains("File Too Large") == true -> 
                        "Image is too large. Please select a smaller image."
                    else -> e.message ?: "Failed to upload image. Please try again."
                }
                _uploadState.value = _uploadState.value.copy(
                    isUploading = false,
                    error = errorMessage
                )
            }
        }
    }
    
    fun uploadSingleImage(
        context: Context,
        imageUri: Uri,
        folder: String = "civicwatch/issues"
    ) {
        uploadImages(context, listOf(imageUri), folder)
    }
    
    fun clearUploadState() {
        _uploadState.value = UploadState()
    }
    
    fun retryUpload(context: Context, imageUris: List<Uri>) {
        uploadImages(context, imageUris)
    }
    
    fun generateOptimizedImageUrl(
        imageUrl: String,
        width: Int? = null,
        height: Int? = null
    ): String {
        return imgbbRepository.generateOptimizedImageUrl(
            imageUrl = imageUrl,
            width = width,
            height = height
        )
    }
    
    fun generateThumbnailUrl(imageUrl: String, size: Int = 200): String {
        return imgbbRepository.generateThumbnailUrl(imageUrl, size)
    }
}
