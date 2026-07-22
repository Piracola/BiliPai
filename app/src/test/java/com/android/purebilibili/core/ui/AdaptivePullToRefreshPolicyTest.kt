package com.android.purebilibili.core.ui

import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AdaptivePullToRefreshPolicyTest {

    @Test
    fun `miuix variant routes to miuix bridged renderer`() {
        assertEquals(
            PresetPrimitiveRenderer.MIUIX_BRIDGED,
            resolveAdaptivePullToRefreshRenderer(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
    }

    @Test
    fun `material md3 variant keeps material renderer`() {
        assertEquals(
            PresetPrimitiveRenderer.MATERIAL3,
            resolveAdaptivePullToRefreshRenderer(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3
            )
        )
    }

    @Test
    fun `ios preset keeps ios renderer`() {
        assertEquals(
            PresetPrimitiveRenderer.IOS,
            resolveAdaptivePullToRefreshRenderer(
                uiPreset = UiPreset.IOS,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
    }

    @Test
    fun `miuix refresh texts use localized home hints`() {
        assertEquals(
            listOf("下拉刷新...", "松手刷新", "正在刷新...", "刷新完成"),
            resolveMiuixPullToRefreshTexts()
        )
    }

    @Test
    fun `pull refresh indicator top inset clamps overlay chrome height`() {
        assertEquals(0f, resolvePullRefreshIndicatorTopInsetDp(-8f), 0.001f)
        assertEquals(0f, resolvePullRefreshIndicatorTopInsetDp(0f), 0.001f)
        assertEquals(96f, resolvePullRefreshIndicatorTopInsetDp(96f), 0.001f)
        assertEquals(0f, resolveScaffoldedPullRefreshIndicatorTopInsetDp(), 0.001f)
    }

    @Test
    fun `adaptive pull to refresh box applies indicator top inset for miuix and default indicator`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/core/ui/AdaptivePullToRefreshBox.kt")
        assertTrue(source.contains("indicatorTopInset"))
        assertTrue(source.contains("mergedContentPadding"))
        assertTrue(source.contains("padding(top = indicatorTopInset)"))
        assertTrue(source.contains("AdaptivePullToRefreshDefaultIndicator("))
        assertTrue(source.contains("PullToRefreshDefaults.LoadingIndicator("))
        assertTrue(source.contains("PresetPrimitiveRenderer.MATERIAL3"))
    }

    @Test
    fun `home material default pull refresh uses official loading indicator`() {
        val home = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        assertTrue(home.contains("PullToRefreshDefaults.LoadingIndicator("))
        assertTrue(home.contains("HomePullRefreshIndicatorStyle.MATERIAL_DEFAULT"))
    }

    @Test
    fun `overlay screens pass non zero indicator top inset`() {
        val home = loadSource("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        val dynamic = loadSource("app/src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt")
        val partition = loadSource("app/src/main/java/com/android/purebilibili/feature/partition/PartitionScreen.kt")

        assertTrue(home.contains("indicatorTopInset = homeRefreshIndicatorTopInset"))
        assertTrue(dynamic.contains("indicatorTopInset = dynamicRefreshIndicatorTopInset"))
        assertTrue(partition.contains("indicatorTopInset = partitionRefreshIndicatorTopInset"))
    }

    @Test
    fun `scaffolded screens pin indicator top inset at zero`() {
        val screens = listOf(
            "app/src/main/java/com/android/purebilibili/feature/message/InboxScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/message/feed/LikeMeScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/message/feed/AtMeScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/message/feed/ReplyMeScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/message/feed/SystemNoticeScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/category/CategoryScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/search/SearchTrendingScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/following/FollowingListScreen.kt"
        )
        screens.forEach { path ->
            val source = loadSource(path)
            assertTrue(
                "$path must set indicatorTopInset = 0.dp for scaffolded/list-region boxes",
                source.contains("indicatorTopInset = 0.dp")
            )
        }
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
