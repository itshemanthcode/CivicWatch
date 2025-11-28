package com.example.claudeapp.data.mapbox

import android.content.Context
// Temporarily disabled Mapbox imports
// import com.mapbox.geocoding.v5.GeocodingCriteria
// import com.mapbox.geocoding.v5.MapboxGeocoding
// import com.mapbox.geocoding.v5.models.GeocodingResponse
// import com.mapbox.search.MapboxSearchSdk
// import com.mapbox.search.SearchEngine
// import com.mapbox.search.SearchEngineSettings
// import com.mapbox.search.result.SearchResult
// import com.mapbox.search.result.SearchSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapboxRepository @Inject constructor() {
    
    // Temporarily disabled - will be re-enabled when Mapbox dependencies are configured
    /*
    private var searchEngine: SearchEngine? = null
    
    fun initializeSearchEngine(context: Context) {
        if (searchEngine == null) {
            MapboxSearchSdk.initialize(context)
            searchEngine = SearchEngine.createSearchEngineWithBuiltInDataProviders(
                SearchEngineSettings()
            )
        }
    }
    
    /**
     * Forward geocoding - convert address to coordinates
     */
    suspend fun geocodeAddress(
        context: Context,
        address: String,
        accessToken: String
    ): Result<List<GeocodingResult>> = withContext(Dispatchers.IO) {
        try {
            val client = MapboxGeocoding.builder()
                .accessToken(accessToken)
                .query(address)
                .geocodingTypes(GeocodingCriteria.TYPE_PLACE)
                .build()
            
            val response: Response<GeocodingResponse> = client.executeCall()
            
            if (response.isSuccessful) {
                val results = response.body()?.features?.map { feature ->
                    GeocodingResult(
                        address = feature.placeName ?: "",
                        latitude = feature.center()?.latitude() ?: 0.0,
                        longitude = feature.center()?.longitude() ?: 0.0,
                        context = feature.context?.map { it.text ?: "" } ?: emptyList()
                    )
                } ?: emptyList()
                
                Result.success(results)
            } else {
                Result.failure(Exception("Geocoding failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Reverse geocoding - convert coordinates to address
     */
    suspend fun reverseGeocode(
        context: Context,
        latitude: Double,
        longitude: Double,
        accessToken: String
    ): Result<GeocodingResult> = withContext(Dispatchers.IO) {
        try {
            val client = MapboxGeocoding.builder()
                .accessToken(accessToken)
                .query(com.mapbox.geocoding.v5.models.CarmenFeature.builder()
                    .center(listOf(longitude, latitude))
                    .build())
                .build()
            
            val response: Response<GeocodingResponse> = client.executeCall()
            
            if (response.isSuccessful) {
                val feature = response.body()?.features?.firstOrNull()
                if (feature != null) {
                    val result = GeocodingResult(
                        address = feature.placeName ?: "",
                        latitude = latitude,
                        longitude = longitude,
                        context = feature.context?.map { it.text ?: "" } ?: emptyList()
                    )
                    Result.success(result)
                } else {
                    Result.failure(Exception("No address found for coordinates"))
                }
            } else {
                Result.failure(Exception("Reverse geocoding failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Search for places using Mapbox Search SDK
     */
    suspend fun searchPlaces(
        query: String,
        proximity: Pair<Double, Double>? = null
    ): Result<List<SearchSuggestion>> = withContext(Dispatchers.IO) {
        try {
            val engine = searchEngine ?: return@withContext Result.failure(
                Exception("Search engine not initialized")
            )
            
            val searchOptions = com.mapbox.search.SearchOptions().apply {
                proximity?.let { (lat, lng) ->
                    this.proximity = com.mapbox.search.Coordinate(lat, lng)
                }
            }
            
            val response = engine.search(query, searchOptions)
            
            if (response.isSuccessful) {
                Result.success(response.suggestions)
            } else {
                Result.failure(Exception("Search failed: ${response.error}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get detailed information for a search suggestion
     */
    suspend fun getSearchResult(suggestion: SearchSuggestion): Result<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val engine = searchEngine ?: return@withContext Result.failure(
                Exception("Search engine not initialized")
            )
            
            val response = engine.select(suggestion)
            
            if (response.isSuccessful) {
                Result.success(response.results.first())
            } else {
                Result.failure(Exception("Failed to get search result: ${response.error}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    */
}

data class GeocodingResult(
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val context: List<String>
)
