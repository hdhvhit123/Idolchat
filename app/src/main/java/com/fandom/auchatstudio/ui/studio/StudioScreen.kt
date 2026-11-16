package com.fandom.auchatstudio.ui.studio

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import com.fandom.auchatstudio.ui.theme.AUChatStudioTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.fandom.auchatstudio.ui.components.SystemBarsForBackground
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fandom.auchatstudio.core.NameRules
import com.fandom.auchatstudio.data.MessageEntity
import com.fandom.auchatstudio.ui.appContainer
import com.fandom.auchatstudio.ui.chat.ChatHeader
import com.fandom.auchatstudio.ui.chat.ChatMessageRow
import com.fandom.auchatstudio.ui.components.TextInputDialog

@Composable
fun StudioScreen(
    storyId: Long,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onOpenCharacters: () -> Unit,
) {
    val container = appContainer()
    val vm: StudioViewModel = viewModel(key = "studio_$storyId") {
        StudioViewModel(storyId, container.repository, container.exporter)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(vm) {
        vm.eventFlow.collect { event ->
            // Each event gets its own coroutine: showSnackbar suspends until the bar is gone, and
            // handling events one after another used to leave stale messages queued on screen.
            launch {
                snackbar.currentSnackbarData?.dismiss()
                when (event) {
                    is StudioEvent.Toast -> snackbar.showSnackbar(event.message, duration = SnackbarDuration.Short)
                    is StudioEvent.MessageDeleted -> {
                        val result = snackbar.showSnackbar(
                            message = "Đã xóa tin nhắn",
                            actionLabel = "Hoàn tác",
                            duration = SnackbarDuration.Long,
                        )
                        if (result == SnackbarResult.ActionPerformed) vm.undoDelete(event.message) else vm.finalizeDelete(event.message)
                    }
                    is StudioEvent.Exported -> {
                        val n = event.uris.size
                        val result = snackbar.showSnackbar(
                            message = if (n == 1) "Đã lưu ảnh vào Pictures/AU Chat Studio" else "Đã lưu $n ảnh vào Pictures/AU Chat Studio",
                            actionLabel = "Chia sẻ",
                            duration = SnackbarDuration.Long,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            context.startActivity(Intent.createChooser(shareIntent(event.uris), "Chia sẻ ảnh"))
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(state.missing) { if (state.missing) onBack() }

    StudioContent(
        state = state,
        snackbar = snackbar,
        onBack = onBack,
        onPlay = onPlay,
        onOpenCharacters = onOpenCharacters,
        onCycleTheme = vm::cycleTheme,
        onExport = vm::export,
        onRename = vm::rename,
        onSetChatTitle = vm::setChatTitle,
        onSelectSpeaker = vm::selectSpeaker,
        onSendText = vm::sendText,
        onSendVoice = vm::sendVoice,
        onSendImage = vm::sendImage,
        onUpdateMessage = vm::updateMessage,
        onChangeSpeaker = vm::changeSpeaker,
        onMove = vm::move,
        onDelete = vm::delete,
    )
}

private fun shareIntent(uris: List<Uri>): Intent {
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
    }
    intent.type = "image/png"
    intent.clipData = ClipData.newRawUri(null, uris[0]).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return intent
}

@Composable
fun StudioContent(
    state: StudioUiState,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onOpenCharacters: () -> Unit,
    onCycleTheme: () -> Unit,
    onExport: () -> Unit,
    onRename: (String) -> Unit,
    onSetChatTitle: (String) -> Unit,
    onSelectSpeaker: (Speaker) -> Unit,
    onSendText: (String) -> Boolean,
    onSendVoice: (Int) -> Unit,
    onSendImage: (Uri) -> Unit,
    onUpdateMessage: (MessageEntity, String, String, Int) -> Unit,
    onChangeSpeaker: (MessageEntity, Long) -> Unit,
    onMove: (MessageEntity, Int) -> Unit,
    onDelete: (MessageEntity) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var editingChatTitle by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var editing by remember { mutableStateOf<MessageEntity?>(null) }
    var askVoice by remember { mutableStateOf(false) }
    val style = state.style
    val listState = rememberLazyListState()
    var lastCount by remember { mutableIntStateOf(-1) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onSendImage(uri)
    }

    // Follow the newest message like a real chat, but don't jump on first load or edits.
    LaunchedEffect(state.items.size) {
        val count = state.items.size
        if (count > 0 && (lastCount == -1 || count > lastCount)) {
            // Index `count` is the footer, so the newest message and the footer are both in view.
            if (lastCount == -1) listState.scrollToItem(count) else listState.animateScrollToItem(count)
        }
        if (!state.loading) lastCount = count
    }

    SystemBarsForBackground(darkBackground = style.isDark)

    Scaffold(
        // The header draws behind the status bar itself; the composer handles the bottom insets.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ChatHeader(
                title = state.headerTitle,
                partners = state.partners,
                style = style,
                modifier = Modifier.background(Color(style.headerBackground)).statusBarsPadding(),
                onBack = onBack,
                onTitleClick = { editingChatTitle = true },
                subtitle = {
                    // Nothing until the story has loaded, rather than a half-filled line.
                    if (state.story != null) Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            state.story?.title ?: "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false).testTag("studio_story_title"),
                        )
                        Text(" · ")
                        Text(
                            style.preset.displayName,
                            maxLines = 1,
                            modifier = Modifier
                                .testTag("studio_theme_label")
                                .semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onCycleTheme, modifier = Modifier.testTag("cycle_theme")) {
                        Icon(Icons.Outlined.Palette, contentDescription = "Đổi phong cách khung chat")
                    }
                    // The one filled action in the header. Icon colour is the skin's "text on
                    // accent" colour, which is checked for contrast in ThemeContrastTest.
                    FilledIconButton(
                        onClick = onPlay,
                        enabled = state.items.isNotEmpty(),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(style.accent),
                            contentColor = Color(style.outgoingText),
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = LocalContentColor.current.copy(alpha = 0.38f),
                        ),
                        modifier = Modifier.testTag("play_story"),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Phát kiểu Tap-to-Chat")
                    }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = "Thêm") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(if (state.exporting) "Đang xuất ảnh…" else "Xuất ảnh cuộn dài") },
                                leadingIcon = { Icon(Icons.Outlined.IosShare, null) },
                                enabled = !state.exporting,
                                onClick = { menu = false; onExport() },
                            )
                            DropdownMenuItem(
                                text = { Text("Đổi tên kịch bản") },
                                leadingIcon = { Icon(Icons.Outlined.DriveFileRenameOutline, null) },
                                onClick = { menu = false; renaming = true },
                            )
                            DropdownMenuItem(
                                text = { Text("Tên trên khung chat") },
                                leadingIcon = { Icon(Icons.Outlined.Badge, null) },
                                onClick = { menu = false; editingChatTitle = true },
                            )
                            DropdownMenuItem(
                                text = { Text("Quản lý nhân vật") },
                                leadingIcon = { Icon(Icons.Outlined.Groups, null) },
                                onClick = { menu = false; onOpenCharacters() },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            // The composer takes the skin's brightness, so a dark skin is dark from top to bottom
            // even when the phone is in light mode (and the other way round).
            if (!state.loading) AUChatStudioTheme(darkTheme = style.isDark) {
            Composer(
                characters = state.characters,
                speaker = state.speaker,
                accent = Color(style.accent),
                onSelectSpeaker = onSelectSpeaker,
                onSendText = onSendText,
                onPickImage = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onVoice = { askVoice = true },
                onAddCharacter = onOpenCharacters,
            )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(style.background)),
        ) {
            if (state.exporting) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color(style.accent))
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color(style.accent))
                    state.items.isEmpty() -> EmptyChatHint(Color(style.narrationText), Modifier.align(Alignment.Center))
                    else -> LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.fillMaxSize().testTag("studio_messages"),
                    ) {
                        itemsIndexed(state.items, key = { _, it -> it.id }) { _, item ->
                            ChatMessageRow(
                                item = item,
                                style = style,
                                showSenderName = state.isGroup,
                                selected = selectedId == item.id,
                                onClick = { selectedId = item.id },
                                onLongClick = { selectedId = item.id },
                                modifier = Modifier.animateItem(),
                            )
                        }
                        item(key = "footer") {
                            ScriptFooter(
                                accent = Color(style.accent),
                                content = Color(style.narrationText),
                                exporting = state.exporting,
                                onPlay = onPlay,
                                onExport = onExport,
                            )
                        }
                    }
                }
            }
        }
    }

    val selectedIndex = state.items.indexOfFirst { it.id == selectedId }
    if (selectedIndex >= 0) {
        val item = state.items[selectedIndex]
        MessageActionsSheet(
            item = item,
            characters = state.characters,
            canMoveUp = selectedIndex > 0,
            canMoveDown = selectedIndex < state.items.lastIndex,
            onDismiss = { selectedId = null },
            onEdit = { editing = item.message; selectedId = null },
            onChangeSpeaker = { id -> onChangeSpeaker(item.message, id) },
            onMoveUp = { onMove(item.message, -1) },
            onMoveDown = { onMove(item.message, 1) },
            onDelete = { onDelete(item.message); selectedId = null },
        )
    }
    editing?.let { message ->
        EditMessageDialog(
            message = message,
            onDismiss = { editing = null },
            onSave = { text, time, seconds ->
                onUpdateMessage(message, text, time, seconds)
                editing = null
            },
        )
    }
    if (askVoice) {
        VoiceNoteDialog(
            onDismiss = { askVoice = false },
            onConfirm = { seconds ->
                onSendVoice(seconds)
                askVoice = false
            },
        )
    }
    if (renaming) {
        TextInputDialog(
            title = "Đổi tên kịch bản",
            label = "Tên kịch bản",
            initial = state.story?.title ?: "",
            confirmText = "Lưu",
            maxLength = NameRules.MAX_TITLE_LENGTH,
            onDismiss = { renaming = false },
            onConfirm = { onRename(it); renaming = false },
        )
    }
    if (editingChatTitle) {
        TextInputDialog(
            title = "Tên hiển thị trên khung chat",
            label = "VD: Minho 🐰",
            initial = state.story?.chatTitle ?: state.headerTitle,
            confirmText = "Lưu",
            maxLength = NameRules.MAX_NAME_LENGTH,
            onDismiss = { editingChatTitle = false },
            onConfirm = { onSetChatTitle(it); editingChatTitle = false },
        )
    }
}

/**
 * What to do with a finished script, right where the script ends. Shown only in the editor; it
 * is not part of the chat and never appears in the player or the exported image.
 */
@Composable
private fun ScriptFooter(accent: Color, content: Color, exporting: Boolean, onPlay: () -> Unit, onExport: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        // Label in the skin's readable secondary text colour; the accent only tints the outline.
        val colors = ButtonDefaults.outlinedButtonColors(contentColor = content, disabledContentColor = content.copy(alpha = 0.5f))
        val border = BorderStroke(1.dp, accent.copy(alpha = 0.6f))
        OutlinedButton(onClick = onPlay, colors = colors, border = border, modifier = Modifier.testTag("footer_play")) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Phát thử")
        }
        OutlinedButton(
            onClick = onExport,
            enabled = !exporting,
            colors = colors,
            border = border,
            modifier = Modifier.testTag("footer_export"),
        ) {
            Icon(Icons.Outlined.IosShare, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (exporting) "Đang xuất…" else "Xuất ảnh")
        }
    }
}

@Composable
private fun EmptyChatHint(color: Color, modifier: Modifier = Modifier) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("💬", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Chọn người nói ở thanh bên dưới rồi gõ tin nhắn đầu tiên.\nChạm vào một tin nhắn để sửa, đổi thứ tự hoặc xóa.",
            color = color,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
