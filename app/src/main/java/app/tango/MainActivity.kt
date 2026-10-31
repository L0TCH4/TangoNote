package app.tango

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import org.json.JSONObject
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import java.util.Locale
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewAssetLoader
import androidx.work.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var wv: WebView
    private var tts: TextToSpeech? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Bridge.pending = selectedText(intent)
        Notifier.ensureChannel(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "reminder", ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<ReminderWorker>(30, TimeUnit.MINUTES).build()
        )

        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this)).build()
        tts = TextToSpeech(this) { st -> if (st == TextToSpeech.SUCCESS) tts?.setLanguage(Locale.US) }
        wv = WebView(this)
        setContentView(wv)
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(v: WebView, r: WebResourceRequest) = loader.shouldInterceptRequest(r.url)
        }
        wv.addJavascriptInterface(Bridge(this) { text -> tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tango") }, "Android")
        wv.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }

    private fun selectedText(i: Intent?): String? =
        i?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()?.takeIf { it.isNotBlank() }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        selectedText(intent)?.let {
            wv.evaluateJavascript("window.handleText&&handleText(" + JSONObject.quote(it) + ")", null)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { if (wv.canGoBack()) wv.goBack() else super.onBackPressed() }
}

class Bridge(private val ctx: Context, private val speaker: (String) -> Unit) {
    @JavascriptInterface fun speak(t: String) { speaker(t) }
    companion object { @Volatile var pending: String? = null }
    @JavascriptInterface fun pending(): String { val p = pending ?: ""; pending = null; return p }
    @JavascriptInterface fun sync(json: String) {
        ctx.getSharedPreferences("t", 0).edit().putString("due", json).apply()
    }
    @JavascriptInterface fun test() { Notifier.show(ctx, 3) }
}
