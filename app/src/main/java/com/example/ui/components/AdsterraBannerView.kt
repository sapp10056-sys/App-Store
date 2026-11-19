package com.example.ui.components

import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun AdsterraBannerView(
    modifier: Modifier = Modifier
) {
    val adHtml = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body { margin: 0; padding: 0; background: transparent; display: flex; justify-content: center; align-items: center; height: 100vh; overflow: hidden; }
            </style>
        </head>
        <body>
            <script type="text/javascript">
                atOptions = {
                    'key' : 'c9e23b9f09f4aeac887add61cb10085d',
                    'format' : 'iframe',
                    'height' : 50,
                    'width' : 320,
                    'params' : {}
                };
            </script>
            <script type="text/javascript" src="https://bicea.org/22/c9e23b9f09f4aeac887add61cb10085d"></script>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadsImagesAutomatically = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(false)
                }
                setBackgroundColor(0)
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                loadDataWithBaseURL("https://bicea.org", adHtml, "text/html", "UTF-8", null)
            }
        }
    )
}
