package com.jhaiian.clint.summarize

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.jhaiian.clint.browser.MainActivity
import okhttp3.MediaType.Companion.toMediaType

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Webpage summarize engine: sends extracted page text to an OpenAI-compatible
 * chat completions endpoint and returns a plain-text summary.
 */
object PageSummarizer {

    const val PREF_PROVIDER = "summarize_provider"
    const val PREF_API_KEY = "summarize_api_key"
    const val PREF_MODEL = "summarize_model"
    const val PREF_BASE_URL = "summarize_base_url"
    const val PREF_TOOLBAR_BUTTON = "summarize_toolbar_button"

    const val PROVIDER_OPENAI = "openai"
    const val PROVIDER_GROQ = "groq"
    const val PROVIDER_OPENROUTER = "openrouter"
    const val PROVIDER_CUSTOM = "custom"

    data class Provider(val id: String, val baseUrl: String, val defaultModel: String)

    val PROVIDERS = listOf(
        Provider(PROVIDER_OPENAI, "https://api.openai.com/v1", "gpt-4o-mini"),
        Provider(PROVIDER_GROQ, "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile"),
        Provider(PROVIDER_OPENROUTER, "https://openrouter.ai/api/v1", "openai/gpt-4o-mini"),
        Provider(PROVIDER_CUSTOM, "", ""),
    )

    private const val MAX_PAGE_CHARS = 12000

    private val mainHandler = Handler(Looper.getMainLooper())

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    fun providerFor(prefs: SharedPreferences): Provider =
        PROVIDERS.firstOrNull { it.id == prefs.getString(PREF_PROVIDER, null) } ?: PROVIDERS.first()

    fun resolvedModel(prefs: SharedPreferences): String {
        val stored = prefs.getString(PREF_MODEL, "")?.trim().orEmpty()
        return stored.ifBlank { providerFor(prefs).defaultModel }
    }

    fun resolvedBaseUrl(prefs: SharedPreferences): String {
        val provider = providerFor(prefs)
        if (provider.id != PROVIDER_CUSTOM) return provider.baseUrl
        val stored = prefs.getString(PREF_BASE_URL, "")?.trim().orEmpty().trimEnd('/')
        return stored.ifBlank { provider.baseUrl }
    }

    /**
     * Requests a summary for [pageText]. Calls [onResult] on the main thread.
     */
    fun summarize(prefs: SharedPreferences, title: String, pageText: String, onResult: (Result<String>) -> Unit) {
        val apiKey = prefs.getString(PREF_API_KEY, "")?.trim().orEmpty()
        if (apiKey.isEmpty()) {
            onResult(Result.failure(IllegalStateException("missing api key")))
            return
        }
        val model = resolvedModel(prefs)
        val url = resolvedBaseUrl(prefs) + "/chat/completions"
        val systemPrompt = "You summarize webpages for a browser's summarize feature. " +
            "Reply with a concise plain-text summary: one short paragraph followed by a few " +
            "bullet points of the key points. Keep it under 200 words. No markdown headings."
        val userPrompt = buildString {
            if (title.isNotBlank()) append("Page title: ").append(title).append("\n\n")
            append("Page content:\n")
            append(pageText.take(MAX_PAGE_CHARS))
        }
        Thread {
            val result = runCatching {
                val payload = org.json.JSONObject()
                    .put("model", model)
                    .put("temperature", 0.3)
                    .put(
                        "messages",
                        org.json.JSONArray()
                            .put(org.json.JSONObject().put("role", "system").put("content", systemPrompt))
                            .put(org.json.JSONObject().put("role", "user").put("content", userPrompt))
                    )
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $apiKey")
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) throw java.io.IOException(errorMessage(body, response.code))
                    val content = org.json.JSONObject(body)
                        .optJSONArray("choices")?.optJSONObject(0)
                        ?.optJSONObject("message")?.optString("content")
                        .orEmpty()
                    if (content.isBlank()) throw java.io.IOException("empty response")
                    content.trim()
                }
            }
            mainHandler.post { onResult(result) }
        }.start()
    }

    private fun errorMessage(body: String, code: Int): String {
        val apiMessage = runCatching {
            org.json.JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
        }.getOrNull()
        return apiMessage?.takeIf { it.isNotBlank() } ?: "HTTP $code"
    }
}

/**
 * Extracts the readable text of the active page using the reader-mode script
 * and hands [onResult] the page title plus stripped plain text (main thread).
 */
internal fun MainActivity.extractPageForSummarize(onResult: (title: String, text: String) -> Unit) {
    val webView = tabManager.activeTab?.webView
    if (webView == null) {
        onResult("", "")
        return
    }
    val js = assets.open("JavaScript/reader_mode.js").bufferedReader().use { it.readText() }
    webView.evaluateJavascript(js) { raw ->
        val json = runCatching {
            val unescaped = raw?.removeSurrounding("\"")
                ?.replace("\\\"", "\"")
                ?.replace("\\\\", "\\")
                ?.replace("\\n", "\n")
                ?.replace("\\t", "\t")
                ?: ""
            org.json.JSONObject(unescaped)
        }.getOrNull()
        val title = json?.optString("title", "").orEmpty()
        val content = stripHtml(json?.optString("content", "").orEmpty())
        onResult(title, content)
    }
}

internal fun stripHtml(html: String): String = html
    .replace(Regex("(?is)<(script|style|noscript|svg)[^>]*>.*?</\\1>"), " ")
    .replace(Regex("(?i)<(br|/p|/div|/li|/h[1-6]|/tr)[^>]*>"), "\n")
    .replace(Regex("<[^>]*>"), " ")
    .replace("&nbsp;", " ")
    .replace("&amp;", "&")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
    .replace(Regex("\\n\\s*\\n+"), "\n\n")
    .trim()
