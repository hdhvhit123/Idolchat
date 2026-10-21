package com.fandom.auchatstudio.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.core.NameRules
import com.fandom.auchatstudio.core.ThemePreset
import com.fandom.auchatstudio.data.StorySummary
import com.fandom.auchatstudio.ui.appContainer
import com.fandom.auchatstudio.ui.chat.ChatStyle
import com.fandom.auchatstudio.ui.chat.ThemeSwatch
import com.fandom.auchatstudio.ui.components.ConfirmDialog
import com.fandom.auchatstudio.ui.components.TextInputDialog
import com.fandom.auchatstudio.ui.theme.BrandPink
import com.fandom.auchatstudio.ui.theme.BrandViolet

@Composable
fun HomeScreen(
    onOpenStory: (Long) -> Unit,
    onPlayStory: (Long) -> Unit,
    onOpenCharacters: () -> Unit,
) {
    val container = appContainer()
    val vm: HomeViewModel = viewModel { HomeViewModel(container.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onOpenStory = onOpenStory,
        onPlayStory = onPlayStory,
        onOpenCharacters = onOpenCharacters,
        onCreateStory = { title, theme -> vm.createStory(title, theme, onOpenStory) },
        onRenameStory = vm::renameStory,
        onDeleteStory = vm::deleteStory,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    state: HomeUiState,
    onOpenStory: (Long) -> Unit,
    onPlayStory: (Long) -> Unit,
    onOpenCharacters: () -> Unit,
    onCreateStory: (String, ThemePreset) -> Unit,
    onRenameStory: (Long, String) -> Unit,
    onDeleteStory: (Long) -> Unit,
) {
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<StorySummary?>(null) }
    var deleting by remember { mutableStateOf<StorySummary?>(null) }
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("AU Chat Studio", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenCharacters) {
                        Icon(Icons.Outlined.Groups, contentDescription = "Nhân vật")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                expanded = fabExpanded,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Kịch bản mới") },
                modifier = Modifier.testTag("fab_new_story"),
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().testTag("home_list"),
        ) {
            item(key = "hero") {
                HeroCard(
                    storyCount = state.stories.size,
                    characterCount = state.characters.size,
                    onOpenCharacters = onOpenCharacters,
                )
            }
            item(key = "section") {
                Text(
                    "Kịch bản của bạn",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                )
            }
            if (!state.loading && state.stories.isEmpty()) {
                item(key = "empty") { EmptyStories(onCreate = { showCreate = true }) }
            }
            items(state.stories, key = { it.story.id }) { summary ->
                StoryCard(
                    summary = summary,
                    onOpen = { onOpenStory(summary.story.id) },
                    onPlay = { onPlayStory(summary.story.id) },
                    onRename = { renaming = summary },
                    onDelete = { deleting = summary },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    if (showCreate) {
        NewStoryDialog(
            onDismiss = { showCreate = false },
            onCreate = { title, theme ->
                showCreate = false
                onCreateStory(title, theme)
            },
        )
    }
    renaming?.let { s ->
        TextInputDialog(
            title = "Đổi tên kịch bản",
            label = "Tên kịch bản",
            initial = s.story.title,
            confirmText = "Lưu",
            maxLength = NameRules.MAX_TITLE_LENGTH,
            onDismiss = { renaming = null },
            onConfirm = {
                onRenameStory(s.story.id, it)
                renaming = null
            },
        )
    }
    deleting?.let { s ->
        ConfirmDialog(
            title = "Xóa kịch bản?",
            message = "\"${s.story.title}\" và ${s.messageCount} tin nhắn sẽ bị xóa vĩnh viễn khỏi máy.",
            confirmText = "Xóa",
            onDismiss = { deleting = null },
            onConfirm = {
                onDeleteStory(s.story.id)
                deleting = null
            },
        )
    }
}

@Composable
private fun HeroCard(storyCount: Int, characterCount: Int, onOpenCharacters: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(BrandViolet, BrandPink)))
            .padding(20.dp),
    ) {
        Column {
            Text(
                "Dựng kịch bản AU\ncủa riêng bạn",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Tap-to-Chat · 3 phong cách nền tảng · 100% ngoại tuyến",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroPill(Icons.Outlined.AutoStories, "$storyCount kịch bản")
                HeroPill(Icons.Outlined.Groups, "$characterCount nhân vật", onClick = onOpenCharacters)
                HeroPill(Icons.Outlined.Lock, "Riêng tư")
            }
        }
    }
}

@Composable
private fun HeroPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: (() -> Unit)? = null) {
    val base = Modifier
        .clip(RoundedCornerShape(50))
        .background(Color.White.copy(alpha = 0.2f))
    Row(
        modifier = (if (onClick != null) base.clickable(onClick = onClick) else base)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun EmptyStories(onCreate: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("✨", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text("Chưa có kịch bản nào", style = MaterialTheme.typography.titleMedium)
            Text(
                "Tạo kịch bản đầu tiên rồi chọn người nói để bắt đầu dàn dựng tin nhắn.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onCreate) { Text("Tạo kịch bản") }
        }
    }
}

@Composable
private fun StoryCard(
    summary: StorySummary,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    val style = ChatStyle.of(summary.story.theme)
    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThemeSwatch(style, Modifier.size(width = 62.dp, height = 52.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    summary.story.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    previewOf(summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${summary.story.theme.displayName} · ${summary.messageCount} tin nhắn",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            FilledTonalIconButton(onClick = onPlay, enabled = summary.messageCount > 0) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Phát ${summary.story.title}")
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Tùy chọn ${summary.story.title}")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Đổi tên") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                        onClick = { menu = false; onRename() },
                    )
                    DropdownMenuItem(
                        text = { Text("Xóa") },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                        onClick = { menu = false; onDelete() },
                    )
                }
            }
        }
    }
}

private fun previewOf(summary: StorySummary): String = when {
    summary.messageCount == 0 -> "Chưa có tin nhắn"
    summary.lastType == MessageType.VOICE -> "🎙 Tin nhắn thoại"
    summary.lastType == MessageType.IMAGE -> "🖼 Ảnh"
    summary.lastText.isNullOrBlank() -> "…"
    else -> summary.lastText
}

@Composable
fun NewStoryDialog(onDismiss: () -> Unit, onCreate: (String, ThemePreset) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var theme by rememberSaveable { mutableStateOf(ThemePreset.Default) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kịch bản mới") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= NameRules.MAX_TITLE_LENGTH) title = it },
                    label = { Text("Tên kịch bản") },
                    placeholder = { Text("VD: Hẹn hò bí mật sau concert") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth().testTag("new_story_title"),
                )
                Spacer(Modifier.height(16.dp))
                Text("Phong cách", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemePreset.entries.forEach { preset ->
                        ThemeOption(preset, selected = preset == theme, onSelect = { theme = preset })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(title, theme) }, modifier = Modifier.testTag("new_story_confirm")) { Text("Tạo") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

@Composable
fun ThemeOption(preset: ThemePreset, selected: Boolean, onSelect: () -> Unit) {
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThemeSwatch(ChatStyle.of(preset), Modifier.size(width = 56.dp, height = 44.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(preset.displayName, style = MaterialTheme.typography.titleSmall)
            Text(preset.tagline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RadioButton(selected = selected, onClick = null, colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = border))
    }
}
