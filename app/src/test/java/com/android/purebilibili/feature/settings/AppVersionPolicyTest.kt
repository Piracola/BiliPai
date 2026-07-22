package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class AppVersionPolicyTest {

    @Test
    fun appVersion_isUpdatedToNineNineEight() {
        val buildFile = listOf(
            File("app/build.gradle.kts"),
            File("build.gradle.kts")
        ).first { it.exists() }.readText()

        assertTrue(buildFile.contains("versionCode = 263"))
        assertTrue(buildFile.contains("versionName = \"9.9.8.8\""))
    }
}
