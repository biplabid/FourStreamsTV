package com.streamcast.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamcast.mobile.ui.theme.StreamCastColors
import com.streamcast.mobile.viewmodel.CastViewModel
import com.streamcast.mobile.viewmodel.ChannelsViewModel

@Composable
fun FavoritesScreen(
    channelsViewModel: ChannelsViewModel,
    castViewModel: CastViewModel,
    onOpenPlayer: (String) -> Unit,
    onExplore: () -> Unit
) {
    val favourites by channelsViewModel.favouriteChannels.collectAsState()
    val favouriteIds by channelsViewModel.favouriteIds.collectAsState()
    val playlist by channelsViewModel.playlist.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Favourites",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                text = when {
                    favourites.isNotEmpty() ->
                        "${favourites.size} saved ${if (favourites.size == 1) "channel" else "channels"}"
                    favouriteIds.isNotEmpty() && playlist.channels.isEmpty() -> "Waiting for playlist…"
                    else -> "Tap the heart on any channel to save it here"
                },
                color = StreamCastColors.TextSecondary,
                fontSize = 11.sp
            )
        }

        if (favourites.isEmpty()) {
            Box(modifier = Modifier.padding(16.dp)) {
                StatePlaceholder(
                    title = if (favouriteIds.isEmpty()) {
                        "No favourites yet"
                    } else {
                        "Saved channels not in this playlist"
                    },
                    message = if (favouriteIds.isEmpty()) {
                        "Save the channels you watch most and they will show up here, ready to cast."
                    } else {
                        "You have ${favouriteIds.size} saved channels, but none of them are in the " +
                            "playlist currently loaded. They will reappear if you switch back."
                    },
                    icon = Icons.Rounded.FavoriteBorder,
                    actionLabel = "Browse channels",
                    onAction = onExplore
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(favourites, key = { it.id }) { channel ->
                ChannelListRow(
                    channel = channel,
                    isFavourite = true,
                    onClick = { onOpenPlayer(channel.id) },
                    onCast = { castViewModel.castChannel(channel) },
                    onToggleFavourite = { channelsViewModel.toggleFavourite(channel.id) }
                )
            }
        }
    }
}
