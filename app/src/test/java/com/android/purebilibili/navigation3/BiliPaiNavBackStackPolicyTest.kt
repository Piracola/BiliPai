package com.android.purebilibili.navigation3

import com.android.purebilibili.navigation.ScreenRoutes
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BiliPaiNavBackStackPolicyTest {

    @Test
    fun initialBackStack_usesOnboardingWhenRequired() {
        assertEquals(
            listOf(BiliPaiNavKey.Unknown("onboarding")),
            resolveInitialBiliPaiBackStack(
                firstRoute = ScreenRoutes.Home.route,
                onboardingRequired = true
            )
        )
    }

    @Test
    fun initialBackStack_usesMainHostForMainApp() {
        assertEquals(
            listOf(BiliPaiNavKey.MainHost),
            resolveInitialBiliPaiBackStack(
                firstRoute = ScreenRoutes.Profile.route,
                onboardingRequired = false
            )
        )
    }

    @Test
    fun push_skipsDuplicateTopEntry() {
        val stack = listOf(BiliPaiNavKey.MainHost)

        assertEquals(stack, pushBiliPaiNavKey(stack, BiliPaiNavKey.MainHost))
    }

    @Test
    fun pop_keepsRootEntry() {
        assertEquals(
            listOf(BiliPaiNavKey.MainHost),
            popBiliPaiNavKey(listOf(BiliPaiNavKey.MainHost))
        )
        assertEquals(
            listOf(BiliPaiNavKey.MainHost),
            popBiliPaiNavKey(listOf(BiliPaiNavKey.MainHost, BiliPaiNavKey.VideoDetail("BV1")))
        )
    }

    @Test
    fun onboardingFinishEntersMainHostInsteadOfDirectHomeRoute() {
        val sourceFile = listOf(
            File("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt"),
            File("src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")
        ).first { it.exists() }
        val source = sourceFile.readText()
        val onboardingFinishBlock = source
            .substringAfter("BiliPaiNavEntryContentRole.ONBOARDING")
            .substringBefore("BiliPaiNavEntryContentRole.SETTINGS")

        assertTrue(onboardingFinishBlock.contains("navigation3BackStack = listOf(BiliPaiNavKey.MainHost)"))
        assertFalse(onboardingFinishBlock.contains("navigation3BackStack = listOf(BiliPaiNavKey.Home)"))
    }
}
