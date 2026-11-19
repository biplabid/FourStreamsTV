package com.streamcast.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TvOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamcast.mobile.ui.theme.StreamCastColors
import com.streamcast.shared.model.ChannelCategory

@Composable
fun LiveTvScreen(
    initialCategoryName: String? = null,
    onOpenPlayer: (String) -> Unit,
    onCast: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember(initialCategoryName) {
        mutableStateOf(
            if (initialCategoryName != null) {
                ChannelCategory.entries.firstOrNull { it.name == initialCategoryName } ?: ChannelCategory.ALL
            } else {
                ChannelCategory.ALL
            }
        )
    }
    var isGridView by remember { mutableStateOf(true) }

    val filteredChannels = remember(searchQuery, selectedCategory) {
        StreamCastMockData.Channels.filter { item ->
            val matchesCategory = selectedCategory == ChannelCategory.ALL || item.channel.category == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                    item.channel.name.contains(searchQuery, ignoreCase = true) ||
                    item.currentShow.contains(searchQuery, ignoreCase = true) ||
                    item.channel.category.displayName.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    // Dynamic category counts calculated directly from available channels
    val filterCategories = remember {
        listOf(ChannelCategory.ALL) + ChannelCategory.entries.filter { it != ChannelCategory.ALL && it != ChannelCategory.OTHERS }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamCastColors.Background)
    ) {
        // Top Navigation Header & Search Bar Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Live TV Channel Browser",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StreamCastColors.TextPrimary
                    )
                    Text(
                        text = "${StreamCastMockData.Channels.size} Live Streams available",
                        style = MaterialTheme.typography.bodySmall,
                        color = StreamCastColors.TextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StreamCastColors.CardSurface)
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh",
                            tint = StreamCastColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isGridView) StreamCastColors.PrimaryViolet else StreamCastColors.CardSurface)
                            .clickable { isGridView = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GridView,
                            contentDescription = "Grid View",
                            tint = if (isGridView) Color.White else StreamCastColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (!isGridView) StreamCastColors.PrimaryViolet else StreamCastColors.CardSurface)
                            .clickable { isGridView = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ViewList,
                            contentDescription = "List View",
                            tint = if (!isGridView) Color.White else StreamCastColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Search Bar Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(StreamCastColors.CardSurface)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = StreamCastColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )

                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    cursorBrush = SolidColor(StreamCastColors.PrimaryViolet),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = StreamCastColors.TextPrimary),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search channels by name, genre...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StreamCastColors.TextSecondary
                            )
                        }
                        innerTextField()
                    }
                )

                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear",
                            tint = StreamCastColors.TextSecondary
                        )
                    }
                }
            }

            // Dynamic Category Filter Pills Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterCategories) { category ->
                    val isSelected = selectedCategory == category
                    val count = if (category == ChannelCategory.ALL) {
                        StreamCastMockData.Channels.size
                    } else {
                        StreamCastMockData.Channels.count { it.channel.category == category }
                    }
                    val pillBg = if (isSelected) StreamCastColors.PrimaryViolet else StreamCastColors.CardSurface
                    val textCol = if (isSelected) Color.White else StreamCastColors.TextSecondary
                    val countBg = if (isSelected) Color.White.copy(alpha = 0.25f) else StreamCastColors.SurfaceSecondary

                    Row(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(CircleShape)
                            .background(pillBg)
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = textCol
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(countBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = textCol
                            )
                        }
                    }
                }
            }
        }

        // Channel Content (Grid or List or Empty State)
        if (filteredChannels.isEmpty()) {
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
                        imageVector = Icons.Rounded.TvOff,
                        contentDescription = null,
                        tint = StreamCastColors.TextSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No live channels found",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StreamCastColors.TextPrimary
                )
                Text(
                    text = "Try searching for another keyword or selecting All Channels.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StreamCastColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        searchQuery = ""
                        selectedCategory = ChannelCategory.ALL
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StreamCastColors.PrimaryViolet)
                ) {
                    Text("Reset Filters")
                }
            }
        } else if (isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredChannels, key = { it.channel.id }) { item ->
                    ChannelGridCard(
                        item = item,
                        onOpenPlayer = onOpenPlayer,
                        onCast = { onCast(item.channel.id) }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredChannels, key = { it.channel.id }) { item ->
                    TrendingChannelRow(
                        item = item,
                        onOpenPlayer = onOpenPlayer,
                        onCast = { onCast(item.channel.id) }
                    )
                }
            }
        }
    }
}
