---
description: How to distribute the app for free (Sideloading)
---

# Free Distribution Guide (Sideloading)

This guide explains how to distribute your CivicWatch app directly to users without using the Google Play Store. This method is free and allows anyone with an Android device to install your app.

## 1. Build the APK

You need to generate an APK (Android Package Kit) file.

1.  **Open Terminal** in your project root.
2.  **Run the build command**:
    ```bash
    ./gradlew assembleDebug
    ```
    *Note: We are using the debug build for simplicity. It works on all devices but will show a security warning on installation.*

3.  **Locate the APK**:
    After the build finishes, find the file at:
    `app/build/outputs/apk/debug/app-debug.apk`

## 2. Host the APK

You need a place to host the file so users can download it.

### Option A: GitHub Releases (Recommended)
If your code is on GitHub:
1.  Go to your repository on GitHub.
2.  Click **Releases** > **Draft a new release**.
3.  Tag version: `v1.0.0`.
4.  Release title: `CivicWatch v1.0.0`.
5.  **Attach binaries**: Drag and drop the `app-debug.apk` file here.
6.  Click **Publish release**.
7.  **Share the link**: Send the release URL to your users.

### Option B: Google Drive / Dropbox
1.  Upload `app-debug.apk` to your Google Drive or Dropbox.
2.  Right-click the file and select **Share**.
3.  Change access to **"Anyone with the link"**.
4.  **Share the link**: Send this link to your users.

### Option C: Firebase App Distribution (Advanced)
1.  Go to the [Firebase Console](https://console.firebase.google.com/).
2.  Select your project.
3.  Go to **App Distribution** in the left menu.
4.  Upload your APK.
5.  Add tester emails to send them an invite.

## 3. How Users Install It

Since the app is not from the Play Store, users need to enable "Unknown Sources".

**Instructions for Users:**
1.  **Download** the APK file from the link you shared.
2.  Tap the downloaded file to open it.
3.  **Security Warning**: You will see a prompt saying "For your security, your phone is not allowed to install unknown apps from this source."
4.  Tap **Settings**.
5.  Toggle **Allow from this source** to ON.
6.  Go back and tap **Install**.
7.  Tap **Open** to launch CivicWatch!

## 4. Important Notes

*   **Updates**: To update the app, you must build a new APK (with a higher `versionCode` in `build.gradle.kts`) and users must download and install it again.
*   **Security**: Since this is a debug build signed with a debug key, users might see warnings. For a more professional approach without the Play Store, you can generate a self-signed Release APK, but the installation process is similar.
