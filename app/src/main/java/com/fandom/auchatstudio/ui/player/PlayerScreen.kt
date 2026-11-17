package com.fandom.auchatstudio.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fandom.auchatstudio.R
import com.fandom.auchatstudio.ui.appContainer
import com.fandom.auchatstudio.ui.chat.ChatHeader
import com.fandom.auchatstudio.ui.chat.ChatMessageRow
import com.fandom.auchatstudio.ui.chat.TypingIndicatorRow
import com.fandom.auchatstudio.ui.components.SystemBarsForBackground
import com.fandom.auchatstudio.ui.theme.AUChatStudioTheme

@Composable
fun PlayerScreen(storyId: Long, onBack: () -> Unit) {
    val container = appContainer()
    val vm: PlayerViewModel = viewModel(key = "player_$storyId") { PlayerViewModel(storyId, container.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    PlayerContent(
        state = state,
        onTap = vm::tap,
        onRestart = vm::restart,
        onAutoPlay = vm::setAutoPlay,
        onBack = onBack,
    )
}

@Composable
fun PlayerContent(
    state: PlayerUiState,
    onTap: () -> Unit,
    onRestart: () -> Unit,
    onAutoPlay: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val style = state.style
    val accent = Color(style.accent)
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    SystemBarsForBackground(darkBackground = style.isDark)

    val revealed = state.playback.revealed
    LaunchedEffect(revealed, state.isTyping) {
        val count = state.visible.size + if (state.isTyping) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }
    LaunchedEffect(revealed) {
        if (revealed > 0) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val nextLabel = stringResource(R.string.player_next_action)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(style.background))
            // Tap anywhere to advance. Not `clickable`, which would merge every bubble and
            // button into one accessibility node.
            .pointerInput(onTap) { detectTapGestures(onTap = { onTap() }) }
            .semantics { onClick(label = nextLabel) { onTap(); true } }
            .testTag("player_surface"),
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.background(Color(style.headerBackground)).statusBarsPadding()) {
                ChatHeader(title = state.headerTitle, partners = state.partners, style = style, onBack = onBack)
            }
            val progress by animateFloatAsState(state.playback.progress, tween(300), label = "progress")
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = accent,
                trackColor = accent.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Butt,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
            val notStarted = !state.loading && state.playback.total > 0 && state.playback.revealed == 0 && !state.isTyping
            // Purely visual: it must not take the tap, which belongs to the whole surface.
            androidx.compose.animation.AnimatedVisibility(
                visible = notStarted,
                enter = fadeIn(),
                exit = fadeOut(tween(150)),
                modifier = Modifier.align(Alignment.Center),
            ) {
                StartHint(
                    total = state.playback.total,
                    accent = accent,
                    onAccent = Color(style.outgoingText),
                    textColor = Color(style.narrationText),
                )
            }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier.fillMaxSize().testTag("player_messages"),
            ) {
                items(state.visible, key = { it.id }) { item ->
                    PopIn {
                        ChatMessageRow(item = item, style = style, showSenderName = state.isGroup)
                    }
                }
                if (state.isTyping) {
                    item(key = "typing") {
                        TypingIndicatorRow(
                            speaker = state.typingSpeaker,
                            outgoing = state.typingOutgoing,
                            style = style,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
            }
            // Material controls here (the auto-play switch) follow the skin, not the phone's
            // light/dark setting, like the composer in the studio.
            AUChatStudioTheme(darkTheme = style.isDark) {
                BottomBar(state = state, accent = accent, onRestart = onRestart, onAutoPlay = onAutoPlay, onBack = onBack)
            }
        }
    }
}

/** Fills the otherwise blank first screen and says what to do, without being a button itself. */
@Composable
private fun StartHint(total: Int, accent: Color, onAccent: Color, textColor: Color) {
    val pulse = rememberInfiniteTransition(label = "start")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "startScale",
    )
    Column(
        modifier = Modifier.padding(32.dp).testTag("player_start_hint"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(60.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.TouchApp, contentDescription = null, tint = onAccent, modifier = Modifier.size(30.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.player_tap_to_start), color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            pluralStringResource(R.plurals.player_start_hint, total, total),
            color = textColor.copy(alpha = 0.85f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/** Springy "pop" for a freshly revealed bubble. */
@Composable
private fun PopIn(content: @Composable () -> Unit) {
    val scale = remember { Animatable(0.88f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(160))
    }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
    }
    Box(
        Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            this.alpha = alpha.value
        },
    ) { content() }
}

@Composable
private fun BottomBar(
    state: PlayerUiState,
    accent: Color,
    onRestart: () -> Unit,
    onAutoPlay: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val style = state.style
    val subtle = Color(style.narrationText)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(style.headerBackground))
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.playback.isFinished && !state.loading) {
            Text(
                stringResource(if (state.playback.total == 0) R.string.player_empty else R.string.player_the_end),
                color = Color(style.headerContent),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("player_finished"),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack) { Text(stringResource(R.string.player_exit), color = Color(style.headerContent)) }
                FilledTonalButton(
                    onClick = onRestart,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = accent,
                        contentColor = Color(style.outgoingText),
                    ),
                    modifier = Modifier.testTag("player_restart"),
                ) {
                    Icon(Icons.Filled.Replay, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.player_replay))
                }
            }
        } else {
            val pulse = rememberInfiniteTransition(label = "hint")
            val hintAlpha by pulse.animateFloat(
                initialValue = 0.7f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                label = "hintAlpha",
            )
            val autoPlayLabel = stringResource(R.string.player_auto_play_action)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.TouchApp, null, tint = subtle, modifier = Modifier.size(18.dp).alpha(hintAlpha))
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.player_progress_hint, state.playback.revealed, state.playback.total),
                    color = subtle,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f).alpha(hintAlpha),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .semantics { onClick(label = autoPlayLabel) { onAutoPlay(!state.autoPlay); true } }
                        .padding(start = 8.dp),
                ) {
                    Text(stringResource(R.string.player_auto_play), color = subtle, fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Switch(
                        checked = state.autoPlay,
                        onCheckedChange = onAutoPlay,
                        colors = SwitchDefaults.colors(checkedTrackColor = accent),
                        modifier = Modifier.testTag("player_autoplay"),
                    )
                }
            }
        }
    }
}
