package com.streamcast.tv.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.streamcast.shared.model.Channel

/**
 * Android TV / Google TV 10-foot leanback Compose entry point with D-pad focus support.
 */
class TvMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TvBrowseScreen(channels = emptyList(), onPlayChannel = {})
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvBrowseScreen(
    channels: List<Channel>,
    onPlayChannel: (Channel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101114))
            .padding(horizontal = 48.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(text = "StreamCast TV · Live Indian Channels", color = Color.White)

        LazyRow(
            contentPadding = PaddingValues(end = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(channels, key = { it.id }) { channel ->
                Card(
                    onClick = { onPlayChannel(channel) },
                    modifier = Modifier.size(width = 240.dp, height = 140.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = channel.name, color = Color.White)
                        Text(text = channel.category.displayName, color = Color(0xFFA7A9B2))
                    }
                }
            }
        }
    }
}
