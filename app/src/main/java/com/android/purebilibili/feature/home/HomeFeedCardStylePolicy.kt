package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.HomeFeedCardStyle

internal data class HomeFeedCardLayout(
    val coverAspectRatio: Float,
    val outerPaddingDp: Int,
    val itemSpacingDp: Int,
    val storyCardHorizontalPaddingDp: Int,
    val compactMetadata: Boolean
)

internal fun resolveHomeFeedCardLayout(style: HomeFeedCardStyle): HomeFeedCardLayout {
    return when (style) {
        HomeFeedCardStyle.CURRENT -> HomeFeedCardLayout(
            coverAspectRatio = 16f / 10f,
            outerPaddingDp = 8,
            itemSpacingDp = 8,
            storyCardHorizontalPaddingDp = 16,
            compactMetadata = false
        )

        HomeFeedCardStyle.OFFICIAL -> HomeFeedCardLayout(
            coverAspectRatio = 4f / 3f,
            outerPaddingDp = 0,
            itemSpacingDp = 4,
            storyCardHorizontalPaddingDp = 0,
            compactMetadata = true
        )
    }
}
