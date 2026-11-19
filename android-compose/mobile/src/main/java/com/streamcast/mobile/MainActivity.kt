package com.streamcast.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.streamcast.mobile.ui.navigation.StreamCastNavHost
import com.streamcast.mobile.ui.theme.StreamCastTheme
import com.streamcast.mobile.viewmodel.CastViewModel

class MainActivity : ComponentActivity() {

    private lateinit var castViewModel: CastViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        castViewModel = ViewModelProvider(this, CastViewModel.Factory(this))[CastViewModel::class.java]

        setContent {
            StreamCastTheme {
                StreamCastNavHost(castViewModel = castViewModel)
            }
        }
    }
}
