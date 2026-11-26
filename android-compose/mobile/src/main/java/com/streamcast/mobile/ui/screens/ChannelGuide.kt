package com.streamcast.mobile.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamcast.mobile.guide.ChannelInfo
import com.streamcast.mobile.guide.EpgStatus
import com.streamcast.mobile.guide.Programme
import com.streamcast.mobile.ui.theme.StreamCastColors
import com.streamcast.shared.model.Channel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * "About this channel": an encyclopaedia summary where one exists, the
 * catalogue facts, and what is on right now.
 *
 * [scrollable] lets it scroll on its own when it is given a fixed slot beside
 * the schedule; inside a lazy list it must not, or it would nest scrolling.
 */
@Composable
fun AboutChannelCard(
    channel: Channel,
    info: ChannelInfo?,
    loading: Boolean,
    onNow: Programme?,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false
) {
    val context = LocalContext.current
    var expanded by rememberSaveable(channel.id) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(StreamCastColors.SurfaceSecondary)
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(14.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PanelTitle(icon = Icons.Rounded.Info, text = "About ${channel.name}")

        if (onNow != null) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("ON NOW", color = StreamCastColors.PrimaryFixedDim, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(onNow.title, color = Color.White, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold)
            }
        }

        val summary = info?.summary
        when {
            summary != null -> {
                Text(
                    text = summary,
                    color = StreamCastColors.TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    maxLines = if (expanded) Int.MAX_VALUE else 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { expanded = !expanded }
                )
                val url = info.summaryUrl
                Text(
                    text = "Source: ${info.summarySource}" + if (url != null) " ↗" else "",
                    color = StreamCastColors.TextSecondary,
                    fontSize = 10.sp,
                    modifier = Modifier.clickable(enabled = url != null) {
                        url?.let { context.openUrl(it) }
                    }
                )
            }

            loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    color = StreamCastColors.PrimaryFixedDim,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(14.dp)
                )
                Text("  Looking up this channel…", color = StreamCastColors.TextSecondary,
                    fontSize = 12.sp)
            }

            else -> Text(
                text = fallbackDescription(channel, info),
                color = StreamCastColors.TextPrimary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }

        // Facts from the catalogue and the playlist itself.
        info?.network?.let { FactRow("Network", it) }
        info?.owners?.takeIf { it.isNotEmpty() }?.let { FactRow("Owned by", it.joinToString(", ")) }
        info?.launched?.let { FactRow("On air since", formatLaunch(it)) }
        FactRow("Genre", channel.category.displayName)
        channel.language?.let { FactRow("Language", it) }
        channel.quality?.let { FactRow("Stream", it) }

        info?.website?.let { site ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { context.openUrl(site) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null,
                    tint = StreamCastColors.PrimaryFixedDim, modifier = Modifier.size(14.dp))
                Text("Official website", color = StreamCastColors.PrimaryFixedDim,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * The broadcast schedule, opening on what is on now. [fillHeight] gives it a
 * scrolling list of its own; otherwise give the modifier a fixed height.
 */
@Composable
fun ScheduleCard(
    channel: Channel,
    programmes: List<Programme>,
    status: EpgStatus,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Ticks so the NOW marker and progress bar move without user input.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }

    // Show a little of what just finished for context, then the rest of today on.
    val visible = remember(programmes, now / 600_000) {
        val firstCurrent = programmes.indexOfFirst { it.stopMillis > now }
        if (firstCurrent < 0) emptyList() else programmes.drop((firstCurrent - 2).coerceAtLeast(0))
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(StreamCastColors.SurfaceSecondary)
            .padding(top = 14.dp, start = 14.dp, end = 14.dp)
    ) {
        PanelTitle(icon = Icons.Rounded.CalendarMonth, text = "Schedule")

        when {
            visible.isNotEmpty() -> {
                val listState = rememberLazyListState()
                // Open on the programme that is on now.
                LaunchedEffect(channel.id, visible.isNotEmpty()) {
                    val index = visible.indexOfFirst { it.isOnAt(now) }
                    if (index > 0) listState.scrollToItem(index)
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(top = 8.dp)
                ) {
                    itemsIndexed(visible, key = { _, p -> p.startMillis }) { index, programme ->
                        val previous = visible.getOrNull(index - 1)
                        if (previous == null || !sameDay(previous.startMillis, programme.startMillis)) {
                            DayHeader(programme.startMillis, now)
                        }
                        ProgrammeRow(programme = programme, now = now)
                    }
                }
            }

            status is EpgStatus.Loading || status is EpgStatus.Idle -> ScheduleMessage(
                text = "Loading the programme guide…",
                busy = true
            )

            status is EpgStatus.Failed -> ScheduleMessage(
                text = status.message,
                actionLabel = "Retry",
                onAction = onRetry
            )

            else -> ScheduleMessage(
                text = "${channel.name} does not publish a schedule to the free programme " +
                    "guide. Listings are available for most major networks."
            )
        }
    }
}

@Composable
private fun ProgrammeRow(programme: Programme, now: Long) {
    val live = programme.isOnAt(now)
    val past = programme.stopMillis <= now
    var expanded by remember(programme.startMillis) { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (live) StreamCastColors.PrimaryViolet.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(enabled = programme.description != null) { expanded = !expanded }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.width(58.dp)) {
            Text(
                text = clock(programme.startMillis),
                color = if (past) StreamCastColors.TextSecondary else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = clock(programme.stopMillis),
                color = StreamCastColors.TextSecondary,
                fontSize = 10.sp
            )
        }
        Column(modifier = Modifier.weight(1f).animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (live) {
                    Text(
                        "NOW",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(StreamCastColors.PrimaryViolet)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                    Box(Modifier.width(6.dp))
                }
                Text(
                    text = programme.title,
                    color = if (past) StreamCastColors.TextSecondary else Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (live) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            programme.description?.let { desc ->
                Text(
                    text = desc,
                    color = StreamCastColors.TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = if (expanded) Int.MAX_VALUE else if (live) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (live) {
                val span = (programme.stopMillis - programme.startMillis).coerceAtLeast(1)
                LinearProgressIndicator(
                    progress = { ((now - programme.startMillis).toFloat() / span).coerceIn(0f, 1f) },
                    color = StreamCastColors.PrimaryFixedDim,
                    trackColor = StreamCastColors.SurfaceBright,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(3.dp)
                )
            }
        }
    }
}

@Composable
private fun DayHeader(millis: Long, now: Long) {
    val label = when {
        sameDay(millis, now) -> "Today"
        sameDay(millis, now + DAY_MS) -> "Tomorrow"
        else -> SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(Date(millis))
    }
    Text(
        text = label.uppercase(),
        color = StreamCastColors.TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 2.dp)
    )
}

@Composable
private fun ScheduleMessage(
    text: String,
    busy: Boolean = false,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (busy) {
                CircularProgressIndicator(
                    color = StreamCastColors.PrimaryFixedDim,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(14.dp)
                )
                Box(Modifier.width(8.dp))
            }
            Text(text, color = StreamCastColors.TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
        }
        if (actionLabel != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null,
                    tint = StreamCastColors.PrimaryFixedDim, modifier = Modifier.size(14.dp))
                Text(actionLabel, color = StreamCastColors.PrimaryFixedDim, fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PanelTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = StreamCastColors.PrimaryFixedDim,
            modifier = Modifier.size(16.dp))
        Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FactRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = StreamCastColors.TextSecondary, fontSize = 12.sp,
            modifier = Modifier.width(110.dp))
        Text(value, color = StreamCastColors.TextPrimary, fontSize = 12.sp,
            fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}

/** Used when no encyclopaedia article exists: a plain sentence from what we know. */
private fun fallbackDescription(channel: Channel, info: ChannelInfo?): String {
    val genre = channel.category.displayName.lowercase()
    val from = info?.network ?: info?.owners?.firstOrNull()
    return buildString {
        append("${channel.name} is a live $genre channel")
        if (from != null) append(" from $from")
        channel.language?.let { append(", broadcasting in $it") }
        append(".")
        if (channel.isGeoRestricted) append(" The stream may only play from within India.")
    }
}

private fun formatLaunch(raw: String): String = runCatching {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(raw)!!
    SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(date)
}.getOrDefault(raw)

private fun clock(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

private fun sameDay(a: Long, b: Long): Boolean {
    val ca = Calendar.getInstance().apply { timeInMillis = a }
    val cb = Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
        ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

private const val DAY_MS = 24L * 60 * 60 * 1000

private fun android.content.Context.openUrl(url: String) {
    runCatching {
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
