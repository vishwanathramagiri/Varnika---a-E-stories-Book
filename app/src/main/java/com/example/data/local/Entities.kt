package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    val authorName: String,
    val shelf: String,
    val ageRange: String,
    val synopsis: String,
    val coverPreset: String,
    val coverUri: String? = null,
    val isOfflineCached: Boolean = true,
    val isCollaborative: Boolean = false,
    val activeVersionTag: String = "v1.0",
    val currentChapterIndex: Int = 0,
    val totalChapters: Int = 1,
    val wordCount: Int = 0,
    val pendingSync: Boolean = false,
    val lastReadAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "story_chapters",
    foreignKeys = [
        ForeignKey(
            entity = StoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["storyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("storyId")]
)
data class StoryChapterEntity(
    @PrimaryKey val id: String,
    val storyId: String,
    val chapterIndex: Int,
    val title: String,
    val content: String,
    val choicePrompt: String? = null,
    val choiceALabel: String? = null,
    val choiceATargetIndex: Int? = null,
    val choiceBLabel: String? = null,
    val choiceBTargetIndex: Int? = null
)

@Entity(
    tableName = "story_versions",
    foreignKeys = [
        ForeignKey(
            entity = StoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["storyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("storyId")]
)
data class StoryVersionEntity(
    @PrimaryKey val id: String,
    val storyId: String,
    val versionLabel: String,
    val authorName: String,
    val commitNote: String,
    val chapterIndex: Int,
    val chapterTitle: String,
    val snapshotContent: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "author_messages",
    foreignKeys = [
        ForeignKey(
            entity = StoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["storyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("storyId")]
)
data class AuthorMessageEntity(
    @PrimaryKey val id: String,
    val storyId: String,
    val senderName: String,
    val senderRole: String,
    val chapterRef: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "highlight_notes")
data class HighlightNoteEntity(
    @PrimaryKey val id: String,
    val storyId: String,
    val storyTitle: String,
    val chapterIndex: Int,
    val paragraphIndex: Int,
    val quotedText: String,
    val noteText: String,
    val colorTag: String = "SAFFRON",
    val encryptedChecksum: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reading_sessions")
data class ReadingSessionEntity(
    @PrimaryKey val id: String,
    val storyId: String,
    val dateKey: String,
    val hourOfDay: Int,
    val minutesRead: Int,
    val pagesTurned: Int,
    val wordsRead: Int,
    val timestamp: Long = System.currentTimeMillis()
)
