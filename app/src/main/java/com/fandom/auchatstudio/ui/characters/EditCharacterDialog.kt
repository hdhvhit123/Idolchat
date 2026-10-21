package com.fandom.auchatstudio.ui.characters

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fandom.auchatstudio.core.ColorMath
import com.fandom.auchatstudio.core.NameRules
import com.fandom.auchatstudio.core.RolePalette
import com.fandom.auchatstudio.data.CharacterEntity
import com.fandom.auchatstudio.ui.components.Avatar

/**
 * Character editor (design document §5.2). Uses the system Photo Picker, which needs no storage
 * permission; the picked file is copied to internal storage by the ViewModel on save.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditCharacterDialog(
    character: CharacterEntity?,
    suggestedColor: Long,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (CharacterDraft) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(character?.name ?: "") }
    var color by remember { mutableStateOf(character?.roleColorHex ?: suggestedColor) }
    var isUser by remember { mutableStateOf(character?.isUser ?: false) }
    var avatar by remember { mutableStateOf<AvatarChange>(AvatarChange.Keep) }
    var nameError by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) avatar = AvatarChange.Picked(uri)
    }
    val previewModel: Any? = when (val a = avatar) {
        AvatarChange.Keep -> character?.avatarPath
        AvatarChange.Remove -> null
        is AvatarChange.Picked -> a.uri
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (character == null) "Thêm nhân vật" else "Tùy chỉnh Nhân vật Thần tượng",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button) {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                        .semantics { contentDescription = "Chọn ảnh đại diện" },
                ) {
                    Avatar(name = name.ifBlank { "?" }, colorHex = color, model = previewModel, size = 96.dp)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.AddAPhoto, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    }
                }
                if (previewModel != null) {
                    TextButton(onClick = { avatar = AvatarChange.Remove }) { Text("Gỡ ảnh") }
                } else {
                    Spacer(Modifier.height(12.dp))
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        if (it.length <= NameRules.MAX_NAME_LENGTH) name = it
                        nameError = false
                    },
                    label = { Text("Tên hiển thị / Idol") },
                    singleLine = true,
                    isError = nameError,
                    supportingText = { if (nameError) Text("Hãy nhập tên nhân vật") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth().testTag("character_name_field"),
                )
                Spacer(Modifier.height(8.dp))
                Text("Màu vai diễn", style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    RolePalette.colors.forEach { c ->
                        val selected = c == color
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                .clickable(role = Role.RadioButton) { color = c }
                                .semantics { contentDescription = "Màu ${RolePalette.colors.indexOf(c) + 1}" },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(
                                    Icons.Filled.Check,
                                    null,
                                    tint = if (ColorMath.prefersDarkText(c)) Color.Black else Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable(role = Role.Switch) { isUser = !isUser }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Đây là tôi", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Tin nhắn của nhân vật này nằm bên phải",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = isUser, onCheckedChange = null, modifier = Modifier.testTag("character_is_user"))
                }
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        TextButton(onClick = onDelete) {
                            Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Xóa")
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Hủy") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = !saving,
                        onClick = {
                            if (name.isBlank()) {
                                nameError = true
                            } else {
                                onSave(CharacterDraft(name, color, isUser, avatar))
                            }
                        },
                        modifier = Modifier.testTag("character_save"),
                    ) { Text("Lưu hồ sơ") }
                }
            }
        }
    }
}
