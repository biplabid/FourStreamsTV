package com.streamcast.tv.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.streamcast.shared.data.ChannelFilters
import com.streamcast.shared.data.PlaylistRepository
import com.streamcast.shared.data.UserPreferences
import com.streamcast.shared.model.Channel
import com.streamcast.shared.model.ChannelCategory
import com.streamcast.shared.model.PlaylistState
import kotlinx.coroutines.launch

/**
 * Android TV / Google TV leanback entry point.
 *
 * Reads the same playlist repository as the phone app, so the channel rows are
 * real rather than placeholder. Playback on the TV itself is not wired up yet -
 * this module exists so a receiver-side app can grow from it.
 */
class TvMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = PlaylistRepository(applicationContext)
        val preferences = UserPreferences(applicationContext)

        lifecycleScope.launch {
            repository.initialise(preferences.settings.value.playlistUrl)
        }

        setContent {
            val state by repository.state.collectAsState()
            TvBrowseScreen(state = state, onPlayChannel = { })
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvBrowseScreen(
    state: PlaylistState,
    onPlayChannel: (Channel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101114))
            .padding(horizontal = 48.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column {
            Text(
                text = "StreamCast TV",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )
            Text(
                text = when {
                    state.isLoading && state.channels.isEmpty() -> "Loading channels…"
                    state.channels.isEmpty() ->
                        state.errorMessage ?: "No channels available"
                    else -> "${state.channels.size} live Indian channels"
                },
                color = Color(0xFFA7A9B2),
                fontSize = 14.sp
            )
        }

        if (state.channels.isEmpty()) return@Column

        val categories = ChannelFilters.availableCategories(state.channels)

        categories.forEach { category ->
            val rowChannels = state.channels.filter { it.category == category }.take(20)
            if (rowChannels.isNotEmpty()) {
                ChannelRow(
                    title = category.displayName,
                    channels = rowChannels,
                    onPlayChannel = onPlayChannel
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChannelRow(
    title: String,
    channels: List<Channel>,
    onPlayChannel: (Channel) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        LazyRow(
            contentPadding = PaddingValues(end = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(channels, key = { it.id }) { channel ->
                Card(
                    onClick = { onPlayChannel(channel) },
                    modifier = Modifier.size(width = 240.dp, height = 150.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(88.dp)
                                .background(Color(0xFF1B1B1F)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (channel.logoUrl != null) {
                                AsyncImage(
                                    model = channel.logoUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize().padding(14.dp)
                                )
                            } else {
                                Text(
                                    text = channel.name.take(2).uppercase(),
                                    color = Color(0xFFC9BEFF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                )
                            }
                        }
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = channel.name,
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = listOfNotNull(channel.language, channel.quality)
                                    .joinToString(" • ")
                                    .ifBlank { channel.category.displayName },
                                color = Color(0xFFA7A9B2),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
