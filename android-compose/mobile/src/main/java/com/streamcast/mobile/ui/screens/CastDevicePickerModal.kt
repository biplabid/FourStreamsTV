package com.streamcast.mobile.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.streamcast.mobile.ui.theme.StreamCastColors
import com.streamcast.mobile.viewmodel.CastViewModel
import com.streamcast.shared.model.DiscoveryState
import com.streamcast.shared.model.TvDevice

/**
 * Device picker driven entirely by live MediaRouter discovery.
 *
 * Requests the local-network permission first, because without it the system
 * returns an empty route list and the user is left staring at "no devices
 * found" with no explanation.
 */
@Composable
fun CastDevicePickerModal(
    castViewModel: CastViewModel,
    onDismissRequest: () -> Unit,
    onOpenRemote: () -> Unit
) {
    val context = LocalContext.current

    val discoveryState by castViewModel.discoveryState.collectAsState()
    val connectedDevice by castViewModel.connectedDevice.collectAsState()
    val hasPermission by castViewModel.hasDiscoveryPermission.collectAsState()
    val isWifiConnected by castViewModel.isWifiConnected.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        castViewModel.onPermissionResult(context, granted)
    }

    LaunchedEffect(Unit) {
        castViewModel.refreshEnvironment(context)
    }

    LaunchedEffect(hasPermission, isWifiConnected) {
        if (hasPermission && isWifiConnected) {
            castViewModel.startDiscovery()
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(StreamCastColors.SurfaceSecondary)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cast to",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Chromecast and Google TV devices on your Wi-Fi",
                        color = StreamCastColors.TextSecondary,
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = { castViewModel.startDiscovery() }) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = "Scan again",
                        tint = StreamCastColors.TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = StreamCastColors.TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            when {
                // Blockers first - these explain an empty list
                !isWifiConnected -> BlockerPanel(
                    icon = Icons.Rounded.WifiOff,
                    title = "Not on Wi-Fi",
                    message = "Cast devices are found over your local network. Connect this " +
                        "phone to the same Wi-Fi as your TV - mobile data cannot discover TVs.",
                    actionLabel = "Check again",
                    onAction = { castViewModel.refreshEnvironment(context) }
                )

                !hasPermission -> BlockerPanel(
                    icon = Icons.Rounded.Cast,
                    title = "Local network access needed",
                    message = "Android requires the nearby-devices permission before an app " +
                        "can discover Cast receivers on your Wi-Fi.",
                    actionLabel = "Grant permission",
                    onAction = { permissionLauncher.launch(castViewModel.requiredPermission) }
                )

                else -> when (val state = discoveryState) {
                    is DiscoveryState.Connected -> ConnectedPanel(
                        device = state.device,
                        onDisconnect = { castViewModel.disconnect() },
                        onOpenRemote = {
                            onDismissRequest()
                            onOpenRemote()
                        }
                    )

                    is DiscoveryState.Connecting -> Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = StreamCastColors.PrimaryViolet,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Connecting to ${state.device.name}…",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Waiting for the TV to accept the session",
                            color = StreamCastColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    is DiscoveryState.Searching -> Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = StreamCastColors.PrimaryViolet,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Looking for devices…",
                            color = StreamCastColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    is DiscoveryState.Found -> DeviceList(
                        devices = state.devices,
                        connectedId = connectedDevice?.id,
                        onConnect = { castViewModel.connectTo(it) }
                    )

                    is DiscoveryState.Failed -> BlockerPanel(
                        icon = Icons.Rounded.ErrorOutline,
                        title = "Could not connect",
                        message = state.reason,
                        actionLabel = "Scan again",
                        onAction = { castViewModel.startDiscovery() }
                    )

                    DiscoveryState.Empty -> BlockerPanel(
                        icon = Icons.Rounded.SearchOff,
                        title = "No devices found",
                        message = "Make sure your Chromecast or Google TV is powered on and " +
                            "joined to this same Wi-Fi network. Some routers block the mDNS " +
                            "traffic Cast relies on - check that AP isolation is off.",
                        actionLabel = "Scan again",
                        onAction = { castViewModel.startDiscovery() }
                    )

                    DiscoveryState.Idle -> Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = StreamCastColors.PrimaryViolet,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceList(
    devices: List<TvDevice>,
    connectedId: String?,
    onConnect: (TvDevice) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(devices, key = { it.id }) { device ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(StreamCastColors.CardSurface)
                    .clickable { onConnect(device) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(StreamCastColors.PrimaryViolet.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (device.id == connectedId) {
                            Icons.Rounded.CastConnected
                        } else {
                            Icons.Rounded.Tv
                        },
                        contentDescription = null,
                        tint = StreamCastColors.PrimaryFixedDim,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = device.deviceType,
                        color = StreamCastColors.TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = if (device.id == connectedId) "Connected" else "Connect",
                    color = StreamCastColors.PrimaryFixedDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ConnectedPanel(
    device: TvDevice,
    onDisconnect: () -> Unit,
    onOpenRemote: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(StreamCastColors.PrimaryViolet.copy(alpha = 0.16f))
                .padding(12.dp),
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
                    text = device.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Connected • ${device.deviceType}",
                    color = StreamCastColors.TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton(
                text = "Open remote",
                onClick = onOpenRemote,
                modifier = Modifier.weight(1f)
            )
            PillButton(
                text = "Disconnect",
                onClick = onDisconnect,
                filled = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BlockerPanel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StreamCastColors.SurfaceLow)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = StreamCastColors.TextSecondary,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        Text(text = message, color = StreamCastColors.TextSecondary, fontSize = 11.sp)
        PillButton(
            text = actionLabel,
            onClick = onAction,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }
}
