package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.StoryEntity
import com.example.data.preferences.AccessibilityPreset
import com.example.data.preferences.UserPreferences
import com.example.ui.components.StoryCoverImage
import com.example.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    stories: List<StoryEntity>,
    preferences: UserPreferences,
    searchQuery: String,
    selectedShelf: String,
    isNetworkConnected: Boolean,
    onSearchChange: (String) -> Unit,
    onShelfSelect: (String) -> Unit,
    onApplyPreset: (AccessibilityPreset) -> Unit,
    onToggleTheme: () -> Unit,
    onSyncNow: () -> Unit,
    onOpenStory: (String) -> Unit,
    onOpenStudio: (String) -> Unit,
    onToggleOffline: (StoryEntity) -> Unit,
    onChangeShelf: (StoryEntity, String) -> Unit,
    onImportTextFile: (Uri, (String, String) -> Unit) -> Unit,
    onCreateStory: (
        title: String,
        subtitle: String,
        authorName: String,
        shelf: String,
        ageRange: String,
        synopsis: String,
        bodyContent: String,
        coverPreset: String,
        coverUri: String?,
        isCollaborative: Boolean,
        choiceALabel: String?,
        choiceBLabel: String?
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateSheet by remember { mutableStateOf(false) }
    var shelfMenuTarget by remember { mutableStateOf<StoryEntity?>(null) }

    val shelves = listOf("All", "Offline", "Shared", "Kids 5-10", "Collaborative", "Favorites", "Uploaded")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.testTag("add_story_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add_story))
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val columns = when {
                maxWidth >= 920.dp -> 3
                maxWidth >= 600.dp -> 2
                else -> 1
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Hero Sanctuary Banner
                item(span = { GridItemSpan(columns) }) {
                    VarnikaHeroBanner(
                        preferences = preferences,
                        isNetworkConnected = isNetworkConnected,
                        pendingCount = stories.count { it.pendingSync },
                        onToggleTheme = onToggleTheme,
                        onSyncNow = onSyncNow
                    )
                }

                // Inclusive Ages 5 to 85 Quick Accessibility Row
                item(span = { GridItemSpan(columns) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Inclusive Reading Comfort (Ages 5–85)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AccessibilityPreset.entries.forEach { preset ->
                                FilterChip(
                                    selected = preferences.accessibilityPreset == preset,
                                    onClick = { onApplyPreset(preset) },
                                    label = { Text("${preset.label} • ${preset.ageBadge}") },
                                    modifier = Modifier.testTag("preset_${preset.name.lowercase()}")
                                )
                            }
                        }
                    }
                }

                // Search & Shelf Organizer
                item(span = { GridItemSpan(columns) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            placeholder = { Text("Search stories, authors, age range...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_stories_input")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            shelves.forEach { shelf ->
                                FilterChip(
                                    selected = selectedShelf == shelf,
                                    onClick = { onShelfSelect(shelf) },
                                    label = { Text(shelf) },
                                    modifier = Modifier.testTag("shelf_chip_${shelf.lowercase().replace(" ", "_")}")
                                )
                            }
                        }
                    }
                }

                if (stories.isEmpty()) {
                    item(span = { GridItemSpan(columns) }) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.AutoStories,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "No matching stories on this shelf",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Tap + to compose an interactive story or import a .txt / .md manuscript.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(stories, key = { it.id }) { story ->
                        StoryCard(
                            story = story,
                            onRead = { onOpenStory(story.id) },
                            onCollaborate = { onOpenStudio(story.id) },
                            onToggleOffline = { onToggleOffline(story) },
                            onOrganizeShelf = { shelfMenuTarget = story }
                        )
                    }
                }

                item(span = { GridItemSpan(columns) }) {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    shelfMenuTarget?.let { target ->
        ModalBottomSheet(onDismissRequest = { shelfMenuTarget = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Organize \"${target.title}\"",
                    style = MaterialTheme.typography.titleLarge
                )
                listOf("Favorites", "Kids 5-10", "Collaborative", "Uploaded").forEach { shelfOption ->
                    OutlinedButton(
                        onClick = {
                            onChangeShelf(target, shelfOption)
                            shelfMenuTarget = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Move to $shelfOption")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showCreateSheet) {
        CreateUploadStorySheet(
            defaultAuthor = preferences.readerDisplayName,
            onDismiss = { showCreateSheet = false },
            onImportFile = onImportTextFile,
            onSubmit = { title, sub, author, shelf, age, syn, body, preset, uri, collab, cA, cB ->
                onCreateStory(title, sub, author, shelf, age, syn, body, preset, uri, collab, cA, cB)
                showCreateSheet = false
            }
        )
    }
}

@Composable
private fun VarnikaHeroBanner(
    preferences: UserPreferences,
    isNetworkConnected: Boolean,
    pendingCount: Int,
    onToggleTheme: () -> Unit,
    onSyncNow: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_varnika),
                contentDescription = stringResource(R.string.cd_hero_banner),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x88101615),
                                Color(0xDD101615)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xCC1B3B36),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable { onSyncNow() }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    preferences.offlineOnlyMode -> Icons.Filled.CloudOff
                                    isNetworkConnected -> Icons.Filled.CloudDone
                                    else -> Icons.Filled.Sync
                                },
                                contentDescription = null,
                                tint = Color(0xFF7FD4C0),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = when {
                                    preferences.offlineOnlyMode -> "Travel Offline Mode"
                                    pendingCount > 0 -> "Local Vault • $pendingCount pending"
                                    isNetworkConnected -> "Local Vault Ready"
                                    else -> "Offline • Cached"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.testTag("hero_theme_toggle")
                    ) {
                        Icon(
                            imageVector = if (preferences.themeMode == ThemeMode.DARK) {
                                Icons.Filled.LightMode
                            } else {
                                Icons.Filled.DarkMode
                            },
                            contentDescription = "Toggle dark reading theme",
                            tint = Color(0xFFF5B26B)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Varnika",
                        style = MaterialTheme.typography.displayLarge,
                        color = Color(0xFFFAF6EE)
                    )
                    Text(
                        text = "Interactive E-Stories, Co-Author Versioning & Accessible Audio",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE3DAC8)
                    )
                }
            }
        }
    }
}

@Composable
private fun StoryCard(
    story: StoryEntity,
    onRead: () -> Unit,
    onCollaborate: () -> Unit,
    onToggleOffline: () -> Unit,
    onOrganizeShelf: () -> Unit
) {
    val progress = if (story.totalChapters > 0) {
        ((story.currentChapterIndex + 1).toFloat() / story.totalChapters.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("story_card_${story.id}")
            .clickable { onRead() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(144.dp)
            ) {
                StoryCoverImage(
                    coverPreset = story.coverPreset,
                    coverUri = story.coverUri,
                    contentDescription = story.title,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0x99101615))
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text(
                            text = story.ageRange,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text(
                            text = story.activeVersionTag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onToggleOffline,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = if (story.isOfflineCached) Icons.Filled.DownloadDone else Icons.Outlined.CloudDownload,
                        contentDescription = stringResource(R.string.cd_offline_badge),
                        tint = if (story.isOfflineCached) Color(0xFF7FD4C0) else Color.White
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = story.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${story.authorName} • ${story.shelf} • ~${(story.wordCount / 140).coerceAtLeast(2)} min",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = story.synopsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onRead,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("read_story_${story.id}")
                    ) {
                        Text(if (story.currentChapterIndex > 0) "Continue" else "Read")
                    }
                    OutlinedButton(
                        onClick = onCollaborate,
                        modifier = Modifier.testTag("collab_story_${story.id}")
                    ) {
                        Icon(
                            Icons.Filled.Groups,
                            contentDescription = "Open collaborative studio",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Studio")
                    }
                    TextButton(onClick = onOrganizeShelf) {
                        Text("Shelf")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateUploadStorySheet(
    defaultAuthor: String,
    onDismiss: () -> Unit,
    onImportFile: (Uri, (String, String) -> Unit) -> Unit,
    onSubmit: (
        title: String,
        subtitle: String,
        authorName: String,
        shelf: String,
        ageRange: String,
        synopsis: String,
        bodyContent: String,
        coverPreset: String,
        coverUri: String?,
        isCollaborative: Boolean,
        choiceALabel: String?,
        choiceBLabel: String?
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var author by remember { mutableStateOf(defaultAuthor) }
    var shelf by remember { mutableStateOf("Uploaded") }
    var ageRange by remember { mutableStateOf("All Ages 5-85") }
    var synopsis by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var coverPreset by remember { mutableStateOf("forest") }
    var pickedCoverUri by remember { mutableStateOf<String?>(null) }
    var isCollaborative by remember { mutableStateOf(true) }
    var choiceA by remember { mutableStateOf("") }
    var choiceB by remember { mutableStateOf("") }

    val textImporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportFile(uri) { parsedTitle, parsedBody ->
                if (title.isBlank()) title = parsedTitle
                body = parsedBody
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedCoverUri = uri.toString()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Compose or Upload E-Story", style = MaterialTheme.typography.headlineMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { textImporter.launch(arrayOf("text/plain", "text/markdown", "text/*")) },
                    label = { Text("Import .txt / .md") },
                    leadingIcon = { Icon(Icons.Filled.FileOpen, contentDescription = null) },
                    modifier = Modifier.testTag("import_text_file_btn")
                )
                AssistChip(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    label = { Text(if (pickedCoverUri != null) "Cover Selected" else "Pick Cover Art") },
                    leadingIcon = { Icon(Icons.Filled.Image, contentDescription = null) },
                    modifier = Modifier.testTag("pick_cover_image_btn")
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Story Title") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_story_title")
            )
            OutlinedTextField(
                value = subtitle,
                onValueChange = { subtitle = it },
                label = { Text("Subtitle (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text("Author Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Target Reader Age", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("5-10 yrs", "8-14 yrs", "All Ages 5-85", "16+").forEach { age ->
                    FilterChip(
                        selected = ageRange == age,
                        onClick = { ageRange = age },
                        label = { Text(age) }
                    )
                }
            }

            Text("Cover Theme Preset", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("forest" to "Woodland", "cosmos" to "Cosmos", "coastal" to "Coastal").forEach { (id, label) ->
                    FilterChip(
                        selected = coverPreset == id,
                        onClick = { coverPreset = id },
                        label = { Text(label) }
                    )
                }
            }

            OutlinedTextField(
                value = synopsis,
                onValueChange = { synopsis = it },
                label = { Text("Short Synopsis") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Chapter 1 Narrative (separate paragraphs with blank line)") },
                minLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_story_body")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enable Co-Author Versioning & Chat", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Tracks revision snapshots and author discussion threads",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isCollaborative,
                    onCheckedChange = { isCollaborative = it }
                )
            }

            OutlinedTextField(
                value = choiceA,
                onValueChange = { choiceA = it },
                label = { Text("Interactive Choice A (optional branching path)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = choiceB,
                onValueChange = { choiceB = it },
                label = { Text("Interactive Choice B (optional alternate path)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (title.isNotBlank() && body.isNotBlank()) {
                        onSubmit(
                            title, subtitle, author, shelf, ageRange, synopsis, body,
                            coverPreset, pickedCoverUri, isCollaborative, choiceA, choiceB
                        )
                    }
                },
                enabled = title.isNotBlank() && body.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("publish_story_button")
            ) {
                Text("Save & Publish Offline Story")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
