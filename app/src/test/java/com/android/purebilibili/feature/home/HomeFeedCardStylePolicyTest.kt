package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.HomeFeedCardStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeFeedCardStylePolicyTest {

    @Test
    fun currentStyle_keepsExistingRatioAndSpacing() {
        val layout = resolveHomeFeedCardLayout(HomeFeedCardStyle.CURRENT)

        assertEquals(16f / 10f, layout.coverAspectRatio)
        assertEquals(8, layout.outerPaddingDp)
        assertEquals(8, layout.itemSpacingDp)
        assertEquals(16, layout.storyCardHorizontalPaddingDp)
        assertEquals(false, layout.compactMetadata)
    }

    @Test
    fun officialStyle_usesFourByThreeAndCompactSpacing() {
        val layout = resolveHomeFeedCardLayout(HomeFeedCardStyle.OFFICIAL)

        assertEquals(4f / 3f, layout.coverAspectRatio)
        assertEquals(0, layout.outerPaddingDp)
        assertEquals(4, layout.itemSpacingDp)
        assertEquals(0, layout.storyCardHorizontalPaddingDp)
        assertEquals(true, layout.compactMetadata)
    }
}
