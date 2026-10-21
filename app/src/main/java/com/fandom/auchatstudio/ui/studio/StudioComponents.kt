package com.fandom.auchatstudio.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.core.VoiceFormat
import com.fandom.auchatstudio.data.CharacterEntity
import com.fandom.auchatstudio.data.MessageEntity
import com.fandom.auchatstudio.ui.chat.ChatItem
import com.fandom.auchatstudio.ui.components.Avatar
import kotlin.math.roundToInt

/** Bottom composer: Director Switcher (who speaks next) + input row. */
@Composable
fun Composer(
    characters: List<CharacterEntity>,
    speaker: Speaker?,
    accent: Color,
    onSelectSpeaker: (Speaker) -> Unit,
    onSendText: (String) -> Boolean,
    onPickImage: () -> Unit,
    onVoice: () -> Unit,
    onAddCharacter: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val haptics = LocalHapticFeedback.current
    val speakerName = when (speaker) {
        Speaker.Narrator -> "người dẫn truyện"
        is Speaker.Character -> characters.firstOrNull { it.id == speaker.id }?.name ?: ""
        null -> ""
    }
    val isNarrator = speaker == Speaker.Narrator

    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Column(Modifier.navigationBarsPadding().imePadding()) {
            DirectorSwitcher(
                characters = characters,
                speaker = speaker,
                accent = accent,
                onSelect = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectSpeaker(it)
                },
                onAddCharacter = onAddCharacter,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPickImage, enabled = !isNarrator && characters.isNotEmpty()) {
                    Icon(Icons.Outlined.Image, contentDescription = "Gửi ảnh")
                }
                IconButton(onClick = onVoice, enabled = !isNarrator && characters.isNotEmpty()) {
                    Icon(Icons.Outlined.Mic, contentDescription = "Gửi tin nhắn thoại")
                }
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = {
                        Text(
                            if (speakerName.isBlank()) "Thêm nhân vật để bắt đầu" else "Nhắn với tư cách $speakerName…",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.weight(1f).testTag("composer_input"),
                )
                Spacer(Modifier.width(6.dp))
                FilledIconButton(
                    onClick = { if (onSendText(text)) text = "" },
                    enabled = text.isNotBlank() && speaker != null,
                    modifier = Modifier.testTag("composer_send"),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi")
                }
            }
        }
    }
}

@Composable
private fun DirectorSwitcher(
    characters: List<CharacterEntity>,
    speaker: Speaker?,
    accent: Color,
    onSelect: (Speaker) -> Unit,
    onAddCharacter: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().testTag("director_switcher"),
    ) {
        items(characters, key = { it.id }) { c ->
            val selected = speaker == Speaker.Character(c.id)
            SpeakerChip(
                label = c.name,
                selected = selected,
                accent = accent,
                onClick = { onSelect(Speaker.Character(c.id)) },
                leading = { Avatar(c.name, c.roleColorHex, c.avatarPath, size = 28.dp) },
            )
        }
        item(key = "narrator") {
            SpeakerChip(
                label = "Dẫn truyện",
                selected = speaker == Speaker.Narrator,
                accent = accent,
                onClick = { onSelect(Speaker.Narrator) },
                leading = {
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.AutoMirrored.Outlined.Notes, null, modifier = Modifier.size(16.dp)) }
                },
            )
        }
        item(key = "add") {
            SpeakerChip(
                label = "Thêm",
                selected = false,
                accent = accent,
                onClick = onAddCharacter,
                leading = {
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp)) }
                },
            )
        }
    }
}

@Composable
private fun SpeakerChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val bg = if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .border(if (selected) 2.dp else 0.dp, if (selected) accent else Color.Transparent, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) {
                this.selected = selected
                contentDescription = "Người nói: $label"
            }
            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 120.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageActionsSheet(
    item: ChatItem,
    characters: List<CharacterEntity>,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onChangeSpeaker: (Long) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(bottom = 16.dp)) {
            Text(
                text = when (item.message.type) {
                    MessageType.NARRATION -> "Lời dẫn truyện"
                    MessageType.VOICE -> "Tin nhắn thoại · ${VoiceFormat.format(item.message.voiceSeconds)}"
                    MessageType.IMAGE -> "Ảnh"
                    MessageType.TEXT -> "Tin nhắn"
                } + (item.speaker?.let { " của ${it.name}" } ?: ""),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            if (!item.isNarration && characters.size > 1) {
                Text(
                    "Đổi người nói",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(characters, key = { it.id }) { c ->
                        val current = c.id == item.message.characterId
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !current) { onChangeSpeaker(c.id) }
                                .padding(6.dp),
                        ) {
                            Avatar(
                                c.name,
                                c.roleColorHex,
                                c.avatarPath,
                                size = 44.dp,
                                ringColor = if (current) MaterialTheme.colorScheme.primary else null,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                c.name,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 64.dp),
                            )
                        }
                    }
                }
            }
            val itemColors = ListItemDefaults.colors(containerColor = Color.Transparent)
            if (item.message.type != MessageType.IMAGE) {
                ListItem(
                    headlineContent = { Text("Sửa nội dung & giờ") },
                    leadingContent = { Icon(Icons.Outlined.Edit, null) },
                    colors = itemColors,
                    modifier = Modifier.clickable(onClick = onEdit),
                )
            }
            if (canMoveUp) {
                ListItem(
                    headlineContent = { Text("Đưa lên trên") },
                    leadingContent = { Icon(Icons.Outlined.ArrowUpward, null) },
                    colors = itemColors,
                    modifier = Modifier.clickable(onClick = onMoveUp),
                )
            }
            if (canMoveDown) {
                ListItem(
                    headlineContent = { Text("Đưa xuống dưới") },
                    leadingContent = { Icon(Icons.Outlined.ArrowDownward, null) },
                    colors = itemColors,
                    modifier = Modifier.clickable(onClick = onMoveDown),
                )
            }
            ListItem(
                headlineContent = { Text("Xóa tin nhắn", color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                colors = itemColors,
                modifier = Modifier.clickable(onClick = onDelete).testTag("delete_message"),
            )
        }
    }
}

@Composable
fun EditMessageDialog(
    message: MessageEntity,
    onDismiss: () -> Unit,
    onSave: (text: String, time: String, voiceSeconds: Int) -> Unit,
) {
    var text by remember { mutableStateOf(message.text) }
    var time by remember { mutableStateOf(message.timeLabel ?: "") }
    var seconds by remember { mutableFloatStateOf(message.voiceSeconds.coerceAtLeast(1).toFloat()) }
    val isVoice = message.type == MessageType.VOICE
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sửa tin nhắn") },
        text = {
            Column {
                if (isVoice) {
                    Text("Độ dài: ${VoiceFormat.format(seconds.roundToInt())}")
                    Slider(value = seconds, onValueChange = { seconds = it }, valueRange = 1f..120f)
                } else {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Nội dung") },
                        minLines = 2,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth().testTag("edit_message_text"),
                    )
                }
                if (message.type != MessageType.NARRATION) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = time,
                        onValueChange = { if (it.length <= 5) time = it },
                        label = { Text("Giờ hiển thị (tùy chọn)") },
                        placeholder = { Text("23:41") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text, time, seconds.roundToInt()) }, modifier = Modifier.testTag("edit_message_save")) {
                Text("Lưu")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

@Composable
fun VoiceNoteDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var seconds by remember { mutableFloatStateOf(8f) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tin nhắn thoại bí mật") },
        text = {
            Column {
                Text(
                    "Sóng âm được vẽ giả lập, ứng dụng không ghi âm thật.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Text("Độ dài: ${VoiceFormat.format(seconds.roundToInt())}", style = MaterialTheme.typography.titleMedium)
                Slider(value = seconds, onValueChange = { seconds = it }, valueRange = 1f..120f)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(seconds.roundToInt()) }, modifier = Modifier.testTag("voice_confirm")) { Text("Gửi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}
