package com.fandom.auchatstudio.ui.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.core.ThemePreset
import com.fandom.auchatstudio.core.VoiceFormat
import com.fandom.auchatstudio.data.CharacterEntity
import com.fandom.auchatstudio.ui.components.Avatar
import com.fandom.auchatstudio.ui.theme.ChatTextStyle
import java.io.File

private val AvatarSize = 30.dp
private val AvatarGap = 8.dp

/** Mock voice-note waveform drawn on a Canvas (design document §6.1). */
@Composable
fun AudioWaveformMock(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val barCount = WaveformHeights.size
        val barWidth = size.width / (barCount * 1.6f)
        for (i in 0 until barCount) {
            val barHeight = size.height * WaveformHeights[i]
            val startX = i * barWidth * 1.6f + barWidth / 2
            val startY = (size.height - barHeight) / 2
            drawLine(
                color = color,
                start = Offset(startX, startY),
                end = Offset(startX, startY + barHeight),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

val WaveformHeights = listOf(
    0.4f, 0.7f, 1.0f, 0.6f, 0.3f, 0.8f, 0.5f, 0.9f, 0.4f,
    0.7f, 0.9f, 0.5f, 0.8f, 0.3f, 0.6f, 0.8f, 0.5f, 0.4f,
)

fun ChatStyle.bubbleShape(outgoing: Boolean, lastInRun: Boolean): Shape {
    val r = cornerDp.dp
    val tail = 5.dp
    return when {
        !lastInRun -> RoundedCornerShape(r)
        outgoing -> RoundedCornerShape(topStart = r, topEnd = r, bottomStart = r, bottomEnd = tail)
        else -> RoundedCornerShape(topStart = r, topEnd = r, bottomStart = tail, bottomEnd = r)
    }
}

private fun Modifier.bubbleSurface(style: ChatStyle, outgoing: Boolean, shape: Shape): Modifier {
    var m = this
    if (style.shadow) m = m.shadow(elevation = 2.dp, shape = shape, ambientColor = Color(0x337CB8EB), spotColor = Color(0x337CB8EB))
    m = m.clip(shape)
    m = if (outgoing) m.background(style.outgoingBrush) else m.background(Color(style.incomingBubble))
    val border = style.bubbleBorder
    if (border != null && !outgoing) m = m.border(style.bubbleBorderWidthDp.dp, Color(border), shape)
    return m
}

/** One row of the fake chat: narration pill or avatar + bubble. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageRow(
    item: ChatItem,
    style: ChatStyle,
    showSenderName: Boolean,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val clickModifier = if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(onClick = { onClick?.invoke() }, onLongClick = onLongClick)
    } else {
        Modifier
    }
    val highlight = if (selected) Modifier.background(Color(style.accent).copy(alpha = 0.16f)) else Modifier
    val topGap = if (item.firstInRun) 8.dp else 2.dp

    if (item.isNarration) {
        Box(
            modifier = modifier.fillMaxWidth().then(highlight).then(clickModifier).padding(vertical = 8.dp, horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.message.text,
                color = Color(style.narrationText),
                style = ChatTextStyle.copy(fontSize = 12.sp, lineHeight = 16.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(style.narrationBackground))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(highlight)
            .then(clickModifier)
            .padding(start = 12.dp, end = 12.dp, top = topGap),
        horizontalAlignment = if (item.outgoing) Alignment.End else Alignment.Start,
    ) {
        if (showSenderName && !item.outgoing && item.firstInRun) {
            Text(
                text = item.speaker?.name ?: "Không rõ",
                color = Color(style.senderName),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = AvatarSize + AvatarGap + 4.dp, bottom = 2.dp),
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            if (!item.outgoing) {
                if (item.lastInRun) {
                    val s = item.speaker
                    Avatar(
                        name = s?.name ?: "?",
                        colorHex = s?.roleColorHex ?: 0xFF9E9E9E,
                        model = s?.avatarPath,
                        size = AvatarSize,
                    )
                } else {
                    Spacer(Modifier.width(AvatarSize))
                }
                Spacer(Modifier.width(AvatarGap))
            } else {
                Spacer(Modifier.width(48.dp))
            }
            MessageBubble(item = item, style = style)
            if (!item.outgoing) Spacer(Modifier.width(48.dp))
        }
        val time = item.message.timeLabel
        if (time != null && item.lastInRun) {
            Text(
                text = time,
                color = Color(style.timeText),
                fontSize = 11.sp,
                modifier = Modifier.padding(
                    top = 2.dp,
                    start = if (item.outgoing) 0.dp else AvatarSize + AvatarGap + 4.dp,
                    end = 4.dp,
                ),
            )
        }
    }
}

@Composable
private fun MessageBubble(item: ChatItem, style: ChatStyle) {
    val shape = style.bubbleShape(item.outgoing, item.lastInRun)
    val textColor = Color(if (item.outgoing) style.outgoingText else style.incomingText)
    val m = item.message
    when (m.type) {
        MessageType.IMAGE -> {
            val path = m.imagePath
            Box(
                modifier = Modifier
                    .widthIn(max = 220.dp)
                    .heightIn(max = 280.dp)
                    .clip(RoundedCornerShape(style.cornerDp.dp))
                    .background(Color(style.incomingBubble)),
            ) {
                if (path != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(File(path)).crossfade(true).build(),
                        contentDescription = "Ảnh trong tin nhắn",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.widthIn(min = 120.dp, max = 220.dp).heightIn(min = 120.dp, max = 280.dp),
                    )
                }
            }
        }

        MessageType.VOICE -> Row(
            modifier = Modifier
                .bubbleSurface(style, item.outgoing, shape)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .semantics { contentDescription = "Tin nhắn thoại ${VoiceFormat.format(m.voiceSeconds)}" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(textColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            AudioWaveformMock(color = textColor.copy(alpha = 0.9f), modifier = Modifier.width(110.dp).height(26.dp))
            Spacer(Modifier.width(8.dp))
            Text(VoiceFormat.format(m.voiceSeconds), color = textColor, fontSize = 12.sp)
        }

        else -> Text(
            text = m.text,
            color = textColor,
            style = ChatTextStyle,
            modifier = Modifier
                .bubbleSurface(style, item.outgoing, shape)
                .padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

/** Three bouncing dots in an incoming/outgoing bubble while a message is being "typed". */
@Composable
fun TypingIndicatorRow(
    speaker: CharacterEntity?,
    outgoing: Boolean,
    style: ChatStyle,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "typing")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val dotColor = Color(if (outgoing) style.outgoingText else style.incomingText)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp)
            .semantics { contentDescription = "Đang soạn tin" },
        horizontalArrangement = if (outgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!outgoing) {
            Avatar(
                name = speaker?.name ?: "?",
                colorHex = speaker?.roleColorHex ?: 0xFF9E9E9E,
                model = speaker?.avatarPath,
                size = AvatarSize,
            )
            Spacer(Modifier.width(AvatarGap))
        }
        Row(
            modifier = Modifier
                .bubbleSurface(style, outgoing, style.bubbleShape(outgoing, lastInRun = true))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) { i ->
                // Each dot peaks one third of a cycle after the previous one.
                val distance = ((phase - i + 3f) % 3f)
                val lift = if (distance < 1f) 1f - kotlin.math.abs(distance * 2f - 1f) else 0f
                Box(
                    modifier = Modifier
                        .padding(bottom = (lift * 4f).dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(dotColor.copy(alpha = 0.45f + 0.55f * lift)),
                )
            }
        }
    }
}

/** Themed fake app header (back arrow, partner avatars, name, call icons). */
@Composable
fun ChatHeader(
    title: String,
    partners: List<CharacterEntity>,
    style: ChatStyle,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val content = Color(style.headerContent)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(style.headerBackground))
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = content)
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        HeaderAvatars(partners, style.headerBackground)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = content, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = when (style.preset) {
                    ThemePreset.LYSN_BUBBLE -> "Bubble · đang hoạt động"
                    ThemePreset.WEVERSE_DM -> "● Online"
                    ThemePreset.INSTAGRAM_DM -> "Đang hoạt động"
                },
                color = Color(style.headerSubtle),
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
        Icon(Icons.Outlined.Call, contentDescription = null, tint = content, modifier = Modifier.padding(horizontal = 8.dp))
        Icon(Icons.Outlined.Videocam, contentDescription = null, tint = content, modifier = Modifier.padding(start = 8.dp, end = 12.dp))
    }
}

@Composable
private fun HeaderAvatars(partners: List<CharacterEntity>, background: Long, size: Dp = 36.dp) {
    if (partners.isEmpty()) {
        Avatar(name = "?", colorHex = 0xFF9E9E9E, model = null, size = size)
        return
    }
    if (partners.size == 1) {
        val p = partners[0]
        Avatar(name = p.name, colorHex = p.roleColorHex, model = p.avatarPath, size = size)
        return
    }
    // Two overlapping avatars for group chats.
    Box(Modifier.size(size)) {
        val small = size * 0.72f
        val a = partners[0]
        val b = partners[1]
        Avatar(a.name, a.roleColorHex, a.avatarPath, Modifier.align(Alignment.TopStart), small)
        Avatar(b.name, b.roleColorHex, b.avatarPath, Modifier.align(Alignment.BottomEnd), small, ringColor = Color(background))
    }
}

/** Small strip of the preset's colours, used on cards and pickers. */
@Composable
fun ThemeSwatch(style: ChatStyle, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(style.background))
            .border(1.dp, Color(0x22000000), RoundedCornerShape(12.dp))
            .padding(8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                Modifier
                    .width(34.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(style.incomingBubble))
                    .border(0.5.dp, style.bubbleBorder?.let { Color(it) } ?: Color.Transparent, RoundedCornerShape(5.dp)),
            )
            Box(
                Modifier
                    .padding(start = 12.dp)
                    .width(34.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(style.outgoingBrush),
            )
        }
    }
}
