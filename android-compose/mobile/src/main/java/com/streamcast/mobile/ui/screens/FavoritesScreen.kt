package com.streamcast.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.streamcast.mobile.ui.theme.StreamCastColors

@Composable
fun FavoritesScreen(
    onExplore: () -> Unit,
    onOpenPlayer: (String) -> Unit,
    onCast: (String) -> Unit = {}
) {
    val favoriteChannels = remember {
        StreamCastMockData.Channels.filter { it.isFavorite }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamCastColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = StreamCastColors.Error,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Favorite Channels",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = StreamCastColors.TextPrimary
                )
            }
            Text(
                text = "${favoriteChannels.size} Bookmarked Live Streams",
                style = MaterialTheme.typography.bodySmall,
                color = StreamCastColors.TextSecondary
            )
        }

        if (favoriteChannels.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(StreamCastColors.CardSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = StreamCastColors.TextSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No favorite channels yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StreamCastColors.TextPrimary
                )
                Text(
                    text = "Bookmark channels from the Live TV browser for quick access here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StreamCastColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onExplore,
                    colors = ButtonDefaults.buttonColors(containerColor = StreamCastColors.PrimaryViolet)
                ) {
                    Text("Explore Channels")
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(favoriteChannels) { item ->
                    ChannelGridCard(
                        item = item,
                        onOpenPlayer = onOpenPlayer,
                        onCast = { onCast(item.channel.id) }
                    )
                }
            }
        }
    }
}
