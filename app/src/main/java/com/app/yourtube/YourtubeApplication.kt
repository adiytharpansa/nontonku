package com.app.yourtube

import android.app.Application
import com.app.yourtube.data.VideoRepository

class YourtubeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Pemanasan mesin ekstraksi (tetap dimuat malas di repositori bila dilewati).
        Thread { try { VideoRepository.init() } catch (e: Exception) { } }.start()
    }
}
