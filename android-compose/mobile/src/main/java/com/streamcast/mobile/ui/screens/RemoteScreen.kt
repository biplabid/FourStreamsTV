package com.streamcast.mobile.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.streamcast.mobile.atv.AtvDevice
import com.streamcast.mobile.atv.AtvSessionState
import com.streamcast.mobile.atv.PairingPhase
import com.streamcast.mobile.atv.VoiceState
import com.streamcast.mobile.ui.theme.StreamCastColors
import com.streamcast.mobile.viewmodel.RemoteViewModel
import com.streamcast.shared.atv.AtvAppLink
import com.streamcast.shared.atv.AtvKey
import com.streamcast.shared.atv.PairingCrypto

/**
 * The Android TV remote.
 *
 * This drives the Android TV Remote v2 protocol directly - the same one the
 * Google TV app speaks - rather than Google Cast. Cast can only carry media
 * transport and volume for content this app itself started; it has no concept
 * of a D-pad, a home button, or the TV's own menus. Speaking the remote
 * protocol means the phone can drive the whole television, including apps this
 * app knows nothing about.
 */
@Composable
fun RemoteScreen(
    remoteViewModel: RemoteViewModel,
    onFindCastDevice: () -> Unit
) {
    val devices by remoteViewModel.devices.collectAsState()
    val searching by remoteViewModel.searching.collectAsState()
    val activeDevice by remoteViewModel.activeDevice.collectAsState()
    val sessionState by remoteViewModel.sessionState.collectAsState()
    val volume by remoteViewModel.volume.collectAsState()
    val currentApp by remoteViewModel.currentApp.collectAsState()
    val tvInfo by remoteViewModel.tvInfo.collectAsState()
    val textFieldLabel by remoteViewModel.textFieldLabel.collectAsState()
    val pairing by remoteViewModel.pairing.collectAsState()
    val connecting by remoteViewModel.connecting.collectAsState()
    val message by remoteViewModel.message.collectAsState()

    val voiceState by remoteViewModel.voiceState.collectAsState()
    val voiceAmplitude by remoteViewModel.voiceAmplitude.collectAsState()

    var showKeyboard by remember { mutableStateOf(false) }
    var showDevicePicker by remember { mutableStateOf(false) }

    // Remembered across process death: a user who prefers buttons should not
    // have to choose again every time.
    var useTrackpad by rememberSaveable { mutableStateOf(true) }

    val ready = sessionState is AtvSessionState.Ready

    // Microphone permission, asked for only when the mic is first held rather
    // than up front - the remote is fully usable without it.
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (!granted) remoteViewModel.clearVoiceState()
    }

    // Discovery costs multicast traffic and battery, so it only runs while
    // this screen is actually in front of the user.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> remoteViewModel.startDiscovery()
                Lifecycle.Event.ON_STOP -> remoteViewModel.stopDiscovery()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            remoteViewModel.stopDiscovery()
        }
    }

    // When the TV focuses a text field, offer the keyboard without being asked:
    // this is the moment the phone is genuinely better than the plastic remote.
    LaunchedEffect(textFieldLabel) {
        if (textFieldLabel != null && ready) showKeyboard = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RemoteHeader(
            device = activeDevice,
            sessionState = sessionState,
            connecting = connecting,
            tvModel = tvInfo?.let { listOfNotNull(it.vendor, it.model).joinToString(" ") },
            currentApp = currentApp,
            onPower = { remoteViewModel.key(AtvKey.POWER) },
            onChangeDevice = { showDevicePicker = true }
        )

        if (!ready) {
            ConnectionPanel(
                devices = devices,
                searching = searching,
                connecting = connecting,
                sessionState = sessionState,
                onConnect = remoteViewModel::connect,
                onPair = remoteViewModel::beginPairing,
                onRescan = {
                    remoteViewModel.stopDiscovery()
                    remoteViewModel.startDiscovery()
                },
                onFindCastDevice = onFindCastDevice
            )
        }

        PadModeToggle(
            trackpadSelected = useTrackpad,
            onSelect = { useTrackpad = it }
        )

        // Explicitly typed: the ViewModel's send methods return Unit? because
        // they go through a nullable session, which would otherwise infer a
        // () -> Unit? that no callback parameter accepts.
        val longPressCentre: () -> Unit = {
            remoteViewModel.beginLongPress(AtvKey.DPAD_CENTER)
            remoteViewModel.endLongPress(AtvKey.DPAD_CENTER)
        }

        if (useTrackpad) {
            TrackpadSurface(
                enabled = ready,
                onDirection = remoteViewModel::key,
                onSelect = { remoteViewModel.key(AtvKey.DPAD_CENTER) },
                onLongSelect = longPressCentre,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.08f)
            )
        } else {
            DpadSurface(
                enabled = ready,
                onKey = remoteViewModel::key,
                onLongSelect = longPressCentre,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
        }

        NavigationRow(enabled = ready, onKey = remoteViewModel::key)

        InputRow(
            enabled = ready,
            onSearch = { remoteViewModel.key(AtvKey.SEARCH) },
            onKeyboard = { showKeyboard = true },
            onAllApps = { remoteViewModel.key(AtvKey.ALL_APPS) }
        )

        SectionLabel("Voice search")
        HoldToTalkButton(
            enabled = ready,
            state = voiceState,
            amplitude = voiceAmplitude,
            onStart = {
                if (hasMicPermission) {
                    remoteViewModel.startVoice(true)
                } else {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onStop = { remoteViewModel.stopVoice() }
        )

        SectionLabel("Playback")
        TransportRow(enabled = ready, onKey = remoteViewModel::key)

        SectionLabel("Volume")
        VolumeControl(enabled = ready, volume = volume, onKey = remoteViewModel::key)

        SectionLabel("Jump to an app")
        AppShortcutRow(
            enabled = ready,
            links = AtvAppLink.DEFAULTS,
            currentApp = currentApp,
            onLaunch = { remoteViewModel.launchApp(it.uri) }
        )

        SectionLabel("Channel")
        ChannelPad(enabled = ready, onKey = remoteViewModel::key)
    }

    if (showKeyboard) {
        KeyboardDialog(
            fieldLabel = textFieldLabel,
            enabled = ready,
            onSend = { remoteViewModel.sendText(it) },
            onEnter = { remoteViewModel.key(AtvKey.ENTER) },
            onBackspace = { remoteViewModel.key(AtvKey.DEL) },
            onDismiss = { showKeyboard = false }
        )
    }

    if (showDevicePicker) {
        DevicePickerDialog(
            devices = devices,
            searching = searching,
            onConnect = {
                showDevicePicker = false
                remoteViewModel.connect(it)
            },
            onPair = {
                showDevicePicker = false
                remoteViewModel.beginPairing(it)
            },
            onForget = remoteViewModel::forget,
            onResetIdentity = {
                showDevicePicker = false
                remoteViewModel.resetIdentity()
            },
            onDismiss = { showDevicePicker = false }
        )
    }

    PairingDialog(
        phase = pairing,
        onSubmit = remoteViewModel::submitPairingCode,
        onCancel = remoteViewModel::cancelPairing,
        onConnect = remoteViewModel::connectAfterPairing
    )

    message?.let { text ->
        AlertDialog(
            onDismissRequest = remoteViewModel::consumeMessage,
            confirmButton = {
                TextButton(onClick = remoteViewModel::consumeMessage) { Text("OK") }
            },
            title = { Text("Remote") },
            text = { Text(text) },
            containerColor = StreamCastColors.SurfaceSecondary
        )
    }
}

// ------------------------------------------------------------------ header

@Composable
private fun RemoteHeader(
    device: AtvDevice?,
    sessionState: AtvSessionState,
    connecting: Boolean,
    tvModel: String?,
    currentApp: String?,
    onPower: () -> Unit,
    onChangeDevice: () -> Unit
) {
    val ready = sessionState is AtvSessionState.Ready
    val screenOn = (sessionState as? AtvSessionState.Ready)?.screenOn ?: false

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        StatusDot(ready = ready, connecting = connecting)
        Spacer(Modifier.size(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device?.name ?: "No TV connected",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1
            )
            Text(
                text = when {
                    connecting -> "Connecting…"
                    ready && !screenOn -> "Connected · screen off"
                    ready -> listOfNotNull(
                        tvModel?.takeIf { it.isNotBlank() },
                        currentApp?.substringAfterLast('.')?.replaceFirstChar { it.uppercase() }
                    ).joinToString(" · ").ifBlank { "Connected" }

                    sessionState is AtvSessionState.Disconnected ->
                        sessionState.reason ?: "Disconnected"

                    else -> "Choose a TV to begin"
                },
                color = StreamCastColors.TextSecondary,
                fontSize = 11.sp,
                maxLines = 2
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(StreamCastColors.SurfaceHighest)
                .clickable(onClick = onChangeDevice),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Tv,
                contentDescription = "Choose a TV",
                tint = StreamCastColors.TextSecondary,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(Modifier.size(8.dp))
        PowerButton(enabled = ready, onClick = onPower)
    }
}

/** Green when live, a slow pulse while connecting, grey otherwise. */
@Composable
private fun StatusDot(ready: Boolean, connecting: Boolean) {
    val transition = rememberInfiniteTransition(label = "status")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "pulse"
    )
    val colour = when {
        ready -> StreamCastColors.LiveGreen
        connecting -> StreamCastColors.PrimaryFixedDim.copy(alpha = pulse)
        else -> StreamCastColors.Outline
    }
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(colour)
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = StreamCastColors.TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp
    )
}

// -------------------------------------------------------------- connecting

@Composable
private fun ConnectionPanel(
    devices: List<AtvDevice>,
    searching: Boolean,
    connecting: Boolean,
    sessionState: AtvSessionState,
    onConnect: (AtvDevice) -> Unit,
    onPair: (AtvDevice) -> Unit,
    onRescan: () -> Unit,
    onFindCastDevice: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(StreamCastColors.SurfaceSecondary)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.SettingsRemote,
                contentDescription = null,
                tint = StreamCastColors.PrimaryFixedDim,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = if (devices.isEmpty()) "Looking for your TV" else "Pick a TV",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            if (searching) {
                CircularProgressIndicator(
                    color = StreamCastColors.PrimaryFixedDim,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = "Search again",
                    tint = StreamCastColors.TextSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onRescan)
                )
            }
        }

        if (devices.isEmpty()) {
            Text(
                text = "Your TV needs to be switched on and on the same Wi-Fi as this phone. " +
                    "Most Android TV and Google TV devices appear within a few seconds.",
                color = StreamCastColors.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        } else {
            devices.forEach { device ->
                DeviceRow(
                    device = device,
                    busy = connecting,
                    onClick = { if (device.isPaired) onConnect(device) else onPair(device) }
                )
            }
        }

        (sessionState as? AtvSessionState.Disconnected)?.reason?.let { reason ->
            Text(
                text = reason,
                color = StreamCastColors.Error,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onFindCastDevice)
                .padding(vertical = 6.dp)
        ) {
            Icon(
                Icons.Rounded.Cast,
                contentDescription = null,
                tint = StreamCastColors.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "Just want to cast a channel instead?",
                color = StreamCastColors.TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun DeviceRow(
    device: AtvDevice,
    busy: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(StreamCastColors.SurfaceHighest)
            .clickable(enabled = !busy, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Icon(
            Icons.Rounded.Tv,
            contentDescription = null,
            tint = if (device.online) StreamCastColors.LiveGreen else StreamCastColors.TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = when {
                    device.isPaired && device.online -> "Paired · ready"
                    device.isPaired -> "Paired · not seen on this network"
                    else -> "Tap to pair"
                },
                color = StreamCastColors.TextSecondary,
                fontSize = 11.sp
            )
        }
        Text(
            text = if (device.isPaired) "Connect" else "Pair",
            color = StreamCastColors.PrimaryFixedDim,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ----------------------------------------------------------------- pairing

/**
 * The pairing wizard.
 *
 * A mistyped code never reaches the TV - the last four characters are checked
 * against the first two locally - so a typo leaves the dialog open with the
 * same code still valid on screen rather than forcing the whole handshake to
 * start over.
 */
@Composable
private fun PairingDialog(
    phase: PairingPhase,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
    onConnect: (AtvDevice) -> Unit
) {
    if (phase is PairingPhase.Idle) return

    var code by remember(phase is PairingPhase.AwaitingCode) { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(phase) {
        if (phase is PairingPhase.AwaitingCode) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (phase !is PairingPhase.Submitting) onCancel()
        },
        containerColor = StreamCastColors.SurfaceSecondary,
        title = {
            Text(
                text = when (phase) {
                    is PairingPhase.Succeeded -> "Paired"
                    is PairingPhase.Failed -> "Pairing failed"
                    else -> "Pair with your TV"
                },
                color = Color.White
            )
        },
        text = {
            when (phase) {
                is PairingPhase.Connecting -> PairingBusy("Asking ${phase.device.name} for a code…")

                is PairingPhase.Submitting -> PairingBusy("Checking the code…")

                is PairingPhase.AwaitingCode -> Column {
                    Text(
                        text = "Your TV is showing a six-character code. Type it here.",
                        color = StreamCastColors.TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = code,
                        onValueChange = { input ->
                            // Hex only, so a stray letter cannot be typed at all.
                            code = input
                                .filter { it.isDigit() || it.uppercaseChar() in 'A'..'F' }
                                .take(PairingCrypto.CODE_LENGTH)
                                .uppercase()
                        },
                        singleLine = true,
                        label = { Text("Code") },
                        placeholder = { Text("A1B2C3") },
                        isError = phase.error != null,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (code.length == PairingCrypto.CODE_LENGTH) onSubmit(code)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                    phase.error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(text = it, color = StreamCastColors.Error, fontSize = 12.sp)
                    }
                }

                is PairingPhase.Failed -> Text(
                    text = phase.message,
                    color = StreamCastColors.TextSecondary,
                    fontSize = 13.sp
                )

                is PairingPhase.Succeeded -> Text(
                    text = "${phase.device.name} will remember this phone. " +
                        "You will not need the code again.",
                    color = StreamCastColors.TextSecondary,
                    fontSize = 13.sp
                )

                PairingPhase.Idle -> Unit
            }
        },
        confirmButton = {
            when (phase) {
                is PairingPhase.AwaitingCode -> TextButton(
                    enabled = code.length == PairingCrypto.CODE_LENGTH,
                    onClick = { onSubmit(code) }
                ) { Text("Pair") }

                is PairingPhase.Succeeded -> TextButton(
                    onClick = { onConnect(phase.device) }
                ) { Text("Use this TV") }

                is PairingPhase.Failed -> TextButton(onClick = onCancel) { Text("Close") }

                else -> Unit
            }
        },
        dismissButton = {
            if (phase is PairingPhase.AwaitingCode || phase is PairingPhase.Connecting) {
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun PairingBusy(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            color = StreamCastColors.PrimaryFixedDim,
            strokeWidth = 2.dp,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.size(12.dp))
        Text(text = label, color = StreamCastColors.TextSecondary, fontSize = 13.sp)
    }
}

// ----------------------------------------------------------- device picker

@Composable
private fun DevicePickerDialog(
    devices: List<AtvDevice>,
    searching: Boolean,
    onConnect: (AtvDevice) -> Unit,
    onPair: (AtvDevice) -> Unit,
    onForget: (AtvDevice) -> Unit,
    onResetIdentity: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StreamCastColors.SurfaceSecondary,
        title = { Text("Televisions", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (devices.isEmpty()) {
                    Text(
                        text = if (searching) "Searching the network…" else "Nothing found yet.",
                        color = StreamCastColors.TextSecondary,
                        fontSize = 13.sp
                    )
                }
                devices.forEach { device ->
                    DeviceRow(
                        device = device,
                        busy = false,
                        onClick = { if (device.isPaired) onConnect(device) else onPair(device) }
                    )
                    if (device.isPaired) {
                        Text(
                            text = "Forget ${device.name}",
                            color = StreamCastColors.Error,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clickable { onForget(device) }
                                .padding(start = 4.dp, bottom = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = "If a TV keeps refusing this phone, reset the pairing identity and " +
                        "pair again. You will need to re-pair every TV.",
                    color = StreamCastColors.TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = {
            TextButton(onClick = onResetIdentity) {
                Text("Reset identity", color = StreamCastColors.Error)
            }
        }
    )
}

// --------------------------------------------------------------- keyboard

/**
 * Sends typed text to whatever field the TV has focused.
 *
 * On-screen keyboards on a television are a grid you steer a cursor around one
 * letter at a time. Typing on the phone instead is the single biggest thing
 * this remote does that the plastic one cannot.
 */
@Composable
private fun KeyboardDialog(
    fieldLabel: String?,
    enabled: Boolean,
    onSend: (String) -> Unit,
    onEnter: () -> Unit,
    onBackspace: () -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StreamCastColors.SurfaceSecondary,
        modifier = Modifier.imePadding(),
        title = {
            Text(
                text = fieldLabel?.takeIf { it.isNotBlank() } ?: "Type on the TV",
                color = Color.White
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    placeholder = { Text("Text to send") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (text.isNotEmpty()) {
                                onSend(text)
                                text = ""
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Text lands wherever the cursor is on the TV. If nothing appears, " +
                        "the TV has no text field open.",
                    color = StreamCastColors.TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(enabled = enabled, onClick = onBackspace) { Text("Backspace") }
                    TextButton(enabled = enabled, onClick = onEnter) { Text("Enter") }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = enabled && text.isNotEmpty(),
                onClick = {
                    onSend(text)
                    text = ""
                }
            ) { Text("Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
