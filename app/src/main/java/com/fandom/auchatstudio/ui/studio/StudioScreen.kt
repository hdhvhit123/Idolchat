package com.fandom.auchatstudio.ui.studio

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
            when (event) {
                is StudioEvent.Toast -> {
                    snackbar.currentSnackbarData?.dismiss()
                    snackbar.showSnackbar(event.message, duration = SnackbarDuration.Short)
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

@OptIn(ExperimentalMaterial3Api::class)
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
            if (lastCount == -1) listState.scrollToItem(count - 1) else listState.animateScrollToItem(count - 1)
        }
        if (!state.loading) lastCount = count
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.story?.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "Studio · ${style.preset.displayName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại") }
                },
                actions = {
                    IconButton(onClick = onCycleTheme, modifier = Modifier.testTag("cycle_theme")) {
                        Icon(Icons.Outlined.Palette, contentDescription = "Đổi phong cách")
                    }
                    IconButton(onClick = onPlay, enabled = state.items.isNotEmpty(), modifier = Modifier.testTag("play_story")) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Chế độ Tap-to-Chat")
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
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(style.background)),
        ) {
            ChatHeader(title = state.headerTitle, partners = state.partners, style = style)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
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
                        item(key = "bottom_space") { Spacer(Modifier.height(8.dp)) }
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
