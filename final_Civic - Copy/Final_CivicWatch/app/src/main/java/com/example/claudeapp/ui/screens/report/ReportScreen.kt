package com.example.claudeapp.ui.screens.report
import com.civicwatch.claudeapp.ai.verifyIssueWithGemini
import android.provider.MediaStore


import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.claudeapp.ui.components.ImageViewerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import com.example.claudeapp.ui.components.ImageViewer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.claudeapp.data.location.LocationService
import com.example.claudeapp.data.service.GeocodingService
import com.example.claudeapp.data.model.Issue
import com.example.claudeapp.data.model.IssueCategory
import com.example.claudeapp.data.model.IssueSeverity
import com.example.claudeapp.ui.viewmodel.IssueViewModel
import com.example.claudeapp.ui.viewmodel.ImageUploadViewModel
import com.example.claudeapp.data.camera.SimpleCameraService
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.layout.statusBarsPadding
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay // Added import for delay
import androidx.compose.ui.text.style.TextAlign

// Enum for validation message types
enum class ValidationMessageType {
    ERROR,
    INFO,
    SUCCESS,
    VERIFYING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    onBack: () -> Unit = {},
    viewModel: IssueViewModel = hiltViewModel(),
    imageUploadViewModel: ImageUploadViewModel = hiltViewModel()
) {
    var selectedCategory by remember { mutableStateOf<IssueCategory?>(null) }
    var selectedSeverity by remember { mutableStateOf(IssueSeverity.MEDIUM) }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("Getting location...") }
    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }
    var addressInfo by remember { mutableStateOf<GeocodingService.AddressInfo?>(null) }
    var capturedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var confirmedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var uploadedImageUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSuccessPopup by remember { mutableStateOf(false) }
    var isUploadingImages by remember { mutableStateOf(false) }
    var waitingForUpload by remember { mutableStateOf(false) }
    
    // Image viewer states
    var showImageViewer by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableStateOf(0) }
    
    // Validation states
    var isLocationValid by remember { mutableStateOf(false) }
    var validationErrors by remember { mutableStateOf<List<String>>(emptyList()) }
    var showValidationPopup by remember { mutableStateOf(false) }
    var validationMessageType by remember { mutableStateOf<ValidationMessageType>(ValidationMessageType.ERROR) }


    val uiState by viewModel.uiState.collectAsState()
    val uploadState by imageUploadViewModel.uploadState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Location service
    val locationService = remember { LocationService(context, GeocodingService(context)) }

    // Store the current image URI for camera capture
    var currentImageUri by remember { mutableStateOf<Uri?>(null) }

    // Camera launcher using TakePicture contract
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        println("Camera result: success=$success, currentImageUri=$currentImageUri")
        if (success && currentImageUri != null) {
            // Image captured successfully
            println("Image captured successfully, adding to capturedImages")
            
            // Verify the file actually exists before adding
            val uri = currentImageUri!!
            try {
                // Check if file is accessible
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    inputStream.close()
                    // File exists and is readable, add to captured images
                    capturedImages = capturedImages + uri
                    println("Updated capturedImages: ${capturedImages.size} images")
                    // Clear any previous errors
                    validationErrors = emptyList()
                } else {
                    println("Image file not accessible")
                    validationErrors = listOf("Image was captured but couldn't be accessed. Please try again.")
                }
            } catch (e: Exception) {
                println("Error accessing image file: ${e.message}")
                validationErrors = listOf("Image capture completed but couldn't process the file. Please try again.")
            }
            
            currentImageUri = null
        } else {
            println("Camera cancelled or failed")
            currentImageUri = null
            // Don't show error for cancellation - user might have just cancelled
            // Only show error if they explicitly tried to capture
            if (success == false) {
                validationErrors = listOf("Camera capture was cancelled. You can try again or use gallery to select images.")
            }
        }
    }

    // Alternative: Use GetContent for image selection as fallback
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            println("Image selected from gallery: $it")
            capturedImages = capturedImages + it
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, launch camera
            try {
                val (imageFile, imageUri) = SimpleCameraService.createImageFileAndUri(context)
                currentImageUri = imageUri
                println("Permission granted, launching camera with URI: $imageUri")
                println("File path: ${imageFile.absolutePath}")

                // Grant URI permissions before launching
                context.grantUriPermission(
                    "com.android.camera",
                    imageUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                cameraLauncher.launch(imageUri)
            } catch (e: Exception) {
                println("Error launching camera: ${e.message}")
                e.printStackTrace()
                validationErrors = listOf("Failed to launch camera: ${e.message}")
            }
        } else {
            // Permission denied
            println("Camera permission denied")
            validationErrors = listOf("Camera permission is required to take photos. Please grant permission in settings.")
        }
    }

    // Get current location on screen load
    LaunchedEffect(Unit) {
        scope.launch {
            when (val result = locationService.getCurrentLocation()) {
                is com.example.claudeapp.data.location.LocationResult.Success -> {
                    latitude = result.location.latitude
                    longitude = result.location.longitude
                    addressInfo = result.addressInfo
                    location = GeocodingService(context).formatAddress(result.addressInfo)
                    isLocationValid = true
                }
                is com.example.claudeapp.data.location.LocationResult.Error -> {
                    location = "Location unavailable: ${result.message}"
                    isLocationValid = false
                }
            }
        }
    }
    
    // Validation function
    fun validateForm(): List<String> {
        val errors = mutableListOf<String>()
        
        if (selectedCategory == null) {
            errors.add("Please select an issue type")
        }
        
        if (description.isBlank()) {
            errors.add("Please provide a description of the issue")
        }
        
        if (!isLocationValid || latitude == 0.0 || longitude == 0.0) {
            errors.add("Please ensure location is available")
        }
        
        if (confirmedImages.isEmpty()) {
            errors.add("Please take and confirm at least one photo")
        }
        
        if (uploadedImageUrls.isEmpty() && confirmedImages.isNotEmpty() && isUploadingImages) {
            errors.add("Please wait for photos to upload")
        }
        
        return errors
    }

    // Function to submit the report
    fun submitReport() {
        println("ReportScreen: Submitting report...")
        waitingForUpload = true
        // Get current user ID
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: "anonymous"

        // Create issue with actual data
        val issue = Issue(
            reportedBy = userId,
            category = selectedCategory!!.name,
            severity = selectedSeverity.name.lowercase(),
            description = description,
            images = uploadedImageUrls,
            location = com.example.claudeapp.data.model.IssueLocation(
                latitude = latitude,
                longitude = longitude,
                address = addressInfo?.fullAddress ?: location,
                area = addressInfo?.area ?: "",
                city = addressInfo?.city ?: "",
                state = addressInfo?.state ?: "",
                country = addressInfo?.country ?: ""
            )
        )

        println("ReportScreen: Creating issue with ${uploadedImageUrls.size} images")
        // Create issue and wait for completion
        viewModel.createIssue(issue)
    }
    
    // Watch for successful issue creation
    LaunchedEffect(uiState.isLoading, uiState.error) {
        // Show success popup when issue creation completes successfully
        if (!uiState.isLoading && uiState.error == null && waitingForUpload) {
            if (!showSuccessPopup) {
                showSuccessPopup = true
                waitingForUpload = false
                println("ReportScreen: Showing success popup - Issue created successfully")
            }
        }
        
        // Handle error in issue creation
        if (uiState.error != null && !uiState.isLoading && waitingForUpload) {
            waitingForUpload = false
            validationErrors = listOf("Failed to submit report: ${uiState.error}")
            validationMessageType = ValidationMessageType.ERROR
            showValidationPopup = true
            println("ReportScreen: Error creating issue: ${uiState.error}")
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Text(
                        text = "Report Issue",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Report,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                Text(
                                text = "Help us improve your city",
                    style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Report community issues",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Category Selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Issue Type *",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // Dropdown state
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.displayName ?: "Select issue type",
                                onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                                leadingIcon = {
                                    if (selectedCategory != null) {
                                        Surface(
                                            modifier = Modifier.size(16.dp),
                                            shape = CircleShape,
                                            color = getCategoryColor(selectedCategory!!.name)
                                        ) {}
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.Category,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = expanded
                                    )
                                },
                        placeholder = { Text("Choose issue type") },
                        colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (selectedCategory == null) 
                                        MaterialTheme.colorScheme.error 
                                    else MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = if (selectedCategory == null) 
                                        MaterialTheme.colorScheme.error 
                                    else MaterialTheme.colorScheme.outline,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clip(RoundedCornerShape(12.dp))
                    ) {
                        IssueCategory.values().forEach { category ->
                            DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Surface(
                                                    modifier = Modifier.size(12.dp),
                                                    shape = CircleShape,
                                                    color = getCategoryColor(category.name)
                                                ) {}
                                                Text(
                                                    text = category.displayName,
                                                    fontWeight = if (selectedCategory == category) 
                                                        FontWeight.Bold 
                                                    else FontWeight.Normal
                                                )
                                            }
                                        },
                                        leadingIcon = if (selectedCategory == category) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        } else null,
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                                }
                            }
                        }
                    }
                }

                // Severity Selection - Now in a Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                Text(
                                text = "Severity Level *",
                    style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IssueSeverity.values().forEach { severity ->
                                val isSelected = selectedSeverity == severity
                                Card(
                        modifier = Modifier
                                        .weight(1f)
                            .selectable(
                                            selected = isSelected,
                                onClick = { selectedSeverity = severity }
                                        ),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) 
                                            MaterialTheme.colorScheme.secondaryContainer 
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    border = if (isSelected) 
                                        BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
                                    else null,
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = if (isSelected) 4.dp else 0.dp
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val icon = when (severity) {
                                            IssueSeverity.LOW -> Icons.Outlined.Info
                                            IssueSeverity.MEDIUM -> Icons.Outlined.Warning
                                            IssueSeverity.HIGH -> Icons.Outlined.Error
                                        }
                                        val color = when (severity) {
                                            IssueSeverity.LOW -> Color(0xFF4CAF50)
                                            IssueSeverity.MEDIUM -> Color(0xFFFF9800)
                                            IssueSeverity.HIGH -> Color(0xFFF44336)
                                        }
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = color,
                                            modifier = Modifier.size(28.dp)
                                        )
                        Text(
                            text = severity.displayName,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Description Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                Text(
                                text = "Description *",
                    style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                )
                        }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Describe the issue in detail...") },
                    minLines = 3,
                            maxLines = 5,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (description.isEmpty()) 
                                    MaterialTheme.colorScheme.error 
                                else MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = if (description.isEmpty()) 
                                    MaterialTheme.colorScheme.error 
                                else MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Location Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLocationValid) 
                            MaterialTheme.colorScheme.surface 
                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(
                        width = if (isLocationValid) 0.dp else 1.dp,
                        color = if (isLocationValid) Color.Transparent else MaterialTheme.colorScheme.error
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (isLocationValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Location *",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isLocationValid) 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                                    imageVector = if (isLocationValid) Icons.Filled.LocationOn else Icons.Outlined.LocationOff,
                            contentDescription = null,
                                    tint = if (isLocationValid) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                        )
                                Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = location,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            color = if (isLocationValid) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                )
                                if (isLocationValid) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Valid",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Photo Section with Camera Capture - Now in a Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(
                        width = if (capturedImages.isEmpty()) 1.dp else 0.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoCamera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Photos *",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (capturedImages.isEmpty()) {
                                Text(
                                    text = "Required",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        
                        // Inner content area
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (capturedImages.isNotEmpty()) 
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (capturedImages.isEmpty()) {
                            // Camera and Gallery buttons
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Camera button
                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(120.dp)
                                                    .clickable {
                                        println("Camera button clicked")
                                        when {
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.CAMERA
                                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                                                println("Camera permission granted, launching camera")
                                                try {
                                                    val (imageFile, imageUri) = SimpleCameraService.createImageFileAndUri(context)
                                                    currentImageUri = imageUri
                                                    println("Launching camera with URI: $imageUri")
                                                    println("File path: ${imageFile.absolutePath}")
                                                            
                                                            // Grant URI permissions to camera app
                                                            val flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                            
                                                            // Try to grant permissions to common camera apps
                                                            val cameraPackages = listOf(
                                                                "com.android.camera",
                                                                "com.google.android.GoogleCamera",
                                                                "com.samsung.android.camera",
                                                                "com.sec.android.camera"
                                                            )
                                                            cameraPackages.forEach { packageName ->
                                                                try {
                                                                    context.grantUriPermission(
                                                                        packageName,
                                                                        imageUri,
                                                                        flags
                                                                    )
                                                                } catch (e: Exception) {
                                                                    // Ignore - package might not exist
                                                                }
                                                            }
                                                            
                                                    cameraLauncher.launch(imageUri)
                                                } catch (e: Exception) {
                                                    println("Error launching camera: ${e.message}")
                                                    e.printStackTrace()
                                                            validationErrors = listOf("Failed to launch camera: ${e.message}. Please try again or use gallery.")
                                                }
                                            }
                                            else -> {
                                                println("Requesting camera permission")
                                                permissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        }
                                    },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                    ) {
                                        Column(
                                    modifier = Modifier
                                                .fillMaxSize()
                                                .padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                                imageVector = Icons.Default.AddAPhoto,
                                        contentDescription = "Take Photo",
                                                tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                            Text(
                                                text = "Camera",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    // Gallery button
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(120.dp)
                                            .clickable {
                                        println("Gallery button clicked")
                                        imagePickerLauncher.launch("image/*")
                                    },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                    ) {
                                        Column(
                                    modifier = Modifier
                                                .fillMaxSize()
                                                .padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                                imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = "Select from Gallery",
                                                tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                            Text(
                                                text = "Gallery",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                                
                            Text(
                                    text = "Tap to take or select photos",
                                    style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            }
                        } else {
                            // Show captured images with confirmation
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(capturedImages) { imageUri ->
                                    val isConfirmed = confirmedImages.contains(imageUri)
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                width = if (isConfirmed) 3.dp else 1.dp,
                                                color = if (isConfirmed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                    ) {
                                        AsyncImage(
                                            model = imageUri,
                                            contentDescription = "Captured Image",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable {
                                                    selectedImageIndex = capturedImages.indexOf(imageUri)
                                                    showImageViewer = true
                                                },
                                            contentScale = ContentScale.Crop
                                        )
                                        
                                        // Confirmation overlay
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    if (isConfirmed) 
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                                    else 
                                                        Color.Black.copy(alpha = 0.3f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (isConfirmed) {
                                                        // Remove from confirmed
                                                        confirmedImages = confirmedImages.filter { it != imageUri }
                                                    } else {
                                                        // Add to confirmed
                                                        confirmedImages = confirmedImages + imageUri
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (isConfirmed) Icons.Default.Check else Icons.Default.Check,
                                                    contentDescription = if (isConfirmed) "Confirmed" else "Confirm",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }
                                        
                                        // Remove button
                                        IconButton(
                                            onClick = {
                                                capturedImages = capturedImages.filter { it != imageUri }
                                                confirmedImages = confirmedImages.filter { it != imageUri }
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(24.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.error,
                                                    CircleShape
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
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Help text
                            Text(
                                text = "Tap the checkmark to confirm photos for submission (${confirmedImages.size}/${capturedImages.size} confirmed)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Add more photos buttons
                            if (capturedImages.size < 5) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Camera button
                                    Button(
                                        onClick = {
                                            when {
                                                ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.CAMERA
                                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                                                    // Permission already granted, launch camera
                                                    try {
                                                        val (imageFile, imageUri) = SimpleCameraService.createImageFileAndUri(context)
                                                        currentImageUri = imageUri
                                                        println("Add another photo - launching camera with URI: $imageUri")
                                                        println("File path: ${imageFile.absolutePath}")
                                                        
                                                        // Grant URI permissions to camera app
                                                        val flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                        
                                                        // Try to grant permissions to common camera apps
                                                        val cameraPackages = listOf(
                                                            "com.android.camera",
                                                            "com.google.android.GoogleCamera",
                                                            "com.samsung.android.camera",
                                                            "com.sec.android.camera"
                                                        )
                                                        cameraPackages.forEach { packageName ->
                                                            try {
                                                                context.grantUriPermission(
                                                                    packageName,
                                                                    imageUri,
                                                                    flags
                                                                )
                                                            } catch (e: Exception) {
                                                                // Ignore - package might not exist
                                                            }
                                                        }
                                                        
                                                        cameraLauncher.launch(imageUri)
                                                    } catch (e: Exception) {
                                                        println("Error launching camera: ${e.message}")
                                                        e.printStackTrace()
                                                        validationErrors = listOf("Failed to launch camera: ${e.message}. Please try again or use gallery.")
                                                    }
                                                }
                                                else -> {
                                                    // Request permission
                                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Take Photo",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Camera")
                                    }

                                    // Gallery button
                                    Button(
                                        onClick = {
                                            println("Add photo from gallery")
                                            imagePickerLauncher.launch("image/*")
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = "Select from Gallery",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Gallery")
                                    }
                                }
                                    }
                                }
                            }
                        }
                    }
                }

                // Upload captured images to Cloudinary - Only when user clicks submit
                // Removed automatic upload to prevent form reset issues

                // Update uploaded image URLs when upload completes
                LaunchedEffect(uploadState.uploadedImages, uploadState.error, uploadState.isUploading) {
                    // Handle successful upload
                    if (uploadState.uploadedImages.isNotEmpty() && !uploadState.isUploading && isUploadingImages) {
                        uploadedImageUrls = uploadState.uploadedImages.map { it.url }
                        isUploadingImages = false
                        // Close verification popup when upload completes
                        showValidationPopup = false
                        // Submit the report after successful upload
                        delay(300) // Small delay for better UX
                        submitReport()
                    }
                    
                    // Handle upload errors - show in popup
                    if (uploadState.error != null && !uploadState.isUploading) {
                        if (isUploadingImages) {
                            isUploadingImages = false
                        }
                        validationErrors = listOf("Upload failed: ${uploadState.error}. Please try again or use smaller images.")
                        validationMessageType = ValidationMessageType.ERROR
                        showValidationPopup = true
                        // Clear the error after showing it
                        imageUploadViewModel.clearUploadState()
                    }
                }
                
                // Add upload timeout (2 minutes)
                LaunchedEffect(isUploadingImages) {
                    if (isUploadingImages) {
                        delay(120000) // 2 minutes timeout
                        if (uploadState.isUploading) {
                            isUploadingImages = false
                            validationErrors = listOf("Upload timed out. Please check your internet connection and try again with fewer or smaller images.")
                            validationMessageType = ValidationMessageType.ERROR
                            showValidationPopup = true
                            imageUploadViewModel.clearUploadState()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Button
                Button(
                    onClick = {
                        val errors = validateForm()
                        if (errors.isNotEmpty()) {
                            validationErrors = errors
                            validationMessageType = ValidationMessageType.ERROR
                            showValidationPopup = true
                        } else if (confirmedImages.isNotEmpty()) {
                            val firstImageUri = confirmedImages.first()

                            try {
                                val bitmap = MediaStore.Images.Media.getBitmap(
                                    context.contentResolver,
                                    firstImageUri
                                )

                                // Start verification using Gemini before uploading
                                isUploadingImages = true
                                validationErrors = listOf("Verifying image, please wait...")
                                validationMessageType = ValidationMessageType.VERIFYING
                                showValidationPopup = true

                                scope.launch {
                                    try {
                                        val (label, confidence) = verifyIssueWithGemini(bitmap)
                                        println("Gemini result: $label ($confidence%)")

                                        val selected = selectedCategory?.name?.lowercase() ?: ""
                                        
                                        // Normalize both labels for comparison
                                        val normalizedLabel = normalizeCategoryLabel(label)
                                        val normalizedSelected = normalizeCategoryLabel(selected)

                                        // Check if labels match (considering variations)
                                        val labelsMatch = normalizedLabel == normalizedSelected || 
                                                         normalizedLabel.contains(normalizedSelected) || 
                                                         normalizedSelected.contains(normalizedLabel) ||
                                                         areCategoriesMatching(normalizedLabel, normalizedSelected)

                                        if (labelsMatch && confidence >= 70) {
                                            // ✅ Correct issue — proceed to upload
                                            validationErrors = listOf("Verified as ${label.replace("_", " ")} ($confidence%). Uploading...")
                                            validationMessageType = ValidationMessageType.SUCCESS
                                            showValidationPopup = true

                                            imageUploadViewModel.uploadImages(context, confirmedImages)

                                        } else {
                                            // ❌ Mismatch or low confidence
                                            isUploadingImages = false
                                            val displayLabel = label.replace("_", " ").replaceFirstChar { it.uppercaseChar() }
                                            val displaySelected = selected.replace("_", " ").replaceFirstChar { it.uppercaseChar() }
                                            validationErrors = listOf(
                                                "Invalid photo: Detected '$displayLabel' ($confidence%). " +
                                                        "Expected '$displaySelected'. Please retake or select the correct issue photo."
                                            )
                                            validationMessageType = ValidationMessageType.ERROR
                                            showValidationPopup = true
                                        }

                                    } catch (e: Exception) {
                                        isUploadingImages = false
                                        validationErrors = listOf("Verification failed: ${e.message}")
                                        validationMessageType = ValidationMessageType.ERROR
                                        showValidationPopup = true
                                        e.printStackTrace()
                                    }
                                }

                            } catch (e: Exception) {
                                validationErrors = listOf("Could not process the image: ${e.message}")
                                validationMessageType = ValidationMessageType.ERROR
                                showValidationPopup = true
                            }

                        } else {
                            validationErrors = listOf("Please confirm at least one photo before submitting.")
                            validationMessageType = ValidationMessageType.ERROR
                            showValidationPopup = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading && !isUploadingImages
                ) {
                    when {
                        isUploadingImages || uiState.isLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        else -> {
                            Text("Submit Report")
                        }
                    }
                }




                // Error messages removed - now shown in popup only
            }
        }

        // Success popup - rendered at top level
        if (showSuccessPopup) {
            SuccessPopup(
                onDismiss = {
                    showSuccessPopup = false
                },
                // Removed reportId and onViewReport arguments
                onNavigateToHome = onBack
            )
        }
        
        // Validation popup - rendered at top level
        if (showValidationPopup && validationErrors.isNotEmpty()) {
            ValidationPopup(
                messages = validationErrors,
                messageType = validationMessageType,
                onDismiss = {
                    showValidationPopup = false
                    // Clear errors when dismissed
                    validationErrors = emptyList()
                },
                isDismissible = validationMessageType != ValidationMessageType.VERIFYING
            )
        }
        
        // Image viewer for zooming captured images
        ImageViewerDialog(
            imageUrls = capturedImages.map { it.toString() },
            initialIndex = selectedImageIndex,
            isVisible = showImageViewer,
            onDismiss = { showImageViewer = false }
        )
    }
}

// ====================================================================================
// VALIDATION POPUP
// ====================================================================================

@Composable
fun ValidationPopup(
    messages: List<String>,
    messageType: ValidationMessageType,
    onDismiss: () -> Unit,
    isDismissible: Boolean = true
) {
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(600)
    )

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(500, delayMillis = 100)
    )

    // Determine colors and icon based on message type
    val backgroundColor = when (messageType) {
        ValidationMessageType.ERROR -> Color(0xFFFFEBEE) // Light red background
        ValidationMessageType.SUCCESS -> Color(0xFFE3F2FD) // Light blue background
        ValidationMessageType.VERIFYING -> Color(0xFFE3F2FD) // Light blue background
        ValidationMessageType.INFO -> Color(0xFFE3F2FD) // Light blue background
    }
    
    val textColor = when (messageType) {
        ValidationMessageType.ERROR -> Color(0xFFC62828) // Dark red text
        ValidationMessageType.SUCCESS -> Color(0xFF1565C0) // Dark blue text
        ValidationMessageType.VERIFYING -> Color(0xFF1565C0) // Dark blue text
        ValidationMessageType.INFO -> Color(0xFF1565C0) // Dark blue text
    }
    
    val iconColor = when (messageType) {
        ValidationMessageType.ERROR -> Color(0xFFD32F2F) // Red icon
        ValidationMessageType.SUCCESS -> Color(0xFF2196F3) // Blue icon
        ValidationMessageType.VERIFYING -> Color(0xFF2196F3) // Blue icon
        ValidationMessageType.INFO -> Color(0xFF2196F3) // Blue icon
    }
    
    val icon = when (messageType) {
        ValidationMessageType.ERROR -> Icons.Filled.Error
        ValidationMessageType.SUCCESS -> Icons.Filled.CheckCircle
        ValidationMessageType.VERIFYING -> Icons.Filled.Info
        ValidationMessageType.INFO -> Icons.Filled.Info
    }

    // Dark background overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .alpha(alpha)
            .clickable(enabled = isDismissible) {
                if (isDismissible) onDismiss()
            },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .scale(scale)
                .padding(vertical = 24.dp)
                .clickable(enabled = false) {}, // Prevent click through
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(backgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (messageType == ValidationMessageType.VERIFYING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(50.dp),
                            color = iconColor,
                            strokeWidth = 4.dp
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Title
                Text(
                    text = when (messageType) {
                        ValidationMessageType.ERROR -> "Validation Error"
                        ValidationMessageType.SUCCESS -> "Verification Successful"
                        ValidationMessageType.VERIFYING -> "Verifying"
                        ValidationMessageType.INFO -> "Information"
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Messages
                messages.forEach { message ->
                    Text(
                        text = message,
                        fontSize = 16.sp,
                        color = Color(0xFF424242),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Dismiss button (only show if dismissible)
                if (isDismissible) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = iconColor
                        ),
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        Text(
                            text = "OK",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ====================================================================================
// MODIFIED SUCCESS POPUP
// ====================================================================================

@Composable
fun SuccessPopup(
    onDismiss: () -> Unit,
    onNavigateToHome: () -> Unit // Only one action needed: navigate home
) {
    val scope = rememberCoroutineScope()

    // Auto-navigate after 4 seconds
    LaunchedEffect(Unit) {
        delay(4000) // 4 seconds delay
        onDismiss()
        onNavigateToHome()
    }

    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(600)
    )

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(500, delayMillis = 100)
    )

    // Simulate the dark, full-screen background effect
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .scale(scale)
                .padding(vertical = 24.dp), // Adjusted vertical padding for screen fit
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp), // Tighter padding to match design
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- 1. Visual Element: Checkmark Icon with Confetti ---
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00C853)), // Bright Green Circle
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Success",
                        tint = Color.White,
                        modifier = Modifier.size(90.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- 2. Prominent, Cheerful Confirmation Message ---
                Text(
                    text = "Report Submitted!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1B5E20), // Dark Green Text
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Thank you for making a difference.",
                    fontSize = 16.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))
                Divider(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    color = Color(0xFFEEEEEE),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(24.dp))

                // --- 3. Estimated Resolution Time & Redirection Message ---
                Text(
                    text = "Your report will be sent to the Local Authorities immediately.",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Redirection Indicator (Replaced CTA Buttons)
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Redirecting to home screen...",
                        fontSize = 16.sp,
                        color = Color(0xFF999999),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF4CAF50),
                        strokeWidth = 3.dp
                    )
                }
            }
        }
    }
}

// Helper function to get category color
private fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "potholes" -> Color(0xFFE91E63)
        "broken_street_lights" -> Color(0xFFFF9800)
        "garbage" -> Color(0xFF4CAF50)
        "water_logging" -> Color(0xFF2196F3)
        else -> Color(0xFF9E9E9E)
    }
}

// Helper function to normalize category labels for comparison
private fun normalizeCategoryLabel(label: String): String {
    if (label.isBlank()) return ""
    
    // Convert to lowercase and replace spaces with underscores
    var normalized = label.lowercase().trim()
        .replace(" ", "_")
        .replace("-", "_")
    
    // Handle plural/singular variations for potholes
    if (normalized == "pothole") {
        normalized = "potholes"
    }
    
    // Handle variations for broken street lights
    if (normalized.contains("street") && normalized.contains("light")) {
        normalized = "broken_street_lights"
    }
    
    // Handle variations for water logging
    if (normalized.contains("water") && normalized.contains("log")) {
        normalized = "water_logging"
    }
    
    // Handle garbage variations (already singular)
    if (normalized == "garbage" || normalized == "trash" || normalized == "waste") {
        normalized = "garbage"
    }
    
    return normalized
}

// Helper function to check if two category labels match (handling variations)
private fun areCategoriesMatching(label1: String, label2: String): Boolean {
    if (label1.isBlank() || label2.isBlank()) return false
    
    val norm1 = normalizeCategoryLabel(label1)
    val norm2 = normalizeCategoryLabel(label2)
    
    // Exact match after normalization
    if (norm1 == norm2) return true
    
    // Check if one contains the other (for partial matches)
    if (norm1.contains(norm2) || norm2.contains(norm1)) return true
    
    // Special cases for known variations
    val categoryMap = mapOf(
        "potholes" to listOf("pothole", "potholes", "pot_hole", "pot_holes"),
        "water_logging" to listOf("water_logging", "waterlogging", "water_log", "flooding", "flood"),
        "broken_street_lights" to listOf("broken_street_lights", "broken_street_light", "street_light", "street_lights", "streetlight", "streetlights", "broken_light"),
        "garbage" to listOf("garbage", "trash", "waste", "litter", "rubbish")
    )
    
    // Check if both labels belong to the same category group
    for ((standard, variations) in categoryMap) {
        val match1 = variations.any { norm1.contains(it) || it.contains(norm1) }
        val match2 = variations.any { norm2.contains(it) || it.contains(norm2) }
        if (match1 && match2) return true
    }
    
    return false
}