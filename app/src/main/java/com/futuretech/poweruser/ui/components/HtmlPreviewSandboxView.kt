package com.futuretech.poweruser.ui.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun HtmlPreviewSandboxView(
    htmlContent: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.apply {
                    javaScriptEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                }
            }
        },
        update = { webView ->
            val wrappedHtml = """
                <! shelter html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { font-family: sans-serif; padding: 12px; margin: 0; background-color: #f8fafc; color: #0f172a; }
                    </style>
                </head>
                <body>
                    $htmlContent
                </body>
                </html>
            """.trimIndent()
            webView.loadDataWithBaseURL(null, wrappedHtml, "text/html", "UTF-8", null)
        },
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}
