package com.example.claudeapp

import android.app.Application
import com.example.claudeapp.data.mapbox.MapboxConfig
import com.example.claudeapp.data.mapbox.MapboxRepository
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CivicWatchApplication : Application() {
    
    
    @Inject
    lateinit var mapboxConfig: MapboxConfig
    
    @Inject
    lateinit var mapboxRepository: MapboxRepository
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        try {
            // Initialize Mapbox when available
            val mapboxToken = getString(R.string.mapbox_access_token)
            if (mapboxToken != "YOUR_MAPBOX_ACCESS_TOKEN") {
                try {
                    mapboxConfig.initialize(this, mapboxToken)
                    // Mapbox Search SDK initialization is temporarily disabled
                    // mapboxRepository.initializeSearchEngine(this)
                } catch (e: Exception) {
                    // Mapbox initialization failed, but app can still work
                    android.util.Log.w("CivicWatch", "Mapbox initialization failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            // Log error but don't crash the app
            android.util.Log.e("CivicWatch", "Application initialization error: ${e.message}")
        }
    }
}

