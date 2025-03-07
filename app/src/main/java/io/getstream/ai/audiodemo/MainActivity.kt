package io.getstream.ai.audiodemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var isPermissionGranted by remember { mutableStateOf(false) }
            if (isPermissionGranted) {
                Body()
            } else {
                MicrophonePermissionScreen(onPermissionGranted = {
                    isPermissionGranted = true
                })
            }
        }
    }
}

@Composable
fun Body() {
    val viewModel: MainViewModel = viewModel(key = "MainViewModel")
    LaunchedEffect(Unit) {
        viewModel.initCredentials()
    }
    val creds = viewModel.credentials.collectAsStateWithLifecycle()
    when (creds.value) {
        is DataLayerResponse.Initial -> {
            val message = (creds.value as DataLayerResponse.Initial<Credentials>).message
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(message, color = Color.White)
            }
        }

        is DataLayerResponse.Success -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AiContentUi()
            }

        }

        is DataLayerResponse.Error -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text("Getting Credentials Failed", color = Color.White)
            }
        }
    }
}

@Composable
fun AiContentUi() {
    val viewModel: MainViewModel = viewModel(key = "MainViewModel")
    val callUiState by viewModel.callUiState.collectAsStateWithLifecycle()

    when (callUiState) {
        CallUiState.IDLE -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(colors = ButtonDefaults.buttonColors()
                    .copy(contentColor = Color.White, containerColor = Color.Black),
                    onClick = {
                        viewModel.joinCall()
                    }) {
                    Text("Click to talk to Ai", color = Color.White)
                }
            }

        }

        CallUiState.JOINING -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Waiting for AI Agent to Join...",
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.width(24.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }

        CallUiState.ACTIVE -> {
            Box(Modifier.fillMaxSize()) {
                AISpeakingContainerView(viewModel.call?.state!!)
                CallEndButton(modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp), onClick = {
                    viewModel.disconnect()
                })
            }
        }

        CallUiState.ERROR -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Something went wrong, please re-try", color = Color.White)
                    Button(colors = ButtonDefaults.buttonColors()
                        .copy(contentColor = Color.White, containerColor = Color.Black),
                        onClick = {
                            viewModel.joinCall()
                        }) {
                        Text("Connect to Ai Agent", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun CallEndButton(modifier: Modifier, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color.Red)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.call_end),
            contentDescription = "End call",
            tint = Color.White
        )
    }
}

enum class CallUiState {
    IDLE, JOINING, ACTIVE, ERROR
}
