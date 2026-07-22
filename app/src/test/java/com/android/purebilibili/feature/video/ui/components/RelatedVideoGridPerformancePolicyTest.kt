package com.android.purebilibili.feature.video.ui.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RelatedVideoGridPerformancePolicyTest {

    @Test
    fun sharedTransition_isDisabledWhileRelatedGridScrolls() {
        assertTrue(
            shouldEnableRelatedVideoGridSharedTransition(
                sharedTransitionEnabled = true,
                isListScrolling = false,
            ),
        )
        assertFalse(
            shouldEnableRelatedVideoGridSharedTransition(
                sharedTransitionEnabled = true,
                isListScrolling = true,
            ),
        )
        assertFalse(
            shouldEnableRelatedVideoGridSharedTransition(
                sharedTransitionEnabled = false,
                isListScrolling = false,
            ),
        )
    }
}
