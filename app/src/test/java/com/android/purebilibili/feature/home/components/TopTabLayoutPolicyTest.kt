package com.android.purebilibili.feature.home.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopTabLayoutPolicyTest {

    @Test
    fun `visible slot count should stay in compact range`() {
        assertEquals(1, resolveTopTabVisibleSlots(1))
        assertEquals(3, resolveTopTabVisibleSlots(3))
        assertEquals(4, resolveTopTabVisibleSlots(4))
        assertEquals(5, resolveTopTabVisibleSlots(5, longestLabelLength = 6))
        assertEquals(6, resolveTopTabVisibleSlots(6, longestLabelLength = 2))
        assertEquals(4, resolveTopTabVisibleSlots(5, longestLabelLength = 9))
        assertEquals(4, resolveTopTabVisibleSlots(8, longestLabelLength = 10))
    }

    @Test
    fun `floating style should enforce wider min width to avoid clipping`() {
        assertEquals(72f, resolveTopTabItemWidthDp(260f, 5, isFloatingStyle = true), 0.001f)
    }

    @Test
    fun `docked style should keep a denser minimum width`() {
        assertEquals(64f, resolveTopTabItemWidthDp(260f, 5, isFloatingStyle = false), 0.001f)
    }

    @Test
    fun `wide containers should use proportional width`() {
        assertEquals(100f, resolveTopTabItemWidthDp(500f, 5, isFloatingStyle = true), 0.001f)
    }

    @Test
    fun `ios top tab action shares centered slot with visible categories`() {
        assertEquals(1, resolveTopTabVisibleCategorySlots(1, longestLabelLength = 2))
        assertEquals(3, resolveTopTabVisibleCategorySlots(3, longestLabelLength = 2))
        assertEquals(5, resolveTopTabVisibleCategorySlots(5, longestLabelLength = 6))
        assertEquals(150f, resolveTopTabActionSlotWidthDp(600f, 3, longestLabelLength = 2), 0.001f)
        assertEquals(100f, resolveTopTabActionSlotWidthDp(600f, 5, longestLabelLength = 6), 0.001f)
        assertEquals(100f, resolveTopTabItemWidthDp(500f, 5, isFloatingStyle = false), 0.001f)
    }

    @Test
    fun `md3 top tabs use compact scrollable item widths instead of fixed four slots`() {
        assertEquals(3, resolveMd3TopTabVisibleSlots())
        assertEquals(106.666f, resolveMd3TopTabItemWidthDp(containerWidthDp = 320f), 0.001f)
        assertEquals(120f, resolveMd3TopTabItemWidthDp(containerWidthDp = 360f), 0.001f)
        assertEquals(213.333f, resolveMd3TopTabItemWidthDp(containerWidthDp = 640f), 0.001f)
        assertEquals(60f, resolveMd3TopTabItemWidthDp(containerWidthDp = 360f, visibleSlots = 6), 0.001f)
    }

    @Test
    fun `md3 top tabs center sparse categories in three slot viewport`() {
        val itemWidth = resolveMd3TopTabItemWidthDp(containerWidthDp = 360f)

        assertEquals(60f, resolveMd3TopTabContentPaddingDp(360f, itemWidth, categoryCount = 2), 0.001f)
        assertEquals(120f, resolveMd3TopTabContentPaddingDp(360f, itemWidth, categoryCount = 1), 0.001f)
        assertEquals(0f, resolveMd3TopTabContentPaddingDp(360f, itemWidth, categoryCount = 3), 0.001f)
    }

    @Test
    fun `md3 and miuix multi-tab rows keep leading edge for all label modes`() {
        val itemWidth = resolveMd3TopTabItemWidthDp(containerWidthDp = 400f, visibleSlots = 5)

        assertEquals(72f, itemWidth, 0.001f)
        // Text-only and icon+text: lead-align (leftover must not center-push "推荐")
        assertEquals(
            0f,
            resolveMd3TopTabContentPaddingDp(
                containerWidthDp = 400f,
                itemWidthDp = itemWidth,
                categoryCount = 5,
                labelMode = 2
            ),
            0.001f
        )
        assertEquals(
            0f,
            resolveMd3TopTabContentPaddingDp(
                containerWidthDp = 400f,
                itemWidthDp = itemWidth,
                categoryCount = 5,
                labelMode = 0
            ),
            0.001f
        )
        // Sparse 1–2 tabs still center residual so a lone tab is not glued left
        // 2 × 120 on 360 → leftover 120 → padding 60
        assertEquals(
            60f,
            resolveMd3TopTabContentPaddingDp(
                containerWidthDp = 360f,
                itemWidthDp = 120f,
                categoryCount = 2,
                labelMode = 0
            ),
            0.001f
        )
    }

    @Test
    fun `md3 indicator translation at index zero sits at leading edge when padding is zero`() {
        // position 0, no content padding: indicator left = (item - indicator) / 2 (slot center)
        assertEquals(
            2f,
            resolveMd3TopTabIndicatorTranslationPx(
                absolutePagerPosition = 0f,
                itemWidthPx = 72f,
                rowScrollOffsetPx = 0f,
                indicatorWidthPx = 68f, // item - 2*2dp gap
                contentPaddingPx = 0f
            ),
            0.001f
        )
        // With leftover centered (old bug): first indicator jumps right by padding
        assertEquals(
            22f,
            resolveMd3TopTabIndicatorTranslationPx(
                absolutePagerPosition = 0f,
                itemWidthPx = 72f,
                rowScrollOffsetPx = 0f,
                indicatorWidthPx = 68f,
                contentPaddingPx = 20f
            ),
            0.001f
        )
    }

    @Test
    fun `md3 top tabs show all tabs for every label mode when partition is an inline page`() {
        assertEquals(
            6,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 6,
                labelMode = 2,
                showPartitionAction = false
            )
        )
        assertEquals(
            5,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 5,
                labelMode = 2,
                showPartitionAction = false
            )
        )
        assertEquals(
            4,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 4,
                labelMode = 2,
                showPartitionAction = false
            )
        )
        assertEquals(
            6,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 6,
                labelMode = 0,
                showPartitionAction = false
            )
        )
        assertEquals(
            6,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 6,
                labelMode = 1,
                showPartitionAction = false
            )
        )
    }

    @Test
    fun `md3 top tabs fit five inline tabs including partition within phone width`() {
        assertEquals(
            5,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 5,
                labelMode = 2,
                showPartitionAction = false
            )
        )
        listOf(0, 1, 2).forEach { labelMode ->
            val itemWidth = resolveMd3TopTabItemWidthDp(
                containerWidthDp = 360f,
                visibleSlots = resolveMd3TopTabLayoutVisibleSlots(
                    categoryCount = 5,
                    labelMode = labelMode,
                    showPartitionAction = false
                )
            )
            assertTrue(
                "five tabs must fit within 360dp for labelMode=$labelMode, got ${itemWidth * 5f}",
                itemWidth * 5f <= 360f + 0.001f
            )
        }
    }

    @Test
    fun `ios top tabs show all six tabs for every label mode`() {
        listOf(0, 1, 2).forEach { labelMode ->
            assertEquals(
                6,
                resolveIosTopTabLayoutVisibleSlots(
                    categoryCount = 6,
                    labelMode = labelMode
                )
            )
            val itemWidth = resolveIosTopTabItemWidthDp(
                containerWidthDp = 360f,
                categoryCount = 6,
                labelMode = labelMode
            )
            assertEquals(360f, itemWidth * 6 + 4f, 0.001f)
        }
    }

    @Test
    fun `ios top tabs fit five inline tabs within phone width`() {
        listOf(0, 1, 2).forEach { labelMode ->
            assertEquals(
                5,
                resolveIosTopTabLayoutVisibleSlots(
                    categoryCount = 5,
                    labelMode = labelMode
                )
            )
            val itemWidth = resolveIosTopTabItemWidthDp(
                containerWidthDp = 360f,
                categoryCount = 5,
                labelMode = labelMode
            )
            assertTrue(
                "five ios tabs must fit within 360dp for labelMode=$labelMode, got ${itemWidth * 5f}",
                itemWidth * 5f + 4f <= 360f + 0.001f
            )
        }
    }

    @Test
    fun `md3 top tabs cap expanded custom tabs at six visible slots`() {
        assertEquals(
            6,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 8,
                labelMode = 0,
                showPartitionAction = false
            )
        )
        assertEquals(
            6,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 8,
                labelMode = 2,
                showPartitionAction = false
            )
        )
    }

    @Test
    fun `md3 top tabs keep compact scrollable slots for external partition action`() {
        assertEquals(
            3,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 6,
                labelMode = 2,
                showPartitionAction = true
            )
        )
    }

    @Test
    fun `floating dock should wrap width to tab content instead of full bleed`() {
        assertTrue(
            shouldWrapTopTabDockWidth(
                isFloatingStyle = true,
                hasOuterChromeSurface = true,
                edgeToEdge = false
            )
        )
        assertTrue(
            shouldWrapTopTabDockWidth(
                isFloatingStyle = true,
                hasOuterChromeSurface = false,
                edgeToEdge = false
            )
        )
        assertFalse(
            shouldWrapTopTabDockWidth(
                isFloatingStyle = false,
                hasOuterChromeSurface = false,
                edgeToEdge = false
            )
        )
        assertFalse(
            shouldWrapTopTabDockWidth(
                isFloatingStyle = true,
                hasOuterChromeSurface = true,
                edgeToEdge = true
            )
        )
    }

    @Test
    fun `wrap dock width follows preferred item width times tab count`() {
        // Icon + text floating: 74 × 5 = 370, fits in 400 → wrap to 370
        assertEquals(74f, resolveTopTabWrapItemWidthDp(labelMode = 0, isFloatingStyle = true), 0.001f)
        assertEquals(
            370f,
            resolveTopTabDockWrapWidthDp(
                itemWidthDp = 74f,
                categoryCount = 5,
                maxWidthDp = 400f
            ),
            0.001f
        )
        // Icon only: 56 × 6 = 336
        assertEquals(56f, resolveTopTabWrapItemWidthDp(labelMode = 1, isFloatingStyle = true), 0.001f)
        assertEquals(
            336f,
            resolveTopTabDockWrapWidthDp(
                itemWidthDp = 56f,
                categoryCount = 6,
                maxWidthDp = 400f
            ),
            0.001f
        )
        // Text only: 66 × 5 = 330
        assertEquals(66f, resolveTopTabWrapItemWidthDp(labelMode = 2, isFloatingStyle = true), 0.001f)
        assertEquals(
            330f,
            resolveTopTabDockWrapWidthDp(
                itemWidthDp = 66f,
                categoryCount = 5,
                maxWidthDp = 400f
            ),
            0.001f
        )
        // Overflow clamps to max so small phones still fill
        assertEquals(
            300f,
            resolveTopTabDockWrapWidthDp(
                itemWidthDp = 74f,
                categoryCount = 5,
                maxWidthDp = 300f
            ),
            0.001f
        )
    }

    @Test
    fun `wrap dock item width uses preferred when pack fits otherwise falls back`() {
        val preferred = resolveTopTabWrapItemWidthDp(labelMode = 0, isFloatingStyle = true)
        assertEquals(
            preferred,
            resolveTopTabDockItemWidthDp(
                maxWidthDp = 400f,
                categoryCount = 5,
                labelMode = 0,
                isFloatingStyle = true,
                wrapContent = true,
                fillItemWidthDp = 72f
            ),
            0.001f
        )
        // Too narrow for preferred pack → use fill width
        assertEquals(
            60f,
            resolveTopTabDockItemWidthDp(
                maxWidthDp = 300f,
                categoryCount = 5,
                labelMode = 0,
                isFloatingStyle = true,
                wrapContent = true,
                fillItemWidthDp = 60f
            ),
            0.001f
        )
        // wrapContent off always uses fill
        assertEquals(
            72f,
            resolveTopTabDockItemWidthDp(
                maxWidthDp = 400f,
                categoryCount = 5,
                labelMode = 0,
                isFloatingStyle = true,
                wrapContent = false,
                fillItemWidthDp = 72f
            ),
            0.001f
        )
    }

    @Test
    fun `ios top tabs reserve enough height for icon label modes`() {
        assertEquals(56f, resolveIosTopTabRowHeight(isFloatingStyle = true, labelMode = 2).value, 0.001f)
        assertEquals(56f, resolveIosTopTabRowHeight(isFloatingStyle = true, labelMode = 1).value, 0.001f)
        assertEquals(62f, resolveIosTopTabRowHeight(isFloatingStyle = true, labelMode = 0).value, 0.001f)
        assertEquals(58f, resolveIosTopTabRowHeight(isFloatingStyle = false, labelMode = 0).value, 0.001f)
    }

    @Test
    fun `top tab item content policy avoids clipping icon plus text`() {
        assertEquals(42f, resolveTopTabContentMinHeightDp(labelMode = 0), 0.001f)
        assertEquals(42f, resolveTopTabContentMinHeightDp(labelMode = 1), 0.001f)
        assertEquals(42f, resolveTopTabContentMinHeightDp(labelMode = 2), 0.001f)
        assertEquals(2f, resolveTopTabContentVerticalPaddingDp(labelMode = 0), 0.001f)
        assertEquals(4f, resolveTopTabContentVerticalPaddingDp(labelMode = 1), 0.001f)
        assertEquals(4f, resolveTopTabContentVerticalPaddingDp(labelMode = 2), 0.001f)
    }

    @Test
    fun `md3 top tabs keep every category in scroll order`() {
        assertEquals(
            listOf(0, 1, 2, 3, 4),
            resolveMd3VisibleTabIndices(totalCount = 5, selectedIndex = 0)
        )
        assertEquals(
            listOf(0, 1, 2, 3, 4),
            resolveMd3VisibleTabIndices(totalCount = 5, selectedIndex = 4)
        )
        assertEquals(
            4,
            resolveMd3SelectedVisibleIndex(
                visibleIndices = listOf(0, 1, 2, 3, 4),
                selectedIndex = 4
            )
        )
    }
}

