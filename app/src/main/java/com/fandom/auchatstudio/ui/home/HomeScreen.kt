package com.fandom.auchatstudio.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fandom.auchatstudio.R
import com.fandom.auchatstudio.core.Brand
import com.fandom.auchatstudio.core.MessageType
import com.fandom.auchatstudio.core.NameRules
import com.fandom.auchatstudio.core.StoryTemplate
import com.fandom.auchatstudio.core.ThemePreset
import com.fandom.auchatstudio.data.CharacterEntity
import com.fandom.auchatstudio.data.StorySummary
import com.fandom.auchatstudio.i18n.descriptionRes
import com.fandom.auchatstudio.i18n.rememberStoryTemplates
import com.fandom.auchatstudio.i18n.resolve
import com.fandom.auchatstudio.ui.appContainer
import com.fandom.auchatstudio.ui.chat.ChatStyle
import com.fandom.auchatstudio.ui.chat.ThemeSwatch
import com.fandom.auchatstudio.ui.components.Avatar
import com.fandom.auchatstudio.ui.components.ChatBubblesArt
import com.fandom.auchatstudio.ui.components.ConfirmDialog
import com.fandom.auchatstudio.ui.components.SectionHeader
import com.fandom.auchatstudio.ui.components.TextInputDialog
import com.fandom.auchatstudio.ui.theme.BrandGradient
import com.fandom.auchatstudio.ui.theme.Space

@Composable
fun HomeScreen(
    onOpenStory: (Long) -> Unit,
    onPlayStory: (Long) -> Unit,
    onOpenCharacters: () -> Unit,
) {
    val container = appContainer()
    val vm: HomeViewModel = viewModel { HomeViewModel(container.repository, container.prefs) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(vm) {
        vm.messageFlow.collect {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(it.resolve(context))
        }
    }
    HomeContent(
        state = state,
        onOpenStory = onOpenStory,
        onPlayStory = onPlayStory,
        onOpenCharacters = onOpenCharacters,
        onCreateStory = { title, theme -> vm.createStory(title, theme, onOpenStory) },
        onRenameStory = vm::renameStory,
        onDeleteStory = vm::deleteStory,
        onUseTemplate = { vm.createFromTemplate(it, onOpenStory) },
        onDuplicateStory = vm::duplicateStory,
        onDismissGuide = vm::dismissGuide,
        snackbar = snackbar,
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
    onUseTemplate: (StoryTemplate) -> Unit = {},
    onDuplicateStory: (Long) -> Unit = {},
    onDismissGuide: () -> Unit = {},
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<StorySummary?>(null) }
    var deleting by remember { mutableStateOf<StorySummary?>(null) }
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val latest = state.latest
    val others = state.stories.drop(1)
    val templates = rememberStoryTemplates()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(Brand.NAME, style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    FilledTonalIconButton(onClick = onOpenCharacters, modifier = Modifier.padding(end = 8.dp)) {
                        Icon(Icons.Outlined.Groups, contentDescription = stringResource(R.string.cd_characters))
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
                text = { Text(stringResource(R.string.new_story)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_new_story"),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + Space.xs,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            modifier = Modifier.fillMaxSize().testTag("home_list"),
        ) {
            item(key = "hero") {
                Box(Modifier.padding(horizontal = Space.screen)) {
                    if (latest != null) {
                        ContinueCard(
                            summary = latest,
                            onOpen = { onOpenStory(latest.story.id) },
                            onPlay = { onPlayStory(latest.story.id) },
                            onRename = { renaming = latest },
                            onDuplicate = { onDuplicateStory(latest.story.id) },
                            onDelete = { deleting = latest },
                        )
                    } else {
                        FirstStoryCard(onCreate = { showCreate = true })
                    }
                }
            }
            item(key = "guide") {
                AnimatedVisibility(visible = state.showGuide) {
                    GuideCard(
                        onDismiss = onDismissGuide,
                        modifier = Modifier.padding(start = Space.screen, end = Space.screen, top = Space.lg),
                    )
                }
            }
            item(key = "templates") {
                Column {
                    SectionHeader(
                        stringResource(R.string.section_templates),
                        Modifier.padding(start = Space.screen, end = Space.sm, top = Space.md),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Space.screen),
                        horizontalArrangement = Arrangement.spacedBy(Space.md),
                    ) {
                        items(templates, key = { it.id }) { template ->
                            TemplateCard(template, onClick = { onUseTemplate(template) })
                        }
                    }
                }
            }
            item(key = "cast") {
                Column {
                    SectionHeader(
                        stringResource(R.string.cast_title),
                        Modifier.padding(start = Space.screen, end = Space.sm, top = Space.md),
                        actionLabel = stringResource(R.string.action_manage),
                        onAction = onOpenCharacters,
                    )
                    CastStrip(state.characters, onOpenCharacters)
                }
            }
            if (others.isNotEmpty()) {
                item(key = "others_header") {
                    SectionHeader(
                        stringResource(R.string.section_other_stories),
                        Modifier.padding(start = Space.screen, end = Space.sm, top = Space.md),
                    )
                }
                items(others, key = { it.story.id }) { summary ->
                    StoryCard(
                        summary = summary,
                        onOpen = { onOpenStory(summary.story.id) },
                        onPlay = { onPlayStory(summary.story.id) },
                        onRename = { renaming = summary },
                        onDuplicate = { onDuplicateStory(summary.story.id) },
                        onDelete = { deleting = summary },
                        modifier = Modifier
                            .animateItem()
                            .padding(horizontal = Space.screen)
                            .padding(bottom = Space.md),
                    )
                }
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
            title = stringResource(R.string.rename_story),
            label = stringResource(R.string.story_name),
            initial = s.story.title,
            confirmText = stringResource(R.string.action_save),
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
            title = stringResource(R.string.delete_story_title),
            message = pluralStringResource(R.plurals.delete_story_message, s.messageCount, s.story.title, s.messageCount),
            confirmText = stringResource(R.string.action_delete),
            onDismiss = { deleting = null },
            onConfirm = {
                onDeleteStory(s.story.id)
                deleting = null
            },
        )
    }
}

/** The most recently edited story, one tap from being continued or played. */
@Composable
private fun ContinueCard(
    summary: StorySummary,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(BrandGradient)
            .clickable(onClickLabel = stringResource(R.string.continue_writing), onClick = onOpen)
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.continue_label),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.weight(1f),
            )
            StoryMenu(summary.story.title, onRename, onDuplicate, onDelete, tint = Color.White)
        }
        Column(Modifier.padding(end = 12.dp)) {
            Text(
                summary.story.title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Space.xs))
            Text(
                previewOf(summary),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.92f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                metaOf(summary),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.92f),
            )
            Spacer(Modifier.height(Space.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                HeroButton(stringResource(R.string.continue_writing), filled = true, icon = Icons.Outlined.Edit, onClick = onOpen)
                if (summary.messageCount > 0) {
                    HeroButton(stringResource(R.string.action_play), filled = false, icon = Icons.Filled.PlayArrow, onClick = onPlay)
                }
            }
        }
    }
}

@Composable
private fun HeroButton(
    text: String,
    filled: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    val content = if (filled) Color(0xFF4A2BD6) else Color.White
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(CircleShape)
            .background(if (filled) Color.White else Color.White.copy(alpha = 0.18f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = content, style = MaterialTheme.typography.labelLarge)
    }
}

/** Shown instead of [ContinueCard] when there is nothing to continue yet. */
@Composable
private fun FirstStoryCard(onCreate: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ChatBubblesArt()
            Spacer(Modifier.height(Space.md))
            Text(stringResource(R.string.no_stories_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Space.xs))
            Text(
                stringResource(R.string.no_stories_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Space.md))
            TextButton(onClick = onCreate) { Text(stringResource(R.string.write_from_scratch)) }
        }
    }
}

/** First-run explanation of the core loop. Stays until dismissed, then never returns. */
@Composable
private fun GuideCard(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier.fillMaxWidth().testTag("guide_card"),
    ) {
        Column(Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.guide_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("guide_dismiss")) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.cd_hide_guide))
                }
            }
            GuideStep(1, stringResource(R.string.guide_step_1))
            GuideStep(2, stringResource(R.string.guide_step_2))
            GuideStep(3, stringResource(R.string.guide_step_3))
        }
    }
}

@Composable
private fun GuideStep(number: Int, text: String) {
    Row(Modifier.padding(end = 12.dp, top = Space.sm), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onTertiaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", color = MaterialTheme.colorScheme.tertiaryContainer, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.width(Space.md))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TemplateCard(template: StoryTemplate, onClick: () -> Unit) {
    val style = ChatStyle.of(template.theme)
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.width(168.dp).testTag("template_${template.id}"),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(style.background)),
                    contentAlignment = Alignment.Center,
                ) { Text(template.emoji, style = MaterialTheme.typography.titleLarge) }
                Spacer(Modifier.weight(1f))
                ThemeSwatch(style, Modifier.size(width = 40.dp, height = 34.dp))
            }
            Spacer(Modifier.height(Space.md))
            Text(template.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                template.hook,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Space.sm))
            Text(
                pluralStringResource(R.plurals.template_meta, template.lines.size, template.lines.size, template.theme.displayName),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CastStrip(characters: List<CharacterEntity>, onOpenCharacters: () -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Space.screen),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        modifier = Modifier.testTag("cast_strip"),
    ) {
        // "Add" leads the row: at the end it sat under the "New story" button whenever the
        // cast filled the width of the screen.
        item(key = "add") {
            CastItem(label = stringResource(R.string.action_add), onClick = onOpenCharacters) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            }
        }
        items(characters, key = { it.id }) { c ->
            CastItem(label = c.name, onClick = onOpenCharacters) {
                Avatar(c.name, c.roleColorHex, c.avatarPath, size = 52.dp)
            }
        }
    }
}

@Composable
private fun CastItem(label: String, onClick: () -> Unit, avatar: @Composable () -> Unit) {
    val description = stringResource(R.string.cd_cast_member, label)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(68.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(vertical = 6.dp),
    ) {
        avatar()
        Spacer(Modifier.height(Space.xs))
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StoryCard(
    summary: StorySummary,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = ChatStyle.of(summary.story.theme)
    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThemeSwatch(style, Modifier.size(width = 58.dp, height = 50.dp))
            Spacer(Modifier.width(Space.md))
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
                Text(
                    metaOf(summary),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (summary.messageCount > 0) {
                FilledTonalIconButton(onClick = onPlay) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.cd_play_named, summary.story.title))
                }
            }
            StoryMenu(summary.story.title, onRename, onDuplicate, onDelete, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StoryMenu(
    title: String,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    tint: Color,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.cd_options_named, title), tint = tint)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_rename)) },
                leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                onClick = { open = false; onRename() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_duplicate)) },
                leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                onClick = { open = false; onDuplicate() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = { open = false; onDelete() },
            )
        }
    }
}

@Composable
private fun previewOf(summary: StorySummary): String = when {
    summary.messageCount == 0 -> stringResource(R.string.preview_no_messages)
    summary.lastType == MessageType.VOICE -> stringResource(R.string.preview_voice)
    summary.lastType == MessageType.IMAGE -> stringResource(R.string.preview_photo)
    summary.lastText.isNullOrBlank() -> "…"
    else -> summary.lastText
}

@Composable
private fun metaOf(summary: StorySummary): String =
    pluralStringResource(R.plurals.story_meta, summary.messageCount, summary.story.theme.displayName, summary.messageCount)

@Composable
fun NewStoryDialog(onDismiss: () -> Unit, onCreate: (String, ThemePreset) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var theme by rememberSaveable { mutableStateOf(ThemePreset.Default) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_story)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= NameRules.MAX_TITLE_LENGTH) title = it },
                    label = { Text(stringResource(R.string.story_name)) },
                    placeholder = { Text(stringResource(R.string.story_name_example)) },
                    supportingText = { Text(stringResource(R.string.story_name_optional)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth().testTag("new_story_title"),
                )
                Spacer(Modifier.height(Space.md))
                Text(stringResource(R.string.chat_style), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(Space.sm))
                Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    ThemePreset.entries.forEach { preset ->
                        ThemeOption(
                            preset = preset,
                            selected = preset == theme,
                            onSelect = { theme = preset },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(Space.sm))
                Text(
                    stringResource(R.string.theme_description_hint, stringResource(theme.descriptionRes())),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(title, theme) }, modifier = Modifier.testTag("new_story_confirm")) { Text(stringResource(R.string.action_create)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Compact preview tile so all three skins fit side by side, even on small screens. */
@Composable
fun ThemeOption(preset: ThemePreset, selected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            )
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ThemeSwatch(ChatStyle.of(preset), Modifier.size(width = 56.dp, height = 44.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            preset.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
        )
    }
}
