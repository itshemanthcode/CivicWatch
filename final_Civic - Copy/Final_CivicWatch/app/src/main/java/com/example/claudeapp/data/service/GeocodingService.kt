package com.example.claudeapp.data.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeocodingService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    data class AddressInfo(
        val placeName: String,
        val area: String,
        val fullAddress: String,
        val city: String?,
        val state: String?,
        val country: String?,
        val postalCode: String?
    )
    
    // Get Maps.co API key from resources
    private val mapsCoApiKey = context.getString(com.example.claudeapp.R.string.maps_co_api_key)
    
    private val httpClient = OkHttpClient()
    
    // Cache for geocoding results (thread-safe)
    private val geocodingCache = ConcurrentHashMap<String, AddressInfo>()
    
    // Cache key generation
    private fun getCacheKey(latitude: Double, longitude: Double): String {
        // Round to 4 decimal places for caching (about 11m precision)
        val roundedLat = String.format("%.4f", latitude)
        val roundedLng = String.format("%.4f", longitude)
        return "${roundedLat},${roundedLng}"
    }
    
    suspend fun reverseGeocode(latitude: Double, longitude: Double): Result<AddressInfo> {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                val cacheKey = getCacheKey(latitude, longitude)
                val cachedResult = geocodingCache[cacheKey]
                if (cachedResult != null) {
                    return@withContext Result.success(cachedResult)
                }
                
                // If not in cache, make API call
                val url = "https://geocode.maps.co/reverse?lat=$latitude&lon=$longitude&api_key=$mapsCoApiKey"
                
                val request = Request.Builder()
                    .url(url)
                    .build()
                
                val response: Response = httpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val addressInfo = parseMapsCoResponse(responseBody)
                        // Cache the result
                        geocodingCache[cacheKey] = addressInfo
                        Result.success(addressInfo)
                    } else {
                        Result.failure(Exception("Empty response from Maps.co API"))
                    }
                } else {
                    Result.failure(Exception("Maps.co API error: ${response.code} - ${response.message}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private fun parseMapsCoResponse(jsonResponse: String): AddressInfo {
        val jsonObject = JSONObject(jsonResponse)
        
        // Maps.co API returns a single address object, not an array
        if (jsonObject.has("display_name")) {
            val displayName = jsonObject.optString("display_name", "Unknown Location")
            val address = jsonObject.optJSONObject("address")
            
            // Extract area information (neighborhood, suburb, or city)
            val area = address?.optString("neighbourhood") 
                ?: address?.optString("suburb")
                ?: address?.optString("city")
                ?: address?.optString("town")
                ?: "Unknown Area"
            
            // Extract detailed location components
            val city = address?.optString("city") 
                ?: address?.optString("town")
                ?: address?.optString("village")
            
            val state = address?.optString("state")
            val country = address?.optString("country")
            val postalCode = address?.optString("postcode")
            
            return AddressInfo(
                placeName = displayName,
                area = area,
                fullAddress = displayName,
                city = city?.takeIf { it.isNotEmpty() },
                state = state?.takeIf { it.isNotEmpty() },
                country = country?.takeIf { it.isNotEmpty() },
                postalCode = postalCode?.takeIf { it.isNotEmpty() }
            )
        } else {
            return AddressInfo(
                placeName = "Unknown Location",
                area = "Unknown Area",
                fullAddress = "Unknown Address",
                city = null,
                state = null,
                country = null,
                postalCode = null
            )
        }
    }
    
    /**
     * Get a formatted address string for display
     */
    fun formatAddress(addressInfo: AddressInfo): String {
        return buildString {
            // Prefer area when present
            if (addressInfo.area.isNotEmpty() && addressInfo.area != "Unknown Area") {
                append(addressInfo.area)
            } else if (addressInfo.placeName.isNotEmpty() && addressInfo.placeName != "Unknown Location") {
                append(addressInfo.placeName)
            }
            
            if (addressInfo.city != null && addressInfo.city.isNotEmpty() && addressInfo.city != addressInfo.area) {
                if (isNotEmpty()) append(", ")
                append(addressInfo.city)
            }
            
            if (addressInfo.state != null && addressInfo.state.isNotEmpty()) {
                if (isNotEmpty()) append(", ")
                append(addressInfo.state)
            }
            
            if (addressInfo.country != null && addressInfo.country.isNotEmpty()) {
                if (isNotEmpty()) append(", ")
                append(addressInfo.country)
            }
        }.ifEmpty { "Unknown Location" }
    }
    
    /**
     * Get a short address string (area + city)
     */
    fun formatShortAddress(addressInfo: AddressInfo): String {
        return buildString {
            if (addressInfo.area.isNotEmpty() && addressInfo.area != "Unknown Area") {
                append(addressInfo.area)
            }
            
            if (addressInfo.city != null && addressInfo.city.isNotEmpty() && addressInfo.city != addressInfo.area) {
                if (isNotEmpty()) append(", ")
                append(addressInfo.city)
            }
        }.ifEmpty { addressInfo.city ?: addressInfo.state ?: addressInfo.country ?: "Unknown Area" }
    }
    
    /**
     * Clear the geocoding cache
     */
    fun clearCache() {
        geocodingCache.clear()
    }
    
    /**
     * Get cache size for debugging
     */
    fun getCacheSize(): Int {
        return geocodingCache.size
    }
}
