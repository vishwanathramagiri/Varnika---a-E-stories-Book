package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.ReadingSessionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@DrawableRes
fun resolveCoverPresetDrawable(preset: String): Int = when (preset) {
    "forest" -> R.drawable.img_cover_forest
    "cosmos" -> R.drawable.img_cover_cosmos
    "coastal" -> R.drawable.img_cover_coastal
    else -> R.drawable.img_hero_varnika
}

@Composable
fun StoryCoverImage(
    coverPreset: String,
    coverUri: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    if (!coverUri.isNullOrBlank()) {
        AsyncImage(
            model = coverUri,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            error = painterResource(id = resolveCoverPresetDrawable(coverPreset))
        )
    } else {
        Image(
            painter = painterResource(id = resolveCoverPresetDrawable(coverPreset)),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

data class DailyBarPoint(val dayLabel: String, val minutes: Int)

fun buildWeeklyChartPoints(sessions: List<ReadingSessionEntity>): List<DailyBarPoint> {
    val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val labelFmt = SimpleDateFormat("EEE", Locale.US)
    val byDay = sessions.groupBy { it.dateKey }.mapValues { (_, list) -> list.sumOf { it.minutesRead } }

    return (6 downTo 0).map { daysAgo ->
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
        val key = keyFmt.format(cal.time)
        val label = labelFmt.format(cal.time)
        DailyBarPoint(dayLabel = label, minutes = byDay[key] ?: 0)
    }
}

@Composable
fun WeeklyProgressChart(
    points: List<DailyBarPoint>,
    goalMinutes: Int,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val maxMinutes = (points.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(goalMinutes).coerceAtLeast(20)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .padding(top = 8.dp)
        ) {
            val count = points.size.coerceAtLeast(1)
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.48f).coerceAtMost(44f)

            // Daily goal dashed line
            val goalRatio = (goalMinutes.toFloat() / maxMinutes.toFloat()).coerceIn(0f, 1f)
            val goalY = size.height * (1f - goalRatio)
            drawLine(
                color = secondary,
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )

            points.forEachIndexed { idx, pt ->
                val centerX = idx * slotWidth + slotWidth / 2f
                val left = centerX - barWidth / 2f

                // Background rail
                drawRoundRect(
                    color = track,
                    topLeft = Offset(left, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(10f, 10f)
                )

                val ratio = (pt.minutes.toFloat() / maxMinutes.toFloat()).coerceIn(0f, 1f)
                if (ratio > 0f) {
                    val barHeight = (size.height * ratio).coerceAtLeast(10f)
                    drawRoundRect(
                        color = if (pt.minutes >= goalMinutes) primary else secondary,
                        topLeft = Offset(left, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { pt ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = pt.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${pt.minutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

data class TimeBucketPoint(val label: String, val minutes: Int)

fun buildDailyPatternBuckets(sessions: List<ReadingSessionEntity>): List<TimeBucketPoint> {
    var morning = 0
    var afternoon = 0
    var evening = 0
    var night = 0
    sessions.forEach { s ->
        when (s.hourOfDay) {
            in 5..11 -> morning += s.minutesRead
            in 12..16 -> afternoon += s.minutesRead
            in 17..20 -> evening += s.minutesRead
            else -> night += s.minutesRead
        }
    }
    return listOf(
        TimeBucketPoint("Morning\n5-11", morning),
        TimeBucketPoint("Afternoon\n12-16", afternoon),
        TimeBucketPoint("Evening\n17-20", evening),
        TimeBucketPoint("Night\n21-4", night)
    )
}

@Composable
fun DailyPatternChart(
    buckets: List<TimeBucketPoint>,
    modifier: Modifier = Modifier
) {
    val tertiary = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val maxVal = (buckets.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(10)

    Column(modifier = modifier.fillMaxWidth()) {
        buckets.forEach { bucket ->
            val ratio = (bucket.minutes.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bucket.label.replace("\n", " • "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.42f)
                )
                Canvas(
                    modifier = Modifier
                        .weight(0.44f)
                        .height(14.dp)
                ) {
                    drawRoundRect(
                        color = track,
                        topLeft = Offset.Zero,
                        size = size,
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                    if (ratio > 0f) {
                        drawRoundRect(
                            color = tertiary,
                            topLeft = Offset.Zero,
                            size = Size(size.width * ratio, size.height),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                    }
                }
                Text(
                    text = "${bucket.minutes}m",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(0.14f)
                )
            }
        }
    }
}
