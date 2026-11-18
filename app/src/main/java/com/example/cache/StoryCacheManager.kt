package com.example.cache

import android.util.LruCache
import com.example.data.local.StoryChapterEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CacheTelemetry(
    val cachedChapters: Int = 0,
    val estimatedBytes: Long = 0L,
    val cacheHits: Int = 0,
    val cacheMisses: Int = 0
) {
    val hitRatePercent: Int
        get() {
            val total = cacheHits + cacheMisses
            return if (total == 0) 100 else ((cacheHits * 100f) / total).toInt()
        }
}

class StoryCacheManager {
    private val maxKb = 1024 // 1 MB memory cap for battery & low-RAM efficiency
    private val cache = object : LruCache<String, List<StoryChapterEntity>>(maxKb) {
        override fun sizeOf(key: String, value: List<StoryChapterEntity>): Int {
            val bytes = value.sumOf { it.title.length + it.content.length } * 2
            return (bytes / 1024).coerceAtLeast(1)
        }
    }

    private val _telemetry = MutableStateFlow(CacheTelemetry())
    val telemetry: StateFlow<CacheTelemetry> = _telemetry.asStateFlow()

    fun getChapters(storyId: String): List<StoryChapterEntity>? {
        val hit = cache.get(storyId)
        val current = _telemetry.value
        _telemetry.value = if (hit != null) {
            current.copy(cacheHits = current.cacheHits + 1)
        } else {
            current.copy(cacheMisses = current.cacheMisses + 1)
        }
        return hit
    }

    fun putChapters(storyId: String, chapters: List<StoryChapterEntity>) {
        if (chapters.isEmpty()) return
        cache.put(storyId, chapters)
        recomputeMetrics()
    }

    fun invalidate(storyId: String) {
        cache.remove(storyId)
        recomputeMetrics()
    }

    fun clearAll() {
        cache.evictAll()
        recomputeMetrics()
    }

    private fun recomputeMetrics() {
        val snapshot = cache.snapshot()
        val chapterCount = snapshot.values.sumOf { it.size }
        val totalBytes = snapshot.values.flatten().sumOf { (it.title.length + it.content.length) * 2L }
        _telemetry.value = _telemetry.value.copy(
            cachedChapters = chapterCount,
            estimatedBytes = totalBytes
        )
    }
}
