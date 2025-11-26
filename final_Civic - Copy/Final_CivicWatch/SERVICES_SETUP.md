# CivicWatch Services Setup Guide

This guide will help you configure Firebase, Cloudinary, and Mapbox services for your CivicWatch Android app.

## 🔥 Firebase Setup

### Prerequisites
- Firebase project created at [console.firebase.google.com](https://console.firebase.google.com)
- Google OAuth configured for authentication
- Firestore database enabled
- Analytics enabled

### Configuration Steps

1. **Download Configuration File**
   - Go to Project Settings → General → Your apps
   - Download `google-services.json`
   - Place it in `app/` directory (already done)

2. **Enable Authentication**
   - Go to Authentication → Sign-in method
   - Enable Google sign-in
   - Add your app's SHA-1 fingerprint

3. **Setup Firestore**
   - Go to Firestore Database
   - Create database in production mode
   - Set up security rules

4. **Enable Analytics**
   - Analytics is automatically enabled with Firebase
   - No additional configuration needed

### Features Included
- ✅ Google OAuth Authentication
- ✅ Firestore Database for data storage
- ✅ Firebase Analytics for user tracking
- ✅ Firebase Messaging for notifications
- ✅ Cloudinary for all image/file storage (replaces Firebase Storage)

## ☁️ Cloudinary Setup

### Prerequisites
- Cloudinary account at [cloudinary.com](https://cloudinary.com)
- 25GB storage plan (as specified)

### Configuration Steps

1. **Get Your Credentials**
   - Log in to Cloudinary dashboard
   - Go to Dashboard section
   - Copy your:
     - **Cloud Name**
     - **API Key**
     - **API Secret**

2. **Update Configuration**
   
   Edit `app/src/main/res/values/strings.xml`:
   ```xml
   <!-- Replace with your actual Cloudinary credentials -->
   <string name="cloudinary_cloud_name">YOUR_ACTUAL_CLOUD_NAME</string>
   <string name="cloudinary_api_key">YOUR_ACTUAL_API_KEY</string>
   <string name="cloudinary_api_secret">YOUR_ACTUAL_API_SECRET</string>
   ```

   Edit `app/src/main/java/com/example/claudeapp/CivicWatchApplication.kt`:
   ```kotlin
   // Replace with your actual credentials
   val cloudName = "your_actual_cloud_name"
   val apiKey = "your_actual_api_key"
   val apiSecret = "your_actual_api_secret"
   ```

### Features Included
- ✅ 25GB image storage (replaces Firebase Storage)
- ✅ Auto image optimization
- ✅ Multiple format support (JPG, PNG, WebP, GIF)
- ✅ Automatic compression
- ✅ EXIF orientation correction
- ✅ Thumbnail generation
- ✅ Progress tracking during upload
- ✅ Issue image uploads
- ✅ Verification image uploads
- ✅ User profile image uploads

## 🗺️ Mapbox Setup (Temporarily Disabled)

### Current Status
- ⚠️ **Mapbox integration is temporarily disabled** due to repository access requirements
- ✅ **Google Maps is used instead** for map functionality
- 🔄 **Mapbox can be re-enabled** once proper credentials are configured

### Prerequisites (For Future Re-enablement)
- Mapbox account at [mapbox.com](https://mapbox.com)
- Access token with 50,000 map loads/month and 100,000 geocoding requests/month
- Private repository access token for Mapbox SDKs

### Configuration Steps

1. **Get Your Access Token**
   - Log in to Mapbox dashboard
   - Go to Account → Access tokens
   - Copy your default public token or create a new one

2. **Update Configuration**
   
   Edit `app/src/main/res/values/strings.xml`:
   ```xml
   <!-- Replace with your actual Mapbox access token -->
   <string name="mapbox_access_token">YOUR_ACTUAL_MAPBOX_ACCESS_TOKEN</string>
   ```

   Edit `app/src/main/java/com/example/claudeapp/CivicWatchApplication.kt`:
   ```kotlin
   // Replace with your actual access token
   val mapboxAccessToken = "your_actual_mapbox_access_token"
   ```

### Features Included (When Re-enabled)
- ✅ 50,000 map loads/month
- ✅ 100,000 geocoding requests/month
- ✅ Interactive maps with Mapbox Streets style
- ✅ Forward geocoding (address → coordinates)
- ✅ Reverse geocoding (coordinates → address)
- ✅ Place search functionality
- ✅ Custom markers for issue statuses

### Current Features (Google Maps)
- ✅ Interactive maps with Google Maps
- ✅ Issue markers on map
- ✅ Location-based issue display
- ✅ Map navigation and zoom controls

## 🚀 Testing Your Setup

### 1. Build and Run
```bash
./gradlew assembleDebug
```

### 2. Test Each Service

**Firebase Authentication:**
- Navigate to the app
- Try signing in with Google
- Check Firebase console for user creation

**Cloudinary Image Upload (All Storage):**
- Go to Report screen
- Try uploading an image for an issue
- Try uploading verification images
- Check Cloudinary dashboard for uploads
- Verify images are stored in organized folders

**Maps (Google Maps):**
- Navigate to Map screen
- Verify Google Maps loads correctly
- Test issue markers display
- Test map navigation and zoom

### 3. Verify in Dashboards

- **Firebase Console:** Check users, Firestore data, analytics (no storage needed)
- **Cloudinary Dashboard:** Verify all image uploads and storage usage (replaces Firebase Storage)
- **Google Cloud Console:** Monitor Google Maps API usage and requests

## 🔒 Security Best Practices

### For Production Apps

1. **Never hardcode credentials in source code**
2. **Use BuildConfig for sensitive data**
3. **Implement proper access controls**
4. **Use environment-specific configurations**

### Recommended BuildConfig Setup

Add to your `app/build.gradle.kts`:

```kotlin
android {
    buildTypes {
        debug {
            buildConfigField("String", "FIREBASE_PROJECT_ID", "\"your-debug-project-id\"")
            buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"your-debug-cloud-name\"")
            buildConfigField("String", "CLOUDINARY_API_KEY", "\"your-debug-api-key\"")
            buildConfigField("String", "CLOUDINARY_API_SECRET", "\"your-debug-api-secret\"")
            buildConfigField("String", "MAPBOX_ACCESS_TOKEN", "\"your-debug-access-token\"")
        }
        release {
            buildConfigField("String", "FIREBASE_PROJECT_ID", "\"your-production-project-id\"")
            buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"your-production-cloud-name\"")
            buildConfigField("String", "CLOUDINARY_API_KEY", "\"your-production-api-key\"")
            buildConfigField("String", "CLOUDINARY_API_SECRET", "\"your-production-api-secret\"")
            buildConfigField("String", "MAPBOX_ACCESS_TOKEN", "\"your-production-access-token\"")
        }
    }
}
```

## 📊 Usage Monitoring

### Firebase
- Monitor user authentication in Firebase Console
- Track app usage in Analytics
- Monitor Firestore read/write operations
- No storage monitoring needed (using Cloudinary)

### Cloudinary
- Monitor storage usage in dashboard (all file storage)
- Track bandwidth consumption
- Set up usage alerts
- Monitor upload success rates

### Google Maps
- Monitor API usage in Google Cloud Console
- Track map loads and requests
- Set up usage limits and alerts

## 🆘 Troubleshooting

### Common Issues

1. **Firebase Authentication fails**
   - Verify SHA-1 fingerprint is added
   - Check Google OAuth configuration
   - Ensure `google-services.json` is in correct location

2. **Cloudinary uploads fail (all image storage)**
   - Verify API credentials are correct
   - Check network connectivity
   - Ensure Cloudinary account is active
   - Check storage quota limits

3. **Google Maps don't load**
   - Verify Google Maps API key is correct
   - Check internet connectivity
   - Ensure Google Cloud project has Maps API enabled

4. **Build errors**
   - Clean and rebuild project
   - Check all dependencies are properly added
   - Verify all configuration files are in place

### Debug Mode

Enable debug logging in your Application class:

```kotlin
// Enable Cloudinary debug mode
MediaManager.get().setLogLevel(Log.DEBUG)

// Enable Mapbox debug mode
Mapbox.getInstance(this, accessToken)
```

## 📚 Additional Resources

- [Firebase Android Documentation](https://firebase.google.com/docs/android/setup)
- [Cloudinary Android SDK](https://cloudinary.com/documentation/android_integration)
- [Mapbox Android SDK](https://docs.mapbox.com/android/maps/guides/)
- [Hilt Dependency Injection](https://dagger.dev/hilt/)

## ✅ Verification Checklist

- [ ] Firebase project created and configured
- [ ] `google-services.json` added to app directory
- [ ] Google OAuth enabled in Firebase Console
- [ ] Firestore database created
- [ ] Cloudinary account created with 25GB storage
- [ ] Cloudinary credentials added to strings.xml and Application class
- [ ] Mapbox account created with required quotas
- [ ] Mapbox access token added to strings.xml and Application class
- [ ] App builds successfully
- [ ] All three services tested and working
- [ ] Usage monitoring set up in all dashboards

## 🎉 You're All Set!

Your CivicWatch app now has:
- ✅ Firebase Authentication (Google OAuth)
- ✅ Firestore Database
- ✅ Analytics
- ✅ Cloudinary (25GB image storage + auto optimization - replaces Firebase Storage)
- ✅ Google Maps (interactive maps with issue markers)

All services are properly integrated and ready for production use! Cloudinary handles all image and file storage needs, providing better optimization and more storage capacity than Firebase Storage. Google Maps provides reliable map functionality while Mapbox integration can be re-enabled later with proper credentials.
