package com.example

import com.example.data.local.ReadingSessionEntity
import com.example.ui.components.buildDailyPatternBuckets
import com.example.ui.components.buildWeeklyChartPoints
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExampleUnitTest {

    @Test
    fun weeklyChartPoints_aggregatesTodayMinutes() {
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val sessions = listOf(
            ReadingSessionEntity("s1", "story_1", todayKey, 9, 12, 3, 400),
            ReadingSessionEntity("s2", "story_1", todayKey, 21, 8, 2, 250)
        )
        val points = buildWeeklyChartPoints(sessions)
        assertEquals(7, points.size)
        assertEquals(20, points.last().minutes)
    }

    @Test
    fun dailyPatternBuckets_groupsHoursIntoDayParts() {
        val sessions = listOf(
            ReadingSessionEntity("s1", "story_1", "2026-11-18", 8, 10, 2, 200),
            ReadingSessionEntity("s2", "story_1", "2026-11-18", 14, 15, 3, 300),
            ReadingSessionEntity("s3", "story_1", "2026-11-18", 19, 20, 4, 450),
            ReadingSessionEntity("s4", "story_1", "2026-11-18", 23, 5, 1, 100)
        )
        val buckets = buildDailyPatternBuckets(sessions)
        assertEquals(4, buckets.size)
        assertEquals(10, buckets[0].minutes)
        assertEquals(15, buckets[1].minutes)
        assertEquals(20, buckets[2].minutes)
        assertEquals(5, buckets[3].minutes)
    }
}
