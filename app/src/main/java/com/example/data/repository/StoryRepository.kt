package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import com.example.cache.CacheTelemetry
import com.example.cache.StoryCacheManager
import com.example.data.local.AuthorMessageEntity
import com.example.data.local.HighlightNoteEntity
import com.example.data.local.ReadingSessionEntity
import com.example.data.local.SeedLibrary
import com.example.data.local.StoryChapterEntity
import com.example.data.local.StoryEntity
import com.example.data.local.StoryVersionEntity
import com.example.data.local.VarnikaDao
import com.example.data.preferences.UserPreferencesRepository
import com.example.security.VaultCryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class DiffLine(
    val type: DiffType,
    val text: String
)

enum class DiffType { UNCHANGED, ADDED, REMOVED }

class StoryRepository(
    private val context: Context,
    private val dao: VarnikaDao,
    private val cacheManager: StoryCacheManager,
    private val prefsRepo: UserPreferencesRepository
) {
    val cacheTelemetry: StateFlow<CacheTelemetry> = cacheManager.telemetry

    suspend fun initializeSeedData() {
        SeedLibrary.seedIfEmpty(dao)
    }

    fun observeStories(): Flow<List<StoryEntity>> = dao.observeAllStories()

    fun observeStory(storyId: String): Flow<StoryEntity?> = dao.observeStory(storyId)

    fun observeChapters(storyId: String): Flow<List<StoryChapterEntity>> =
        dao.observeChapters(storyId).onEach { chapters ->
            cacheManager.putChapters(storyId, chapters)
        }

    suspend fun getCachedOrLoadChapters(storyId: String, batterySaver: Boolean): List<StoryChapterEntity> {
        cacheManager.getChapters(storyId)?.let { return it }
        val fromDb = dao.getChaptersOnce(storyId)
        if (!batterySaver || fromDb.size <= 5) {
            cacheManager.putChapters(storyId, fromDb)
        }
        return fromDb
    }

    fun observeVersions(storyId: String): Flow<List<StoryVersionEntity>> = dao.observeVersions(storyId)

    fun observeAuthorMessages(storyId: String): Flow<List<AuthorMessageEntity>> =
        dao.observeAuthorMessages(storyId)

    fun observeAllHighlights(): Flow<List<HighlightNoteEntity>> = dao.observeAllHighlights()

    fun observeStoryHighlights(storyId: String): Flow<List<HighlightNoteEntity>> =
        dao.observeStoryHighlights(storyId)

    fun observeReadingSessions(): Flow<List<ReadingSessionEntity>> = dao.observeReadingSessions()

    fun observeNetworkConnected(): Flow<Boolean> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        fun checkNow(): Boolean {
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        trySend(checkNow())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(checkNow()) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }
        }
        cm.registerDefaultNetworkCallback(callback)
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }

    suspend fun updateReadingPosition(storyId: String, chapterIndex: Int) {
        dao.updateReadingPosition(storyId, chapterIndex, System.currentTimeMillis(), pendingSync = true)
    }

    suspend fun toggleOfflineCache(storyId: String, cached: Boolean) {
        dao.updateOfflineCached(storyId, cached, System.currentTimeMillis())
        if (!cached) {
            cacheManager.invalidate(storyId)
        }
    }

    suspend fun updateStoryShelf(storyId: String, newShelf: String) {
        dao.updateShelf(storyId, newShelf, System.currentTimeMillis())
    }

    suspend fun createOrUploadStory(
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
    ): String {
        val storyId = "story_${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()
        val paragraphs = bodyContent.trim().ifBlank { "Once upon a time..." }
        val words = paragraphs.split(Regex("\\s+")).count { it.isNotBlank() }

        val chapters = mutableListOf<StoryChapterEntity>()
        val hasChoices = !choiceALabel.isNullOrBlank() || !choiceBLabel.isNullOrBlank()
        chapters.add(
            StoryChapterEntity(
                id = "${storyId}_ch_0",
                storyId = storyId,
                chapterIndex = 0,
                title = "Chapter 1: Opening",
                content = paragraphs,
                choicePrompt = if (hasChoices) "Choose the path ahead:" else null,
                choiceALabel = choiceALabel?.takeIf { it.isNotBlank() },
                choiceATargetIndex = if (!choiceALabel.isNullOrBlank()) 1 else null,
                choiceBLabel = choiceBLabel?.takeIf { it.isNotBlank() },
                choiceBTargetIndex = if (!choiceBLabel.isNullOrBlank()) 2 else null
            )
        )
        if (!choiceALabel.isNullOrBlank()) {
            chapters.add(
                StoryChapterEntity(
                    id = "${storyId}_ch_1",
                    storyId = storyId,
                    chapterIndex = 1,
                    title = "Path A: $choiceALabel",
                    content = "You chose \"$choiceALabel\". Continue authoring this branch in the Collaborative Studio to expand your interactive narrative.",
                    choicePrompt = "Branch navigation",
                    choiceALabel = "Return to Opening",
                    choiceATargetIndex = 0
                )
            )
        }
        if (!choiceBLabel.isNullOrBlank()) {
            chapters.add(
                StoryChapterEntity(
                    id = "${storyId}_ch_2",
                    storyId = storyId,
                    chapterIndex = chapters.size,
                    title = "Path B: $choiceBLabel",
                    content = "You chose \"$choiceBLabel\". Open the Collaborative Studio to write this alternate chapter and invite co-authors.",
                    choicePrompt = "Branch navigation",
                    choiceALabel = "Return to Opening",
                    choiceATargetIndex = 0
                )
            )
        }

        val story = StoryEntity(
            id = storyId,
            title = title.trim(),
            subtitle = subtitle.trim().ifBlank { "Interactive E-Story" },
            authorName = authorName.trim().ifBlank { "Reader" },
            shelf = shelf,
            ageRange = ageRange,
            synopsis = synopsis.trim().ifBlank { paragraphs.take(140) },
            coverPreset = coverPreset,
            coverUri = coverUri,
            isOfflineCached = true,
            isCollaborative = isCollaborative,
            activeVersionTag = "v1.0",
            currentChapterIndex = 0,
            totalChapters = chapters.size,
            wordCount = words,
            pendingSync = true,
            lastReadAt = now,
            createdAt = now,
            updatedAt = now
        )

        val initialVersion = StoryVersionEntity(
            id = "ver_${UUID.randomUUID().toString().take(8)}",
            storyId = storyId,
            versionLabel = "v1.0",
            authorName = story.authorName,
            commitNote = "Initial story upload",
            chapterIndex = 0,
            chapterTitle = chapters.first().title,
            snapshotContent = paragraphs,
            timestamp = now
        )

        val initialMessages = if (isCollaborative) {
            listOf(
                AuthorMessageEntity(
                    id = "msg_${UUID.randomUUID().toString().take(8)}",
                    storyId = storyId,
                    senderName = story.authorName,
                    senderRole = "Lead Author",
                    chapterRef = "Ch. 1",
                    message = "Created collaborative workspace for \"${story.title}\". Add revisions and feedback below!",
                    timestamp = now
                )
            )
        } else {
            emptyList()
        }

        dao.insertFullStory(story, chapters, initialVersion, initialMessages)
        cacheManager.putChapters(storyId, chapters)
        return storyId
    }

    suspend fun commitChapterRevision(
        storyId: String,
        chapter: StoryChapterEntity,
        newTitle: String,
        newContent: String,
        authorName: String,
        commitNote: String
    ) {
        val story = dao.getStoryById(storyId) ?: return
        val nextTag = bumpVersionTag(story.activeVersionTag)
        val now = System.currentTimeMillis()

        val updatedChapter = chapter.copy(
            title = newTitle.trim().ifBlank { chapter.title },
            content = newContent.trim()
        )
        dao.upsertChapter(updatedChapter)

        val allChapters = dao.getChaptersOnce(storyId)
        val totalWords = allChapters.sumOf { ch ->
            ch.content.split(Regex("\\s+")).count { it.isNotBlank() }
        }
        dao.upsertStory(
            story.copy(
                activeVersionTag = nextTag,
                wordCount = totalWords,
                isCollaborative = true,
                pendingSync = true,
                updatedAt = now
            )
        )
        dao.insertVersion(
            StoryVersionEntity(
                id = "ver_${UUID.randomUUID().toString().take(8)}",
                storyId = storyId,
                versionLabel = nextTag,
                authorName = authorName.ifBlank { "Co-Author" },
                commitNote = commitNote.ifBlank { "Updated ${updatedChapter.title}" },
                chapterIndex = updatedChapter.chapterIndex,
                chapterTitle = updatedChapter.title,
                snapshotContent = updatedChapter.content,
                timestamp = now
            )
        )
        cacheManager.invalidate(storyId)
    }

    suspend fun addChapterToStory(
        storyId: String,
        chapterTitle: String,
        chapterContent: String,
        authorName: String
    ) {
        val story = dao.getStoryById(storyId) ?: return
        val existing = dao.getChaptersOnce(storyId)
        val nextIdx = existing.size
        val now = System.currentTimeMillis()
        val nextTag = bumpVersionTag(story.activeVersionTag)

        val newChapter = StoryChapterEntity(
            id = "${storyId}_ch_${UUID.randomUUID().toString().take(6)}",
            storyId = storyId,
            chapterIndex = nextIdx,
            title = chapterTitle.trim().ifBlank { "Chapter ${nextIdx + 1}" },
            content = chapterContent.trim()
        )
        dao.upsertChapter(newChapter)

        val allChapters = existing + newChapter
        val totalWords = allChapters.sumOf { ch ->
            ch.content.split(Regex("\\s+")).count { it.isNotBlank() }
        }

        dao.upsertStory(
            story.copy(
                totalChapters = allChapters.size,
                activeVersionTag = nextTag,
                wordCount = totalWords,
                isCollaborative = true,
                pendingSync = true,
                updatedAt = now
            )
        )
        dao.insertVersion(
            StoryVersionEntity(
                id = "ver_${UUID.randomUUID().toString().take(8)}",
                storyId = storyId,
                versionLabel = nextTag,
                authorName = authorName.ifBlank { "Co-Author" },
                commitNote = "Added ${newChapter.title}",
                chapterIndex = nextIdx,
                chapterTitle = newChapter.title,
                snapshotContent = newChapter.content,
                timestamp = now
            )
        )
        cacheManager.invalidate(storyId)
    }

    suspend fun restoreVersion(storyId: String, version: StoryVersionEntity, authorName: String) {
        val chapters = dao.getChaptersOnce(storyId)
        val targetChapter = chapters.firstOrNull { it.chapterIndex == version.chapterIndex }
            ?: chapters.firstOrNull() ?: return
        commitChapterRevision(
            storyId = storyId,
            chapter = targetChapter,
            newTitle = version.chapterTitle,
            newContent = version.snapshotContent,
            authorName = authorName,
            commitNote = "Restored snapshot ${version.versionLabel}"
        )
    }

    suspend fun postAuthorMessage(
        storyId: String,
        senderName: String,
        senderRole: String,
        chapterRef: String,
        message: String
    ) {
        if (message.isBlank()) return
        dao.insertAuthorMessage(
            AuthorMessageEntity(
                id = "msg_${UUID.randomUUID().toString().take(8)}",
                storyId = storyId,
                senderName = senderName.trim().ifBlank { "Collaborator" },
                senderRole = senderRole,
                chapterRef = chapterRef.ifBlank { "General" },
                message = message.trim(),
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveHighlightNote(
        storyId: String,
        storyTitle: String,
        chapterIndex: Int,
        paragraphIndex: Int,
        quotedText: String,
        noteText: String,
        colorTag: String
    ) {
        val digest = VaultCryptoManager.sha256Preview("$storyId|$quotedText|$noteText")
        dao.upsertHighlight(
            HighlightNoteEntity(
                id = "hl_${UUID.randomUUID().toString().take(8)}",
                storyId = storyId,
                storyTitle = storyTitle,
                chapterIndex = chapterIndex,
                paragraphIndex = paragraphIndex,
                quotedText = quotedText.trim(),
                noteText = noteText.trim(),
                colorTag = colorTag,
                encryptedChecksum = digest,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteHighlight(highlightId: String) {
        dao.deleteHighlight(highlightId)
    }

    suspend fun recordReadingSession(storyId: String, minutes: Int, pages: Int, words: Int) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        dao.insertReadingSession(
            ReadingSessionEntity(
                id = "sess_${UUID.randomUUID().toString().take(8)}",
                storyId = storyId,
                dateKey = dateKey,
                hourOfDay = hour,
                minutesRead = minutes.coerceAtLeast(1),
                pagesTurned = pages.coerceAtLeast(0),
                wordsRead = words.coerceAtLeast(0),
                timestamp = now
            )
        )
    }

    suspend fun syncLocalVaultQueue(): Int {
        dao.clearAllPendingSync()
        val now = System.currentTimeMillis()
        prefsRepo.updateLastSyncTimestamp(now)
        return dao.getStoryCount()
    }

    suspend fun readStoryTextFromUri(uri: Uri): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val raw = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return@withContext Result.failure(IllegalStateException("Unable to open file"))
            val firstLine = raw.lineSequence().firstOrNull { it.isNotBlank() }
                ?.trimStart('#', ' ')
                ?.take(60)
                ?: "Imported E-Story"
            Result.success(firstLine to raw)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportEncryptedBackupToUri(uri: Uri, passphrase: String): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val highlights = dao.getAllHighlightsOnce()
                val sessions = dao.getAllSessionsOnce()
                val payload = JSONObject().apply {
                    put("exportedAt", System.currentTimeMillis())
                    put("app", "Varnika")
                    put("highlights", JSONArray().apply {
                        highlights.forEach { h ->
                            put(JSONObject().apply {
                                put("id", h.id)
                                put("storyId", h.storyId)
                                put("storyTitle", h.storyTitle)
                                put("chapterIndex", h.chapterIndex)
                                put("paragraphIndex", h.paragraphIndex)
                                put("quotedText", h.quotedText)
                                put("noteText", h.noteText)
                                put("colorTag", h.colorTag)
                                put("encryptedChecksum", h.encryptedChecksum)
                                put("createdAt", h.createdAt)
                            })
                        }
                    })
                    put("sessionsCount", sessions.size)
                }.toString()

                val encrypted = VaultCryptoManager.encryptJsonPayload(payload, passphrase)
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(encrypted.toByteArray(Charsets.UTF_8))
                } ?: return@withContext Result.failure(IllegalStateException("Cannot write to target file"))
                Result.success(highlights.size)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun importEncryptedBackupFromUri(uri: Uri, passphrase: String): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: return@withContext Result.failure(IllegalStateException("Cannot open backup file"))
                val decryptedJson = VaultCryptoManager.decryptJsonPayload(raw, passphrase)
                val obj = JSONObject(decryptedJson)
                val arr = obj.getJSONArray("highlights")
                val imported = mutableListOf<HighlightNoteEntity>()
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    imported.add(
                        HighlightNoteEntity(
                            id = item.getString("id"),
                            storyId = item.getString("storyId"),
                            storyTitle = item.getString("storyTitle"),
                            chapterIndex = item.getInt("chapterIndex"),
                            paragraphIndex = item.getInt("paragraphIndex"),
                            quotedText = item.getString("quotedText"),
                            noteText = item.getString("noteText"),
                            colorTag = item.optString("colorTag", "SAFFRON"),
                            encryptedChecksum = item.optString("encryptedChecksum", ""),
                            createdAt = item.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                dao.upsertHighlights(imported)
                Result.success(imported.size)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun computeParagraphDiff(oldText: String, newText: String): List<DiffLine> {
        val oldLines = oldText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val newLines = newText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val oldSet = oldLines.toSet()
        val newSet = newLines.toSet()
        val result = mutableListOf<DiffLine>()
        oldLines.forEach { line ->
            if (line !in newSet) result.add(DiffLine(DiffType.REMOVED, line))
        }
        newLines.forEach { line ->
            if (line in oldSet) result.add(DiffLine(DiffType.UNCHANGED, line))
            else result.add(DiffLine(DiffType.ADDED, line))
        }
        return result
    }

    private fun bumpVersionTag(current: String): String {
        val parts = current.removePrefix("v").split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 1
        val minor = (parts.getOrNull(1)?.toIntOrNull() ?: 0) + 1
        return "v$major.$minor"
    }
}
