package com.mouradsoft.translator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mouradsoft.translator.data.*
import com.mouradsoft.translator.session.SessionViewModel
import com.mouradsoft.translator.ui.TranslatorApp

class MainActivity : ComponentActivity() {
    private val session by viewModels<SessionViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val service = if (BuildConfig.LIVE_ENABLED) LiveTranslationService(BuildConfig.BACKEND_URL, BuildConfig.DEBUG)
                    else DemoTranslationService(applicationContext::getString)
                return SessionViewModel(TranslationRepository(service)) as T
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT))
        setContent { TranslatorApp(session) }
    }
}
