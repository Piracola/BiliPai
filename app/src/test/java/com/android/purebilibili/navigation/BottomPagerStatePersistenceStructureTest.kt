package com.android.purebilibili.navigation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomPagerStatePersistenceStructureTest {

    @Test
    fun `bottom tabs are hosted by main horizontal pager state`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")

        assertTrue(source.contains("BiliPaiNavDisplayHost("))
        assertTrue(source.contains("rememberPagerState("))
        assertTrue(source.contains("rememberMainBottomPagerState("))
        assertTrue(source.contains("HorizontalPager("))
        assertTrue(source.contains("rememberSaveableStateHolder()"))
        assertTrue(source.contains("SaveableStateProvider(resolveBottomPagerSaveableStateKey(item))"))
        assertTrue(source.contains("var historyHasActivated by rememberSaveable"))
        assertTrue(source.contains("userScrollEnabled = shouldEnableBottomPagerUserScroll()"))
        assertTrue(source.contains("resolveBottomPagerRenderBudget(isNavigating = mainBottomPagerState.isNavigating)"))
        assertFalse(source.contains("pendingBottomTabTransitionRoute"))
        assertFalse(source.contains("retainedBottomNavItem"))
        assertFalse(source.contains("resolveBottomTabTransitionTargetRoute"))
        assertFalse(source.contains("VerticalPager("))
    }

    @Test
    fun `main bottom pager avoids pager pre jump animation`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/navigation/MainBottomPagerState.kt")
        val animatedNavigationSource = source
            .substringAfter("fun animateToPage(")
            .substringBefore("fun syncPage(")

        assertTrue(source.contains("navigationStartPage"))
        assertTrue(source.contains("pagerState.animateScrollBy("))
        assertFalse(source.contains("pagerState.animateScrollToPage("))
        assertFalse(source.contains("shouldUseDirectBottomPagerJump("))
        assertFalse(animatedNavigationSource.contains("pagerState.scrollToPage("))
    }

    @Test
    fun `main bottom pager defers silent snap until next frame after idle`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/navigation/MainBottomPagerState.kt")
        val snapNavigationSource = source
            .substringAfter("fun snapToPage(")
            .substringBefore("private suspend fun")

        assertCallsInOrder(
            snapNavigationSource,
            "awaitScrollIdle()",
            "awaitNextFrame()",
            "pagerState.scrollToPage(targetIndex)"
        )
    }

    private fun assertCallsInOrder(source: String, vararg calls: String) {
        var previousIndex = -1
        calls.forEach { call ->
            val currentIndex = source.indexOf(call)
            assertTrue(currentIndex > previousIndex, "$call should appear after previous call")
            previousIndex = currentIndex
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
