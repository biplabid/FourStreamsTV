package com.streamcast.mobile.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamcast.mobile.atv.VoiceState
import com.streamcast.mobile.ui.theme.StreamCastColors

/**
 * Hold-to-talk for TV voice search.
 *
 * ## Why hold rather than tap
 * The protocol has no endpointing: the TV acts on the audio when the session
 * closes, and nothing decides when speech has finished. Holding makes the user
 * the endpoint detector, which is both simpler and more predictable than a
 * timer guessing at pauses. It also means the microphone is open only while a
 * finger is on the button, which is a privacy property worth having and worth
 * being able to see.
 *
 * The ring tracks the microphone's actual level, so a muted or blocked
 * microphone is visible immediately rather than discovered when the TV
 * searches for nothing.
 */
@Composable
fun HoldToTalkButton(
    enabled: Boolean,
    state: VoiceState,
    amplitude: Float,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val listening = state is VoiceState.Listening

    // The ring breathes with the voice; smoothing keeps it from jittering on
    // every buffer.
    val level by animateFloatAsState(
        targetValue = if (listening) amplitude.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(90),
        label = "voiceLevel"
    )
    val buttonScale by animateFloatAsState(
        targetValue = if (listening) 1.06f else 1f,
        label = "voiceScale"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Outer ring, driven by loudness.
            if (listening) {
                Box(
                    modifier = Modifier
                        .size((96 + level * 54).dp)
                        .clip(CircleShape)
                        .background(StreamCastColors.PrimaryViolet.copy(alpha = 0.16f))
                )
            }

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .scale(buttonScale)
                    .clip(CircleShape)
                    .background(
                        if (listening) StreamCastColors.PrimaryViolet
                        else StreamCastColors.SurfaceHighest
                    )
                    .border(
                        width = if (listening) 2.dp else 1.dp,
                        color = if (listening) {
                            StreamCastColors.PrimaryFixedDim
                        } else {
                            StreamCastColors.Outline.copy(alpha = 0.8f)
                        },
                        shape = CircleShape
                    )
                    .alpha(if (enabled) 1f else 0.4f)
                    .pointerInput(enabled) {
                        if (!enabled) return@pointerInput
                        detectTapGestures(
                            onPress = {
                                view.tick(strong = true)
                                onStart()
                                try {
                                    // The microphone stays open exactly as long
                                    // as the finger does.
                                    tryAwaitRelease()
                                } finally {
                                    onStop()
                                    view.tick()
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = "Hold to speak",
                    tint = if (listening) Color.White else StreamCastColors.TextPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Text(
            text = when (state) {
                VoiceState.Listening -> "Listening — release to search"
                VoiceState.Preparing -> "Opening the microphone…"
                VoiceState.NeedsPermission -> "Microphone permission needed"
                VoiceState.Unsupported -> "This TV did not offer voice search"
                is VoiceState.Failed -> state.message
                VoiceState.Idle -> if (enabled) "Hold to speak to your TV" else "Connect a TV first"
            },
            color = when (state) {
                VoiceState.Listening -> StreamCastColors.PrimaryFixedDim
                VoiceState.Unsupported, is VoiceState.Failed, VoiceState.NeedsPermission ->
                    StreamCastColors.Error

                else -> StreamCastColors.TextSecondary
            },
            fontSize = 12.sp,
            fontWeight = if (listening) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )

        AnimatedVisibility(
            visible = state is VoiceState.Unsupported,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = "Some TVs only accept voice from their own remote. The search " +
                    "button still opens search on the TV.",
                color = StreamCastColors.TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(2.dp))
    }
}
