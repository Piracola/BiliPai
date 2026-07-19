package com.android.purebilibili.feature.partition

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.util.resolveReplaceRefreshPage
import com.android.purebilibili.data.model.response.BangumiType
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        assertTrue(source.contains("KernelSuBottomBarIndicatorLayer("))
        assertTrue(source.contains("liquidGlassIndicatorEnabled = liquidGlassIndicatorEnabled"))
        assertFalse(source.contains("partitionSideRailSweepSelection("))
        assertTrue(source.contains("CardPositionManager.recordVideoCardPosition("))
        assertTrue(source.contains("videoCardShellSharedBoundsOrEmpty("))
        assertTrue(source.contains("shouldUseVideoCardShellSharedBounds("))
        assertFalse(source.contains("videoTitleSharedElementKey("))
        assertTrue(source.contains("sourceRoute = sharedElementSourceRoute"))
        assertTrue(source.contains("LocalVideoCardSharedElementSourceRoute.current"))
        assertTrue(source.contains("VideoRepository.getPopularVideos(page = currentPage)"))
        assertTrue(source.contains("VideoRepository.getRegionVideos(tid = partition.id, page = currentPage)"))
        assertTrue(source.contains("resolvePartitionBangumiType(partition.id)"))
        assertTrue(source.contains("onBangumiClick(bangumiType)"))
        assertFalse(source.contains("LazyVerticalGrid("))
        assertTrue(source.contains("AdaptivePullToRefreshBox("))
        assertTrue(source.contains("onRefresh = viewModel::refresh"))
        assertTrue(source.contains("fun refresh()"))
        assertTrue(source.contains("resolveReplaceRefreshPage("))
    }

    @Test
    fun `partition refresh advances page to surface other videos`() {
        assertEquals(2, resolveReplaceRefreshPage(nextLoadPage = 2, hasMore = true))
        assertEquals(1, resolveReplaceRefreshPage(nextLoadPage = 5, hasMore = false))
        assertEquals(1, resolveReplaceRefreshPage(nextLoadPage = 0, hasMore = true))
    }

    @Test
    fun `side rail leaves vertical drag to lazy list scrolling`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/partition/PartitionScreen.kt")

        assertFalse(source.contains("pointerInput(partitions)"))
        assertFalse(source.contains("PointerEventPass.Initial"))
        assertTrue(source.contains("awaitLongPressOrCancellation("))
        assertTrue(source.contains("verticalDrag("))
        assertTrue(source.contains("shouldStartPartitionSideRailIndicatorDrag("))
    }

    @Test
    fun `side rail drag starts only from current indicator bounds`() {
        assertTrue(
            shouldStartPartitionSideRailIndicatorDrag(
                pointerY = 64f,
                indicatorTopPx = 60f,
                indicatorHeightPx = 48f
            )
        )
        assertFalse(
            shouldStartPartitionSideRailIndicatorDrag(
                pointerY = 40f,
                indicatorTopPx = 60f,
                indicatorHeightPx = 48f
            )
        )
        assertFalse(
            shouldStartPartitionSideRailIndicatorDrag(
                pointerY = 64f,
                indicatorTopPx = 60f,
                indicatorHeightPx = 0f
            )
        )
    }

    @Test
    fun `side rail indicator offset tracks lazy list scroll`() {
        assertTrue(
            resolvePartitionSideRailIndicatorOffsetPx(
                indicatorPosition = 10f,
                firstVisibleItemIndex = 8,
                firstVisibleItemScrollOffsetPx = 12,
                contentTopPaddingPx = 16f,
                itemSlotHeightPx = 52f
            ) == 108f
        )
    }

    @Test
    fun `side rail indicator uses the same horizontal padding as items`() {
        val ltrPadding = resolvePartitionSideRailIndicatorHorizontalPadding(
            contentPadding = PaddingValues(start = 16.dp, end = 4.dp),
            layoutDirection = LayoutDirection.Ltr
        )
        val rtlPadding = resolvePartitionSideRailIndicatorHorizontalPadding(
            contentPadding = PaddingValues(start = 16.dp, end = 4.dp),
            layoutDirection = LayoutDirection.Rtl
        )

        assertTrue(ltrPadding.start == 16.dp)
        assertTrue(ltrPadding.end == 4.dp)
        assertTrue(rtlPadding.start == 16.dp)
        assertTrue(rtlPadding.end == 4.dp)
    }

    @Test
    fun `side rail indicator uses layout offset without extra vertical drift`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/partition/PartitionScreen.kt")

        assertTrue(source.contains("indicatorOffsetPxProvider: () -> Float"))
        assertTrue(source.contains("indicatorTranslationYPx = indicatorOffsetPxProvider()"))
        assertTrue(source.contains("swapMotionAxes = true"))
        assertTrue(source.contains("indicatorAlignment = Alignment.TopStart"))
        assertFalse(source.contains("centerLayerOnIndicatorY"))
        assertFalse(source.contains("translationY = panelOffsetPx"))
        assertFalse(source.contains("val panelOffsetPx"))
    }

    @Test
    fun `side rail item color follows moving indicator position`() {
        assertTrue(resolvePartitionSideRailItemSelectionProgress(itemIndex = 3, indicatorPosition = 3f) == 1f)
        assertTrue(resolvePartitionSideRailItemSelectionProgress(itemIndex = 3, indicatorPosition = 3.5f) == 0.5f)
        assertTrue(resolvePartitionSideRailItemSelectionProgress(itemIndex = 3, indicatorPosition = 4.2f) == 0f)
    }

    @Test
    fun `pgc partitions map to bangumi page types`() {
        assertEquals(BangumiType.ANIME.value, resolvePartitionBangumiType(13))
        assertEquals(BangumiType.GUOCHUANG.value, resolvePartitionBangumiType(167))
        assertEquals(BangumiType.MOVIE.value, resolvePartitionBangumiType(23))
        assertEquals(BangumiType.TV_SHOW.value, resolvePartitionBangumiType(11))
        assertEquals(BangumiType.DOCUMENTARY.value, resolvePartitionBangumiType(177))
        assertNull(resolvePartitionBangumiType(3))
    }

    @Test
    fun `side rail label mode follows top tab display modes`() {
        assertTrue(shouldShowPartitionSideRailIcon(labelMode = 0))
        assertTrue(shouldShowPartitionSideRailText(labelMode = 0))
        assertTrue(shouldShowPartitionSideRailIcon(labelMode = 1))
        assertFalse(shouldShowPartitionSideRailText(labelMode = 1))
        assertFalse(shouldShowPartitionSideRailIcon(labelMode = 2))
        assertTrue(shouldShowPartitionSideRailText(labelMode = 2))
    }

    @Test
    fun `side rail item content is centered without manual left spacer`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/partition/PartitionScreen.kt")
        val itemSource = source
            .substringAfter("private fun PartitionSideRailItem(")
            .substringBefore("internal fun shouldStartPartitionSideRailIndicatorDrag(")

        assertTrue(itemSource.contains("horizontalAlignment = Alignment.CenterHorizontally"))
        assertTrue(itemSource.contains("textAlign = TextAlign.Center"))
        assertFalse(itemSource.contains("Spacer(modifier = Modifier.width(14.dp))"))
    }

    @Test
    fun `partition video card obeys global shared transition switch`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/partition/PartitionScreen.kt")
        val rowSource = source.substringAfter("private fun PartitionVideoRow(")

        assertTrue(rowSource.contains("val sharedTransitionEnabled = LocalSharedTransitionEnabled.current"))
        assertTrue(rowSource.contains("transitionEnabled = sharedTransitionEnabled"))
        assertFalse(rowSource.contains("spring(dampingRatio = 0.8f, stiffness = 200f)"))
        assertFalse(rowSource.contains("transitionEnabled = true"))
    }

    @Test
    fun `video list push follows long press drag then can return to rest`() {
        assertTrue(
            resolvePartitionVideoListPushPx(
                pressProgress = 1f,
                dragOffsetPx = 0f,
                itemSlotHeightPx = 52f,
                maxPushPx = 20f
            ) == 20f
        )
        assertTrue(
            resolvePartitionVideoListPushPx(
                pressProgress = 0f,
                dragOffsetPx = 52f,
                itemSlotHeightPx = 52f,
                maxPushPx = 20f
            ) > 16f
        )
        assertTrue(
            resolvePartitionVideoListPushPx(
                pressProgress = 0f,
                dragOffsetPx = 0f,
                itemSlotHeightPx = 52f,
                maxPushPx = 20f
            ) == 0f
        )
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
