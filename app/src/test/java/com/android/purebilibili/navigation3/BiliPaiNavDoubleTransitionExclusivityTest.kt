package com.android.purebilibili.navigation3

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 守护「双轨动画互斥」不变量（docs/PERFORMANCE_REVIEW_PLAN.md A-5）：
 *
 * 实际页面转场完全由 Miuix NavDisplay 轨驱动（biliPaiMiuixNavTransition /
 * miuixVideoCardNavTransition）。Compose ContentTransform 轨必须保持休眠——
 * 任何路由若同时激活两条轨，会出现双层转场（视觉 pop + 双倍动画开销）。
 *
 * 两个不变量：
 *  1. 能与 Miuix 转场并存的受管路由（卡片 morph 与关闭卡片后的方向化路由）在
 *     Compose 轨上必须解析为 None/None；
 *  2. [resolveBiliPaiNavContentTransform] 在 main 源码中必须保持无调用点
 *     （待命策略代码）；若新路由需要接入 Compose 轨，必须先让对应场景的
 *     Miuix 轨显式为 None，并同步更新本测试。
 */
class BiliPaiNavDoubleTransitionExclusivityTest {

    @Test
    fun morphAndManagedRoutesResolveToNoOpOnComposeTrack() {
        val managedRoutes = listOf(
            BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT,
            BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_FORWARD_FROM_LEFT,
            BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_FORWARD_FROM_RIGHT,
            BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_LEFT,
            BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_RIGHT,
        )
        for (route in managedRoutes) {
            val transform = resolveBiliPaiNavContentTransform(route)
            assertEquals(
                EnterTransition.None,
                transform.targetContentEnter,
                "$route 必须在 Compose 轨上无入场动画（Miuix 轨负责动画）"
            )
            assertEquals(
                ExitTransition.None,
                transform.initialContentExit,
                "$route 必须在 Compose 轨上无退场动画（Miuix 轨负责动画）"
            )
        }
    }

    @Test
    fun contentTransformTrackStaysDormantInMainSources() {
        val consumers = mainSourceFiles()
            .filter { it.name != "BiliPaiNavContentTransformPolicy.kt" }
            .filter { file -> file.readText().contains("resolveBiliPaiNavContentTransform(") }
            .map { it.name }

        assertEquals(
            emptyList(),
            consumers,
            "main 源码不得调用 resolveBiliPaiNavContentTransform：Compose ContentTransform " +
                "轨必须保持休眠，否则与 Miuix NavDisplay 转场叠加成双层动画"
        )
    }

    @Test
    fun everyRouteTransitionHasExplicitComposeTrackResolution() {
        val policySource = mainSourceFile("navigation3/BiliPaiNavContentTransformPolicy.kt").readText()
        for (route in BiliPaiNavRouteTransition.entries) {
            assertTrue(
                policySource.contains("BiliPaiNavRouteTransition.${route.name}"),
                "新增路由 ${route.name} 必须在 BiliPaiNavContentTransformPolicy 中显式声明 " +
                    "Compose 轨解析（None 或受管动画），禁止隐式落入 FALLBACK"
            )
        }
    }

    private fun mainSourceFiles(): List<File> =
        File("app/src/main/java/com/android/purebilibili").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

    private fun mainSourceFile(relativePath: String): File =
        listOf(
            File("app/src/main/java/com/android/purebilibili/$relativePath"),
            File("src/main/java/com/android/purebilibili/$relativePath"),
        ).first { it.exists() }
}
