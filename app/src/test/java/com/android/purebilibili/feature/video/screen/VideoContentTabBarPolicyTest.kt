package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.core.theme.UiPreset
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoContentTabBarPolicyTest {

    @Test
    fun `tab bar layout reserves trailing danmaku action area`() {
        val spec = resolveVideoContentTabBarLayoutSpec(widthDp = 412)

        assertEquals(1f, spec.tabsRowWeight)
        assertTrue(spec.tabsRowScrollable)
        assertEquals(12, spec.containerHorizontalPaddingDp)
        assertEquals(12, spec.tabHorizontalPaddingDp)
        assertEquals(40, spec.segmentedControlHeightDp)
        assertEquals(27, spec.segmentedControlIndicatorHeightDp)
        assertTrue(
            hasVideoContentTabBarIndicatorScaleClearance(
                containerHeightDp = spec.segmentedControlHeightDp,
                indicatorHeightDp = spec.segmentedControlIndicatorHeightDp
            )
        )
    }

    @Test
    fun `danmaku input stays visible when player is expanded`() {
        assertTrue(
            shouldShowDanmakuSendInput(
                isPlayerCollapsed = false
            )
        )
    }

    @Test
    fun `danmaku input hidden when player is collapsed`() {
        assertFalse(
            shouldShowDanmakuSendInput(
                isPlayerCollapsed = true
            )
        )
    }

    @Test
    fun `danmaku action layout keeps settings target comfortably tappable`() {
        val policy = resolveVideoContentTabBarDanmakuActionLayoutPolicy(widthDp = 412)

        assertEquals("发弹幕", policy.sendLabel)
        assertEquals(36, policy.secondaryControlHeightDp)
        assertEquals(20, policy.secondaryControlCornerRadiusDp)
        assertEquals(36, policy.settingsButtonSizeDp)
        assertEquals(18, policy.settingsIconSizeDp)
    }

    @Test
    fun `compact phone layout tightens tabs and danmaku actions`() {
        val spec = resolveVideoContentTabBarLayoutSpec(widthDp = 393)
        val policy = resolveVideoContentTabBarDanmakuActionLayoutPolicy(widthDp = 393)

        assertEquals(8, spec.containerHorizontalPaddingDp)
        assertEquals(8, spec.tabHorizontalPaddingDp)
        assertEquals(10, spec.tabSpacingDp)
        assertEquals(16, spec.selectedTabFontSizeSp)
        assertEquals(40, spec.segmentedControlHeightDp)
        assertTrue(
            hasVideoContentTabBarIndicatorScaleClearance(
                containerHeightDp = spec.segmentedControlHeightDp,
                indicatorHeightDp = spec.segmentedControlIndicatorHeightDp
            )
        )
        assertEquals("发弹幕", policy.sendLabel)
        assertEquals(36, policy.secondaryControlHeightDp)
        assertEquals(20, policy.secondaryControlCornerRadiusDp)
        assertEquals(36, policy.settingsButtonSizeDp)
        assertEquals(18, policy.settingsIconSizeDp)
    }

    @Test
    fun `info comment tab bar disables tap press refraction`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/screen/VideoContentSection.kt"
        )
        val tabBarBlock = source
            .substringAfter("fun VideoContentTabBar(")
            .substringBefore("// [新增] 恢复画面按钮")

        assertTrue(tabBarBlock.contains("tapPressRefractionEnabled = false"))
    }

    @Test
    fun `video content section wires chrome backdrop into tab and comment segmented controls`() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/screen/VideoContentSection.kt"
        )

        assertTrue(source.contains("val videoContentChromeBackdrop = rememberLayerBackdrop()"))
        assertTrue(source.contains("chromeBackdrop = videoContentChromeBackdrop"))
        assertTrue(source.contains("backdrop = videoContentChromeBackdrop"))
        assertTrue(source.contains("backdrop = chromeBackdrop"))
        assertTrue(source.contains("Column(modifier = modifier.fillMaxSize())"))
        assertTrue(
            source.contains(
                "采样层只挂在 Tab 页滚动内容上；排序栏/顶栏分段控件必须在捕获区外"
            )
        )
        val commentTabSource = source.substringAfter("private fun VideoCommentTab(")
            .substringBefore("private fun VideoHeaderContent(")
        assertTrue(commentTabSource.contains("CommentSortFilterBar("))
        assertFalse(commentTabSource.contains("item {\n                CommentSortFilterBar("))
        val pagerBlock = source
            .substringAfter("HorizontalPager(")
            .substringBefore(") { page ->")
        assertFalse(
            pagerBlock.contains("layerBackdrop"),
            "Pager must not capture backdrop; segmented controls inside would self-sample and overflow RenderThread stack on MIUI"
        )
        assertTrue(source.contains("forceLiquidChrome = homeSettings.androidNativeLiquidGlassEnabled"))
        assertTrue(source.contains("liquidGlassEffectsEnabled = liquidChromeSpec.liquidGlassEffectsEnabled"))
    }

    @Test
    fun `ios preset uses calmer intro comment tab switch motion`() {
        val iosSpec = resolveVideoContentTabSwitchAnimationSpec(UiPreset.IOS)
        val md3Spec = resolveVideoContentTabSwitchAnimationSpec(UiPreset.MD3)

        assertEquals(360, iosSpec.durationMs)
        assertEquals(240, md3Spec.durationMs)
        assertTrue(iosSpec.durationMs > md3Spec.durationMs)
        assertEquals(iosSpec.durationMs, resolveInlinePortraitPlayerCommentCollapseDurationMillis(iosSpec))
        assertEquals(md3Spec.durationMs, resolveInlinePortraitPlayerCommentCollapseDurationMillis(md3Spec))
    }

    @Test
    fun `effective selected tab follows target while pager is switching`() {
        assertEquals(
            1,
            resolveVideoContentEffectiveSelectedTabIndex(
                currentPage = 0,
                targetPage = 1,
                isScrollInProgress = true,
                pageCount = 2
            )
        )
    }

    @Test
    fun `effective selected tab uses current page when pager is idle`() {
        assertEquals(
            0,
            resolveVideoContentEffectiveSelectedTabIndex(
                currentPage = 0,
                targetPage = 1,
                isScrollInProgress = false,
                pageCount = 2
            )
        )
    }

    @Test
    fun `effective selected tab falls back to current page for invalid target`() {
        assertEquals(
            0,
            resolveVideoContentEffectiveSelectedTabIndex(
                currentPage = 0,
                targetPage = 3,
                isScrollInProgress = true,
                pageCount = 2
            )
        )
    }

    @Test
    fun `intro action row exposes a labeled comment entry and opens comment tab`() {
        val contentSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/screen/VideoContentSection.kt"
        )
        val actionSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/video/ui/section/VideoActionSection.kt"
        )

        assertTrue(contentSource.contains("onCommentClick = { onTabSelected(1) }"))
        assertTrue(actionSource.contains("text = \"评论 \${FormatUtils.formatStat(info.stat.reply.toLong())}\""))
        assertTrue(actionSource.contains("onClick = onCommentClick"))
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
