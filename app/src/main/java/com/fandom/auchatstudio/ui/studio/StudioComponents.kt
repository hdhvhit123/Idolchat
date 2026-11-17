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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fandom.auchatstudio.R
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
    // Saved across rotation and process death, so a half-typed line is never lost.
    var text by rememberSaveable { mutableStateOf("") }
    val haptics = LocalHapticFeedback.current
    val isNarrator = speaker == Speaker.Narrator
    val speakerName = when (speaker) {
        Speaker.Narrator -> ""
        is Speaker.Character -> characters.firstOrNull { it.id == speaker.id }?.name ?: ""
        null -> ""
    }

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
            if (isNarrator) {
                // One-tap stage directions: the lines people type most as a narrator.
                val presets = stringArrayResource(R.array.narration_presets).toList()
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("narration_presets"),
                ) {
                    items(presets, key = { it }) { preset ->
                        AssistChip(onClick = { onSendText(preset) }, label = { Text(preset) })
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPickImage, enabled = !isNarrator && characters.isNotEmpty()) {
                    Icon(Icons.Outlined.Image, contentDescription = stringResource(R.string.cd_send_photo))
                }
                IconButton(onClick = onVoice, enabled = !isNarrator && characters.isNotEmpty()) {
                    Icon(Icons.Outlined.Mic, contentDescription = stringResource(R.string.cd_send_voice))
                }
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = {
                        Text(
                            when {
                                isNarrator -> stringResource(R.string.composer_hint_narrator)
                                speakerName.isBlank() -> stringResource(R.string.composer_hint_no_cast)
                                else -> stringResource(R.string.composer_hint, speakerName)
                            },
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
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.action_send))
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
    val rowState = rememberLazyListState()
    // Keep the active speaker on screen: with a long cast it used to end up scrolled out of view.
    LaunchedEffect(speaker, characters.size) {
        val index = when (speaker) {
            is Speaker.Character -> characters.indexOfFirst { it.id == speaker.id }
            Speaker.Narrator -> characters.size
            null -> -1
        }
        val visible = rowState.layoutInfo.visibleItemsInfo
        val fullyVisible = visible.any {
            it.index == index && it.offset >= 0 && it.offset + it.size <= rowState.layoutInfo.viewportEndOffset
        }
        if (index >= 0 && !fullyVisible) rowState.animateScrollToItem(index)
    }
    LazyRow(
        state = rowState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
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
                label = stringResource(R.string.narrator),
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
                label = stringResource(R.string.action_add),
                description = stringResource(R.string.add_character),
                role = Role.Button,
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
    description: String = stringResource(R.string.cd_speaker, label),
    role: Role = Role.RadioButton,
    leading: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val bg = if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        modifier = Modifier
            // 40dp tall pill inside a 48dp touch target.
            .minimumInteractiveComponentSize()
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(bg)
            .border(if (selected) 2.dp else 0.dp, if (selected) accent else Color.Transparent, shape)
            .clickable(role = role, onClick = onClick)
            .semantics(mergeDescendants = true) {
                if (role == Role.RadioButton) this.selected = selected
                contentDescription = description
            }
            .padding(start = 6.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
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
    onChangeSpeaker: (Long?) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(bottom = 16.dp)) {
            Text(
                text = run {
                    val kind = when (item.message.type) {
                        MessageType.NARRATION -> stringResource(R.string.kind_narration)
                        MessageType.VOICE -> stringResource(R.string.kind_voice, VoiceFormat.format(item.message.voiceSeconds))
                        MessageType.IMAGE -> stringResource(R.string.kind_photo)
                        MessageType.TEXT -> stringResource(R.string.kind_text)
                    }
                    item.speaker?.let { stringResource(R.string.kind_by, kind, it.name) } ?: kind
                },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            // A written line can be handed to the narrator and back; voice notes and photos can
            // only move between characters.
            val canNarrate = item.message.type == MessageType.TEXT || item.isNarration
            if (characters.isNotEmpty() && (canNarrate || characters.size > 1)) {
                Text(
                    stringResource(R.string.change_speaker),
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
                    if (canNarrate) item(key = "narrator") {
                        val current = item.isNarration
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !current) { onChangeSpeaker(null) }
                                .testTag("speaker_narrator")
                                .padding(6.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .then(
                                        if (current) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.AutoMirrored.Outlined.Notes, null, modifier = Modifier.size(22.dp)) }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.narrator),
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
                    headlineContent = { Text(stringResource(R.string.edit_text_and_time)) },
                    leadingContent = { Icon(Icons.Outlined.Edit, null) },
                    colors = itemColors,
                    modifier = Modifier.clickable(onClick = onEdit),
                )
            }
            if (canMoveUp) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.move_up)) },
                    leadingContent = { Icon(Icons.Outlined.ArrowUpward, null) },
                    colors = itemColors,
                    modifier = Modifier.clickable(onClick = onMoveUp),
                )
            }
            if (canMoveDown) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.move_down)) },
                    leadingContent = { Icon(Icons.Outlined.ArrowDownward, null) },
                    colors = itemColors,
                    modifier = Modifier.clickable(onClick = onMoveDown),
                )
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.delete_message), color = MaterialTheme.colorScheme.error) },
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
        title = { Text(stringResource(R.string.edit_message)) },
        text = {
            Column {
                if (isVoice) {
                    Text(stringResource(R.string.voice_length, VoiceFormat.format(seconds.roundToInt())))
                    VoiceLengthSlider(seconds) { seconds = it }
                } else {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text(stringResource(R.string.message_text)) },
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
                        label = { Text(stringResource(R.string.time_label_optional)) },
                        placeholder = { Text("23:41") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text, time, seconds.roundToInt()) }, modifier = Modifier.testTag("edit_message_save")) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun VoiceNoteDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var seconds by remember { mutableFloatStateOf(8f) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.voice_message)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.voice_disclaimer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.voice_length, VoiceFormat.format(seconds.roundToInt())), style = MaterialTheme.typography.titleMedium)
                VoiceLengthSlider(seconds) { seconds = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(seconds.roundToInt()) }, modifier = Modifier.testTag("voice_confirm")) { Text(stringResource(R.string.action_send)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private const val VOICE_MIN_SECONDS = 1f
private const val VOICE_MAX_SECONDS = 120f

/**
 * Length picker for a voice note. The two ends are labelled so that a short default such as 0:08,
 * which sits close to the left edge of a two-minute scale, does not read as "nothing chosen".
 */
@Composable
private fun VoiceLengthSlider(seconds: Float, onChange: (Float) -> Unit) {
    val sliderLabel = stringResource(R.string.cd_voice_length)
    Column {
        Slider(
            value = seconds,
            onValueChange = onChange,
            valueRange = VOICE_MIN_SECONDS..VOICE_MAX_SECONDS,
            modifier = Modifier.semantics {
                contentDescription = sliderLabel
                stateDescription = VoiceFormat.format(seconds.roundToInt())
            },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val style = MaterialTheme.typography.labelSmall
            val color = MaterialTheme.colorScheme.onSurfaceVariant
            Text(VoiceFormat.format(VOICE_MIN_SECONDS.toInt()), style = style, color = color)
            Text(VoiceFormat.format(VOICE_MAX_SECONDS.toInt()), style = style, color = color)
        }
    }
}
