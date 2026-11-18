package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.cache.CacheTelemetry
import com.example.cache.StoryCacheManager
import com.example.data.local.AuthorMessageEntity
import com.example.data.local.HighlightNoteEntity
import com.example.data.local.ReadingSessionEntity
import com.example.data.local.StoryChapterEntity
import com.example.data.local.StoryEntity
import com.example.data.local.StoryVersionEntity
import com.example.data.local.VarnikaDatabase
import com.example.data.preferences.AccessibilityPreset
import com.example.data.preferences.ReaderFontOption
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.DiffLine
import com.example.data.repository.StoryRepository
import com.example.notifications.ReadingReminderReceiver
import com.example.notifications.ReadingReminderScheduler
import com.example.security.VaultCryptoManager
import com.example.tts.StoryTtsController
import com.example.tts.TtsUiState
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VarnikaViewModel(
    private val appContext: Context,
    private val repository: StoryRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val ttsController: StoryTtsController
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    val stories: StateFlow<List<StoryEntity>> = repository.observeStories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHighlights: StateFlow<List<HighlightNoteEntity>> = repository.observeAllHighlights()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readingSessions: StateFlow<List<ReadingSessionEntity>> = repository.observeReadingSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cacheTelemetry: StateFlow<CacheTelemetry> = repository.cacheTelemetry

    val isNetworkConnected: StateFlow<Boolean> = repository.observeNetworkConnected()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val ttsState: StateFlow<TtsUiState> = ttsController.state

    private val _selectedStoryId = MutableStateFlow<String?>(null)
    val selectedStoryId: StateFlow<String?> = _selectedStoryId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedShelf = MutableStateFlow("All")
    val selectedShelf: StateFlow<String> = _selectedShelf.asStateFlow()

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _snackEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackEvents: SharedFlow<String> = _snackEvents.asSharedFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeStory: StateFlow<StoryEntity?> = _selectedStoryId
        .flatMapLatest { id -> if (id == null) flowOf(null) else repository.observeStory(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeChapters: StateFlow<List<StoryChapterEntity>> = _selectedStoryId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.observeChapters(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeVersions: StateFlow<List<StoryVersionEntity>> = _selectedStoryId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.observeVersions(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeAuthorMessages: StateFlow<List<AuthorMessageEntity>> = _selectedStoryId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.observeAuthorMessages(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeStoryHighlights: StateFlow<List<HighlightNoteEntity>> = _selectedStoryId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.observeStoryHighlights(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredStories: StateFlow<List<StoryEntity>> = combine(
        stories,
        _searchQuery,
        _selectedShelf,
        preferences
    ) { all, query, shelf, prefs ->
        all.filter { story ->
            val matchesOffline = !prefs.offlineOnlyMode || story.isOfflineCached
            val matchesShelf = when (shelf) {
                "All" -> true
                "Offline" -> story.isOfflineCached
                "Shared" -> story.isCollaborative
                else -> story.shelf.equals(shelf, ignoreCase = true)
            }
            val q = query.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                story.title.lowercase().contains(q) ||
                story.authorName.lowercase().contains(q) ||
                story.ageRange.lowercase().contains(q) ||
                story.synopsis.lowercase().contains(q)
            matchesOffline && matchesShelf && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var readingTimerJob: Job? = null
    private var activePagesTurned = 0
    private var activeReadSeconds = 0

    init {
        viewModelScope.launch {
            repository.initializeSeedData()
            val firstId = stories.value.firstOrNull()?.id
            if (_selectedStoryId.value == null && firstId != null) {
                _selectedStoryId.value = firstId
            }
        }
        // Auto-sync local vault queue whenever connectivity returns and not in offline-only mode
        viewModelScope.launch {
            combine(isNetworkConnected, preferences) { connected, prefs ->
                connected && !prefs.offlineOnlyMode
            }.collect { canAutoSync ->
                if (canAutoSync && stories.value.any { it.pendingSync }) {
                    repository.syncLocalVaultQueue()
                }
            }
        }
    }

    fun selectStory(storyId: String) {
        _selectedStoryId.value = storyId
        viewModelScope.launch {
            repository.getCachedOrLoadChapters(storyId, preferences.value.travelBatterySaverEnabled)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedShelf(shelf: String) {
        _selectedShelf.value = shelf
    }

    fun startReadingTimer(storyId: String) {
        readingTimerJob?.cancel()
        activePagesTurned = 0
        activeReadSeconds = 0
        readingTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                activeReadSeconds += 1
            }
        }
    }

    fun stopAndRecordReadingSession(storyId: String, currentChapterWords: Int) {
        readingTimerJob?.cancel()
        readingTimerJob = null
        val elapsedMins = ((activeReadSeconds + 29) / 60).coerceAtLeast(if (activePagesTurned > 0 || activeReadSeconds >= 5) 1 else 0)
        if (elapsedMins > 0) {
            val pages = activePagesTurned
            val words = (pages + 1) * currentChapterWords.coerceAtLeast(60)
            viewModelScope.launch {
                repository.recordReadingSession(storyId, elapsedMins, pages, words)
            }
        }
        activeReadSeconds = 0
        activePagesTurned = 0
    }

    fun navigateChapter(storyId: String, targetChapterIndex: Int) {
        activePagesTurned += 1
        ttsController.stop()
        viewModelScope.launch {
            repository.updateReadingPosition(storyId, targetChapterIndex)
        }
    }

    fun toggleOfflineCached(storyId: String, currentlyCached: Boolean) {
        viewModelScope.launch {
            repository.toggleOfflineCache(storyId, !currentlyCached)
            _snackEvents.tryEmit(
                if (!currentlyCached) "Saved for offline reading" else "Removed from offline cache"
            )
        }
    }

    fun updateStoryShelf(storyId: String, newShelf: String) {
        viewModelScope.launch {
            repository.updateStoryShelf(storyId, newShelf)
            _snackEvents.tryEmit("Moved to $newShelf shelf")
        }
    }

    fun createOrUploadStory(
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
        choiceBLabel: String?,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val id = repository.createOrUploadStory(
                title = title,
                subtitle = subtitle,
                authorName = authorName.ifBlank { preferences.value.readerDisplayName },
                shelf = shelf,
                ageRange = ageRange,
                synopsis = synopsis,
                bodyContent = bodyContent,
                coverPreset = coverPreset,
                coverUri = coverUri,
                isCollaborative = isCollaborative,
                choiceALabel = choiceALabel,
                choiceBLabel = choiceBLabel
            )
            _selectedStoryId.value = id
            _snackEvents.tryEmit("Published \"$title\" to your Varnika library")
            onCreated(id)
        }
    }

    fun importStoryFile(uri: Uri, onLoaded: (title: String, body: String) -> Unit) {
        viewModelScope.launch {
            repository.readStoryTextFromUri(uri)
                .onSuccess { (title, body) -> onLoaded(title, body) }
                .onFailure { _snackEvents.tryEmit("Could not read file: ${it.localizedMessage}") }
        }
    }

    fun commitChapterRevision(
        storyId: String,
        chapter: StoryChapterEntity,
        newTitle: String,
        newContent: String,
        commitNote: String
    ) {
        viewModelScope.launch {
            repository.commitChapterRevision(
                storyId = storyId,
                chapter = chapter,
                newTitle = newTitle,
                newContent = newContent,
                authorName = preferences.value.readerDisplayName,
                commitNote = commitNote
            )
            _snackEvents.tryEmit("Saved new version snapshot")
        }
    }

    fun addChapterToStory(storyId: String, title: String, content: String) {
        viewModelScope.launch {
            repository.addChapterToStory(
                storyId = storyId,
                chapterTitle = title,
                chapterContent = content,
                authorName = preferences.value.readerDisplayName
            )
            _snackEvents.tryEmit("Added new chapter & version snapshot")
        }
    }

    fun restoreVersion(storyId: String, version: StoryVersionEntity) {
        viewModelScope.launch {
            repository.restoreVersion(storyId, version, preferences.value.readerDisplayName)
            _snackEvents.tryEmit("Restored ${version.versionLabel}")
        }
    }

    fun computeDiff(oldText: String, newText: String): List<DiffLine> =
        repository.computeParagraphDiff(oldText, newText)

    fun postAuthorMessage(storyId: String, role: String, chapterRef: String, message: String) {
        viewModelScope.launch {
            repository.postAuthorMessage(
                storyId = storyId,
                senderName = preferences.value.readerDisplayName,
                senderRole = role,
                chapterRef = chapterRef,
                message = message
            )
        }
    }

    fun saveHighlight(
        storyId: String,
        storyTitle: String,
        chapterIndex: Int,
        paragraphIndex: Int,
        quotedText: String,
        noteText: String,
        colorTag: String
    ) {
        viewModelScope.launch {
            repository.saveHighlightNote(
                storyId = storyId,
                storyTitle = storyTitle,
                chapterIndex = chapterIndex,
                paragraphIndex = paragraphIndex,
                quotedText = quotedText,
                noteText = noteText,
                colorTag = colorTag
            )
            _snackEvents.tryEmit("Encrypted highlight saved")
        }
    }

    fun deleteHighlight(id: String) {
        viewModelScope.launch {
            repository.deleteHighlight(id)
        }
    }

    fun exportEncryptedBackup(uri: Uri, passphrase: String) {
        viewModelScope.launch {
            repository.exportEncryptedBackupToUri(uri, passphrase)
                .onSuccess { count ->
                    _snackEvents.tryEmit("Encrypted & exported $count highlights (AES-256-GCM)")
                }
                .onFailure {
                    _snackEvents.tryEmit("Backup failed: ${it.localizedMessage}")
                }
        }
    }

    fun importEncryptedBackup(uri: Uri, passphrase: String) {
        viewModelScope.launch {
            repository.importEncryptedBackupFromUri(uri, passphrase)
                .onSuccess { count ->
                    _snackEvents.tryEmit("Decrypted & restored $count highlights")
                }
                .onFailure {
                    _snackEvents.tryEmit("Decryption failed: check passphrase or file")
                }
        }
    }

    // TTS Controls
    fun playTts(paragraphs: List<String>, startIndex: Int = 0) {
        ttsController.speakParagraphs(paragraphs, startIndex)
    }

    fun stopTts() {
        ttsController.stop()
    }

    fun setTtsRate(rate: Float) {
        ttsController.setSpeechRate(rate)
    }

    // Preferences & Accessibility
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun setFontSize(sizeSp: Float) {
        viewModelScope.launch { prefsRepo.setFontSize(sizeSp) }
    }

    fun setLineHeight(multiplier: Float) {
        viewModelScope.launch { prefsRepo.setLineHeight(multiplier) }
    }

    fun setReaderFont(font: ReaderFontOption) {
        viewModelScope.launch { prefsRepo.setReaderFont(font) }
    }

    fun applyAccessibilityPreset(preset: AccessibilityPreset) {
        viewModelScope.launch {
            prefsRepo.applyAccessibilityPreset(preset)
            _snackEvents.tryEmit("Applied ${preset.label} (${preset.ageBadge}) reading profile")
        }
    }

    fun setHapticPageTurn(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setHapticPageTurn(enabled) }
    }

    fun setTravelBatterySaver(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setTravelBatterySaver(enabled) }
    }

    fun setOfflineOnlyMode(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setOfflineOnlyMode(enabled) }
    }

    fun setDailyGoalMinutes(minutes: Int) {
        viewModelScope.launch {
            prefsRepo.setDailyGoalMinutes(minutes)
            val current = preferences.value
            if (current.reminderEnabled) {
                ReadingReminderScheduler.scheduleDailyReminder(
                    appContext, current.reminderHour, current.reminderMinute, minutes
                )
            }
        }
    }

    fun setReminderSchedule(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            prefsRepo.setReminderSchedule(enabled, hour, minute)
            val goal = preferences.value.dailyGoalMinutes
            if (enabled) {
                ReadingReminderScheduler.scheduleDailyReminder(appContext, hour, minute, goal)
                _snackEvents.tryEmit(String.format("Daily reminder set for %02d:%02d", hour, minute))
            } else {
                ReadingReminderScheduler.cancelReminder(appContext)
                _snackEvents.tryEmit("Reading reminders paused")
            }
        }
    }

    fun sendTestReminderNow() {
        ReadingReminderReceiver.showReadingGoalNotification(
            appContext,
            preferences.value.dailyGoalMinutes
        )
        _snackEvents.tryEmit("Sent reading goal reminder notification")
    }

    fun setReaderDisplayName(name: String) {
        viewModelScope.launch {
            prefsRepo.setReaderDisplayName(name)
            _snackEvents.tryEmit("Updated author display name")
        }
    }

    fun syncLocalVaultNow() {
        viewModelScope.launch {
            val count = repository.syncLocalVaultQueue()
            _snackEvents.tryEmit("Local vault verified ($count stories cached offline)")
        }
    }

    // Two-Factor Local Vault Lock
    fun enableVaultLock(pin: String): String {
        val salt = VaultCryptoManager.generateSaltBase64()
        val pinHash = VaultCryptoManager.hashPin(pin, salt)
        val recoveryCode = VaultCryptoManager.generateRecoveryCode()
        viewModelScope.launch {
            prefsRepo.enableVaultLock(pinHash, salt, recoveryCode)
            _isVaultUnlocked.value = true
            _snackEvents.tryEmit("2-Factor Vault Protection enabled")
        }
        return recoveryCode
    }

    fun disableVaultLock() {
        viewModelScope.launch {
            prefsRepo.disableVaultLock()
            _isVaultUnlocked.value = true
            _snackEvents.tryEmit("Vault lock disabled")
        }
    }

    fun unlockWithPinOrRecovery(input: String): Boolean {
        val prefs = preferences.value
        val validPin = VaultCryptoManager.verifyPin(input.trim(), prefs.vaultPinHash, prefs.vaultSalt)
        val validRecovery = prefs.twoFactorRecoveryCode.isNotBlank() &&
            input.trim().equals(prefs.twoFactorRecoveryCode, ignoreCase = true)
        return if (validPin || validRecovery) {
            _isVaultUnlocked.value = true
            true
        } else {
            false
        }
    }

    override fun onCleared() {
        super.onCleared()
        readingTimerJob?.cancel()
        ttsController.shutdown()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val db = VarnikaDatabase.getInstance(app)
                val prefsRepo = UserPreferencesRepository(app)
                val cacheManager = StoryCacheManager()
                val repo = StoryRepository(app, db.varnikaDao(), cacheManager, prefsRepo)
                val tts = StoryTtsController(app)
                return VarnikaViewModel(app, repo, prefsRepo, tts) as T
            }
        }
    }
}
