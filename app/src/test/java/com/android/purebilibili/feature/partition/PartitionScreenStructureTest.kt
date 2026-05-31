package com.android.purebilibili.feature.partition

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PartitionScreenStructureTest {

    @Test
    fun `partition page uses side rail and feed list layout`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/partition/PartitionScreen.kt")

        assertTrue(source.contains("PartitionSideRail("))
        assertTrue(source.contains("PartitionVideoList("))
        assertTrue(source.contains("PartitionVideoRow("))
        assertTrue(source.contains("SettingsManager.getHomeSettings(context)"))
        assertTrue(source.contains("resolveEffectiveLiquidGlassEnabled("))
        assertTrue(source.contains("partitionLiquidGlassIndicator("))
        assertTrue(source.contains("liquidGlassIndicatorEnabled = liquidGlassIndicatorEnabled"))
        assertTrue(source.contains("VideoRepository.getPopularVideos(page = currentPage)"))
        assertTrue(source.contains("VideoRepository.getRegionVideos(tid = partition.id, page = currentPage)"))
        assertFalse(source.contains("LazyVerticalGrid("))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
