package com.app.nontonku

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.nontonku.theme.NontonKuTheme
import com.app.nontonku.ui.MainViewModel
import com.app.nontonku.ui.NontonApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        // Kait uji otomatis (emulator): buka video pertama di beranda tanpa sentuhan.
        val autoFirst = intent?.getIntExtra("auto_first", 0) == 1
        setContent {
            NontonKuTheme {
                val vm: MainViewModel = viewModel()
                vm.applyAutoFirst(autoFirst)
                NontonApp(vm)
            }
        }
    }
}
