package com.example.claudeapp.data.mapbox

import android.content.Context
// import com.mapbox.maps.Mapbox // Temporarily disabled
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapboxConfig @Inject constructor() {
    
    fun initialize(context: Context, accessToken: String) {
        // Mapbox.getInstance(context, accessToken) // Temporarily disabled
        // TODO: Re-enable when Mapbox dependencies are properly configured
    }
    
    fun initializeFromResources(context: Context) {
        // Temporarily disabled - will be re-enabled when Mapbox is configured
        /*
        val accessToken = context.getString(
            context.resources.getIdentifier(
                "mapbox_access_token",
                "string",
                context.packageName
            )
        )
        Mapbox.getInstance(context, accessToken)
        */
    }
}
