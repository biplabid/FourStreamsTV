package com.streamcast.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamcast.mobile.ui.theme.StreamCastColors

@Composable
fun WifiRemoteScreen(onFindTv: () -> Unit) {
    var volumeLevel by remember { mutableIntStateOf(18) }
    var currentChannelNum by remember { mutableIntStateOf(1) }
    var isPlaying by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamCastColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Connected Device Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(StreamCastColors.CardSurface)
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(StreamCastColors.SecondaryContainer.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CastConnected,
                        contentDescription = null,
                        tint = StreamCastColors.SecondaryCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LivePulseDot()
                        Text(
                            text = "CONNECTED",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = StreamCastColors.LiveGreen
                        )
                    }
                    Text(
                        text = StreamCastMockData.ActiveConnectedTv.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StreamCastColors.TextPrimary
                    )
                    Text(
                        text = StreamCastMockData.ActiveConnectedTv.ipAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = StreamCastColors.TextSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(CircleShape)
                    .background(StreamCastColors.SurfaceSecondary)
                    .clickable { onFindTv() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Switch TV",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = StreamCastColors.SecondaryCyan
                )
            }
        }

        // 2. Primary Top Controls (Power, Voice Search, Mute, Home)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RemoteCircularButton(
                icon = Icons.Rounded.PowerSettingsNew,
                label = "Power",
                iconColor = Color.Red,
                onClick = { }
            )
            RemoteCircularButton(
                icon = Icons.Rounded.Mic,
                label = "Voice",
                iconColor = StreamCastColors.SecondaryCyan,
                onClick = { }
            )
            RemoteCircularButton(
                icon = Icons.Rounded.Home,
                label = "Home",
                iconColor = StreamCastColors.TextPrimary,
                onClick = { }
            )
            RemoteCircularButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                label = "Back",
                iconColor = StreamCastColors.TextPrimary,
                onClick = { }
            )
        }

        // 3. Tactile D-Pad Controller Circle
        Box(
            modifier = Modifier
                .size(230.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            StreamCastColors.CardSurface,
                            StreamCastColors.SurfaceSecondary,
                            StreamCastColors.SurfaceLow
                        )
                    )
                )
                .border(1.dp, StreamCastColors.Outline.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Up Button
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Up",
                    tint = StreamCastColors.TextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Down Button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Down",
                    tint = StreamCastColors.TextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Left Button
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 10.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Left",
                    tint = StreamCastColors.TextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Right Button
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Right",
                    tint = StreamCastColors.TextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Center OK Button
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(StreamCastColors.PrimaryViolet)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "OK",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // 4. Volume & Channel Rockers Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Volume Rocker Column
            Column(
                modifier = Modifier
                    .width(72.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(StreamCastColors.CardSurface)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { volumeLevel = (volumeLevel + 1).coerceAtMost(100) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Vol Up",
                        tint = StreamCastColors.TextPrimary
                    )
                }

                Text(
                    text = "VOL\n$volumeLevel",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = StreamCastColors.SecondaryCyan,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { volumeLevel = (volumeLevel - 1).coerceAtLeast(0) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "Vol Down",
                        tint = StreamCastColors.TextPrimary
                    )
                }
            }

            // Playback Toggle Center Column
            Column(
                modifier = Modifier
                    .width(72.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(StreamCastColors.CardSurface)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { isPlaying = !isPlaying },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = StreamCastColors.PrimaryViolet
                    )
                }

                Text(
                    text = "MEDIA",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = StreamCastColors.TextSecondary
                )

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { volumeLevel = 0 },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (volumeLevel == 0) Icons.AutoMirrored.Rounded.VolumeMute else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Mute",
                        tint = StreamCastColors.TextPrimary
                    )
                }
            }

            // Channel Rocker Column
            Column(
                modifier = Modifier
                    .width(72.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(StreamCastColors.CardSurface)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { currentChannelNum++ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "CH Up",
                        tint = StreamCastColors.TextPrimary
                    )
                }

                Text(
                    text = "CH\n#$currentChannelNum",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = StreamCastColors.LiveGreen,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { currentChannelNum = (currentChannelNum - 1).coerceAtLeast(1) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "CH Down",
                        tint = StreamCastColors.TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun RemoteCircularButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(StreamCastColors.CardSurface)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = StreamCastColors.TextSecondary
        )
    }
}
