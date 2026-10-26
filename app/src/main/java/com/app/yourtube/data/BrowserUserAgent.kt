package com.app.yourtube.data

/** User-Agent peramban desktop yang dipakai untuk semua permintaan jaringan. */
const val BROWSER_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"

private const val ANDROID_USER_AGENT = "com.google.android.youtube/19.28.35 (Linux; U; Android 14) gzip"
private const val IOS_USER_AGENT = "com.google.ios.youtube/19.45.4 (iPhone16,2; U; CPU iOS 18_1_0 like Mac OS X;)"

/**
 * URL stream YouTube hanya diterima bila User-Agent cocok dengan klien yang membuatnya
 * (parameter `c=` pada URL). Selain itu dipakai User-Agent peramban.
 */
fun userAgentForStream(url: String): String = when {
    url.contains("c=ANDROID") -> ANDROID_USER_AGENT
    url.contains("c=IOS") -> IOS_USER_AGENT
    else -> BROWSER_USER_AGENT
}
