  # CivicWatch - Civic Engagement Platform

CivicWatch is a mobile-first civic engagement platform that enables citizens to report, track, and resolve local infrastructure issues through community-driven verification and direct escalation to local authorities.

## Features

### Core Features (MVP)
- **User Authentication**: Google OAuth integration
- **Issue Reporting**: Camera capture, GPS location, categorization
- **Community Verification**: Upvoting and photo verification system
- **Issue Tracking**: Real-time status updates and notifications
- **Interactive Maps**: Geographic visualization of issues
- **Admin Dashboard**: Web-based management interface for authorities

### Advanced Features (Future)
- **Gamification**: Points, badges, and leaderboards
- **Offline Support**: Queue reports when offline
- **Push Notifications**: Real-time updates
- **Analytics Dashboard**: Performance metrics and insights

## Technical Stack

- **Frontend**: Android (Kotlin + Jetpack Compose)
- **Backend**: Firebase (Firestore, Authentication, Analytics)
- **Storage**: Cloudinary (25GB image storage + auto optimization)
- **Maps**: Mapbox (50,000 map loads/month + 100,000 geocoding requests/month)
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Hilt

## Project Structure

```
app/src/main/java/com/civicwatch/app/
├── data/
│   ├── model/           # Data models (User, Issue, etc.)
│   └── repository/      # Repository classes for data access
├── ui/
│   ├── components/      # Reusable UI components
│   ├── screens/         # Screen composables
│   ├── navigation/      # Navigation setup
│   ├── theme/          # Material Design theme
│   └── viewmodel/      # ViewModels for state management
└── MainActivity.kt     # Main activity
```

## Setup Instructions

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24+ (Android 7.0)
- Google Cloud Platform account
- Firebase project

### 1. Firebase Setup

1. Create a new Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Enable the following services:
   - Authentication (Google provider)
   - Firestore Database
   - Analytics
   - Messaging (for notifications)
3. Download `google-services.json` and replace the placeholder file in `app/`
4. Configure Authentication:
   - Go to Authentication > Sign-in method
   - Enable Google sign-in
   - Add your app's SHA-1 fingerprint

### 2. Mapbox Setup

1. Create a Mapbox account at [mapbox.com](https://mapbox.com)
2. Get your access token from the dashboard
3. Replace `YOUR_MAPBOX_ACCESS_TOKEN` in `app/src/main/res/values/strings.xml`
4. Update the Application class with your access token

### 3. Cloudinary Setup

1. Create a Cloudinary account at [cloudinary.com](https://cloudinary.com)
2. Get your credentials (Cloud Name, API Key, API Secret)
3. Replace the placeholder values in `app/src/main/res/values/strings.xml`
4. Update the Application class with your credentials

### 4. Build Configuration

1. Update the following in `app/build.gradle.kts`:
   - Replace Firebase configuration with your actual project details
   - Update package name if needed

2. Update `app/src/main/res/values/strings.xml`:
   - Replace `YOUR_MAPBOX_ACCESS_TOKEN` with your actual access token
   - Replace Cloudinary credentials with your actual values

3. Update `app/src/main/java/com/example/claudeapp/CivicWatchApplication.kt`:
   - Replace placeholder credentials with your actual values

> **📋 For detailed setup instructions, see [SERVICES_SETUP.md](SERVICES_SETUP.md)**

### 5. Permissions

The app requires the following permissions (already configured in AndroidManifest.xml):
- `CAMERA` - For capturing issue photos
- `ACCESS_FINE_LOCATION` - For GPS location
- `ACCESS_COARSE_LOCATION` - For network-based location
- `INTERNET` - For API calls
- `POST_NOTIFICATIONS` - For push notifications

## Data Models

### User
```kotlin
data class User(
    val userId: String,
    val email: String,
    val displayName: String,
    val photoURL: String,
    val points: Int,
    val badges: List<String>,
    val reportsCount: Int,
    val resolvedCount: Int,
    val reputation: Int,
    val notificationSettings: NotificationSettings
)
```

### Issue
```kotlin
data class Issue(
    val issueId: String,
    val reportedBy: String,
    val category: String,
    val severity: String,
    val description: String,
    val status: String,
    val location: IssueLocation,
    val images: List<String>,
    val upvotes: Int,
    val verifications: Int,
    val priorityScore: Double
)
```

## API Endpoints

The app uses Firebase Firestore for data storage. Key collections:
- `users` - User profiles and settings
- `issues` - Reported civic issues
- `verifications` - Community verifications
- `authorities` - Government authority accounts
- `notifications` - Push notifications

## Development Phases

### Phase 1: MVP (Current)
- [x] Project setup and configuration
- [x] Data models and repository layer
- [x] Basic UI screens (Auth, Home, Report)
- [ ] Google OAuth integration
- [ ] Camera and location services
- [ ] Issue reporting flow
- [ ] Basic admin dashboard

### Phase 2: Community Features
- [ ] Photo verification system
- [ ] Duplicate detection
- [ ] Gamification (points, badges)
- [ ] Push notifications
- [ ] Enhanced filtering

### Phase 3: Scale & Optimize
- [ ] Offline mode with sync
- [ ] Advanced analytics
- [ ] Performance optimization
- [ ] Content moderation tools
- [ ] Multi-language support

## Testing

### Unit Tests
- Repository layer tests
- ViewModel tests
- Utility function tests

### Integration Tests
- Firebase integration tests
- API endpoint tests

### UI Tests
- Screen navigation tests
- User interaction tests

## Deployment

### Development
1. Build debug APK: `./gradlew assembleDebug`
2. Install on device: `adb install app-debug.apk`

### Production
1. Configure release signing
2. Build release APK: `./gradlew assembleRelease`
3. Upload to Google Play Store

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions:
- Create an issue in the GitHub repository
- Contact the development team

## Roadmap

### Short Term (3 months)
- Complete MVP features
- Launch in 3 pilot cities
- Partner with 5+ local authorities

### Medium Term (6 months)
- Scale to 20+ cities
- Implement advanced features
- Achieve 50,000+ active users

### Long Term (1 year)
- National expansion
- API for third-party integrations
- Advanced analytics and insights
- Revenue model implementation

---

**Version**: 1.0.0  
**Last Updated**: October 2024  
**Next Review**: November 2024

