package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AuthorMessageEntity
import com.example.data.local.StoryChapterEntity
import com.example.data.local.StoryEntity
import com.example.data.local.StoryVersionEntity
import com.example.data.repository.DiffLine
import com.example.data.repository.DiffType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    stories: List<StoryEntity>,
    activeStory: StoryEntity?,
    chapters: List<StoryChapterEntity>,
    versions: List<StoryVersionEntity>,
    messages: List<AuthorMessageEntity>,
    authorName: String,
    onSelectStory: (String) -> Unit,
    onCommitRevision: (StoryEntity, StoryChapterEntity, String, String, String) -> Unit,
    onAddChapter: (String, String, String) -> Unit,
    onRestoreVersion: (String, StoryVersionEntity) -> Unit,
    onComputeDiff: (String, String) -> List<DiffLine>,
    onPostMessage: (String, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Collaborative Author Studio",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Real-time local versioning, revision diffs, and co-author discussion.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Story Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            stories.forEach { story ->
                FilterChip(
                    selected = activeStory?.id == story.id,
                    onClick = { onSelectStory(story.id) },
                    label = { Text("${story.title} (${story.activeVersionTag})") }
                )
            }
        }

        if (activeStory == null) {
            Text(
                text = "Select a story above to edit drafts, inspect version history, or chat with collaborators.",
                style = MaterialTheme.typography.bodyLarge
            )
            return@Column
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 840.dp
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        DraftEditorPane(
                            story = activeStory,
                            chapters = chapters,
                            onCommitRevision = onCommitRevision,
                            onAddChapter = onAddChapter
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        PrimaryTabRow(selectedTabIndex = selectedTab.coerceAtMost(1)) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Versions (${versions.size})") },
                                icon = { Icon(Icons.Filled.History, contentDescription = null) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Author Chat (${messages.size})") },
                                icon = { Icon(Icons.Filled.Forum, contentDescription = null) }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (selectedTab == 0) {
                            VersionHistoryPane(
                                storyId = activeStory.id,
                                versions = versions,
                                onComputeDiff = onComputeDiff,
                                onRestoreVersion = onRestoreVersion
                            )
                        } else {
                            AuthorChatPane(
                                storyId = activeStory.id,
                                authorName = authorName,
                                messages = messages,
                                onPostMessage = onPostMessage
                            )
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    PrimaryTabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Draft") },
                            icon = { Icon(Icons.Filled.EditNote, contentDescription = null) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Versions") },
                            icon = { Icon(Icons.Filled.History, contentDescription = null) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Chat") },
                            icon = { Icon(Icons.Filled.Forum, contentDescription = null) }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    when (selectedTab) {
                        0 -> DraftEditorPane(
                            story = activeStory,
                            chapters = chapters,
                            onCommitRevision = onCommitRevision,
                            onAddChapter = onAddChapter
                        )
                        1 -> VersionHistoryPane(
                            storyId = activeStory.id,
                            versions = versions,
                            onComputeDiff = onComputeDiff,
                            onRestoreVersion = onRestoreVersion
                        )
                        else -> AuthorChatPane(
                            storyId = activeStory.id,
                            authorName = authorName,
                            messages = messages,
                            onPostMessage = onPostMessage
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DraftEditorPane(
    story: StoryEntity,
    chapters: List<StoryChapterEntity>,
    onCommitRevision: (StoryEntity, StoryChapterEntity, String, String, String) -> Unit,
    onAddChapter: (String, String, String) -> Unit
) {
    var selectedChapterIdx by remember(story.id) { mutableIntStateOf(0) }
    val activeChapter = chapters.getOrNull(selectedChapterIdx) ?: chapters.firstOrNull()

    var titleDraft by remember(activeChapter?.id, activeChapter?.title) {
        mutableStateOf(activeChapter?.title.orEmpty())
    }
    var contentDraft by remember(activeChapter?.id, activeChapter?.content) {
        mutableStateOf(activeChapter?.content.orEmpty())
    }
    var commitNote by remember { mutableStateOf("") }
    var showNewChapterForm by remember { mutableStateOf(false) }
    var newChapterTitle by remember { mutableStateOf("") }
    var newChapterBody by remember { mutableStateOf("") }

    LaunchedEffect(chapters.size) {
        if (selectedChapterIdx >= chapters.size && chapters.isNotEmpty()) {
            selectedChapterIdx = chapters.lastIndex
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            chapters.forEachIndexed { idx, ch ->
                FilterChip(
                    selected = idx == selectedChapterIdx,
                    onClick = { selectedChapterIdx = idx },
                    label = { Text("Ch. ${idx + 1}") }
                )
            }
            OutlinedButton(onClick = { showNewChapterForm = !showNewChapterForm }) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Chapter")
            }
        }

        if (showNewChapterForm) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Append New Chapter", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = newChapterTitle,
                        onValueChange = { newChapterTitle = it },
                        label = { Text("Chapter Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newChapterBody,
                        onValueChange = { newChapterBody = it },
                        label = { Text("Chapter Narrative") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (newChapterBody.isNotBlank()) {
                                onAddChapter(story.id, newChapterTitle, newChapterBody)
                                newChapterTitle = ""
                                newChapterBody = ""
                                showNewChapterForm = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Commit New Chapter")
                    }
                }
            }
        }

        if (activeChapter != null) {
            OutlinedTextField(
                value = titleDraft,
                onValueChange = { titleDraft = it },
                label = { Text("Chapter Heading") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = contentDraft,
                onValueChange = { contentDraft = it },
                label = { Text("Collaborative Narrative Text") },
                minLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("studio_draft_editor")
            )
            OutlinedTextField(
                value = commitNote,
                onValueChange = { commitNote = it },
                label = { Text("Version Commit Note (e.g., Refined dialogue pacing)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("studio_commit_note")
            )
            Button(
                onClick = {
                    onCommitRevision(story, activeChapter, titleDraft, contentDraft, commitNote)
                    commitNote = ""
                },
                enabled = contentDraft.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("commit_version_btn")
            ) {
                Text("Commit Version Snapshot (${story.activeVersionTag} → next)")
            }
        }
    }
}

@Composable
private fun VersionHistoryPane(
    storyId: String,
    versions: List<StoryVersionEntity>,
    onComputeDiff: (String, String) -> List<DiffLine>,
    onRestoreVersion: (String, StoryVersionEntity) -> Unit
) {
    var expandedDiffId by remember { mutableStateOf<String?>(null) }
    val dateFmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(versions.indices.toList(), key = { idx -> versions[idx].id }) { idx ->
            val version = versions[idx]
            val previous = versions.getOrNull(idx + 1)
            val showDiff = expandedDiffId == version.id

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text(
                                text = version.versionLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = dateFmt.format(Date(version.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = version.commitNote,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "${version.authorName} • ${version.chapterTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            expandedDiffId = if (showDiff) null else version.id
                        }) {
                            Icon(Icons.Filled.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showDiff) "Hide Diff" else "Inspect Diff")
                        }
                        TextButton(
                            onClick = { onRestoreVersion(storyId, version) },
                            modifier = Modifier.testTag("restore_version_${version.versionLabel}")
                        ) {
                            Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore")
                        }
                    }

                    if (showDiff) {
                        val diffLines = remember(version.id, previous?.id) {
                            onComputeDiff(previous?.snapshotContent.orEmpty(), version.snapshotContent)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            diffLines.forEach { line ->
                                val prefix = when (line.type) {
                                    DiffType.ADDED -> "+ "
                                    DiffType.REMOVED -> "- "
                                    DiffType.UNCHANGED -> "  "
                                }
                                val color = when (line.type) {
                                    DiffType.ADDED -> Color(0xFF1E7B57)
                                    DiffType.REMOVED -> MaterialTheme.colorScheme.tertiary
                                    DiffType.UNCHANGED -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Text(
                                    text = "$prefix${line.text}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = color
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthorChatPane(
    storyId: String,
    authorName: String,
    messages: List<AuthorMessageEntity>,
    onPostMessage: (String, String, String, String) -> Unit
) {
    var draftMessage by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Co-Writer") }
    var chapterRef by remember { mutableStateOf("Ch. 1") }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Lead Author", "Co-Writer", "Editor", "Illustrator").forEach { role ->
                FilterChip(
                    selected = selectedRole == role,
                    onClick = { selectedRole = role },
                    label = { Text(role) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Text(
                        text = "Start an author discussion thread for this story as $authorName.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            items(messages, key = { it.id }) { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${msg.senderName} • ${msg.senderRole}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${msg.chapterRef} • ${timeFmt.format(Date(msg.timestamp))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = msg.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = chapterRef,
                onValueChange = { chapterRef = it },
                label = { Text("Ref") },
                singleLine = true,
                modifier = Modifier.width(84.dp)
            )
            OutlinedTextField(
                value = draftMessage,
                onValueChange = { draftMessage = it },
                placeholder = { Text("Message co-authors...") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("author_chat_input")
            )
            IconButton(
                onClick = {
                    if (draftMessage.isNotBlank()) {
                        onPostMessage(storyId, selectedRole, chapterRef, draftMessage)
                        draftMessage = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("author_chat_send_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send author message")
            }
        }
    }
}
