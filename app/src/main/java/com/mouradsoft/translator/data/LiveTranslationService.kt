package com.mouradsoft.translator.data

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class LiveTranslationService(
    baseUrl: String,
    allowLocalHttp: Boolean = false,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build()
) : TranslationService {
    override val isDemo = false
    private val endpoint = baseUrl.toHttpUrlOrNull()?.takeIf {
        it.username.isEmpty() && it.password.isEmpty() && it.query == null && it.fragment == null &&
            (it.isHttps || (allowLocalHttp && it.host in setOf("10.0.2.2", "127.0.0.1", "localhost")))
    }?.newBuilder()?.addPathSegments("v1/translate")?.build()

    override suspend fun translate(request: TranslationRequest): TranslationOutcome {
        val url = endpoint ?: return TranslationOutcome.Failed(Problem.Unavailable)
        val body = JSONObject().put("text", request.text).put("language", request.language.code)
            .put("direction", request.direction.wire).toString()
        val call = client.newCall(Request.Builder().url(url)
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaType())).build())
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resume(TranslationOutcome.Failed(
                        if (e is InterruptedIOException) Problem.Timeout else Problem.Connection))
                }
                override fun onResponse(call: Call, response: Response) {
                    val outcome = response.use {
                        when (it.code) {
                            200 -> try {
                                val source = it.body?.source()
                                // Bound decoded data before allocating a string (including chunked responses).
                                if (source == null || source.request(16_385)) TranslationOutcome.Failed(Problem.InvalidResponse)
                                else parseResult(source.readUtf8())
                            } catch (_: Exception) { TranslationOutcome.Failed(Problem.InvalidResponse) }
                            422 -> TranslationOutcome.Failed(Problem.Refused)
                            429 -> TranslationOutcome.Failed(Problem.RateLimited)
                            504 -> TranslationOutcome.Failed(Problem.Timeout)
                            503 -> TranslationOutcome.Failed(Problem.Unavailable)
                            else -> TranslationOutcome.Failed(Problem.InvalidResponse)
                        }
                    }
                    if (continuation.isActive) continuation.resume(outcome)
                }
            })
        }
    }
}

internal fun parseResult(raw: String): TranslationOutcome = try {
    val json = JSONObject(raw)
    val keys = setOf("translation", "shortExplanation", "needsMoreContext", "clarificationQuestion")
    require(json.keys().asSequence().toSet() == keys)
    require(json.get("needsMoreContext") is Boolean)
    fun strictString(key: String): String { require(json.get(key) is String); return json.getString(key).trim() }
    val translation = strictString("translation")
    val explanation = strictString("shortExplanation")
    val question = strictString("clarificationQuestion")
    if (json.getBoolean("needsMoreContext")) {
        require(translation.isEmpty() && explanation.isEmpty() && meaningful(question, 300) && question.length >= 8)
        TranslationOutcome.Clarify(question)
    } else {
        require(meaningful(translation, 2000) && explanation.length <= 600 && question.isEmpty())
        TranslationOutcome.Ready(Translation(translation, explanation))
    }
} catch (_: Exception) { TranslationOutcome.Failed(Problem.InvalidResponse) }
