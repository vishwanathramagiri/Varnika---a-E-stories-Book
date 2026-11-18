package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        StoryEntity::class,
        StoryChapterEntity::class,
        StoryVersionEntity::class,
        AuthorMessageEntity::class,
        HighlightNoteEntity::class,
        ReadingSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VarnikaDatabase : RoomDatabase() {
    abstract fun varnikaDao(): VarnikaDao

    companion object {
        @Volatile
        private var INSTANCE: VarnikaDatabase? = null

        fun getInstance(context: Context): VarnikaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VarnikaDatabase::class.java,
                    "varnika_stories.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

object SeedLibrary {
    suspend fun seedIfEmpty(dao: VarnikaDao) {
        if (dao.getStoryCount() > 0) return

        val now = System.currentTimeMillis()
        val hour = 3_600_000L

        val stories = listOf(
            StoryEntity(
                id = "story_lantern_fox",
                title = "The Lantern Fox of Whispering Woods",
                subtitle = "An interactive woodland adventure for curious minds",
                authorName = "Maya Deshmukh",
                shelf = "Kids 5-10",
                ageRange = "5-10 yrs",
                synopsis = "Follow Pip, a small red fox carrying a warm amber lantern, as you choose which forest paths to explore before moonrise.",
                coverPreset = "forest",
                isOfflineCached = true,
                isCollaborative = true,
                activeVersionTag = "v1.2",
                currentChapterIndex = 0,
                totalChapters = 3,
                wordCount = 340,
                lastReadAt = now - 2 * hour,
                createdAt = now - 48 * hour,
                updatedAt = now - 3 * hour
            ),
            StoryEntity(
                id = "story_clockwork_astrolabe",
                title = "The Clockwork Astrolabe",
                subtitle = "Origami stars and charting unknown skies",
                authorName = "Arjun Rao & Elena Vance",
                shelf = "Collaborative",
                ageRange = "All Ages 5-85",
                synopsis = "Inside an observatory built on brass gears, an apprentice navigator discovers a map that rewrites itself depending on who reads it aloud.",
                coverPreset = "cosmos",
                isOfflineCached = true,
                isCollaborative = true,
                activeVersionTag = "v2.1",
                currentChapterIndex = 0,
                totalChapters = 3,
                wordCount = 410,
                lastReadAt = now - 5 * hour,
                createdAt = now - 72 * hour,
                updatedAt = now - 4 * hour
            ),
            StoryEntity(
                id = "story_terracotta_cliff",
                title = "Letters from the Terracotta Cliff",
                subtitle = "Quiet coastal mornings and shared memories",
                authorName = "Clara Lindqvist",
                shelf = "Favorites",
                ageRange = "All Ages 5-85",
                synopsis = "A gentle anthology of lighthouse keepers, morning tea rituals, and handwritten letters carried across calm turquoise tides.",
                coverPreset = "coastal",
                isOfflineCached = true,
                isCollaborative = false,
                activeVersionTag = "v1.0",
                currentChapterIndex = 0,
                totalChapters = 2,
                wordCount = 295,
                lastReadAt = now - 24 * hour,
                createdAt = now - 96 * hour,
                updatedAt = now - 24 * hour
            )
        )

        val chapters = listOf(
            // Story 1: Lantern Fox (interactive branching)
            StoryChapterEntity(
                id = "fox_ch_0",
                storyId = "story_lantern_fox",
                chapterIndex = 0,
                title = "Chapter 1: The Firefly Fork",
                content = "At the edge of Whispering Woods, the moss glows soft emerald beneath the evening mist. Pip holds up a brass lantern filled with captured starlight.\n\nTwo paths wind through the ancient oaks. To the left, tiny silver bells chime near the brook. To the right, warm cinnamon smoke curls from a hollow cedar tree.\n\n\"Every good story begins with a brave step,\" whispers the wind through the ferns.",
                choicePrompt = "Where should Pip carry the lantern next?",
                choiceALabel = "Follow the silver brook bells",
                choiceATargetIndex = 1,
                choiceBLabel = "Visit the warm cedar bakery",
                choiceBTargetIndex = 2
            ),
            StoryChapterEntity(
                id = "fox_ch_1",
                storyId = "story_lantern_fox",
                chapterIndex = 1,
                title = "Chapter 2A: The Song of the Brook",
                content = "By the rushing stream, three river otters are tuning pebbles like xylophone keys. Pip sets the amber lantern on a flat slate stone.\n\nThe golden light dances across the ripples, revealing a lost paper boat folded from sheet music. Together, Pip and the otters hum the melody until the fireflies gather overhead like a living constellation.",
                choicePrompt = "Continue the journey?",
                choiceALabel = "Walk back toward the cedar bakery",
                choiceATargetIndex = 2,
                choiceBLabel = "Start Chapter 1 again",
                choiceBTargetIndex = 0
            ),
            StoryChapterEntity(
                id = "fox_ch_2",
                storyId = "story_lantern_fox",
                chapterIndex = 2,
                title = "Chapter 2B: The Cedar Lantern Feast",
                content = "Inside the cozy cedar hollow, Grandmother Hedgehog is baking saffron honey loaves for every traveler in the woods.\n\nPip hangs the starlight lantern beside the doorway, sharing stories of the mossy trail while everyone sips warm spiced apple cider. Tonight, no traveler in Whispering Woods walks in the dark.",
                choicePrompt = "What would you like to do next?",
                choiceALabel = "Explore the Silver Brook path",
                choiceATargetIndex = 1,
                choiceBLabel = "Revisit the Firefly Fork",
                choiceBTargetIndex = 0
            ),

            // Story 2: Clockwork Astrolabe
            StoryChapterEntity(
                id = "cosmos_ch_0",
                storyId = "story_clockwork_astrolabe",
                chapterIndex = 0,
                title = "Prologue: Brass & Starlight",
                content = "High above the cloudline, the Meridian Observatory ticks like a patient pocket watch. Young astronomer Lyra polishes the great brass astrolabe before dusk.\n\nWhen she aligns the third ring with Vega, an origami star unfolds out of the brass housing and projects two shimmering trajectories across the vaulted dome.",
                choicePrompt = "Which constellation route should Lyra chart?",
                choiceALabel = "Chart the Amber Nebula drift",
                choiceATargetIndex = 1,
                choiceBLabel = "Decode the Silent Comet cipher",
                choiceBTargetIndex = 2
            ),
            StoryChapterEntity(
                id = "cosmos_ch_1",
                storyId = "story_clockwork_astrolabe",
                chapterIndex = 1,
                title = "Log I: Inside the Amber Nebula",
                content = "Through the wide brass lens, clouds of saffron dust swirl like warm ink dropped into water. Lyra notices tiny lantern satellites left by navigators centuries ago.\n\nEach satellite carries a spoken greeting recorded by travelers of every generation—proof that curiosity bridges every age and distance.",
                choicePrompt = "Shift the astrolabe rings?",
                choiceALabel = "Inspect the Silent Comet cipher",
                choiceATargetIndex = 2,
                choiceBLabel = "Return to Observatory Prologue",
                choiceBTargetIndex = 0
            ),
            StoryChapterEntity(
                id = "cosmos_ch_2",
                storyId = "story_clockwork_astrolabe",
                chapterIndex = 2,
                title = "Log II: The Origami Cipher",
                content = "Carefully unfolding the paper star, Lyra discovers inked coordinates written in calm, deliberate calligraphy: 'Look not only at distant suns, but at who stands beside you under the same sky.'\n\nShe logs the discovery into the shared observatory chronicle so every co-author on Earth can add their own chapter.",
                choicePrompt = "Continue exploring?",
                choiceALabel = "Visit the Amber Nebula",
                choiceATargetIndex = 1,
                choiceBLabel = "Return to Prologue",
                choiceBTargetIndex = 0
            ),

            // Story 3: Terracotta Cliff
            StoryChapterEntity(
                id = "coastal_ch_0",
                storyId = "story_terracotta_cliff",
                chapterIndex = 0,
                title = "Part I: Morning Tea by the Jetty",
                content = "Every morning at six, the sea breeze carries salt and wild rosemary up the terracotta steps of Lantern Point.\n\nMr. Krishnan opens his brass reading glasses, sets a stoneware mug beside his notebook, and listens to the distant gulls while reading aloud to his five-year-old granddaughter.",
                choicePrompt = "Turn to the next page?",
                choiceALabel = "Read Part II: The Evening Sail",
                choiceATargetIndex = 1,
                choiceBLabel = null,
                choiceBTargetIndex = null
            ),
            StoryChapterEntity(
                id = "coastal_ch_1",
                storyId = "story_terracotta_cliff",
                chapterIndex = 1,
                title = "Part II: The Evening Sail",
                content = "As the sun settles behind the turquoise horizon, small wooden sailboats glide toward the cove. In the harbor library, generations gather—children tracing painted maps, elders sharing verses remembered from youth.\n\nGood stories do not rush. They keep pace with breathing, tide, and quiet companionship.",
                choicePrompt = "Finished reading?",
                choiceALabel = "Return to Part I",
                choiceATargetIndex = 0,
                choiceBLabel = null,
                choiceBTargetIndex = null
            )
        )

        val versions = listOf(
            StoryVersionEntity(
                id = "ver_fox_1_0",
                storyId = "story_lantern_fox",
                versionLabel = "v1.0",
                authorName = "Maya Deshmukh",
                commitNote = "Initial draft of Chapter 1 woodland opening",
                chapterIndex = 0,
                chapterTitle = "Chapter 1: The Firefly Fork",
                snapshotContent = "At the edge of Whispering Woods, the moss glows beneath the evening mist. Pip holds up a brass lantern.\n\nTwo paths wind through the oaks. Left leads to the brook, right leads to a cedar tree.",
                timestamp = now - 48 * hour
            ),
            StoryVersionEntity(
                id = "ver_fox_1_1",
                storyId = "story_lantern_fox",
                versionLabel = "v1.1",
                authorName = "Samir Patel",
                commitNote = "Added sensory details for younger listeners (TTS rhythm)",
                chapterIndex = 0,
                chapterTitle = "Chapter 1: The Firefly Fork",
                snapshotContent = "At the edge of Whispering Woods, the moss glows soft emerald beneath the evening mist. Pip holds up a brass lantern filled with starlight.\n\nTwo paths wind through the ancient oaks. To the left, tiny silver bells chime near the brook. To the right, warm cinnamon smoke curls from a hollow cedar tree.",
                timestamp = now - 24 * hour
            ),
            StoryVersionEntity(
                id = "ver_fox_1_2",
                storyId = "story_lantern_fox",
                versionLabel = "v1.2",
                authorName = "Maya Deshmukh",
                commitNote = "Added concluding whisper line for read-aloud pacing",
                chapterIndex = 0,
                chapterTitle = "Chapter 1: The Firefly Fork",
                snapshotContent = chapters[0].content,
                timestamp = now - 3 * hour
            ),
            StoryVersionEntity(
                id = "ver_cosmos_2_0",
                storyId = "story_clockwork_astrolabe",
                versionLabel = "v2.0",
                authorName = "Arjun Rao",
                commitNote = "Expanded observatory dome scene",
                chapterIndex = 0,
                chapterTitle = "Prologue: Brass & Starlight",
                snapshotContent = "High above the cloudline, the Meridian Observatory ticks like a pocket watch. Young astronomer Lyra polishes the brass astrolabe.",
                timestamp = now - 20 * hour
            ),
            StoryVersionEntity(
                id = "ver_cosmos_2_1",
                storyId = "story_clockwork_astrolabe",
                versionLabel = "v2.1",
                authorName = "Elena Vance",
                commitNote = "Added origami star branching projection",
                chapterIndex = 0,
                chapterTitle = "Prologue: Brass & Starlight",
                snapshotContent = chapters[3].content,
                timestamp = now - 4 * hour
            )
        )

        val messages = listOf(
            AuthorMessageEntity(
                id = "msg_1",
                storyId = "story_lantern_fox",
                senderName = "Maya Deshmukh",
                senderRole = "Lead Author",
                chapterRef = "Ch. 1",
                message = "Keep sentences gentle and rhythmic here so TTS sounds warm for 5-year-olds at bedtime.",
                timestamp = now - 20 * hour
            ),
            AuthorMessageEntity(
                id = "msg_2",
                storyId = "story_lantern_fox",
                senderName = "Samir Patel",
                senderRole = "Co-Writer",
                chapterRef = "Ch. 1",
                message = "Updated v1.1 with the silver brook bells and cinnamon smoke cue. Ready for final review!",
                timestamp = now - 10 * hour
            ),
            AuthorMessageEntity(
                id = "msg_3",
                storyId = "story_clockwork_astrolabe",
                senderName = "Elena Vance",
                senderRole = "Editor",
                chapterRef = "Prologue",
                message = "The origami star motif links Prologue and Log II beautifully. Let's keep Chapter 2 open for new branches.",
                timestamp = now - 6 * hour
            )
        )

        dao.upsertStories(stories)
        dao.upsertChapters(chapters)
        dao.insertVersions(versions)
        dao.insertAuthorMessages(messages)
    }
}
