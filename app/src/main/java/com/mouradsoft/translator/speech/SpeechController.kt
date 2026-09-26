package com.mouradsoft.translator.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.mouradsoft.translator.R

enum class SpeechAvailability { OnDevice, Service, Missing }

/** Owned by the input screen; every platform call and callback is on the main thread. */
class SpeechController(
    private val context: Context,
    private val onTranscript: (String, Boolean) -> Unit,
    private val onProblem: (Int) -> Unit,
    private val onFinishing: () -> Unit
) {
    private var recognizer: SpeechRecognizer? = null
    private var generation = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var deadline: Runnable? = null

    fun availability(): SpeechAvailability = when {
        Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context) -> SpeechAvailability.OnDevice
        SpeechRecognizer.isRecognitionAvailable(context) -> SpeechAvailability.Service
        else -> SpeechAvailability.Missing
    }

    fun start(languageTag: String, onDevice: Boolean) {
        check(Looper.myLooper() == Looper.getMainLooper())
        cancel()
        val ticket = generation
        try {
            val engine = if (onDevice && Build.VERSION.SDK_INT >= 31)
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            else SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = engine
            engine.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() {
                    if (ticket == generation) { onFinishing(); scheduleTimeout(5_000) }
                }
                override fun onError(error: Int) {
                    if (ticket != generation) return
                    cancel()
                    onProblem(when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> R.string.voice_no_speech
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER -> R.string.voice_network
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> R.string.voice_denied
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> R.string.voice_busy
                        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> R.string.voice_language
                        else -> R.string.voice_error
                    })
                }
                override fun onResults(results: Bundle?) {
                    if (ticket != generation) return
                    val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    cancel()
                    if (words.isNullOrBlank()) onProblem(R.string.voice_no_speech) else onTranscript(words, true)
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    if (ticket != generation) return
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        ?.takeIf { it.isNotBlank() }?.let { onTranscript(it, false) }
                }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            engine.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            })
            scheduleTimeout(30_000)
        } catch (_: SecurityException) { cancel(); onProblem(R.string.voice_denied)
        } catch (_: Exception) { cancel(); onProblem(R.string.voice_missing) }
    }

    fun stop() {
        try { recognizer?.stopListening(); onFinishing(); scheduleTimeout(5_000) }
        catch (_: Exception) { cancel(); onProblem(R.string.voice_error) }
    }
    fun cancel() {
        generation++
        deadline?.let(handler::removeCallbacks)
        deadline = null
        val old = recognizer
        recognizer = null
        try { old?.cancel() } catch (_: Exception) { /* No content is logged. */ }
        try { old?.destroy() } catch (_: Exception) { /* Already destroyed by service. */ }
    }
    private fun scheduleTimeout(millis: Long) {
        deadline?.let(handler::removeCallbacks)
        deadline = Runnable { cancel(); onProblem(R.string.voice_no_speech) }.also { handler.postDelayed(it, millis) }
    }
}
