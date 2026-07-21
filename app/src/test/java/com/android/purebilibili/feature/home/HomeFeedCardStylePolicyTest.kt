package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.HomeFeedCardStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeFeedCardStylePolicyTest {

    @Test
    fun currentStyle_usesSixteenByNineFullCover() {
        val layout = resolveHomeFeedCardLayout(HomeFeedCardStyle.CURRENT)

        assertEquals(HOME_FEED_FULL_COVER_ASPECT_RATIO, layout.coverAspectRatio, 0.0001f)
        assertEquals(16f / 9f, resolveHomeFeedCoverAspectRatio(HomeFeedCardStyle.CURRENT), 0.0001f)
        assertEquals(8, layout.outerPaddingDp)
        assertEquals(8, layout.itemSpacingDp)
        assertEquals(8, layout.verticalItemSpacingDp)
        assertEquals(16, layout.storyCardHorizontalPaddingDp)
        assertEquals(false, layout.compactMetadata)
        assertEquals("16:9", HomeFeedCardStyle.CURRENT.label)
    }

    @Test
    fun officialStyle_usesFourByThreeLikeOfficialDualColumnFeed() {
        val layout = resolveHomeFeedCardLayout(HomeFeedCardStyle.OFFICIAL)

        // 粉版双列列表框 4:3 + 居中 Crop（CDN 源 16:9 会裁左右，与本家截图一致）
        assertEquals(HOME_FEED_OFFICIAL_COVER_ASPECT_RATIO, layout.coverAspectRatio, 0.0001f)
        assertEquals(4f / 3f, resolveHomeFeedCoverAspectRatio(HomeFeedCardStyle.OFFICIAL), 0.0001f)
        assertEquals(4, layout.outerPaddingDp)
        assertEquals(4, layout.itemSpacingDp)
        assertEquals(6, layout.verticalItemSpacingDp)
        assertEquals(0, layout.storyCardHorizontalPaddingDp)
        assertEquals(true, layout.compactMetadata)
        assertEquals("粉版 4:3", HomeFeedCardStyle.OFFICIAL.label)
    }

    @Test
    fun piliPlusStyle_usesSixteenByTenLikePiliPlusCard() {
        val layout = resolveHomeFeedCardLayout(HomeFeedCardStyle.PILIPLUS)

        assertEquals(HOME_FEED_PILIPLUS_COVER_ASPECT_RATIO, layout.coverAspectRatio, 0.0001f)
        assertEquals(16f / 10f, resolveHomeFeedCoverAspectRatio(HomeFeedCardStyle.PILIPLUS), 0.0001f)
        assertEquals(8, layout.outerPaddingDp)
        assertEquals(8, layout.itemSpacingDp)
        assertEquals(8, layout.verticalItemSpacingDp)
        assertEquals(8, layout.storyCardHorizontalPaddingDp)
        assertEquals(true, layout.compactMetadata)
        assertEquals("PiliPlus", HomeFeedCardStyle.PILIPLUS.label)
        assertEquals(2, HomeFeedCardStyle.PILIPLUS.value)
        assertEquals(HomeFeedCardStyle.PILIPLUS, HomeFeedCardStyle.fromValue(2))
    }
}
