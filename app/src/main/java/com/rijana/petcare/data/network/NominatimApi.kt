package com.rijana.petcare.data.network

import retrofit2.http.GET
import retrofit2.http.Query

data class NominatimResult(
    val lat: String,
    val lon: String,
    val display_name: String,
    val name: String? = null,
    val category: String? = null,
    val type: String? = null,
    val address: Map<String, String>? = null,
    val namedetails: Map<String, String>? = null
)

interface NominatimApi {

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("namedetails") nameDetails: Int = 1,
        @Query("limit") limit: Int = 5
    ): List<NominatimResult>

    @GET("reverse")
    suspend fun reverse(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "jsonv2",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("namedetails") nameDetails: Int = 1,
        @Query("zoom") zoom: Int = 18,
        @Query("layer") layer: String = "address,poi,manmade"
    ): NominatimResult
}