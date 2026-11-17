package com.example.tvtube

import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.util.concurrent.TimeUnit

class AppDownloader : Downloader() {
    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS).build()

    override fun execute(request: Request): Response {
        val method = request.httpMethod()
        val url = request.url()
        val data = request.dataToSend()
        val body: RequestBody? = when {
            data != null -> data.toRequestBody()
            method == "POST" -> ByteArray(0).toRequestBody()
            else -> null
        }
        val b = okhttp3.Request.Builder().method(method, body).url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; rv:128.0) Gecko/20100101 Firefox/128.0")
        request.headers().forEach { (k, vals) ->
            b.removeHeader(k); vals.forEach { b.addHeader(k, it) }
        }
        val r = client.newCall(b.build()).execute()
        if (r.code == 429) { r.close(); throw ReCaptchaException("reCaptcha", url) }
        val text = r.body?.string()
        return Response(r.code, r.message, r.headers.toMultimap(), text, r.request.url.toString())
    }
}
