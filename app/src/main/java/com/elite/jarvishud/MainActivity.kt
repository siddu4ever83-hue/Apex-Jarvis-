package com.elite.jarvishud

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.math.sin

private const val ASSISTANT_NAME = "JARVIS"

// Holographic color palette (matches Mark-XLIX dark HUD style)
val HudCyan = Color(0xFF00E5FF)
val HudCyanDim = Color(0xFF00838F)
val HudRed = Color(0xFFFF3B3B)
val HudBg = Color(0xFF050B0F)
val HudPanel = Color(0xFF0A1418)

class MainActivity : ComponentActivity() {

    private var androidTts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var keyStore: KeyStore
    private val httpClient = OkHttpClient()

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keyStore = KeyStore(this)

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        androidTts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                androidTts?.language = Locale.US
            }
        }

        setContent {
            var showSettings by remember { mutableStateOf(false) }
            if (showSettings) {
                SettingsScreen(keyStore = keyStore, onBack = { showSettings = false })
            } else {
                HudScreen(
                    keyStore = keyStore,
                    httpClient = httpClient,
                    onOpenSettings = { showSettings = true },
                    onListenStart = { onResult, onPartial, onError -> startListening(onResult, onPartial, onError) },
                    onListenStop = { stopListening() },
                    onSpeak = { text, onDone -> speak(text, onDone) }
                )
            }
        }
    }

    private fun startListening(
        onResult: (String) -> Unit,
        onPartial: (Float) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            onError("Speech recognition not available on this device")
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
        }
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) { onPartial(rmsdB) }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { onError("Mic error code $error") }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull().orEmpty()
                if (text.isNotBlank()) onResult(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer?.startListening(intent)
    }

    private fun stopListening() {
        recognizer?.stopListening()
    }

    /**
     * Speaks text using ElevenLabs if a key is configured; if that call fails
     * for any reason (network error, limit reached, invalid key), it silently
     * falls back to the phone's built-in TextToSpeech so the assistant never
     * goes silent. onDone is called once speaking has fully finished either way.
     */
    private fun speak(text: String, onDone: () -> Unit) {
        val elevenKey = keyStore.elevenLabsKey
        if (elevenKey.isBlank()) {
            speakWithAndroidTts(text, onDone)
            return
        }

        val voiceId = keyStore.elevenLabsVoiceId
        val json = JSONObject().apply {
            put("text", text)
            put("model_id", "eleven_multilingual_v2")
            put("voice_settings", JSONObject().apply {
                put("stability", 0.4)
                put("similarity_boost", 0.8)
            })
        }
        val body = RequestBody.create(MediaType.parse("application/json"), json.toString())
        val request = Request.Builder()
            .url("https://api.elevenlabs.io/v1/text-to-speech/$voiceId")
            .addHeader("xi-api-key", elevenKey)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "audio/mpeg")
            .post(body)
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // ElevenLabs unreachable -> fallback to phone TTS
                runOnUiThread { speakWithAndroidTts(text, onDone) }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    // Likely limit reached / invalid key -> fallback to phone TTS
                    runOnUiThread { speakWithAndroidTts(text, onDone) }
                    return
                }
                try {
                    val bytes = response.body()?.bytes()
                    if (bytes == null || bytes.isEmpty()) {
                        runOnUiThread { speakWithAndroidTts(text, onDone) }
                        return
                    }
                    val tempFile = File.createTempFile("tts_", ".mp3", cacheDir)
                    tempFile.writeBytes(bytes)
                    runOnUiThread { playAudioFile(tempFile, onDone) }
                } catch (e: Exception) {
                    runOnUiThread { speakWithAndroidTts(text, onDone) }
                }
            }
        })
    }

    private fun playAudioFile(file: File, onDone: () -> Unit) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnCompletionListener { onDone() }
                setOnErrorListener { _, _, _ -> onDone(); true }
                prepare()
                start()
            }
        } catch (e: Exception) {
            onDone()
        }
    }

    private fun speakWithAndroidTts(text: String, onDone: () -> Unit) {
        androidTts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { runOnUiThread { onDone() } }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { runOnUiThread { onDone() } }
        })
        androidTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utterance_id")
    }

    override fun onDestroy() {
        recognizer?.destroy()
        mediaPlayer?.release()
        androidTts?.stop()
        androidTts?.shutdown()
        super.onDestroy()
    }
}

data class LogEntry(val speaker: String, val text: String, val isUser: Boolean)

@Composable
fun HudScreen(
    keyStore: KeyStore,
    httpClient: OkHttpClient,
    onOpenSettings: () -> Unit,
    onListenStart: ((String) -> Unit, (Float) -> Unit, (String) -> Unit) -> Unit,
    onListenStop: () -> Unit,
    onSpeak: (String, () -> Unit) -> Unit
) {
    var isListening by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("STANDING BY") }
    var micLevel by remember { mutableFloatStateOf(0f) }
    val logs = remember { mutableStateListOf<LogEntry>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var newsTicker by remember { mutableStateOf("") }
    var hasBriefed by remember { mutableStateOf(false) }

    fun addLog(speaker: String, text: String, isUser: Boolean) {
        logs.add(LogEntry(speaker, text, isUser))
        scope.launch { listState.animateScrollToItem(logs.size - 1) }
    }

    fun beginListening() {
        isListening = true
        statusText = "LISTENING..."
        onListenStart(
            { result ->
                isListening = false
                statusText = "STANDING BY"
                addLog("YOU", result, true)
                statusText = "THINKING..."
                callOpenRouter(httpClient, keyStore, result) { reply ->
                    addLog(ASSISTANT_NAME, reply, false)
                    statusText = "SPEAKING..."
                    onSpeak(reply) { statusText = "STANDING BY" }
                }
            },
            { rms -> micLevel = rms },
            { err ->
                isListening = false
                statusText = "STANDING BY"
                addLog(ASSISTANT_NAME, err, false)
            }
        )
    }

    // Auto morning-briefing on launch: fetch news, speak headlines,
    // then say "Cheppandi boss" and open the mic automatically.
    LaunchedEffect(Unit) {
        if (hasBriefed) return@LaunchedEffect
        hasBriefed = true
        if (keyStore.newsApiKey.isBlank()) {
            statusText = "STANDING BY"
            return@LaunchedEffect
        }
        statusText = "FETCHING NEWS..."
        NewsRepository.fetchTopHeadlines(
            apiKey = keyStore.newsApiKey,
            country = keyStore.newsCountry,
            onResult = { headlines ->
                newsTicker = headlines.joinToString("     •     ")
                val topThree = headlines.take(3)
                val briefingText = if (topThree.isEmpty()) {
                    "No news available right now."
                } else {
                    "Good day. Here are today's top headlines. " +
                        topThree.joinToString(". ") + "."
                }
                addLog(ASSISTANT_NAME, briefingText, false)
                statusText = "SPEAKING..."
                onSpeak(briefingText) {
                    val prompt = "Cheppandi boss"
                    addLog(ASSISTANT_NAME, prompt, false)
                    onSpeak(prompt) {
                        statusText = "STANDING BY"
                        beginListening()
                    }
                }
            },
            onError = { err ->
                statusText = "STANDING BY"
                addLog(ASSISTANT_NAME, err, false)
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HudBg)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ===== Header =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = ASSISTANT_NAME,
                        color = HudCyan,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = statusText,
                        color = HudCyanDim,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = HudCyanDim)
                }
            }

            // ===== News scrolling ticker =====
            if (newsTicker.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HudPanel)
                        .border(BorderStroke(1.dp, HudCyanDim))
                        .padding(vertical = 6.dp, horizontal = 8.dp)
                ) {
                    Text(
                        text = "NEWS: $newsTicker",
                        color = HudCyanDim,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== Waveform HUD circle =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                WaveformHud(isActive = isListening, level = micLevel)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== Log / Conversation Panel =====
            Text(
                text = "CONVERSATION",
                color = HudCyanDim,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(HudPanel)
                    .border(BorderStroke(1.dp, HudCyanDim))
                    .padding(12.dp)
            ) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(logs) { entry ->
                        Text(
                            text = "${if (entry.isUser) "YOU" else ASSISTANT_NAME}: ${entry.text}",
                            color = if (entry.isUser) Color.White else HudCyan,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== Mic / Interrupt Button =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FloatingActionButton(
                    onClick = {
                        if (!isListening) {
                            beginListening()
                        } else {
                            isListening = false
                            statusText = "STANDING BY"
                            onListenStop()
                        }
                    },
                    containerColor = if (isListening) HudRed else HudCyan,
                    contentColor = Color.Black,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = "Mic",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

private fun callOpenRouter(
    client: OkHttpClient,
    keyStore: KeyStore,
    userText: String,
    onReply: (String) -> Unit
) {
    val apiKey = keyStore.openRouterKey
    if (apiKey.isBlank()) {
        onReply("OpenRouter API key not set. Please add it in Settings.")
        return
    }
    val json = JSONObject().apply {
        put("model", keyStore.openRouterModel)
        put("messages", JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", "You are $ASSISTANT_NAME, a concise, helpful voice assistant. Keep replies short and spoken-friendly.")
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", userText)
            })
        })
    }
    val body = RequestBody.create(MediaType.parse("application/json"), json.toString())
    val request = Request.Builder()
        .url("https://openrouter.ai/api/v1/chat/completions")
        .addHeader("Authorization", "Bearer $apiKey")
        .addHeader("Content-Type", "application/json")
        .post(body)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onReply("Network error: ${e.message}")
        }
        override fun onResponse(call: Call, response: Response) {
            val resStr = response.body()?.string().orEmpty()
            try {
                val obj = JSONObject(resStr)
                val reply = obj.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                onReply(reply)
            } catch (e: Exception) {
                onReply("Parse error: $resStr")
            }
        }
    })
}

@Composable
fun WaveformHud(isActive: Boolean, level: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "angle"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = Modifier.size(200.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = size.minDimension / 2.4f
        val activeScale = if (isActive) pulse else 1f

        rotate(angle) {
            drawCircle(
                color = HudCyanDim,
                radius = baseRadius * activeScale,
                center = center,
                style = Stroke(width = 2f)
            )
        }
        drawCircle(
            color = HudCyan,
            radius = baseRadius * 0.7f * activeScale,
            center = center,
            style = Stroke(width = 3f)
        )
        drawCircle(
            color = if (isActive) HudRed.copy(alpha = 0.5f) else HudCyan.copy(alpha = 0.25f),
            radius = baseRadius * 0.4f * activeScale,
            center = center
        )
        val bars = 32
        for (i in 0 until bars) {
            val barAngle = (360f / bars) * i
            val amplitude = if (isActive) (10 + (level.coerceIn(0f, 10f) * 4) + (sin(barAngle / 20f) * 5)) else 4f
            val rad = Math.toRadians(barAngle.toDouble())
            val startR = baseRadius * 1.05f
            val endR = startR + amplitude
            val startX = center.x + (startR * kotlin.math.cos(rad)).toFloat()
            val startY = center.y + (startR * kotlin.math.sin(rad)).toFloat()
            val endX = center.x + (endR * kotlin.math.cos(rad)).toFloat()
            val endY = center.y + (endR * kotlin.math.sin(rad)).toFloat()
            drawLine(
                color = HudCyan.copy(alpha = 0.7f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 3f
            )
        }
    }
}
