package io.getstream.ai.audiodemo

import android.graphics.BlurMaskFilter
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutBack
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.util.lerp
import io.getstream.video.android.core.CallState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class SpeakerState {
    AI_SPEAKING,
    USER_SPEAKING,
    IDLE
}

fun SpeakerState.gradientColors(): List<Color> {
    return when (this) {
        SpeakerState.USER_SPEAKING -> listOf(
            Color.Red,
            Color.Red.copy(alpha = 0f)
        )
        else -> listOf(
            Color(0f, 0.976f, 1f),
            Color(0f, 0.227f, 1f, 0f)
        )
    }
}

@Composable
fun AISpeakingView(callState: CallState) {
    val agentId = "lucy"

    var amplitude by remember { mutableFloatStateOf(0f) }
    var audioLevels by remember { mutableStateOf(listOf<Float>()) }
    var speakerState by remember { mutableStateOf(SpeakerState.IDLE) }

    GlowView(amplitude, speakerState)

    LaunchedEffect(Unit) {

        callState.activeSpeakers.collectLatest { speakers->
            speakers.forEach { speaker->
                speaker.audioLevels.collectLatest { audioLevel->
                    if(speaker.userId.value.contains("lucy")) {
                        if(speakerState!=SpeakerState.AI_SPEAKING) {
                            speakerState = SpeakerState.AI_SPEAKING
                        }
                    } else {
                        if(speakerState!=SpeakerState.USER_SPEAKING) {
                            speakerState = SpeakerState.USER_SPEAKING
                        }
                    }
                    audioLevels = audioLevel
                    amplitude =
                        computeSingleAmplitude(audioLevels) * getRandomFloatInRange(1f, 2.5f)
                }
            }
        }
    }

    LaunchedEffect(Unit) {

        callState.activeSpeakers.collectLatest { speaker->
            val aiSpeaker = speaker.find { it.userId.value.contains(agentId) }

            // Find the local user speaking
            val localSpeaker = speaker.find { it.userId.value == callState.me.value!!.userId.value }
            if(aiSpeaker == null && localSpeaker == null){
                speakerState = SpeakerState.IDLE
                audioLevels = emptyList()
                amplitude = 0f
            }
        }
    }
}

fun computeSingleAmplitude(levels: List<Float>): Float {
    val normalized = normalizePeak(levels)
    if (normalized.isEmpty()) return 0f

    return normalized.average().toFloat()
}

fun normalizePeak(levels: List<Float>): List<Float> {
    val maxLevel = levels.maxOfOrNull { abs(it) } ?: return levels
    return if (maxLevel > 0) levels.map { it / maxLevel } else levels
}

@Composable
fun AISpeakingContainerView(callState: CallState) {
    Box(modifier = Modifier.fillMaxSize()) {
        AISpeakingView(
            callState = callState
        )
    }
}

@Composable
fun GlowView(
    amplitude: Float,
    speakerState: SpeakerState = SpeakerState.AI_SPEAKING
) {
    // Animated amplitude for smooth transitions
    val animatedAmplitude = remember { Animatable(0f) }

    // Update animated amplitude when input amplitude changes
    LaunchedEffect(amplitude) {
        animatedAmplitude.animateTo(
            targetValue = amplitude,
            animationSpec = tween(700, easing = EaseInOutBack)
        )
    }

    // Continuous time value for wave animation
    var time by remember { mutableStateOf(0f) }

    // Rotation animation
    val infiniteRotation = rememberInfiniteTransition()
    val rotationAngle by infiniteRotation.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Time progression for wave effect
    LaunchedEffect(Unit) {
        while (true) {
            time = (time + 0.005f) % 1.0f
            delay(16L)
        }
    }

    val gradientColors = speakerState.gradientColors()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)

            // Outer Layer
            drawGlowLayer(
                center = center,
                baseRadiusMin = size.minDimension * 0.30f,
                baseRadiusMax = size.minDimension * 0.50f,
                blurRadius = 60f,
                baseOpacity = 0.35f,
                scaleRange = 0.3f,
                waveRangeMin = 0.2f,
                waveRangeMax = 0.02f,
                time = time,
                amplitude = animatedAmplitude.value,
                rotationAngle = rotationAngle,
                gradientColors = gradientColors
            )

            // Middle Layer
            drawGlowLayer(
                center = center,
                baseRadiusMin = size.minDimension * 0.20f,
                baseRadiusMax = size.minDimension * 0.30f,
                blurRadius = 40f,
                baseOpacity = 0.55f,
                scaleRange = 0.3f,
                waveRangeMin = 0.15f,
                waveRangeMax = 0.03f,
                time = time,
                amplitude = animatedAmplitude.value,
                rotationAngle = rotationAngle,
                gradientColors = gradientColors
            )

            // Inner Core Layer
            drawGlowLayer(
                center = center,
                baseRadiusMin = size.minDimension * 0.10f,
                baseRadiusMax = size.minDimension * 0.20f,
                blurRadius = 20f,
                baseOpacity = 0.9f,
                scaleRange = 0.5f,
                waveRangeMin = 0.35f,
                waveRangeMax = 0.05f,
                time = time,
                amplitude = animatedAmplitude.value,
                rotationAngle = rotationAngle,
                gradientColors = gradientColors
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGlowLayer(
    center: Offset,
    baseRadiusMin: Float,
    baseRadiusMax: Float,
    blurRadius: Float,
    baseOpacity: Float,
    scaleRange: Float,
    waveRangeMin: Float,
    waveRangeMax: Float,
    time: Float,
    amplitude: Float,
    rotationAngle: Float,
    gradientColors: List<Color>
) {
    // Calculate the actual radius based on amplitude
    val baseRadius = lerp(baseRadiusMin, baseRadiusMax, amplitude)

    // Calculate wave range (inverse relationship to amplitude)
    val waveRange = lerp(waveRangeMax, waveRangeMin, 1 - amplitude)

    // Calculate the scale factors
    val shapeWaveSin = sin(2 * PI * time).toFloat()
    val shapeWaveCos = cos(2 * PI * time).toFloat()

    // Scale from amplitude
    val amplitudeScale = 1.0f + scaleRange * amplitude

    // Final x/y scale = amplitude scale + wave
    val xScale = (amplitudeScale + waveRange * shapeWaveSin)
    val yScale = (amplitudeScale + waveRange * shapeWaveCos)

    // Draw the oval with gradient
    drawIntoCanvas { canvas ->
        val paint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
            shader = RadialGradient(
                center.x, center.y, baseRadius,
                intArrayOf(
                    gradientColors[0].copy(alpha = 0.9f).toArgb(),
                    gradientColors[1].toArgb()
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            alpha = (baseOpacity * 255).toInt()
        }

        // Apply blur
        paint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)

        // Save the current state, rotate, draw, and restore
        canvas.save()
        canvas.rotate(rotationAngle, center.x, center.y)

        // Draw the oval with calculated dimensions
        canvas.nativeCanvas.drawOval(
            center.x - baseRadius * xScale,
            center.y - baseRadius * yScale,
            center.x + baseRadius * xScale,
            center.y + baseRadius * yScale,
            paint
        )

        canvas.restore()
    }
}

fun getRandomFloatInRange(min: Float, max: Float): Float {
    return Random.nextFloat() * (max - min) + min
}