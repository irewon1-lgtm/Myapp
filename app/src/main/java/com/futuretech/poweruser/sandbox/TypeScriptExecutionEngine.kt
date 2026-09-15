package com.futuretech.poweruser.sandbox

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayInputStream
import java.util.UUID
import kotlin.coroutines.resume

internal class TypeScriptExecutionEngine(private val context: Context?) {

    suspend fun execute(code: String, timeoutMs: Long): ExecutionResult {
        val ctx = context?.applicationContext
            ?: return ExecutionResult(false, "", "TypeScript runtime requires Android context")
        val startedAt = System.currentTimeMillis()

        return withContext(Dispatchers.Main.immediate) {
            val pageReady = CompletableDeferred<Boolean>()
            val webView = createWebView(ctx) {
                if (!pageReady.isCompleted) pageReady.complete(true)
            }
            try {
                webView.loadUrl("https://appassets.androidplatform.net/assets/ts_runtime.html")
                val loaded = withTimeoutOrNull(5000L) { pageReady.await() } ?: false
                if (!loaded) {
                    return@withContext ExecutionResult(
                        false, "", "TypeScript compiler runtime failed to load",
                        System.currentTimeMillis() - startedAt
                    )
                }

                val token = UUID.randomUUID().toString().replace("-", "")
                val tokenJs = JSONObject.quote(token)
                val codeJs = JSONObject.quote(code)
                eval(webView, "window.__runtimeResults = window.__runtimeResults || {}; window.runTypeScript($codeJs, ${timeoutMs.coerceAtLeast(250L)}).then(function(r){ window.__runtimeResults[$tokenJs] = JSON.stringify(r); }).catch(function(e){ window.__runtimeResults[$tokenJs] = JSON.stringify({success:false,output:'',errorMessage:String(e),isSecurityViolation:false,isTimeout:false}); }); 'started';")

                val rawResult = withTimeoutOrNull(timeoutMs + 5000L) {
                    while (true) {
                        val raw = eval(webView, "window.__runtimeResults && window.__runtimeResults[$tokenJs] ? window.__runtimeResults[$tokenJs] : null")
                        val decoded = decodeJsString(raw)
                        if (!decoded.isNullOrBlank()) return@withTimeoutOrNull decoded
                        delay(40)
                    }
                    @Suppress("UNREACHABLE_CODE")
                    null
                }

                if (rawResult == null) {
                    ExecutionResult(
                        isSuccess = false,
                        output = "",
                        errorMessage = "TypeScript runtime did not return within the host timeout",
                        executionTimeMs = System.currentTimeMillis() - startedAt,
                        isTimeout = true
                    )
                } else {
                    val obj = JSONObject(rawResult)
                    ExecutionResult(
                        isSuccess = obj.optBoolean("success", false),
                        output = obj.optString("output", ""),
                        errorMessage = obj.optString("errorMessage", null),
                        executionTimeMs = System.currentTimeMillis() - startedAt,
                        isSecurityViolation = obj.optBoolean("isSecurityViolation", false),
                        isTimeout = obj.optBoolean("isTimeout", false)
                    )
                }
            } catch (e: Exception) {
                ExecutionResult(
                    isSuccess = false,
                    output = "",
                    errorMessage = "TypeScript runtime error: ${e.message}",
                    executionTimeMs = System.currentTimeMillis() - startedAt
                )
            } finally {
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.destroy()
            }
        }
    }

    private fun createWebView(context: Context, onPageReady: () -> Unit): WebView {
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()

        return WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            webViewClient = object : WebViewClientCompat() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                    return assetLoader.shouldInterceptRequest(request.url) ?: blockedResponse()
                }

                @Suppress("DEPRECATION")
                override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
                    return assetLoader.shouldInterceptRequest(Uri.parse(url)) ?: blockedResponse()
                }

                override fun onPageFinished(view: WebView, url: String) {
                    if (url.startsWith("https://appassets.androidplatform.net/")) onPageReady()
                }
            }
        }
    }

    private fun blockedResponse(): WebResourceResponse = WebResourceResponse(
        "text/plain",
        "UTF-8",
        403,
        "Blocked by learning sandbox",
        mapOf("Cache-Control" to "no-store"),
        ByteArrayInputStream("Network access blocked".toByteArray())
    )

    private suspend fun eval(webView: WebView, script: String): String = suspendCancellableCoroutine { cont ->
        webView.evaluateJavascript(script) { value ->
            if (cont.isActive) cont.resume(value ?: "null")
        }
    }

    private fun decodeJsString(raw: String): String? {
        if (raw == "null" || raw.isBlank()) return null
        return when (val value = JSONTokener(raw).nextValue()) {
            is String -> value
            JSONObject.NULL -> null
            else -> value?.toString()
        }
    }
}
