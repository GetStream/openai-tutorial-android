package io.getstream.ai.audiodemo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MicrophonePermissionScreen(onPermissionGranted: () -> Unit) {
    val microphonePermissionState =
        rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(Unit) {
        if (!microphonePermissionState.status.isGranted) {
            microphonePermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(microphonePermissionState.status.isGranted) {
        if (microphonePermissionState.status.isGranted) {
            onPermissionGranted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            microphonePermissionState.status.isGranted -> {}
            microphonePermissionState.status.shouldShowRationale -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Microphone access is required to proceed.",
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { microphonePermissionState.launchPermissionRequest() }) {
                        Text(text = "Grant Permission", color = Color.White)
                    }
                }
            }

            else -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Microphone permission is needed to proceed.",
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { microphonePermissionState.launchPermissionRequest() }) {
                        Text(text = "Request Permission", color = Color.White)
                    }
                }
            }
        }
    }
}
