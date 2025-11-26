# Cloudinary Integration Setup Guide

This guide will help you set up Cloudinary for image storage in your CivicWatch Android app.

## Prerequisites

1. A Cloudinary account (sign up at [cloudinary.com](https://cloudinary.com))
2. Your Cloudinary credentials (Cloud Name, API Key, API Secret)

## Step 1: Get Your Cloudinary Credentials

1. Log in to your Cloudinary dashboard
2. Go to the Dashboard section
3. Copy your:
   - **Cloud Name**
   - **API Key** 
   - **API Secret**

## Step 2: Configure Your App

### Update String Resources

Edit `app/src/main/res/values/strings.xml` and replace the placeholder values:

```xml
<!-- Cloudinary Configuration - Replace with your actual credentials -->
<string name="cloudinary_cloud_name">YOUR_ACTUAL_CLOUD_NAME</string>
<string name="cloudinary_api_key">YOUR_ACTUAL_API_KEY</string>
<string name="cloudinary_api_secret">YOUR_ACTUAL_API_SECRET</string>
```

### Update Application Class

Edit `app/src/main/java/com/civicwatch/app/CivicWatchApplication.kt` and replace the placeholder values:

```kotlin
// Initialize Cloudinary
val cloudName = "your_actual_cloud_name" // Replace with your cloud name
val apiKey = "your_actual_api_key" // Replace with your API key
val apiSecret = "your_actual_api_secret" // Replace with your API secret
```

## Step 3: Features Included

### ✅ Image Upload
- Automatic image compression before upload
- Support for multiple image formats (JPG, PNG, WebP, GIF)
- Progress tracking during upload
- Error handling and retry functionality

### ✅ Image Processing
- Automatic image optimization
- EXIF orientation correction
- Resize images to optimal dimensions
- Quality optimization

### ✅ Image Management
- Generate optimized URLs for different sizes
- Thumbnail generation
- Public ID management for easy deletion/updates

### ✅ UI Components
- `ImageUploadComponent` - Ready-to-use image upload UI
- Progress indicators
- Image preview with remove functionality
- Error state handling

## Step 4: Usage Examples

### Basic Image Upload

```kotlin
@Composable
fun MyScreen(
    imageUploadViewModel: ImageUploadViewModel = hiltViewModel()
) {
    ImageUploadComponent(
        viewModel = imageUploadViewModel,
        maxImages = 5,
        onImagesUploaded = { imageUrls ->
            // Handle uploaded image URLs
            println("Uploaded images: $imageUrls")
        }
    )
}
```

### Manual Image Upload

```kotlin
// In your ViewModel or Repository
suspend fun uploadImage(context: Context, imageUri: Uri) {
    val result = cloudinaryRepository.uploadImage(
        context = context,
        imageUri = imageUri,
        folder = "my-app/images",
        transformations = mapOf(
            "quality" to "auto",
            "fetch_format" to "auto"
        )
    )
    
    when (result) {
        is Result.success -> {
            val uploadResult = result.getOrNull()!!
            println("Upload successful: ${uploadResult.secureUrl}")
        }
        is Result.failure -> {
            println("Upload failed: ${result.exceptionOrNull()?.message}")
        }
    }
}
```

### Generate Optimized URLs

```kotlin
// Generate thumbnail URL
val thumbnailUrl = cloudinaryRepository.generateThumbnailUrl(
    publicId = "my-image-id",
    size = 200
)

// Generate optimized image URL
val optimizedUrl = cloudinaryRepository.generateOptimizedImageUrl(
    publicId = "my-image-id",
    width = 800,
    height = 600,
    quality = "auto",
    format = "auto"
)
```

## Step 5: Security Best Practices

### For Production Apps

1. **Never hardcode credentials in source code**
2. **Use environment variables or secure configuration**
3. **Consider using Cloudinary's unsigned uploads for client-side uploads**
4. **Implement proper access controls**

### Recommended Approach

```kotlin
// Use BuildConfig for sensitive data
val cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME
val apiKey = BuildConfig.CLOUDINARY_API_KEY
val apiSecret = BuildConfig.CLOUDINARY_API_SECRET
```

Add to your `build.gradle.kts`:

```kotlin
android {
    buildTypes {
        debug {
            buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"your_cloud_name\"")
            buildConfigField("String", "CLOUDINARY_API_KEY", "\"your_api_key\"")
            buildConfigField("String", "CLOUDINARY_API_SECRET", "\"your_api_secret\"")
        }
        release {
            buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"your_production_cloud_name\"")
            buildConfigField("String", "CLOUDINARY_API_KEY", "\"your_production_api_key\"")
            buildConfigField("String", "CLOUDINARY_API_SECRET", "\"your_production_api_secret\"")
        }
    }
}
```

## Step 6: Testing

1. **Build and run your app**
2. **Navigate to the Report screen**
3. **Try uploading images using the new upload component**
4. **Check your Cloudinary dashboard to verify uploads**

## Troubleshooting

### Common Issues

1. **Upload fails with authentication error**
   - Verify your API credentials are correct
   - Check that your Cloudinary account is active

2. **Images not displaying**
   - Ensure you're using the correct URL format
   - Check network connectivity

3. **Large file uploads fail**
   - The app automatically compresses images, but very large files might still fail
   - Consider implementing chunked uploads for very large files

### Debug Mode

Enable debug logging by adding this to your Application class:

```kotlin
// Enable Cloudinary debug mode
MediaManager.get().setLogLevel(Log.DEBUG)
```

## Additional Resources

- [Cloudinary Android SDK Documentation](https://cloudinary.com/documentation/android_integration)
- [Cloudinary Image Transformations](https://cloudinary.com/documentation/image_transformations)
- [Cloudinary Best Practices](https://cloudinary.com/documentation/best_practices)

## Support

If you encounter any issues:
1. Check the Cloudinary dashboard for upload logs
2. Review the Android logs for error messages
3. Verify your network connectivity
4. Ensure your Cloudinary account has sufficient storage quota
