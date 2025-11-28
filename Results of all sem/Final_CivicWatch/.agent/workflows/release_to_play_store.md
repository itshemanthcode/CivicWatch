---
description: How to release the app to the Google Play Store
---

# Release to Google Play Store

Follow these steps to publish your CivicWatch app to the Google Play Store.

## 1. Prepare for Release

1.  **Update Version Code and Name**:
    Open `app/build.gradle.kts` and increment `versionCode` and update `versionName`.
    ```kotlin
    defaultConfig {
        // ...
        versionCode = 2 // Increment this
        versionName = "1.1.0" // Update this
    }
    ```

2.  **Create a Keystore**:
    If you haven't already, create a signed keystore file.
    *   In Android Studio: `Build` > `Generate Signed Bundle / APK...`
    *   Select `Android App Bundle`.
    *   Click `Create new...` under Key store path.
    *   Fill in the details and save the `.jks` file securely. **Do not lose this file!**

3.  **Configure Signing in Gradle** (Optional but recommended):
    Add your signing config to `app/build.gradle.kts` (be careful not to commit secrets to git).

## 2. Build the App Bundle

Run the following command to build the release bundle (`.aab` file):

```bash
./gradlew bundleRelease
```

The output file will be located at:
`app/build/outputs/bundle/release/app-release.aab`

## 3. Create a Google Play Console Account

1.  Go to [Google Play Console](https://play.google.com/console).
2.  Sign in with your Google account.
3.  Pay the one-time registration fee ($25).

## 4. Create the App Listing

1.  Click **Create app**.
2.  Enter the **App name** (CivicWatch).
3.  Select **Default language** (English).
4.  Select **App** (not Game).
5.  Select **Free**.
6.  Accept the declarations.

## 5. Set up the Store Listing

1.  **Main Store Listing**:
    *   Short description: "Report and track civic issues in your community."
    *   Full description: Describe the features (reporting, map, voting, etc.).
    *   Graphics: Upload app icon (512x512), feature graphic (1024x500), and phone screenshots.

2.  **Data Safety**:
    *   Fill out the Data Safety form.
    *   Disclose that you collect Location (Coarse/Fine) for app functionality.
    *   Disclose that you collect User ID (Firebase Auth) for account management.
    *   Disclose that you collect Photos/Videos for issue reporting.

## 6. Upload the Bundle

1.  Go to **Production** (or **Internal testing** first).
2.  Click **Create new release**.
3.  Upload the `app-release.aab` file you generated in Step 2.
4.  Enter release notes (e.g., "Initial release with global support").
5.  Click **Next** and review any warnings.

## 7. Rollout

1.  Once reviewed, click **Start rollout to Production**.
2.  Google will review your app (can take a few days).
3.  Once approved, your app will be available to everyone across the globe!

## 8. Localization (Optional)

Since we added Spanish support:
1.  In the Play Console, go to **Store presence** > **Store listing experiments** or **Main store listing**.
2.  Click **Manage translations**.
3.  Add **Spanish** and provide the translated title, descriptions, and screenshots if they differ.
