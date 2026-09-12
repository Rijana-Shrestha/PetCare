package com.rijana.petcare.data.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NominatimClient {

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val requestWithHeader = chain.request().newBuilder()
                .header("User-Agent", "PetCare-Android-App (CET343 student project)")
                .build()
            chain.proceed(requestWithHeader)
        }
        .build()

    val api: NominatimApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NominatimApi::class.java)
    }
}