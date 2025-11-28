plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("dagger.hilt.android.plugin")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.claudeapp"
    // Consider using the latest stable SDK: 34 or 35
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.claudeapp"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        // Use Java 17+ if possible, but 11 is acceptable if older libraries demand it.
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    // Compose BOM: Always import first for version coherence
    implementation(platform(libs.androidx.compose.bom))

    // Core Compose UI, Graphics, Tooling, Material3
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3) // Material 3 is included via BOM

    // FIX: Include Extended Icons without specifying version if using BOM
    implementation("androidx.compose.material:material-icons-extended")

    // Compose Pager for swipeable image galleries
    implementation("com.google.accompanist:accompanist-pager:0.32.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.32.0")

    // Navigation
    // Check if libs.androidx.navigation.compose exists, otherwise use this:
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Mapbox Maps SDK and NDK dependency (Standard structure)
    // NOTE: Replace <mapbox_version> with the actual version (e.g., 11.15.2)
    implementation("com.mapbox.maps:android:11.15.2")
    // implementation("com.mapbox.maps:plugin-annotation:11.15.2") // Uncomment if you use the plugin

    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Google Play Services (Auth, Location)
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Camera and Image Processing (Consider using the BOM versions)
    implementation("androidx.camera:camera-core:1.4.0")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")
    implementation("androidx.camera:camera-extensions:1.4.0")

    // Image Loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // ExifInterface for image metadata
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Permissions (Google Accompanist)
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // ImgBB API for image hosting
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Mapbox Geocoding API
    implementation("com.mapbox.mapboxsdk:mapbox-sdk-geojson:6.9.0")
    implementation("com.mapbox.mapboxsdk:mapbox-sdk-services:6.9.0")

    // OkHttp for HTTP requests
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    //AI Gemini
    implementation("com.google.ai.client.generativeai:generativeai:0.7.0")

    // Add this for your local tests
    testImplementation("org.mockito:mockito-core:5.11.0")


}