package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface VarnikaDao {

    @Query("SELECT * FROM stories ORDER BY lastReadAt DESC, updatedAt DESC")
    fun observeAllStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE id = :storyId")
    fun observeStory(storyId: String): Flow<StoryEntity?>

    @Query("SELECT * FROM stories WHERE id = :storyId")
    suspend fun getStoryById(storyId: String): StoryEntity?

    @Query("SELECT COUNT(*) FROM stories")
    suspend fun getStoryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStory(story: StoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStories(stories: List<StoryEntity>)

    @Query("UPDATE stories SET currentChapterIndex = :chapterIndex, lastReadAt = :readAt, pendingSync = :pendingSync WHERE id = :storyId")
    suspend fun updateReadingPosition(storyId: String, chapterIndex: Int, readAt: Long, pendingSync: Boolean)

    @Query("UPDATE stories SET isOfflineCached = :cached, updatedAt = :updatedAt WHERE id = :storyId")
    suspend fun updateOfflineCached(storyId: String, cached: Boolean, updatedAt: Long)

    @Query("UPDATE stories SET shelf = :shelf, updatedAt = :updatedAt, pendingSync = 1 WHERE id = :storyId")
    suspend fun updateShelf(storyId: String, shelf: String, updatedAt: Long)

    @Query("UPDATE stories SET pendingSync = 0")
    suspend fun clearAllPendingSync()

    @Query("DELETE FROM stories WHERE id = :storyId")
    suspend fun deleteStory(storyId: String)

    // Chapters
    @Query("SELECT * FROM story_chapters WHERE storyId = :storyId ORDER BY chapterIndex ASC")
    fun observeChapters(storyId: String): Flow<List<StoryChapterEntity>>

    @Query("SELECT * FROM story_chapters WHERE storyId = :storyId ORDER BY chapterIndex ASC")
    suspend fun getChaptersOnce(storyId: String): List<StoryChapterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChapter(chapter: StoryChapterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChapters(chapters: List<StoryChapterEntity>)

    // Versions
    @Query("SELECT * FROM story_versions WHERE storyId = :storyId ORDER BY timestamp DESC")
    fun observeVersions(storyId: String): Flow<List<StoryVersionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: StoryVersionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersions(versions: List<StoryVersionEntity>)

    // Author Discussion Chat
    @Query("SELECT * FROM author_messages WHERE storyId = :storyId ORDER BY timestamp ASC")
    fun observeAuthorMessages(storyId: String): Flow<List<AuthorMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuthorMessage(message: AuthorMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuthorMessages(messages: List<AuthorMessageEntity>)

    // Highlights & Notes
    @Query("SELECT * FROM highlight_notes ORDER BY createdAt DESC")
    fun observeAllHighlights(): Flow<List<HighlightNoteEntity>>

    @Query("SELECT * FROM highlight_notes WHERE storyId = :storyId ORDER BY chapterIndex ASC, paragraphIndex ASC")
    fun observeStoryHighlights(storyId: String): Flow<List<HighlightNoteEntity>>

    @Query("SELECT * FROM highlight_notes")
    suspend fun getAllHighlightsOnce(): List<HighlightNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHighlight(highlight: HighlightNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHighlights(highlights: List<HighlightNoteEntity>)

    @Query("DELETE FROM highlight_notes WHERE id = :highlightId")
    suspend fun deleteHighlight(highlightId: String)

    // Reading Analytics Sessions
    @Query("SELECT * FROM reading_sessions ORDER BY timestamp DESC")
    fun observeReadingSessions(): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions")
    suspend fun getAllSessionsOnce(): List<ReadingSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingSession(session: ReadingSessionEntity)

    @Transaction
    suspend fun insertFullStory(
        story: StoryEntity,
        chapters: List<StoryChapterEntity>,
        initialVersion: StoryVersionEntity,
        initialMessages: List<AuthorMessageEntity> = emptyList()
    ) {
        upsertStory(story)
        upsertChapters(chapters)
        insertVersion(initialVersion)
        if (initialMessages.isNotEmpty()) {
            insertAuthorMessages(initialMessages)
        }
    }
}
