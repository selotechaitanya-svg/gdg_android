package com.tidemark.habits

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tidemark.habits.data.settings.ThemeMode
import com.tidemark.habits.ui.navigation.TidemarkNavGraph
import com.tidemark.habits.ui.theme.TidemarkTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as TidemarkApp
        val container = app.container

        setContent {
            val themeMode by container.settingsRepository.themeModeFlow.collectAsStateWithLifecycle(
                initialValue = ThemeMode.SYSTEM
            )

            val useDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            var dateChangeSignal by remember { mutableStateOf(0L) }

            // Dynamic receiver for live midnight rollover and timezone updates while active
            DisposableEffect(Unit) {
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_DATE_CHANGED)
                    addAction(Intent.ACTION_TIME_SET)
                    addAction(Intent.ACTION_TIMEZONE_CHANGED)
                }
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        dateChangeSignal = System.currentTimeMillis()
                    }
                }
                registerReceiver(receiver, filter)
                onDispose {
                    unregisterReceiver(receiver)
                }
            }

            TidemarkTheme(darkTheme = useDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TidemarkNavGraph(
                        container = container,
                        onDateChangedTrigger = { dateChangeSignal = System.currentTimeMillis() }
                    )
                }
            }
        }
    }
}
