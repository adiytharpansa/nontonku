package com.app.yourtube.data

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.util.concurrent.TimeUnit

/** Pengunduh NewPipeExtractor di atas OkHttp (pola resmi NewPipe). */
class ExtractorDownloader : Downloader() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    override fun execute(request: Request): Response {
        val url = request.url()
        val builder = okhttp3.Request.Builder().url(url)

        var hasUserAgent = false
        request.headers().forEach { (name, values) ->
            values.forEach { value -> builder.addHeader(name, value) }
            if (name.equals("User-Agent", ignoreCase = true)) hasUserAgent = true
        }
        if (!hasUserAgent) builder.header("User-Agent", BROWSER_USER_AGENT)

        val data = request.dataToSend()
        when (request.httpMethod().uppercase()) {
            "POST" -> builder.post((data ?: ByteArray(0)).toRequestBody())
            "PUT" -> builder.put((data ?: ByteArray(0)).toRequestBody())
            "DELETE" -> builder.delete()
            "HEAD" -> builder.head()
            else -> builder.get()
        }

        client.newCall(builder.build()).execute().use { resp ->
            if (resp.code == 429) throw ReCaptchaException("reCaptcha diperlukan", url)
            val body = resp.body?.string()
            val headers: Map<String, List<String>> = resp.headers.toMultimap()
            return Response(resp.code, resp.message, headers, body, url)
        }
    }
}
