package com.android.purebilibili.feature.home.components

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import java.io.File
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopTabStylePolicyTest {

    @Test
    fun `floating plus liquid uses liquid glass`() {
        val state = resolveTopTabStyle(
            isBottomBarFloating = true,
            isBottomBarBlurEnabled = true,
            isLiquidGlassEnabled = true
        )

        assertEquals(true, state.floating)
        assertEquals(TopTabMaterialMode.LIQUID_GLASS, state.materialMode)
    }

    @Test
    fun `floating without liquid but blur enabled uses blur`() {
        val state = resolveTopTabStyle(
            isBottomBarFloating = true,
            isBottomBarBlurEnabled = true,
            isLiquidGlassEnabled = false
        )

        assertEquals(true, state.floating)
        assertEquals(TopTabMaterialMode.BLUR, state.materialMode)
    }

    @Test
    fun `floating without blur and liquid uses plain`() {
        val state = resolveTopTabStyle(
            isBottomBarFloating = true,
            isBottomBarBlurEnabled = false,
            isLiquidGlassEnabled = false
        )

        assertEquals(true, state.floating)
        assertEquals(TopTabMaterialMode.PLAIN, state.materialMode)
    }

    @Test
    fun `docked with blur uses blur`() {
        val state = resolveTopTabStyle(
            isBottomBarFloating = false,
            isBottomBarBlurEnabled = true,
            isLiquidGlassEnabled = false
        )

        assertEquals(false, state.floating)
        assertEquals(TopTabMaterialMode.BLUR, state.materialMode)
    }

    @Test
    fun `docked with liquid uses liquid glass when blur enabled`() {
        val state = resolveTopTabStyle(
            isBottomBarFloating = false,
            isBottomBarBlurEnabled = true,
            isLiquidGlassEnabled = true
        )

        assertEquals(false, state.floating)
        assertEquals(TopTabMaterialMode.LIQUID_GLASS, state.materialMode)
    }

    @Test
    fun `docked with liquid uses liquid glass without blur`() {
        val state = resolveTopTabStyle(
            isBottomBarFloating = false,
            isBottomBarBlurEnabled = false,
            isLiquidGlassEnabled = true
        )

        assertEquals(false, state.floating)
        assertEquals(TopTabMaterialMode.LIQUID_GLASS, state.materialMode)
    }

    @Test
    fun `reduced interaction budget keeps liquid glass home header tab material mode`() {
        assertEquals(
            TopTabMaterialMode.LIQUID_GLASS,
            resolveEffectiveHomeHeaderTabMaterialMode(
                materialMode = TopTabMaterialMode.LIQUID_GLASS,
                interactionBudget = HomeInteractionMotionBudget.REDUCED
            )
        )
        assertEquals(
            TopTabMaterialMode.BLUR,
            resolveEffectiveHomeHeaderTabMaterialMode(
                materialMode = TopTabMaterialMode.BLUR,
                interactionBudget = HomeInteractionMotionBudget.REDUCED
            )
        )
    }

    @Test
    fun `top tab liquid glass follows requested state regardless of interaction budget`() {
        assertTrue(
            resolveEffectiveTopTabLiquidGlassEnabled(
                isLiquidGlassEnabled = true,
                interactionBudget = HomeInteractionMotionBudget.FULL
            )
        )
        assertTrue(
            resolveEffectiveTopTabLiquidGlassEnabled(
                isLiquidGlassEnabled = true,
                interactionBudget = HomeInteractionMotionBudget.REDUCED
            )
        )
        assertFalse(
            resolveEffectiveTopTabLiquidGlassEnabled(
                isLiquidGlassEnabled = false,
                interactionBudget = HomeInteractionMotionBudget.FULL
            )
        )
    }

    @Test
    fun `home top tab material mode only keeps blur or plain`() {
        assertEquals(TopTabMaterialMode.BLUR, resolveHomeTopTabMaterialMode(headerBlurEnabled = true))
        assertEquals(TopTabMaterialMode.PLAIN, resolveHomeTopTabMaterialMode(headerBlurEnabled = false))
    }

    @Test
    fun `home top tab renderer routes by preset and native variant`() {
        assertEquals(
            HomeTopTabRenderer.IOS,
            resolveHomeTopTabRenderer(
                uiPreset = UiPreset.IOS,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3,
                labelMode = 2
            )
        )
        assertEquals(
            HomeTopTabRenderer.MD3,
            resolveHomeTopTabRenderer(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3,
                labelMode = 2
            )
        )
        assertEquals(
            HomeTopTabRenderer.MD3,
            resolveHomeTopTabRenderer(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MIUIX,
                labelMode = 2
            )
        )
        assertEquals(
            HomeTopTabRenderer.MD3,
            resolveHomeTopTabRenderer(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MIUIX,
                labelMode = 0
            )
        )
    }

    @Test
    fun `home top preset style separates ios material3 and miuix text tabs`() {
        val ios = resolveHomeTopPresetStyle(
            uiPreset = UiPreset.IOS,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3,
            labelMode = 2
        )
        val material3 = resolveHomeTopPresetStyle(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3,
            labelMode = 2
        )
        val miuix = resolveHomeTopPresetStyle(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX,
            labelMode = 2
        )

        assertNotEquals(ios.preset, material3.preset)
        assertNotEquals(material3.preset, miuix.preset)
        assertEquals(HomeTopPreset.IOS, ios.preset)
        assertEquals(HomeTopPreset.MATERIAL3, material3.preset)
        assertEquals(HomeTopPreset.MIUIX, miuix.preset)
        assertEquals(HomeTopTabRenderer.IOS, ios.renderer)
        assertEquals(HomeTopTabRenderer.MD3, material3.renderer)
        assertEquals(HomeTopTabRenderer.MD3, miuix.renderer)
        assertEquals(TopTabIndicatorStyle.CAPSULE, ios.indicatorStyle)
        assertEquals(TopTabIndicatorStyle.MATERIAL, material3.indicatorStyle)
        assertEquals(TopTabIndicatorStyle.MATERIAL, miuix.indicatorStyle)
    }

    @Test
    fun `miuix icon modes keep miuix dimensions while falling back to shared md3 renderer`() {
        val iconAndText = resolveHomeTopPresetStyle(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX,
            labelMode = 0
        )

        assertEquals(HomeTopPreset.MIUIX, iconAndText.preset)
        assertEquals(HomeTopTabRenderer.MD3, iconAndText.renderer)
        assertEquals(58.dp, iconAndText.tabRowHeightDocked)
        assertEquals(64.dp, iconAndText.tabRowHeightFloating)
        assertEquals(34.dp, iconAndText.md3VisualSpec.selectedCapsuleHeight)
        assertEquals(44.dp, iconAndText.actionButtonSizeDocked)
    }

    @Test
    fun `miuix top panel reserves extra content gap below category tabs`() {
        val ios = resolveHomeTopPresetStyle(
            uiPreset = UiPreset.IOS,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3,
            labelMode = 2
        )
        val material3 = resolveHomeTopPresetStyle(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3,
            labelMode = 2
        )
        val miuix = resolveHomeTopPresetStyle(
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX,
            labelMode = 2
        )

        assertEquals(5.dp, ios.reservedContentBottomGap)
        assertEquals(5.dp, material3.reservedContentBottomGap)
        assertEquals(12.dp, miuix.reservedContentBottomGap)
        assertEquals(
            12.dp,
            resolveHomeTopReservedContentBottomGap(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
        // Feed air under dock is owned by tabsToContent (list padding), not panel bottom gap.
        assertEquals(6.dp, ios.tabsToContentSpacing)
        assertEquals(6.dp, material3.tabsToContentSpacing)
        assertEquals(6.dp, miuix.tabsToContentSpacing)
    }

    @Test
    fun `miuix top settings button follows action button metrics while other presets keep existing size`() {
        assertEquals(
            40.dp,
            resolveHomeTopSettingsButtonSize(
                uiPreset = UiPreset.IOS,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3
            )
        )
        assertEquals(
            40.dp,
            resolveHomeTopSettingsButtonSize(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3
            )
        )
        assertEquals(
            44.dp,
            resolveHomeTopSettingsButtonSize(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
        assertEquals(
            20.dp,
            resolveHomeTopSettingsIconSize(
                uiPreset = UiPreset.IOS,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3
            )
        )
        assertEquals(
            20.dp,
            resolveHomeTopSettingsIconSize(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3
            )
        )
        assertEquals(
            22.dp,
            resolveHomeTopSettingsIconSize(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
    }


    @Test
    fun `clicking selected top tab scrolls to top while other tabs select`() {
        assertEquals(
            TopTabClickAction.SCROLL_TO_TOP,
            resolveTopTabClickAction(index = 2, selectedIndex = 2)
        )
        assertEquals(
            TopTabClickAction.SELECT_TAB,
            resolveTopTabClickAction(index = 3, selectedIndex = 2)
        )
    }

    @Test
    fun `ios top tab tuning uses enlarged bottom-bar-like footprint adapted for top dock`() {
        val tuning = resolveTopTabVisualTuning(UiPreset.IOS)

        assertEquals(48f, tuning.nonFloatingIndicatorHeightDp, 0.001f)
        assertEquals(24f, tuning.nonFloatingIndicatorCornerDp, 0.001f)
        assertEquals(1.18f, tuning.nonFloatingIndicatorWidthRatio, 0.001f)
        assertEquals(84f, tuning.nonFloatingIndicatorMinWidthDp, 0.001f)
        assertEquals(0f, tuning.nonFloatingIndicatorHorizontalInsetDp, 0.001f)
        assertEquals(48f, tuning.floatingIndicatorHeightDp, 0.001f)
        assertEquals(15f, tuning.tabTextSizeSp, 0.001f)
        assertEquals(20f, tuning.tabTextLineHeightSp, 0.001f)
        assertEquals(42f, tuning.tabContentMinHeightDp, 0.001f)
        assertEquals(20f, tuning.tabIconWithTextSizeDp, 0.001f)
        assertEquals(24f, tuning.tabIconOnlySizeDp, 0.001f)
    }

    @Test
    fun `md3 capsule top tab tuning also uses enlarged top dock shape`() {
        val tuning = resolveTopTabVisualTuning(UiPreset.MD3)

        assertEquals(48f, tuning.nonFloatingIndicatorHeightDp, 0.001f)
        assertEquals(24f, tuning.nonFloatingIndicatorCornerDp, 0.001f)
        assertEquals(48f, tuning.floatingIndicatorHeightDp, 0.001f)
        assertEquals(15f, tuning.tabTextSizeSp, 0.001f)
    }

    @Test
    fun `ios top tab keeps icon plus text scale stable inside large capsule`() {
        assertEquals(
            1f,
            resolveTopTabContentScale(
                selectionFraction = 1f,
                showIcon = true,
                showText = true,
                uiPreset = UiPreset.IOS
            ),
            0.001f
        )
        assertEquals(
            1.03f,
            resolveTopTabContentScale(
                selectionFraction = 1f,
                showIcon = true,
                showText = false,
                uiPreset = UiPreset.IOS
            ),
            0.001f
        )
    }

    @Test
    fun `ios lightweight top tab uses bottom bar capsule indicator shape`() {
        val source = sourceText("app/src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt")
        val itemBlock = source
            .substringAfter("private fun LightweightTopTabItem(")
            .substringBefore("Box(")

        assertTrue(itemBlock.contains("HomeTopTabRenderer.IOS -> resolveSharedBottomBarCapsuleShape()"))
        assertFalse(itemBlock.contains("HomeTopTabRenderer.IOS -> AppShapes.container(ContainerLevel.Pill)"))
    }

    @Test
    fun `top tab indicator reuses bottom bar immediate drag and liquid rendering`() {
        val source = sourceText("app/src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt")
        val gestureBlock = source
            .substringAfter("private fun Modifier.topTabSelectedItemDrag(")
            .substringBefore("@Composable\nprivate fun")
        val iosIndicatorBlock = source
            .substringAfter("if (shouldUseMovingIosCapsule) {")
            .substringBefore("if (shouldUseMd3DockBackedCapsule)")
        val chromeSource = sourceText(
            "app/src/main/java/com/android/purebilibili/feature/home/components/HomeTopTabChrome.kt"
        )
        val bottomBarIndicatorBlock = sourceText(
            "app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt"
        ).substringAfter("internal fun BoxScope.KernelSuBottomBarIndicatorLayer(")
            .substringBefore("@Composable\nprivate fun BoxScope.KernelSuBottomBarInputLayer(")

        assertTrue(gestureBlock.contains("awaitHorizontalTouchSlopOrCancellation"))
        assertFalse(gestureBlock.contains("awaitLongPressOrCancellation"))
        assertTrue(iosIndicatorBlock.contains("KernelSuBottomBarIndicatorLayer("))
        assertEquals(1, iosIndicatorBlock.split("KernelSuBottomBarIndicatorLayer(").size - 1)
        assertTrue(iosIndicatorBlock.contains("glassEnabled = shouldUseLiquidGlassIndicator"))
        assertTrue(iosIndicatorBlock.contains("indicatorEffectsEnabled = shouldUseLiquidGlassIndicator"))
        assertFalse(iosIndicatorBlock.contains(".fillMaxHeight()"))
        assertFalse(source.contains("shouldForceDragLiquidGlassIndicator"))
        assertFalse(chromeSource.contains("Modifier.clip(tabShape)"))
        assertTrue(bottomBarIndicatorBlock.contains("background(\n                        indicatorIdleSurfaceColor,"))
    }

    @Test
    fun `ios lightweight top tab capsule uses gray white while content keeps theme primary`() {
        val colorScheme = lightColorScheme(primary = Color(0xFF2D6A4F))
        val capsuleColor = resolveIosTopTabCapsuleContainerColor(
            isDarkTheme = false,
            selectionFraction = 1f
        )
        val bottomIndicatorColor = resolveBottomBarMovingIndicatorSurfaceColor(isDarkTheme = false)

        assertEquals(bottomIndicatorColor.red, capsuleColor.red, 0.001f)
        assertEquals(bottomIndicatorColor.green, capsuleColor.green, 0.001f)
        assertEquals(bottomIndicatorColor.blue, capsuleColor.blue, 0.001f)
        assertEquals(0.28f, capsuleColor.alpha, 0.002f)
        assertEquals(
            colorScheme.primary,
            resolveIosTopTabSelectedContentColor(colorScheme)
        )
        assertFalse(capsuleColor == colorScheme.primary.copy(alpha = 0.10f))
    }

    @Test
    fun `ios top tab icon modes use readable glyph sizes`() {
        assertEquals(20f, resolveTopTabIconSizeDp(labelMode = 0), 0.001f)
        assertEquals(24f, resolveTopTabIconSizeDp(labelMode = 1), 0.001f)
        assertEquals(3f, resolveTopTabIconTextSpacingDp(labelMode = 0), 0.001f)
        assertEquals(54.dp, resolveIosTopTabRowHeight(isFloatingStyle = false))
        assertEquals(56.dp, resolveIosTopTabRowHeight(isFloatingStyle = true))
        assertEquals(44.dp, resolveIosTopTabActionButtonSize(isFloatingStyle = false))
        assertEquals(22.dp, resolveIosTopTabActionIconSize(isFloatingStyle = false))
    }

    @Test
    fun `md3 top tabs keep material typography spacing`() {
        val textSize = resolveTopTabLabelTextSizeSp(labelMode = 0)
        val lineHeight = resolveTopTabLabelLineHeightSp(labelMode = 0)

        assertEquals(15f, textSize, 0.001f)
        assertEquals(20f, lineHeight, 0.001f)
        assertTrue(lineHeight >= textSize)
    }

    @Test
    fun `md3 top tabs should use compact text first underline sizing`() {
        val spec = resolveMd3TopTabVisualSpec(isFloatingStyle = false)

        assertEquals(54.dp, spec.rowHeight)
        assertEquals(2.dp, spec.selectedCapsuleHeight)
        assertEquals(1.dp, spec.selectedCapsuleCornerRadius)
        assertEquals(22.dp, spec.iconSize)
        assertEquals(15.sp, spec.labelTextSize)
        assertEquals(20.sp, spec.labelLineHeight)
        assertEquals(0.dp, spec.iconLabelSpacing)
        assertEquals(12.dp, spec.itemHorizontalPadding)
        assertEquals(0.dp, spec.selectedCapsuleShadowElevation)
        assertEquals(0.dp, spec.selectedCapsuleTonalElevation)
    }

    @Test
    fun `md3 icon plus text top tabs reserve enough height`() {
        val spec = resolveMd3TopTabVisualSpec(
            isFloatingStyle = false,
            labelMode = 0
        )

        assertEquals(62.dp, spec.rowHeight)
        assertEquals(8.dp, spec.itemHorizontalPadding)
        assertEquals(3.dp, spec.iconLabelSpacing)
        assertEquals(22.dp, spec.iconSize)
        assertEquals(15.sp, spec.labelTextSize)
        assertTrue(spec.labelLineHeight >= spec.labelTextSize)
    }

    @Test
    fun `android native miuix top tabs should promote capsule selection styling`() {
        val spec = resolveMd3TopTabVisualSpec(
            isFloatingStyle = false,
            androidNativeVariant = AndroidNativeVariant.MIUIX
        )

        assertEquals(52.dp, spec.rowHeight)
        assertEquals(34.dp, spec.selectedCapsuleHeight)
        assertEquals(17.dp, spec.selectedCapsuleCornerRadius)
        assertEquals(12.dp, spec.itemHorizontalPadding)
        assertEquals(3.dp, spec.iconLabelSpacing)
        assertEquals(15.sp, spec.labelTextSize)
    }


    @Test
    fun `android native miuix top tabs skip outer chrome surface`() {
        assertFalse(
            shouldDrawHomeTopTabOuterChromeSurface(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MIUIX,
                materialMode = TopTabMaterialMode.LIQUID_GLASS
            )
        )
        assertFalse(
            shouldDrawHomeTopTabOuterChromeSurface(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MIUIX,
                materialMode = TopTabMaterialMode.BLUR
            )
        )
        assertFalse(
            shouldDrawHomeTopTabOuterChromeSurface(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MIUIX,
                materialMode = TopTabMaterialMode.PLAIN
            )
        )
        assertTrue(
            shouldDrawHomeTopTabOuterChromeSurface(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3,
                materialMode = TopTabMaterialMode.LIQUID_GLASS
            )
        )
        assertTrue(
            shouldDrawHomeTopTabOuterChromeSurface(
                uiPreset = UiPreset.IOS,
                androidNativeVariant = AndroidNativeVariant.MIUIX,
                materialMode = TopTabMaterialMode.LIQUID_GLASS
            )
        )
    }

    @Test
    fun `md3 selected top tab should reuse material primary emphasis`() {
        val colorScheme = lightColorScheme(
            surface = Color.White,
            primary = Color(0xFF2D6A4F),
            secondaryContainer = Color(0xFFDCEFD8),
            onSecondaryContainer = Color(0xFF1A1C18),
            onSurface = Color(0xFF1B1C1F),
            onSurfaceVariant = Color(0xFF6A5E61)
        )

        assertEquals(colorScheme.primary, resolveMd3TopTabSelectedContainerColor(colorScheme))
        assertEquals(colorScheme.primary, resolveMd3TopTabSelectedIconColor(colorScheme))
        assertEquals(colorScheme.primary, resolveMd3TopTabSelectedLabelColor(colorScheme))
        assertEquals(colorScheme.onSurfaceVariant, resolveMd3TopTabUnselectedIconColor(colorScheme))
        assertEquals(colorScheme.onSurfaceVariant, resolveMd3TopTabUnselectedLabelColor(colorScheme))
    }

    @Test
    fun `android native miuix top tabs should use miuix secondary container emphasis`() {
        val colorScheme = lightColorScheme(
            primary = Color(0xFF2D6A4F),
            surfaceContainerHigh = Color(0xFFF4ECE1),
            secondaryContainer = Color(0xFFDCEFD8),
            onSecondaryContainer = Color(0xFF1A1C18),
            onSurface = Color(0xFF1E1B16),
            onSurfaceVariant = Color(0xFF6A5E61)
        )

        assertEquals(
            colorScheme.secondaryContainer,
            resolveMd3TopTabSelectedContainerColor(
                colorScheme = colorScheme,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
        assertEquals(
            colorScheme.onSecondaryContainer,
            resolveMd3TopTabSelectedIconColor(
                colorScheme = colorScheme,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
        assertEquals(
            colorScheme.onSecondaryContainer,
            resolveMd3TopTabSelectedLabelColor(
                colorScheme = colorScheme,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
    }


    @Test
    fun `md3 preset uses material tab indicator style`() {
        assertEquals(
            TopTabIndicatorStyle.MATERIAL,
            resolveTopTabIndicatorStyle(UiPreset.MD3)
        )
        assertEquals(
            TopTabIndicatorStyle.CAPSULE,
            resolveTopTabIndicatorStyle(UiPreset.IOS)
        )
    }

    @Test
    fun `md3 top tabs always use material indicator after removing top liquid glass`() {
        assertTrue(
            shouldUseMd3TopTabMaterialIndicator(
                uiPreset = UiPreset.MD3,
                liquidGlassEnabled = true
            )
        )
        assertTrue(
            shouldUseMd3TopTabMaterialIndicator(
                uiPreset = UiPreset.MD3,
                liquidGlassEnabled = false
            )
        )
    }

    @Test
    fun `md3 and miuix use screenshot underline when liquid glass is off`() {
        assertTrue(
            shouldUsePlainMd3TopTabUnderline(
                uiPreset = UiPreset.MD3,
                liquidGlassEnabled = false
            )
        )
        assertFalse(
            shouldUsePlainMd3TopTabUnderline(
                uiPreset = UiPreset.MD3,
                liquidGlassEnabled = true
            )
        )
        assertFalse(
            shouldUsePlainMd3TopTabUnderline(
                uiPreset = UiPreset.IOS,
                liquidGlassEnabled = false
            )
        )
    }

    @Test
    fun `md3 top tabs remove outer dock when liquid glass is off`() {
        assertFalse(
            shouldDrawHomeTopTabOuterChromeSurface(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3,
                materialMode = TopTabMaterialMode.BLUR
            )
        )
        assertFalse(
            shouldDrawHomeTopTabOuterChromeSurface(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3,
                materialMode = TopTabMaterialMode.PLAIN
            )
        )
        assertTrue(
            shouldDrawHomeTopTabOuterChromeSurface(
                uiPreset = UiPreset.MD3,
                androidNativeVariant = AndroidNativeVariant.MATERIAL3,
                materialMode = TopTabMaterialMode.LIQUID_GLASS
            )
        )
    }

    @Test
    fun `md3 top tabs use underline row semantics and tighter action shape`() {
        assertEquals(
            "UNDERLINE_FIXED",
            resolveMd3TopTabRowVariant().name
        )
        assertEquals(16.dp, resolveMd3TopTabActionButtonCorner(isFloatingStyle = true))
        assertEquals(12.dp, resolveMd3TopTabActionButtonCorner(isFloatingStyle = false))
        assertEquals(48.dp, resolveMd3TopTabActionButtonSize(isFloatingStyle = true))
        assertEquals(42.dp, resolveMd3TopTabActionButtonSize(isFloatingStyle = false))
        assertEquals(24.dp, resolveMd3TopTabActionIconSize(isFloatingStyle = true))
        assertEquals(22.dp, resolveMd3TopTabActionIconSize(isFloatingStyle = false))
        assertEquals(4.dp, resolveMd3TopTabActionContentBottomPadding())
        assertEquals(4f, resolveMd3TopTabVerticalLiftDp(), 0.001f)
        assertEquals(8.dp, resolveMd3TopTabIndicatorBottomPadding())
    }

    @Test
    fun `skin top tabs use compact readable underline layout`() {
        assertEquals(46.dp, resolveHomeSkinTopTabRowHeight())
        assertTrue(resolveHomeSkinTopTabRowHeight() >= 44.dp)
        assertTrue(resolveHomeSkinTopTabRowHeight() <= 48.dp)
        assertEquals(44.dp, resolveHomeSkinTopTabActionButtonSize())
        assertEquals(24.dp, resolveHomeSkinTopTabActionIconSize())
        assertEquals(4.dp, resolveHomeSkinTopTabIndicatorBottomPadding())
        assertEquals(32.dp, resolveTopTabSkinStickerIconSize(showText = true))
        assertEquals(36.dp, resolveTopTabSkinStickerIconSize(showText = false))
        assertEquals(32.dp, resolveTopTabSkinPartitionIconSize())
        assertEquals(28.dp, resolveTopTabSkinStickerIndicatorWidth())
        assertEquals(
            64.dp,
            resolveTopTabSkinStickerRowHeight(
                baseRowHeight = 56.dp,
                hasSkinStickerIcons = true,
                showIcon = true,
                showText = true
            )
        )
        assertEquals(
            52.dp,
            resolveTopTabSkinStickerRowHeight(
                baseRowHeight = 52.dp,
                hasSkinStickerIcons = false,
                showIcon = true,
                showText = true
            )
        )
        assertEquals(2.dp, resolveTopTabSkinStickerItemVerticalPadding(showText = true))
    }

    @Test
    fun `skin top tab colors stay readable on light and dark skin backgrounds`() {
        val darkBackgroundContent = resolveHomeSkinTopTabContentColor(Color(0xFF2E2A1E))
        val midDarkBackgroundContent = resolveHomeSkinTopTabContentColor(Color(0xFF778675))
        val lightBackgroundContent = resolveHomeSkinTopTabContentColor(Color(0xFFE4F6FF))

        assertEquals(Color.White.copy(alpha = 0.98f), darkBackgroundContent)
        assertEquals(Color.White.copy(alpha = 0.98f), midDarkBackgroundContent)
        assertEquals(Color(0xFF111820).copy(alpha = 0.96f), lightBackgroundContent)
        assertEquals(
            Color.White.copy(alpha = 0.84f),
            resolveHomeSkinTopTabUnselectedContentColor(darkBackgroundContent)
        )
        assertEquals(
            Color(0xFF111820).copy(alpha = 0.78f),
            resolveHomeSkinTopTabUnselectedContentColor(lightBackgroundContent)
        )
        assertEquals(darkBackgroundContent, resolveHomeSkinTopTabIndicatorColor(darkBackgroundContent))
        assertEquals(lightBackgroundContent, resolveHomeSkinTopTabIndicatorColor(lightBackgroundContent))
    }

    @Test
    fun `skin top tab image backed dark mode uses light content for all presets`() {
        val lightFallbackTint = Color(0xFFDFF5FF)
        val presets = listOf(
            UiPreset.IOS to AndroidNativeVariant.MATERIAL3,
            UiPreset.MD3 to AndroidNativeVariant.MATERIAL3,
            UiPreset.MD3 to AndroidNativeVariant.MIUIX
        )

        presets.forEach { (uiPreset, androidNativeVariant) ->
            resolveHomeTopPresetStyle(
                uiPreset = uiPreset,
                androidNativeVariant = androidNativeVariant,
                labelMode = 0
            )
            val contentColor = resolveHomeSkinTopTabContentColor(
                topAtmosphereTint = lightFallbackTint,
                hasTopAtmosphereImage = true,
                darkTheme = true
            )

            assertEquals(
                "$uiPreset/$androidNativeVariant should stay readable over dark image-backed skin",
                Color.White.copy(alpha = 0.98f),
                contentColor
            )
        }
    }

    @Test
    fun `skin decoration keeps host top tab readability strategy`() {
        assertFalse(shouldUseHomeSkinPlainTopTabs(null))
        assertFalse(
            shouldUseHomeSkinPlainTopTabs(
                HomeUiSkinDecoration(
                    skinId = "test",
                    topAtmosphereTint = Color(0xFFE4F6FF),
                    searchCapsuleTint = Color.White
                )
            )
        )
    }

    @Test
    fun `skin top tabs render sticker image before host vector icon fallback`() {
        val source = sourceText("src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt")
        val rowCallSource = source
            .substringAfter("LightweightHomeTopTabs(")
            .substringBefore("private fun rememberTopTabPagerDragHeld(")
        val itemSource = source
            .substringAfter("private fun LightweightTopTabItem(")
            .substringBefore("@OptIn(ExperimentalMaterial3Api::class)")

        assertTrue(rowCallSource.contains("topTabSkinIconPaths = topTabSkinIconPaths"))
        assertTrue(rowCallSource.contains("partitionSkinIconPath = partitionSkinIconPath"))
        assertTrue(itemSource.contains("skinIconPath"))
        assertTrue(itemSource.contains("AsyncImage("))
        assertTrue(itemSource.contains("model = File(skinIconPath)"))
        assertTrue(itemSource.contains("resolveTopTabSkinStickerIconSize(showText = showText)"))
        assertTrue(rowCallSource.contains("resolveTopTabSkinPartitionIconSize()"))
        assertTrue(rowCallSource.contains("resolveTopTabSkinStickerRowHeight("))
        assertTrue(rowCallSource.contains("if (effectiveRenderer == HomeTopTabRenderer.MD3 && !hasSkinStickerIcons)"))
        assertTrue(itemSource.contains("resolveTopTabSkinStickerItemVerticalPadding(showText = showText)"))
        assertTrue(itemSource.contains("resolveTopTabSkinStickerIndicatorWidth()"))
        assertTrue(itemSource.contains("alpha(selectionFraction)"))
        assertTrue(itemSource.indexOf("AsyncImage(") < itemSource.indexOf("imageVector = icon"))
        assertTrue(itemSource.contains("else {"))
        assertTrue(itemSource.contains("resolveTopTabCategoryIcon(categoryKey, uiPreset)"))
    }

    @Test
    fun `skin top tab stickers keep ios md3 and miuix on shared item indicator path`() {
        val source = sourceText("src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt")
        val categoryTabRowSource = source
            .substringAfter("fun CategoryTabRow(")
            .substringBefore("@Composable\nprivate fun rememberTopTabPagerDragHeld(")

        assertTrue(categoryTabRowSource.contains("val hasSkinStickerIcons = topTabSkinIconPaths.isNotEmpty() || !partitionSkinIconPath.isNullOrBlank()"))
        assertTrue(categoryTabRowSource.contains("topTabSkinIconPaths = topTabSkinIconPaths"))
        assertTrue(categoryTabRowSource.contains("partitionSkinIconPath = partitionSkinIconPath"))
    }

    @Test
    fun `android native miuix top tabs should slightly enlarge action button chrome`() {
        assertEquals(
            18.dp,
            resolveMd3TopTabActionButtonCorner(
                isFloatingStyle = true,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
        assertEquals(
            14.dp,
            resolveMd3TopTabActionButtonCorner(
                isFloatingStyle = false,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
        assertEquals(
            50.dp,
            resolveMd3TopTabActionButtonSize(
                isFloatingStyle = true,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
        assertEquals(
            44.dp,
            resolveMd3TopTabActionButtonSize(
                isFloatingStyle = false,
                androidNativeVariant = AndroidNativeVariant.MIUIX
            )
        )
    }

    private fun sourceText(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
