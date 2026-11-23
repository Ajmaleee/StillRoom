package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.StillroomApp
import com.example.ui.theme.StillroomTheme
import com.example.viewmodel.StillroomViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: StillroomViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val isIdle by viewModel.isIdle.collectAsStateWithLifecycle()

            LaunchedEffect(settings.autoDimEnabled, isIdle, settings.idleBrightness, settings.activeBrightness) {
                val lp = window.attributes
                lp.screenBrightness = if (settings.autoDimEnabled) {
                    if (isIdle) settings.idleBrightness.coerceIn(0.05f, 1f)
                    else settings.activeBrightness.coerceIn(0.1f, 1f)
                } else {
                    WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
                window.attributes = lp
            }

            StillroomTheme {
                StillroomApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshMediaSessions()
        viewModel.refreshCalendar()
    }
}
