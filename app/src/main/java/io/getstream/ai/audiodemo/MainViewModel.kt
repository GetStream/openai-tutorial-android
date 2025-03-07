package io.getstream.ai.audiodemo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainViewModel(val app: Application) : AndroidViewModel(app) {

    var callUiState = MutableStateFlow(CallUiState.IDLE)
    val credentials =
        MutableStateFlow<DataLayerResponse<Credentials>>(DataLayerResponse.Initial("Initial State"))
    var client: StreamVideo? = null
    var call: Call? = null

    fun initCredentials() {
        viewModelScope.launch {
            try {
                credentials.emit(DataLayerResponse.Initial("Getting Credentials"))
                val response = RetrofitInstance.api.getCredentials()
                createStreamClient(response)
                credentials.emit(DataLayerResponse.Success(response))
            } catch (ex: Exception) {
                credentials.emit(DataLayerResponse.Error(ex.message))
            }
        }
    }

    private fun createStreamClient(credentials: Credentials) {
        val userId = credentials.userId

        val user = User(
            id = userId,
            name = "Tutorial",
            image = "https://bit.ly/2TIt8NR",
        )

        // Initialize StreamVideo. For a production app, we recommend adding the client to your Application class or di module.
        client = StreamVideoBuilder(
            context = app.applicationContext,
            apiKey = credentials.apiKey,
            geo = GEO.GlobalEdgeNetwork,
            user = user,
            token = credentials.token,
        ).build()
    }

    fun joinCall() {
        client?.let { client ->
            val credentials = (credentials.value as DataLayerResponse.Success).data
            call = client.call(credentials.callType, credentials.callId)
            connectAi(call!!, credentials.callId, credentials.callType)
        }
    }

    private fun connectAi(call: Call, channelId: String, callType: String) {
        viewModelScope.launch {
            try {
                callUiState.emit(CallUiState.JOINING)
                val encodedChannelId =
                    URLEncoder.encode(channelId, StandardCharsets.UTF_8.toString())
                val encodedCallType = URLEncoder.encode(callType, StandardCharsets.UTF_8.toString())
                val response = RetrofitInstance.api.connectAI(encodedCallType, encodedChannelId)
                call.join(create = true)
                callUiState.emit(CallUiState.ACTIVE)
            } catch (ex: Exception) {
                ex.printStackTrace()
                callUiState.emit(CallUiState.ERROR)
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            call?.end()
            callUiState.emit(CallUiState.IDLE)
        }
    }
}

sealed class DataLayerResponse<T> {
    class Success<T>(val data: T) : DataLayerResponse<T>()
    class Error<T>(val message: String?) : DataLayerResponse<T>()
    class Initial<T>(val message: String) : DataLayerResponse<T>()
}