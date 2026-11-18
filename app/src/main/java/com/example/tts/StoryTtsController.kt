package com.example.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class TtsUiState(
    val isReady: Boolean = false,
    val isSpeaking: Boolean = false,
    val activeParagraphIndex: Int = -1,
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val statusMessage: String? = null
)

class StoryTtsController(context: Context) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var currentParagraphs: List<String> = emptyList()

    private val _state = MutableStateFlow(TtsUiState())
    val state: StateFlow<TtsUiState> = _state.asStateFlow()

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val langResult = tts?.setLanguage(Locale.getDefault())
                val supported = langResult != TextToSpeech.LANG_MISSING_DATA &&
                    langResult != TextToSpeech.LANG_NOT_SUPPORTED
                if (!supported) {
                    tts?.setLanguage(Locale.US)
                }
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        val idx = utteranceId?.removePrefix("p_")?.toIntOrNull() ?: -1
                        _state.value = _state.value.copy(
                            isSpeaking = true,
                            activeParagraphIndex = idx,
                            statusMessage = null
                        )
                    }

                    override fun onDone(utteranceId: String?) {
                        val idx = utteranceId?.removePrefix("p_")?.toIntOrNull() ?: -1
                        if (idx >= currentParagraphs.lastIndex) {
                            _state.value = _state.value.copy(
                                isSpeaking = false,
                                activeParagraphIndex = -1
                            )
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _state.value = _state.value.copy(
                            isSpeaking = false,
                            activeParagraphIndex = -1,
                            statusMessage = "Speech playback interrupted"
                        )
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _state.value = _state.value.copy(
                            isSpeaking = false,
                            activeParagraphIndex = -1,
                            statusMessage = "Text-to-speech unavailable on this device (code $errorCode)"
                        )
                    }
                })
                _state.value = _state.value.copy(isReady = true)
            } else {
                _state.value = _state.value.copy(
                    isReady = false,
                    statusMessage = "Text-to-Speech engine not available"
                )
            }
        }
    }

    fun speakParagraphs(paragraphs: List<String>, startIndex: Int = 0) {
        val engine = tts ?: return
        if (!_state.value.isReady || paragraphs.isEmpty()) {
            _state.value = _state.value.copy(statusMessage = "Speech engine is initializing...")
            return
        }
        currentParagraphs = paragraphs
        engine.setSpeechRate(_state.value.speechRate)
        engine.setPitch(_state.value.pitch)
        engine.stop()

        val safeStart = startIndex.coerceIn(0, paragraphs.lastIndex)
        for (i in safeStart..paragraphs.lastIndex) {
            val mode = if (i == safeStart) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            engine.speak(paragraphs[i], mode, null, "p_$i")
        }
        _state.value = _state.value.copy(
            isSpeaking = true,
            activeParagraphIndex = safeStart,
            statusMessage = null
        )
    }

    fun stop() {
        tts?.stop()
        _state.value = _state.value.copy(isSpeaking = false, activeParagraphIndex = -1)
    }

    fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.6f, 1.8f)
        _state.value = _state.value.copy(speechRate = clamped)
        tts?.setSpeechRate(clamped)
        if (_state.value.isSpeaking && currentParagraphs.isNotEmpty()) {
            val resumeAt = _state.value.activeParagraphIndex.coerceAtLeast(0)
            speakParagraphs(currentParagraphs, resumeAt)
        }
    }

    fun setPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.75f, 1.3f)
        _state.value = _state.value.copy(pitch = clamped)
        tts?.setPitch(clamped)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
