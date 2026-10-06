package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class FitbitActivitySummary(
    val steps: Int? = null,
    val activityCalories: Int? = null,
    val caloriesOut: Int? = null
)

@JsonClass(generateAdapter = true)
data class FitbitActivityResponse(
    val summary: FitbitActivitySummary? = null
)

@JsonClass(generateAdapter = true)
data class FitbitHeartRateValue(
    val restingHeartRate: Int? = null
)

@JsonClass(generateAdapter = true)
data class FitbitHeartRateDay(
    val dateTime: String? = null,
    val value: FitbitHeartRateValue? = null
)

@JsonClass(generateAdapter = true)
data class FitbitHeartRateResponse(
    @Json(name = "activities-heart")
    val activitiesHeart: List<FitbitHeartRateDay>? = null
)

@JsonClass(generateAdapter = true)
data class FitbitSleepSummary(
    val totalMinutesAsleep: Int? = null,
    val totalTimeInBed: Int? = null
)

@JsonClass(generateAdapter = true)
data class FitbitSleepResponse(
    val summary: FitbitSleepSummary? = null
)

interface FitbitService {
    @GET("1/user/-/activities/date/{date}.json")
    suspend fun getDailyActivity(
        @Header("Authorization") authHeader: String,
        @Path("date") date: String
    ): FitbitActivityResponse

    @GET("1/user/-/activities/heart/date/{date}/1d.json")
    suspend fun getDailyHeartRate(
        @Header("Authorization") authHeader: String,
        @Path("date") date: String
    ): FitbitHeartRateResponse

    @GET("1.2/user/-/sleep/date/{date}.json")
    suspend fun getDailySleep(
        @Header("Authorization") authHeader: String,
        @Path("date") date: String
    ): FitbitSleepResponse
}

object FitbitClient {
    private const val BASE_URL = "https://api.fitbit.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val service: FitbitService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FitbitService::class.java)
    }
}
