package com.android.purebilibili.feature.home.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ReusableLiquidGlassBackdropStructureTest {

    @Test
    fun `audio library records pager content outside its segmented control`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/audio/screen/ListenVideoScreen.kt"
        )

        assertTrue(source.contains("val selectionBackdrop = rememberLayerBackdrop()"))
        assertTrue(source.contains("backdrop = selectionBackdrop"))
        assertTrue(source.contains(".layerBackdrop(selectionBackdrop)"))
    }

    @Test
    fun `bangumi tabs sample pager content from a sibling layer`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/bangumi/ui/player/BangumiPlayerContent.kt"
        )

        assertTrue(source.contains("val selectionBackdrop = rememberLayerBackdrop()"))
        assertTrue(source.contains("backdrop = selectionBackdrop"))
        assertTrue(source.contains(".layerBackdrop(selectionBackdrop)"))
    }

    @Test
    fun `live area and player tabs receive sibling content backdrops`() {
        val areaSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/live/LiveAreaScreen.kt"
        )
        val playerSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/live/LivePlayerScreen.kt"
        )

        assertTrue(areaSource.contains("backdrop = selectionBackdrop"))
        assertTrue(areaSource.contains(".layerBackdrop(selectionBackdrop)"))
        assertTrue(playerSource.contains("backdrop = selectionBackdrop"))
        assertTrue(playerSource.contains(".layerBackdrop(selectionBackdrop)"))
    }

    @Test
    fun `music page switcher samples the pager without recording itself`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/audio/screen/MusicPlayerContent.kt"
        )

        assertTrue(source.contains("val selectionBackdrop = rememberLayerBackdrop()"))
        assertTrue(source.contains("backdrop = selectionBackdrop"))
        assertTrue(source.contains(".layerBackdrop(selectionBackdrop)"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(File(path), File(normalizedPath)).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
