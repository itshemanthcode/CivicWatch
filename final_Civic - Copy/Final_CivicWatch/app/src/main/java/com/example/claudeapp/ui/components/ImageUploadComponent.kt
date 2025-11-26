package com.example.claudeapp.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.claudeapp.ui.viewmodel.ImageUploadViewModel
import kotlinx.coroutines.launch

@Composable
fun ImageUploadComponent(
    viewModel: ImageUploadViewModel,
    modifier: Modifier = Modifier,
    maxImages: Int = 5,
    onImagesUploaded: (List<String>) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uploadState by viewModel.uploadState.collectAsState()
    
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showImagePicker by remember { mutableStateOf(false) }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedImages = (selectedImages + uris).take(maxImages)
        if (uris.isNotEmpty()) {
            scope.launch {
                viewModel.uploadImages(context, uris)
            }
        }
    }
    
    // Single image picker launcher
    val singleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImages = (selectedImages + listOf(it)).take(maxImages)
            scope.launch {
                viewModel.uploadSingleImage(context, it)
            }
        }
    }
    
    // Update parent when uploads complete
    LaunchedEffect(uploadState.uploadedImages) {
        if (uploadState.uploadedImages.isNotEmpty() && !uploadState.isUploading) {
            val imageUrls = uploadState.uploadedImages.map { it.url }
            onImagesUploaded(imageUrls)
        }
    }
    
    Column(modifier = modifier) {
        // Upload section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (uploadState.isUploading) {
                    // Upload progress
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            progress = uploadState.uploadProgress,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Uploading images...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Upload button
                    IconButton(
                        onClick = {
                            if (selectedImages.size < maxImages) {
                                imagePickerLauncher.launch("image/*")
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(24.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Images",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add Images (${selectedImages.size}/$maxImages)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Error message
        uploadState.error?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Selected images preview
        if (selectedImages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Selected Images",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedImages) { uri ->
                    ImagePreviewItem(
                        imageUri = uri,
                        onRemove = {
                            selectedImages = selectedImages.filter { it != uri }
                        }
                    )
                }
            }
        }
        
        // Uploaded images preview
        if (uploadState.uploadedImages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Uploaded Images",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uploadState.uploadedImages) { uploadResult ->
                    UploadedImagePreviewItem(
                        uploadResult = uploadResult,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun ImagePreviewItem(
    imageUri: Uri,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline,
                RoundedCornerShape(8.dp)
            )
    ) {
        AsyncImage(
            model = imageUri,
            contentDescription = "Selected Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .background(
                    MaterialTheme.colorScheme.error,
                    RoundedCornerShape(12.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove Image",
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun UploadedImagePreviewItem(
    uploadResult: com.example.claudeapp.data.imgbb.ImgBBRepository.UploadResult,
    viewModel: ImageUploadViewModel
) {
    val thumbnailUrl = remember(uploadResult.id) {
        viewModel.generateThumbnailUrl(uploadResult.url, 80)
    }
    
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary,
                RoundedCornerShape(8.dp)
            )
    ) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = "Uploaded Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Success indicator
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Uploaded",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(20.dp)
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(10.dp)
                )
                .padding(2.dp)
        )
    }
}
