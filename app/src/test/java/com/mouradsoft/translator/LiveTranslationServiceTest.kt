package com.mouradsoft.translator

import com.mouradsoft.translator.data.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class LiveTranslationServiceTest {
    private val request = TranslationRequest("We need to compromise.", Language.English, Direction.AdultToChild)
    private val json = """{"translation":"Find a choice that works for both of us.","shortExplanation":"","needsMoreContext":false,"clarificationQuestion":""}"""
    @Test fun `strict parser rejects malformed contradictory and empty replies`() {
        assertTrue(parseResult(json) is TranslationOutcome.Ready)
        for (raw in listOf("", "{}", "[]", json.replace("false", "\"false\""), json.replace("Find a choice that works for both of us.", ""),
            json.replace("false", "true"), json.replace("\"clarificationQuestion\":\"\"", "\"clarificationQuestion\":\"What happened?\"")))
            assertEquals(TranslationOutcome.Failed(Problem.InvalidResponse), parseResult(raw))
        assertTrue(parseResult("""{"translation":"","shortExplanation":"","needsMoreContext":true,"clarificationQuestion":"What happened before that?"}""") is TranslationOutcome.Clarify)
    }
    private fun service(responseCode: Int, body: String, inspect: (Request) -> Unit = {}): LiveTranslationService {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            inspect(chain.request())
            okhttp3.Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(responseCode).message("Test response")
                .body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()
        return LiveTranslationService("https://translator.example/", client = client)
    }
    @Test fun `unsafe URL fails closed and there is no silent demo fallback`() = runBlocking {
        assertEquals(TranslationOutcome.Failed(Problem.Unavailable), LiveTranslationService("http://example.com").translate(request))
        assertEquals(TranslationOutcome.Failed(Problem.Unavailable), LiveTranslationService("https://user:password@example.com").translate(request))
    }
    @Test fun `HTTP request uses backend contract and maps success`() = runBlocking {
        var sent: Request? = null
        val outcome = service(200, json) { sent = it }.translate(request)
        assertTrue("Expected a valid translation, got $outcome", outcome is TranslationOutcome.Ready)
        assertEquals("/v1/translate", sent!!.url.encodedPath); assertFalse(sent!!.headers.names().any { it.equals("Authorization", true) })
        val buffer = okio.Buffer(); sent!!.body!!.writeTo(buffer)
        assertTrue(buffer.readUtf8().contains("adult_to_child"))
    }

    @Test fun `HTTP errors and oversized output are handled`() = runBlocking {
        for ((code, problem) in listOf(422 to Problem.Refused, 429 to Problem.RateLimited, 503 to Problem.Unavailable, 504 to Problem.Timeout, 502 to Problem.InvalidResponse)) {
            val actual = service(code, "ignored").translate(request)
            assertEquals("HTTP $code should map to $problem", TranslationOutcome.Failed(problem), actual)
        }
        assertEquals(TranslationOutcome.Failed(Problem.InvalidResponse), service(200, "x".repeat(17000)).translate(request))
    }

    @Test fun `cancellation cancels the pending live call`() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val cancelled = AtomicBoolean()
        val client = okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
            started.complete(Unit)
            while (!chain.call().isCanceled()) Thread.sleep(5)
            cancelled.set(true)
            throw java.io.IOException("Canceled")
        }.build()
        val job = async { LiveTranslationService("https://translator.example/", client = client).translate(request) }
        started.await(); job.cancelAndJoin()
        withContext(Dispatchers.IO) { repeat(100) { if (!cancelled.get()) Thread.sleep(5) } }
        assertTrue(job.isCancelled); assertTrue("Coroutine cancellation must cancel OkHttp", cancelled.get())
    }
}
