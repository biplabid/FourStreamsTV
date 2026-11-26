package com.streamcast.mobile.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.PublicOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsCricket
import androidx.compose.material.icons.rounded.TempleHindu
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.streamcast.mobile.ui.preview.PreviewVideoLayer
import com.streamcast.mobile.ui.theme.StreamCastColors
import com.streamcast.shared.model.Channel
import com.streamcast.shared.model.ChannelCategory
import com.streamcast.shared.model.TvDevice

/**
 * Shared building blocks for the channel screens.
 *
 * These render only what an M3U playlist actually provides: name, logo,
 * category, language, resolution hint and the geo-blocked marker.
 */

/** Length of the logo-to-video crossfade on a previewing card. */
const val PREVIEW_DISSOLVE_MS = 1_100

/** Pulsing dot used to mark a live stream. */
@Composable
fun LivePulseDot(
    modifier: Modifier = Modifier,
    color: Color = StreamCastColors.LiveGreen,
    size: Int = 8
) {
    val transition = rememberInfiniteTransition(label = "live-pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live-pulse-alpha"
    )
    Box(
        modifier = modifier
            .size(size.dp)
            .alpha(alpha)
            .background(color, CircleShape)
    )
}

@Composable
fun LiveBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LivePulseDot(size = 6)
        Text(
            text = "LIVE",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Channel logo with a letter-tile fallback. Most iptv-org entries have no
 * tvg-logo, so the fallback is the common case rather than the exception.
 */
@Composable
fun ChannelLogo(
    channel: Channel,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 10
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(StreamCastColors.SurfaceHighest),
        contentAlignment = Alignment.Center
    ) {
        if (channel.logoUrl != null) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
            )
        } else {
            Text(
                text = channel.name.take(2).uppercase(),
                color = StreamCastColors.PrimaryFixedDim,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

/** Small neutral chip for a fact drawn from the playlist. */
@Composable
fun MetaChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = StreamCastColors.TextSecondary
) {
    Row(
        modifier = modifier
            .background(StreamCastColors.SurfaceHighest.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(11.dp))
        }
        Text(text = text, color = tint, fontSize = 10.sp, maxLines = 1)
    }
}

/** Banner shown while a channel is on the TV. */
@Composable
fun ActiveCastBanner(
    device: TvDevice,
    channel: Channel?,
    statusText: String,
    onDisconnect: () -> Unit,
    onOpenRemote: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StreamCastColors.PrimaryViolet.copy(alpha = 0.16f))
            .border(1.dp, StreamCastColors.PrimaryViolet.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Rounded.CastConnected,
                contentDescription = null,
                tint = StreamCastColors.PrimaryFixedDim,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel?.name ?: "Ready to cast",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$statusText • ${device.name}",
                    color = StreamCastColors.TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton(
                text = "Remote",
                onClick = onOpenRemote,
                modifier = Modifier.weight(1f),
                filled = true
            )
            PillButton(
                text = "Disconnect",
                onClick = onDisconnect,
                modifier = Modifier.weight(1f),
                filled = false
            )
        }
    }
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    enabled: Boolean = true
) {
    val background = when {
        !enabled -> StreamCastColors.SurfaceHighest.copy(alpha = 0.4f)
        filled -> StreamCastColors.PrimaryViolet
        else -> Color.Transparent
    }
    val contentColor = when {
        !enabled -> StreamCastColors.TextSecondary.copy(alpha = 0.5f)
        filled -> Color.White
        else -> StreamCastColors.PrimaryFixedDim
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .then(
                if (!filled && enabled) {
                    Modifier.border(
                        1.dp,
                        StreamCastColors.PrimaryFixedDim.copy(alpha = 0.6f),
                        RoundedCornerShape(10.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Category shortcut tile. The count comes from the parsed playlist. */
@Composable
fun CategoryCard(
    category: ChannelCategory,
    channelCount: Int,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(StreamCastColors.CardSurface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(StreamCastColors.PrimaryViolet.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = StreamCastColors.PrimaryFixedDim,
                modifier = Modifier.size(20.dp)
            )
        }
        Column {
            Text(
                text = category.displayName,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1
            )
            Text(
                text = if (channelCount == 1) "1 channel" else "$channelCount channels",
                color = StreamCastColors.TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Wide card used in the horizontal rails on the home screen.
 */
@Composable
fun ChannelRailCard(
    channel: Channel,
    isFavourite: Boolean,
    onClick: () -> Unit,
    onCast: () -> Unit,
    onToggleFavourite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(168.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(StreamCastColors.CardSurface)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(StreamCastColors.SurfaceLow)
        ) {
            ChannelLogo(
                channel = channel,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp, 48.dp),
                cornerRadius = 8
            )
            LiveBadge(modifier = Modifier.align(Alignment.TopStart).padding(6.dp))
            IconButton(
                onClick = onToggleFavourite,
                modifier = Modifier.align(Alignment.TopEnd).size(30.dp)
            ) {
                Icon(
                    imageVector = if (isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (isFavourite) "Remove favourite" else "Add favourite",
                    tint = if (isFavourite) StreamCastColors.Error else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = onCast,
                modifier = Modifier.align(Alignment.BottomEnd).size(30.dp)
            ) {
                Icon(
                    Icons.Rounded.Cast,
                    contentDescription = "Cast ${channel.name}",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = channel.name,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = channelSubtitle(channel),
                color = StreamCastColors.TextSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Grid tile used in the Live TV browser.
 *
 * When [previewPlayer] is non-null this tile owns the single live preview: the
 * player renders underneath the logo straight away, and [previewRevealed]
 * triggers the crossfade once real video is on screen.
 */
@OptIn(UnstableApi::class)
@Composable
fun ChannelGridCard(
    channel: Channel,
    isFavourite: Boolean,
    onClick: () -> Unit,
    onCast: () -> Unit,
    onToggleFavourite: () -> Unit,
    modifier: Modifier = Modifier,
    previewPlayer: ExoPlayer? = null,
    previewRevealed: Boolean = false,
    onFullscreen: (() -> Unit)? = null
) {
    // Drives both halves of the dissolve: video fades up as the logo fades out.
    val videoAlpha by animateFloatAsState(
        targetValue = if (previewPlayer != null && previewRevealed) 1f else 0f,
        animationSpec = tween(durationMillis = PREVIEW_DISSOLVE_MS),
        label = "preview-dissolve"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(StreamCastColors.CardSurface)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(StreamCastColors.SurfaceLow)
        ) {
            PreviewVideoLayer(
                player = previewPlayer,
                alpha = videoAlpha,
                modifier = Modifier.fillMaxSize()
            )

            ChannelLogo(
                channel = channel,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(60.dp, 44.dp)
                    .alpha(1f - videoAlpha),
                cornerRadius = 8
            )
            if (channel.isGeoRestricted) {
                MetaChip(
                    text = "Geo",
                    icon = Icons.Rounded.PublicOff,
                    tint = StreamCastColors.Error,
                    modifier = Modifier.align(Alignment.TopStart).padding(5.dp)
                )
            } else {
                LiveBadge(modifier = Modifier.align(Alignment.TopStart).padding(5.dp))
            }
            IconButton(
                onClick = onToggleFavourite,
                modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
            ) {
                Icon(
                    imageVector = if (isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (isFavourite) "Remove favourite" else "Add favourite",
                    tint = if (isFavourite) StreamCastColors.Error else Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
            Row(modifier = Modifier.align(Alignment.BottomEnd)) {
                if (onFullscreen != null) {
                    IconButton(onClick = onFullscreen, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Rounded.Fullscreen,
                            contentDescription = "Play ${channel.name} full screen",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                IconButton(onClick = onCast, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Rounded.Cast,
                        contentDescription = "Cast ${channel.name}",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(9.dp)) {
            Text(
                text = channel.name,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = channelSubtitle(channel),
                color = StreamCastColors.TextSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Compact row used in list view and the favourites screen. */
@Composable
fun ChannelListRow(
    channel: Channel,
    isFavourite: Boolean,
    onClick: () -> Unit,
    onCast: () -> Unit,
    onToggleFavourite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StreamCastColors.CardSurface)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ChannelLogo(channel = channel, modifier = Modifier.size(52.dp, 38.dp), cornerRadius = 8)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.name,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                MetaChip(text = channel.category.displayName)
                channel.language?.let {
                    MetaChip(text = it, icon = Icons.Rounded.Language)
                }
                channel.quality?.let { MetaChip(text = it) }
                if (channel.isGeoRestricted) {
                    MetaChip(
                        text = "Geo-blocked",
                        icon = Icons.Rounded.PublicOff,
                        tint = StreamCastColors.Error
                    )
                }
            }
        }
        IconButton(onClick = onToggleFavourite, modifier = Modifier.size(34.dp)) {
            Icon(
                imageVector = if (isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (isFavourite) "Remove favourite" else "Add favourite",
                tint = if (isFavourite) StreamCastColors.Error else StreamCastColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onCast, modifier = Modifier.size(34.dp)) {
            Icon(
                Icons.Rounded.Cast,
                contentDescription = "Cast ${channel.name}",
                tint = StreamCastColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** Section heading with an optional trailing action. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            if (subtitle != null) {
                Text(text = subtitle, color = StreamCastColors.TextSecondary, fontSize = 11.sp)
            }
        }
        if (actionLabel != null && onActionClick != null) {
            Text(
                text = actionLabel,
                color = StreamCastColors.PrimaryFixedDim,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/** Shared empty / error / loading placeholder. */
@Composable
fun StatePlaceholder(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.LiveTv,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StreamCastColors.SurfaceSecondary)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = StreamCastColors.TextSecondary,
            modifier = Modifier.size(34.dp)
        )
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
        Text(
            text = message,
            color = StreamCastColors.TextSecondary,
            fontSize = 12.sp,
            style = MaterialTheme.typography.bodySmall
        )
        if (actionLabel != null && onAction != null) {
            PillButton(text = actionLabel, onClick = onAction, modifier = Modifier.width(180.dp))
        }
    }
}

/** Search field used by the Live TV browser. */
@Composable
fun ChannelSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search channels, language or genre"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StreamCastColors.SurfaceSecondary)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = null,
            tint = StreamCastColors.TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        androidx.compose.foundation.text.BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(StreamCastColors.PrimaryFixedDim),
            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = StreamCastColors.TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                inner()
            }
        )
        if (query.isNotEmpty()) {
            Text(
                text = "Clear",
                color = StreamCastColors.PrimaryFixedDim,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onQueryChange("") }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

/** Selectable category chip. */
@Composable
fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingCount: Int? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) StreamCastColors.PrimaryViolet
                else StreamCastColors.SurfaceHighest
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else StreamCastColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (trailingCount != null) {
            Text(
                text = trailingCount.toString(),
                color = if (selected) Color.White.copy(alpha = 0.8f)
                else StreamCastColors.TextSecondary.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
        }
    }
}

/** Second line under a channel name, built only from playlist fields. */
fun channelSubtitle(channel: Channel): String = listOfNotNull(
    channel.category.displayName,
    channel.language,
    channel.quality
).joinToString(" • ")

/** Icon for a category shortcut tile. */
fun iconForCategory(category: ChannelCategory): ImageVector = when (category) {
    ChannelCategory.NEWS -> Icons.Rounded.Newspaper
    ChannelCategory.ENTERTAINMENT -> Icons.Rounded.TheaterComedy
    ChannelCategory.MOVIES -> Icons.Rounded.Movie
    ChannelCategory.SPORTS -> Icons.Rounded.SportsCricket
    ChannelCategory.MUSIC -> Icons.Rounded.MusicNote
    ChannelCategory.REGIONAL -> Icons.Rounded.Translate
    ChannelCategory.KIDS -> Icons.Rounded.ChildCare
    ChannelCategory.DEVOTIONAL -> Icons.Rounded.TempleHindu
    ChannelCategory.OTHERS -> Icons.Rounded.LiveTv
    ChannelCategory.ALL -> Icons.Rounded.LiveTv
}
