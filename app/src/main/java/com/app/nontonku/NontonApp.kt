package com.app.nontonku

import android.app.Application
import com.app.nontonku.data.NpRepository

class NontonApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Pemanasan mesin ekstraksi (tetap dimuat malas di repositori bila dilewati).
        Thread { try { NpRepository.init() } catch (e: Exception) { } }.start()
    }
}
