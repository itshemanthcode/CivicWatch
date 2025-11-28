package com.example.claudeapp.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.example.claudeapp.data.service.GeocodingService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geocodingService: GeocodingService
) {
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    private val geocoder = Geocoder(context, Locale.getDefault())

    suspend fun getCurrentLocation(): LocationResult {
        return try {
            // Check location permission
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && 
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return LocationResult.Error("Location permission not granted")
            }

            // Get current location
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).await()

            if (location != null) {
                // Get detailed address information using Mapbox Geocoding
                val addressInfo = getAddressFromLocation(location.latitude, location.longitude)
                LocationResult.Success(location, addressInfo)
            } else {
                LocationResult.Error("Unable to get current location")
            }
        } catch (e: Exception) {
            LocationResult.Error("Location error: ${e.message}")
        }
    }

    private suspend fun getAddressFromLocation(latitude: Double, longitude: Double): GeocodingService.AddressInfo {
        return try {
            // Try Mapbox Geocoding first for better results
            val mapboxResult = geocodingService.reverseGeocode(latitude, longitude)
            if (mapboxResult.isSuccess) {
                mapboxResult.getOrThrow()
            } else {
                // Fallback to Android Geocoder
                getAddressFromAndroidGeocoder(latitude, longitude)
            }
        } catch (e: Exception) {
            // Fallback to Android Geocoder if Mapbox fails
            getAddressFromAndroidGeocoder(latitude, longitude)
        }
    }
    
    private fun getAddressFromAndroidGeocoder(latitude: Double, longitude: Double): GeocodingService.AddressInfo {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var addressLine = "Location: $latitude, $longitude"
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        addressLine = addresses[0].getAddressLine(0) ?: "Location: $latitude, $longitude"
                    }
                }
                GeocodingService.AddressInfo(
                    placeName = addressLine,
                    area = "Unknown Area",
                    fullAddress = addressLine,
                    city = null,
                    state = null,
                    country = null,
                    postalCode = null
                )
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val addressLine = if (addresses?.isNotEmpty() == true) {
                    addresses[0].getAddressLine(0) ?: "Location: $latitude, $longitude"
                } else {
                    "Location: $latitude, $longitude"
                }
                GeocodingService.AddressInfo(
                    placeName = addressLine,
                    area = "Unknown Area",
                    fullAddress = addressLine,
                    city = null,
                    state = null,
                    country = null,
                    postalCode = null
                )
            }
        } catch (e: Exception) {
            GeocodingService.AddressInfo(
                placeName = "Location: $latitude, $longitude",
                area = "Unknown Area",
                fullAddress = "Location: $latitude, $longitude",
                city = null,
                state = null,
                country = null,
                postalCode = null
            )
        }
    }
}

sealed class LocationResult {
    data class Success(val location: Location, val addressInfo: GeocodingService.AddressInfo) : LocationResult()
    data class Error(val message: String) : LocationResult()
}
