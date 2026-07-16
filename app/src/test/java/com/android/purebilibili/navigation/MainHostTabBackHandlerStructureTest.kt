package com.android.purebilibili.navigation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MainHostTabBackHandlerStructureTest {

    @Test
    fun mainHostTabBackHandler_usesNavigationBackHandlerInsteadOfBackHandler() {
        val source = mainHostTabBackHandlerSource()

        assertTrue(source.contains("NavigationBackHandler("))
        assertTrue(source.contains("rememberNavigationEventState(NavigationEventInfo.None)"))
        assertTrue(source.contains("LocalPredictiveBackGestureEnabled.current"))
        assertTrue(source.contains("reportPredictiveProgress = predictiveBackGestureEnabled"))
        assertFalse(source.contains("import androidx.activity.compose.BackHandler"))
        // 仅需确认没有「直接注册 androidx.activity 的 BackHandler」。之前此处误用
        // `"BackHandler("` 子串断言，会被 `NavigationBackHandler(` 命中，属于 staleness。
        assertFalse(source.contains("androidx.activity.compose.BackHandler "))
    }

    private fun mainHostTabBackHandlerSource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/navigation/MainHostTabBackHandler.kt"),
            File("src/main/java/com/android/purebilibili/navigation/MainHostTabBackHandler.kt"),
        ).first { it.exists() }.readText()
    }
}