package com.example.data.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GoogleGeocodingService {

    @GET("maps/api/place/autocomplete/json")
    suspend fun getPlaceAutocomplete(
        @Query("input") input: String,
        @Query("key") apiKey: String,
        @Query("types") types: String? = "establishment|geocode"
    ): PlacesAutocompleteResponse

    @GET("maps/api/geocode/json")
    suspend fun geocodeAddress(
        @Query("address") address: String,
        @Query("key") apiKey: String
    ): GeocodeResponse

    @GET("maps/api/geocode/json")
    suspend fun geocodePlaceId(
        @Query("place_id") placeId: String,
        @Query("key") apiKey: String
    ): GeocodeResponse

    companion object {
        private const val BASE_URL = "https://maps.googleapis.com/"

        fun create(): GoogleGeocodingService {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            return retrofit.create(GoogleGeocodingService::class.java)
        }
    }
}
