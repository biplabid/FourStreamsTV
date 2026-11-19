package com.streamcast.mobile

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.streamcast.mobile.ui.navigation.StreamCastNavHost
import com.streamcast.mobile.ui.theme.StreamCastTheme
import com.streamcast.mobile.viewmodel.CastViewModel
import com.streamcast.mobile.viewmodel.ChannelsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var castViewModel: CastViewModel
    private lateinit var channelsViewModel: ChannelsViewModel

    /**
     * Notification permission is only needed for the Cast playback notification
     * on Android 13+. It is requested quietly; declining it costs the
     * notification, not the casting.
     */
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        castViewModel = ViewModelProvider(this, CastViewModel.Factory(this))[
            CastViewModel::class.java
        ]
        channelsViewModel = ViewModelProvider(this, ChannelsViewModel.Factory(this))[
            ChannelsViewModel::class.java
        ]

        // Initialises CastContext, which must happen on the main thread.
        castViewModel.initialise()
        castViewModel.refreshEnvironment(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        // Honour the keep-screen-awake preference.
        lifecycleScope.launch {
            channelsViewModel.settings.collectLatest { settings ->
                if (settings.keepScreenAwake) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }

        setContent {
            StreamCastTheme {
                StreamCastNavHost(
                    channelsViewModel = channelsViewModel,
                    castViewModel = castViewModel
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Wi-Fi and permission state can change while the app is backgrounded.
        castViewModel.refreshEnvironment(this)
    }

    override fun onPause() {
        super.onPause()
        // Active scanning is battery-expensive; stop it when not visible.
        castViewModel.stopDiscovery()
    }
}
