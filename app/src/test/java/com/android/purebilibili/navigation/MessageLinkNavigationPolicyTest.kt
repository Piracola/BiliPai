package com.android.purebilibili.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MessageLinkNavigationPolicyTest {

    @Test
    fun resolveMessageLinkNavigationAction_routesAidDeepLinkToVideo() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://video/115391124741470?page=0&comment_root_id=279569905408"
        )

        val videoAction = assertIs<MessageLinkNavigationAction.Video>(action)
        assertEquals("av115391124741470", videoAction.videoId)
    }

    @Test
    fun resolveMessageLinkNavigationAction_routesLikelyDynamicCommentFallbackToDynamic() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://comment/detail/1/1199344045210468386/265141324256"
        )

        val dynamicAction = assertIs<MessageLinkNavigationAction.DynamicComment>(action)
        assertEquals("1199344045210468386", dynamicAction.dynamicId)
    }
}
