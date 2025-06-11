package com.example.chichooappclient

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager // 1. 已添加 CookieManager 的 import
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    private var uploadMessage: ValueCallback<Array<Uri>>? = null
    private val fileChooserResultCode = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // 确保调用 super.onCreate

        // 确保你的Java后端在4567端口运行，并且托管了前端文件
        val startUrl = "http://10.0.2.2:4567/register.html"

        setContentView(R.layout.activity_main)
        WebView.setWebContentsDebuggingEnabled(true)
        webView = findViewById(R.id.webview)

        // 设置 WebViewClient
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                view?.loadUrl(url)
                return true
            }
        }

        // 设置 WebChromeClient
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                myWebView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                uploadMessage?.onReceiveValue(null)
                uploadMessage = filePathCallback
                val intent = fileChooserParams.createIntent()
                try {
                    startActivityForResult(intent, fileChooserResultCode)
                } catch (e: ActivityNotFoundException) {
                    uploadMessage = null
                    Toast.makeText(this@MainActivity, "无法打开文件选择器", Toast.LENGTH_LONG).show()
                    return false
                }
                return true
            }

            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                AlertDialog.Builder(this@MainActivity)
                    .setMessage(message)
                    .setPositiveButton("确定") { _, _ -> result?.confirm() }
                    .setCancelable(false)
                    .create()
                    .show()
                return true
            }
        }

        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true

        // CookieManager 的使用现在是安全的，因为已经 import
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.loadUrl(startUrl)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data) // 2. 已添加 super 调用
        if (requestCode == fileChooserResultCode) {
            if (uploadMessage == null) return
            uploadMessage?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data))
            uploadMessage = null
        }
    }

    override fun onPause() {
        super.onPause() // 3. 已添加 super 调用
        CookieManager.getInstance().flush()
    }

    override fun onResume() {
        super.onResume() // 4. 已添加 super 调用
        CookieManager.getInstance().flush()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed() // 5. 已添加 super 调用
        }
    }
}