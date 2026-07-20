package com.android.purebilibili.feature.settings

import com.android.purebilibili.navigation3.BiliPaiNavKey
import com.android.purebilibili.navigation3.BiliPaiNavRouteTransition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsNavHierarchyPolicyTest {

    @Test
    fun isSettingsSubtreeRoute_includesSettingsRoutes() {
        assertTrue(isSettingsSubtreeRoute("settings"))
        assertTrue(isSettingsSubtreeRoute("settings_category"))
        assertTrue(isSettingsSubtreeRoute("settings_search"))
        assertTrue(isSettingsSubtreeRoute("appearance_settings"))
        assertFalse(isSettingsSubtreeRoute("home"))
    }

    @Test
    fun resolveSettingsNavDepth_mapsHierarchy() {
        assertEquals(0, resolveSettingsNavDepth("settings"))
        assertEquals(1, resolveSettingsNavDepth("settings_category"))
        assertEquals(2, resolveSettingsNavDepth("appearance_settings"))
        assertEquals(3, resolveSettingsNavDepth("icon_settings"))
        assertEquals(3, resolveSettingsNavDepth("animation_settings"))
    }

    @Test
    fun resolveSettingsNavParentRoute_animationUnderAppearance() {
        assertEquals("appearance_settings", resolveSettingsNavParentRoute("animation_settings"))
        assertEquals("appearance_settings", resolveSettingsNavParentRoute("icon_settings"))
        assertEquals("settings_category", resolveSettingsNavParentRoute("appearance_settings"))
    }

    @Test
    fun isSettingsNavHierarchyTransition_matchesParentChild() {
        assertTrue(
            isSettingsNavHierarchyTransition(
                parentRoute = "settings",
                childRoute = "settings_category",
            )
        )
        assertTrue(
            isSettingsNavHierarchyTransition(
                parentRoute = "settings_category",
                childRoute = "appearance_settings",
            )
        )
        assertTrue(
            isSettingsNavHierarchyTransition(
                parentRoute = "settings_search",
                childRoute = "appearance_settings",
            )
        )
        assertTrue(
            isSettingsNavHierarchyTransition(
                parentRoute = "settings",
                childRoute = "appearance_settings",
            )
        )
        assertTrue(
            isSettingsNavHierarchyTransition(
                parentRoute = "settings",
                childRoute = "playback_settings",
            )
        )
        assertTrue(
            isSettingsNavHierarchyTransition(
                parentRoute = "appearance_settings",
                childRoute = "animation_settings",
            )
        )
        assertTrue(
            isSettingsNavHierarchyTransition(
                parentRoute = "settings",
                childRoute = "animation_settings",
            )
        )
        assertFalse(
            isSettingsNavHierarchyTransition(
                parentRoute = "home",
                childRoute = "settings",
            )
        )
    }

    @Test
    fun resolveSettingsNavRouteTransition_returnsIosPushForHierarchy() {
        assertEquals(
            BiliPaiNavRouteTransition.SETTINGS_IOS_PUSH_FORWARD,
            resolveSettingsNavRouteTransition(
                fromRoute = "settings",
                toRoute = "settings_category",
                forward = true,
            )
        )
        assertEquals(
            BiliPaiNavRouteTransition.SETTINGS_IOS_PUSH_POP,
            resolveSettingsNavRouteTransition(
                fromRoute = "appearance_settings",
                toRoute = "settings",
                forward = false,
            )
        )
        assertEquals(
            BiliPaiNavRouteTransition.SETTINGS_IOS_PUSH_FORWARD,
            resolveSettingsNavRouteTransition(
                fromRoute = "settings",
                toRoute = "appearance_settings",
                forward = true,
            )
        )
        assertEquals(
            BiliPaiNavRouteTransition.SETTINGS_IOS_PUSH_POP,
            resolveSettingsNavRouteTransition(
                fromRoute = "animation_settings",
                toRoute = "appearance_settings",
                forward = false,
            )
        )
    }

    @Test
    fun resolveSettingsNavPopTransition_remapsMainHostWhenSettingsTabActive() {
        assertEquals(
            BiliPaiNavRouteTransition.SETTINGS_IOS_PUSH_POP,
            resolveSettingsNavPopTransition(
                fromKey = BiliPaiNavKey.AppearanceSettings,
                toKey = BiliPaiNavKey.MainHost,
                activeMainHostRoute = "settings",
            )
        )
        assertEquals(
            BiliPaiNavRouteTransition.SETTINGS_IOS_PUSH_POP,
            resolveSettingsNavPopTransition(
                fromKey = BiliPaiNavKey.AnimationSettings,
                toKey = BiliPaiNavKey.MainHost,
                activeMainHostRoute = "settings",
            )
        )
        assertNull(
            resolveSettingsNavPopTransition(
                fromKey = BiliPaiNavKey.AppearanceSettings,
                toKey = BiliPaiNavKey.MainHost,
                activeMainHostRoute = "home",
            )
        )
    }

    @Test
    fun resolveSettingsRootCategoryForNavKey_readsCategoryKey() {
        val category = resolveSettingsRootCategoryForNavKey(
            BiliPaiNavKey.SettingsCategory(SettingsRootCategory.CONTENT_PLAYBACK)
        )
        assertEquals(SettingsRootCategory.CONTENT_PLAYBACK, category)
    }
}
