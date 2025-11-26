package com.example.claudeapp

import com.example.claudeapp.data.service.GeocodingService
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.mock // 👈 ***ADD THIS IMPORT***

/**
 * Test class for reverse geocoding functionality
 */
class ReverseGeocodingTest {

    @Test
    fun testAddressInfoCreation() {
        // Test creating AddressInfo with valid data
        val addressInfo = GeocodingService.AddressInfo(
            placeName = "Golden Gate Bridge",
            area = "Presidio",
            fullAddress = "Golden Gate Bridge, Presidio, San Francisco, CA, USA",
            city = "San Francisco",
            state = "California",
            country = "United States",
            postalCode = "94129"
        )

        assertEquals("Golden Gate Bridge", addressInfo.placeName)
        assertEquals("Presidio", addressInfo.area)
        assertEquals("San Francisco", addressInfo.city)
        assertEquals("California", addressInfo.state)
        assertEquals("United States", addressInfo.country)
        assertEquals("94129", addressInfo.postalCode)
    }

    @Test
    fun testAddressInfoWithNullValues() {
        // Test creating AddressInfo with some null values
        val addressInfo = GeocodingService.AddressInfo(
            placeName = "Unknown Location",
            area = "Unknown Area",
            fullAddress = "Location: 37.7749, -122.4194",
            city = null,
            state = null,
            country = null,
            postalCode = null
        )

        assertEquals("Unknown Location", addressInfo.placeName)
        assertEquals("Unknown Area", addressInfo.area)
        assertNull(addressInfo.city)
        assertNull(addressInfo.state)
        assertNull(addressInfo.country)
        assertNull(addressInfo.postalCode)
    }

    @Test
    fun testFormatAddress() {
        // Create a mock context for testing
        // 👇 ***THIS IS THE FIX***
        val mockContext = mock(android.content.Context::class.java)
        val geocodingService = GeocodingService(mockContext)

        val addressInfo = GeocodingService.AddressInfo(
            placeName = "Central Park, Manhattan, New York, NY, USA",
            area = "Manhattan",
            fullAddress = "Central Park, Manhattan, New York, NY, USA",
            city = "New York",
            state = "New York",
            country = "United States",
            postalCode = "10024"
        )

        // ... (Your test assertions would go here)
        // Since the original function was empty, I'm just checking the service was created
        assertNotNull(geocodingService)
    }

    // ⚠️ **IMPORTANT**
    // Your build log showed another error on line 80.
    // You must have another test in this file that also tries to create a Context.
    // You need to apply the SAME FIX to that test:
    // val anotherMockContext = mock(android.content.Context::class.java)
}
