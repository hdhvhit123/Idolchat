package com.fandom.auchatstudio.ui.characters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fandom.auchatstudio.data.CharacterEntity
import com.fandom.auchatstudio.ui.appContainer
import com.fandom.auchatstudio.ui.components.Avatar
import com.fandom.auchatstudio.ui.components.ConfirmDialog

/** Sentinel for "the dialog is open for a new character". */
private val NewCharacter = CharacterEntity(id = -1, name = "", roleColorHex = 0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharactersScreen(onBack: () -> Unit) {
    val container = appContainer()
    val vm: CharactersViewModel = viewModel { CharactersViewModel(container.repository) }
    val characters by vm.characters.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<CharacterEntity?>(null) }
    var deleting by remember { mutableStateOf<CharacterEntity?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dàn nhân vật") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = NewCharacter },
                icon = { Icon(Icons.Filled.PersonAdd, null) },
                text = { Text("Thêm nhân vật") },
                modifier = Modifier.testTag("fab_add_character"),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val list = characters
        if (list == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 4.dp,
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "hint") {
                    Text(
                        "Ứng dụng không kèm ảnh thật của nghệ sĩ. Hãy tự đặt tên và chọn ảnh từ máy cho nhân vật của bạn.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
                if (list.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            "Chưa có nhân vật nào. Bấm \"Thêm nhân vật\" để bắt đầu.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                }
                items(list, key = { it.id }) { c ->
                    CharacterRow(c, onClick = { editing = c }, modifier = Modifier.animateItem())
                }
            }
        }
    }

    editing?.let { target ->
        val existing = target.takeIf { it !== NewCharacter }
        EditCharacterDialog(
            character = existing,
            suggestedColor = vm.suggestedColor(),
            saving = saving,
            onDismiss = { editing = null },
            onSave = { draft -> vm.save(existing, draft) { editing = null } },
            onDelete = if (existing != null) {
                {
                    deleting = existing
                    editing = null
                }
            } else {
                null
            },
        )
    }
    deleting?.let { c ->
        ConfirmDialog(
            title = "Xóa ${c.name}?",
            message = "Mọi tin nhắn của nhân vật này trong các kịch bản cũng sẽ bị xóa.",
            confirmText = "Xóa",
            onDismiss = { deleting = null },
            onConfirm = {
                vm.delete(c)
                deleting = null
            },
        )
    }
}

@Composable
private fun CharacterRow(character: CharacterEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(character.name, character.roleColorHex, character.avatarPath, size = 52.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(character.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Color(character.roleColorHex)))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (character.isUser) "Bạn · tin nhắn bên phải" else "Nhân vật · tin nhắn bên trái",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(Icons.Outlined.Edit, contentDescription = "Sửa ${character.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
