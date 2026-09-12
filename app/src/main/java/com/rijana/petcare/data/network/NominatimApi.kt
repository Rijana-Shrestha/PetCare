package com.rijana.petcare.data.network

import retrofit2.http.GET
import retrofit2.http.Query

data class NominatimResult(
    val lat: String,
    val lon: String,
    val display_name: String
)

interface NominatimApi {

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 5
    ): List<NominatimResult>

    @GET("reverse")
    suspend fun reverse(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "json"
    ): NominatimResult
}