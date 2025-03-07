package io.getstream.ai.audiodemo

import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @GET("credentials")
    suspend fun getCredentials(): Credentials

    @POST("{callType}/{channelId}/connect")
    suspend fun connectAI(
        @Path("callType", encoded = true) callType: String,
        @Path("channelId", encoded = true) channelId: String): ResponseBody
}

object RetrofitInstance {
    private val BASE_URL = "http://10.0.2.2:3000/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}