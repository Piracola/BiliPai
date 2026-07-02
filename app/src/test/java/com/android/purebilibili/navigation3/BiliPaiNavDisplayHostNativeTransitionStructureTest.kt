package com.android.purebilibili.navigation3

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class BiliPaiNavDisplayHostNativeTransitionStructureTest {

    @Test
    fun hostForwardsPredictiveBackProgressAndCancellationToNativeVideoTransition() {
        val source = loadSource()

        assertTrue(source.contains("onNativeVideoBackProgress:"))
        assertTrue(source.contains("NavigationEventTransitionState.InProgress"))
        assertTrue(source.contains("latestEvent?.progress"))
        assertTrue(source.contains("onNativeVideoBackCancelled("))
    }

    private fun loadSource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/navigation3/BiliPaiNavDisplayHost.kt"),
            File("src/main/java/com/android/purebilibili/navigation3/BiliPaiNavDisplayHost.kt")
        ).first { it.exists() }.readText()
    }
}
