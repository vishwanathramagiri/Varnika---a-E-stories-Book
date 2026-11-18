package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.HighlightNoteEntity
import com.example.data.local.StoryChapterEntity
import com.example.data.local.StoryEntity
import com.example.data.preferences.AccessibilityPreset
import com.example.data.preferences.ReaderFontOption
import com.example.data.preferences.UserPreferences
import com.example.tts.TtsUiState
import com.example.ui.components.StoryCoverImage
import com.example.ui.theme.HighlightSaffron
import com.example.ui.theme.HighlightSage
import com.example.ui.theme.HighlightSky
import com.example.ui.theme.HighlightTerracotta
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    story: StoryEntity?,
    chapters: List<StoryChapterEntity>,
    highlights: List<HighlightNoteEntity>,
    preferences: UserPreferences,
    ttsState: TtsUiState,
    onBack: () -> Unit,
    onOpenStudio: (String) -> Unit,
    onStartSession: (String) -> Unit,
    onStopSession: (String, Int) -> Unit,
    onNavigateChapter: (String, Int) -> Unit,
    onPlayTts: (List<String>, Int) -> Unit,
    onStopTts: () -> Unit,
    onChangeTtsRate: (Float) -> Unit,
    onSaveHighlight: (chapterIdx: Int, paraIdx: Int, quote: String, note: String, color: String) -> Unit,
    onSetFontSize: (Float) -> Unit,
    onSetLineHeight: (Float) -> Unit,
    onSetReaderFont: (ReaderFontOption) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onApplyPreset: (AccessibilityPreset) -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val haptics = LocalHapticFeedback.current
    var showAccessibilitySheet by remember { mutableStateOf(false) }
    var selectedParagraphForNote by remember { mutableStateOf<Pair<Int, String>?>(null) }

    val currentChapter = chapters.getOrNull(story?.currentChapterIndex ?: 0) ?: chapters.firstOrNull()
    val paragraphs = remember(currentChapter?.id, currentChapter?.content) {
        currentChapter?.content
            ?.split(Regex("\\n\\s*\\n"))
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
    }

    LaunchedEffect(story?.id) {
        story?.id?.let { onStartSession(it) }
    }

    DisposableEffect(story?.id, currentChapter?.id) {
        val storyId = story?.id
        val words = currentChapter?.content?.split(Regex("\\s+"))?.size ?: 100
        onDispose {
            onStopTts()
            if (storyId != null) {
                onStopSession(storyId, words)
            }
        }
    }

    val activeFontFamily = when (preferences.readerFont) {
        ReaderFontOption.LORA_SERIF -> LoraFontFamily
        ReaderFontOption.JAKARTA_SANS -> PlusJakartaSansFontFamily
        ReaderFontOption.PLAYFAIR_EDITORIAL -> PlayfairDisplayFontFamily
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = story?.title ?: "Story Reader",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        if (story != null) {
                            Text(
                                text = "Ch. ${story.currentChapterIndex + 1} of ${chapters.size.coerceAtLeast(1)} • ${story.activeVersionTag}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("reader_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (story != null) {
                        IconButton(
                            onClick = { onOpenStudio(story.id) },
                            modifier = Modifier.testTag("reader_open_studio_btn")
                        ) {
                            Icon(Icons.Filled.Groups, contentDescription = "Open collaborative studio")
                        }
                    }
                    IconButton(
                        onClick = { showAccessibilitySheet = true },
                        modifier = Modifier.testTag("reader_accessibility_btn")
                    ) {
                        Icon(
                            Icons.Filled.FormatSize,
                            contentDescription = stringResource(R.string.cd_accessibility_controls)
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // TTS Audio Accessibility Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (ttsState.isSpeaking) onStopTts()
                                    else onPlayTts(paragraphs, 0)
                                },
                                modifier = Modifier.testTag("tts_play_stop_btn")
                            ) {
                                Icon(
                                    imageVector = if (ttsState.isSpeaking) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                    contentDescription = if (ttsState.isSpeaking) stringResource(R.string.cd_stop_tts) else stringResource(R.string.cd_play_tts)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (ttsState.isSpeaking) "Stop Audio" else "Read Aloud")
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(0.8f to "0.8x", 1.0f to "1x", 1.25f to "1.25x").forEach { (rate, label) ->
                                FilterChip(
                                    selected = kotlin.math.abs(ttsState.speechRate - rate) < 0.05f,
                                    onClick = { onChangeTtsRate(rate) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = ttsState.statusMessage != null) {
                        Text(
                            text = ttsState.statusMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (story == null || currentChapter == null) {
                Text(
                    text = "Select a story from your Varnika library.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(32.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .widthIn(max = 700.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    if (story.currentChapterIndex == 0) {
                        StoryCoverImage(
                            coverPreset = story.coverPreset,
                            coverUri = story.coverUri,
                            contentDescription = story.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(20.dp))
                        )
                    }

                    Text(
                        text = currentChapter.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Tip: Tap any paragraph to highlight or add an encrypted note.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    paragraphs.forEachIndexed { idx, para ->
                        val existingHl = highlights.firstOrNull {
                            it.chapterIndex == currentChapter.chapterIndex && it.paragraphIndex == idx
                        }
                        val isTtsActive = ttsState.isSpeaking && ttsState.activeParagraphIndex == idx
                        val bgColor = when {
                            isTtsActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            existingHl?.colorTag == "SAFFRON" -> HighlightSaffron
                            existingHl?.colorTag == "SAGE" -> HighlightSage
                            existingHl?.colorTag == "TERRACOTTA" -> HighlightTerracotta
                            existingHl?.colorTag == "SKY" -> HighlightSky
                            else -> Color.Transparent
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgColor)
                                .clickable { selectedParagraphForNote = idx to para }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("reader_paragraph_$idx")
                        ) {
                            Text(
                                text = para,
                                fontFamily = activeFontFamily,
                                fontSize = preferences.fontSizeSp.sp,
                                lineHeight = (preferences.fontSizeSp * preferences.lineHeightMultiplier).sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (existingHl != null && existingHl.noteText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Note: ${existingHl.noteText}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Branching Choices
                    if (!currentChapter.choiceALabel.isNullOrBlank() || !currentChapter.choiceBLabel.isNullOrBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = currentChapter.choicePrompt ?: "Choose your next path",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                currentChapter.choiceALabel?.let { labelA ->
                                    val targetA = currentChapter.choiceATargetIndex ?: 0
                                    Button(
                                        onClick = {
                                            if (preferences.hapticPageTurnEnabled) {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            onNavigateChapter(story.id, targetA)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("choice_a_btn")
                                    ) {
                                        Text(labelA)
                                    }
                                }
                                currentChapter.choiceBLabel?.let { labelB ->
                                    val targetB = currentChapter.choiceBTargetIndex ?: 0
                                    OutlinedButton(
                                        onClick = {
                                            if (preferences.hapticPageTurnEnabled) {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            onNavigateChapter(story.id, targetB)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("choice_b_btn")
                                    ) {
                                        Text(labelB)
                                    }
                                }
                            }
                        }
                    }

                    // Standard Linear Page Turners
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (preferences.hapticPageTurnEnabled) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onNavigateChapter(story.id, (story.currentChapterIndex - 1).coerceAtLeast(0))
                            },
                            enabled = story.currentChapterIndex > 0,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Previous")
                        }

                        Button(
                            onClick = {
                                if (preferences.hapticPageTurnEnabled) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onNavigateChapter(
                                    story.id,
                                    (story.currentChapterIndex + 1).coerceAtMost(chapters.lastIndex)
                                )
                            },
                            enabled = story.currentChapterIndex < chapters.lastIndex,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Next Chapter")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Highlight & Note Modal
    selectedParagraphForNote?.let { (paraIdx, quote) ->
        var noteText by remember { mutableStateOf("") }
        var colorTag by remember { mutableStateOf("SAFFRON") }
        ModalBottomSheet(onDismissRequest = { selectedParagraphForNote = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Save Encrypted Highlight & Note", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "\"${quote.take(140)}${if (quote.length > 140) "..." else ""}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(
                        "SAFFRON" to HighlightSaffron,
                        "SAGE" to HighlightSage,
                        "TERRACOTTA" to HighlightTerracotta,
                        "SKY" to HighlightSky
                    ).forEach { (key, swatch) ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(
                                    width = if (colorTag == key) 3.dp else 1.dp,
                                    color = if (colorTag == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { colorTag = key }
                        )
                    }
                }
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Margin note (optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        val chIdx = currentChapter?.chapterIndex ?: 0
                        onSaveHighlight(chIdx, paraIdx, quote, noteText, colorTag)
                        selectedParagraphForNote = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Filled.BookmarkAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save to Encrypted Vault")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Accessibility & Typography Quick-Sheet
    if (showAccessibilitySheet) {
        ModalBottomSheet(onDismissRequest = { showAccessibilitySheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Reading Accessibility (Ages 5–85)", style = MaterialTheme.typography.headlineMedium)

                Text("Quick Age & Vision Profiles", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccessibilityPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = preferences.accessibilityPreset == preset,
                            onClick = { onApplyPreset(preset) },
                            label = { Text(preset.label) }
                        )
                    }
                }

                Text("Font Size: ${preferences.fontSizeSp.toInt()} sp", style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = preferences.fontSizeSp,
                    onValueChange = onSetFontSize,
                    valueRange = 14f..32f,
                    modifier = Modifier.testTag("font_size_slider")
                )

                Text(
                    "Line Spacing: ${String.format("%.2fx", preferences.lineHeightMultiplier)}",
                    style = MaterialTheme.typography.titleSmall
                )
                Slider(
                    value = preferences.lineHeightMultiplier,
                    onValueChange = onSetLineHeight,
                    valueRange = 1.3f..2.0f
                )

                Text("Typeface", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReaderFontOption.entries.forEach { option ->
                        FilterChip(
                            selected = preferences.readerFont == option,
                            onClick = { onSetReaderFont(option) },
                            label = { Text(option.label) }
                        )
                    }
                }

                Text("Reading Theme", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = preferences.themeMode == mode,
                            onClick = { onSetThemeMode(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Vibration, contentDescription = null)
                        Text("Haptic Page Transitions", style = MaterialTheme.typography.bodyLarge)
                    }
                    Switch(
                        checked = preferences.hapticPageTurnEnabled,
                        onCheckedChange = onSetHaptics
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
