package com.streamcast.mobile.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.streamcast.mobile.ui.theme.StreamCastColors
import com.streamcast.mobile.viewmodel.CastViewModel
import com.streamcast.shared.model.DiscoveryState
import com.streamcast.shared.model.TvDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastDevicePickerModal(
    castViewModel: CastViewModel,
    onDismissRequest: () -> Unit,
    onOpenRemote: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val discoveryState by castViewModel.discoveryState.collectAsState()
    val connectedDevice by castViewModel.connectedDevice.collectAsState()
    val currentCastingChannel by castViewModel.currentCastingChannel.collectAsState()
    val hasApprovals by castViewModel.hasApprovals.collectAsState()
    val isWifiConnected by castViewModel.isWifiConnected.collectAsState()

    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        "android.permission.NEARBY_WIFI_DEVICES"
    } else {
        android.Manifest.permission.ACCESS_FINE_LOCATION
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        castViewModel.onPermissionsApproved(context)
    }

    LaunchedEffect(Unit) {
        castViewModel.checkApprovals(context)
        if (hasApprovals) {
            castViewModel.startDiscovery()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = StreamCastColors.Background,
        scrimColor = Color.Black.copy(alpha = 0.65f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Modal Header Row with Refresh & Close Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(StreamCastColors.SecondaryContainer.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CastConnected,
                            contentDescription = null,
                            tint = StreamCastColors.SecondaryCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Cast to TV Device",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StreamCastColors.TextPrimary
                        )
                        Text(
                            text = if (connectedDevice != null) "Connected: ${connectedDevice?.name}" else "Select a TV device on your Wi-Fi network",
                            style = MaterialTheme.typography.bodySmall,
                            color = StreamCastColors.TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { castViewModel.startDiscovery() }) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh",
                            tint = StreamCastColors.TextSecondary
                        )
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = StreamCastColors.TextSecondary
                        )
                    }
                }
            }

            // 1. Approval / Permission Rationale Card if missing
            if (!hasApprovals) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(StreamCastColors.CardSurface)
                        .border(1.dp, StreamCastColors.PrimaryViolet.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (!isWifiConnected) Icons.Rounded.WifiOff else Icons.Rounded.Security,
                                contentDescription = null,
                                tint = StreamCastColors.SecondaryCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = if (!isWifiConnected) "Wi-Fi Connection Required" else "Device Approvals Required",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = StreamCastColors.TextPrimary
                                )
                                Text(
                                    text = if (!isWifiConnected) "Connect to Wi-Fi to find Google TV & Chromecast devices." else "Grant nearby device permission to discover local TV receivers.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StreamCastColors.TextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = { launcher.launch(permissionToRequest) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StreamCastColors.PrimaryViolet)
                        ) {
                            Text(
                                text = "Grant Approvals & Scan Network",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 2. Currently Connected Banner (with prominent Disconnect & Remote buttons)
            connectedDevice?.let { device ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(StreamCastColors.SurfaceLow)
                        .border(1.dp, StreamCastColors.LiveGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                LivePulseDot()
                                Column {
                                    Text(
                                        text = "CASTING ACTIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StreamCastColors.LiveGreen
                                    )
                                    Text(
                                        text = device.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = StreamCastColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    currentCastingChannel?.let { ch ->
                                        Text(
                                            text = "Now Streaming: ${ch.name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = StreamCastColors.SecondaryCyan,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { castViewModel.disconnectDevice() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StreamCastColors.ErrorContainer)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = StreamCastColors.OnErrorContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text(
                                    text = "Disconnect",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StreamCastColors.OnErrorContainer
                                )
                            }

                            OutlinedButton(
                                onClick = { onOpenRemote() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SettingsRemote,
                                    contentDescription = null,
                                    tint = StreamCastColors.SecondaryCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Remote", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StreamCastColors.SecondaryCyan)
                            }
                        }
                    }
                }
            }

            // 3. Discovery State & Device List
            when (val state = discoveryState) {
                is DiscoveryState.Searching -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = StreamCastColors.PrimaryViolet,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Scanning Wi-Fi network for Chromecast & Google TV...",
                            style = MaterialTheme.typography.bodySmall,
                            color = StreamCastColors.TextSecondary
                        )
                    }
                }
                is DiscoveryState.Found -> {
                    Text(
                        text = "Available TV Devices (${state.devices.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = StreamCastColors.TextSecondary
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        items(state.devices) { device ->
                            val isConnected = device.id == connectedDevice?.id
                            DevicePickerItemRow(
                                device = device,
                                isConnected = isConnected,
                                onConnect = { castViewModel.selectDeviceToCast(device) },
                                onDisconnect = { castViewModel.disconnectDevice() }
                            )
                        }
                    }
                }
                is DiscoveryState.Connecting -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = StreamCastColors.SecondaryCyan,
                            strokeWidth = 2.dp
                        )
                        Column {
                            Text(
                                text = "Connecting to ${state.device.name}...",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = StreamCastColors.TextPrimary
                            )
                            Text(
                                text = "Launching media session on physical TV",
                                style = MaterialTheme.typography.bodySmall,
                                color = StreamCastColors.TextSecondary
                            )
                        }
                    }
                }
                is DiscoveryState.Failed -> {
                    Text(
                        text = state.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = StreamCastColors.Error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DevicePickerItemRow(
    device: TvDevice,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(StreamCastColors.CardSurface)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isConnected) StreamCastColors.SecondaryContainer.copy(alpha = 0.2f)
                        else StreamCastColors.SurfaceSecondary
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Rounded.CastConnected else Icons.Rounded.Tv,
                    contentDescription = null,
                    tint = if (isConnected) StreamCastColors.SecondaryCyan else StreamCastColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = StreamCastColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isConnected) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = StreamCastColors.LiveGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = "${device.deviceType} • ${device.ipAddress}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StreamCastColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isConnected) {
            Button(
                onClick = onDisconnect,
                modifier = Modifier.height(34.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StreamCastColors.ErrorContainer)
            ) {
                Text(
                    text = "Disconnect",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StreamCastColors.OnErrorContainer
                )
            }
        } else {
            Button(
                onClick = onConnect,
                modifier = Modifier.height(34.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StreamCastColors.PrimaryViolet)
            ) {
                Text(
                    text = "Connect",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
