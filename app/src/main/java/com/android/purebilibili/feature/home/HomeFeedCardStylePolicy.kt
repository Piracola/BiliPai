package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.HomeFeedCardStyle

internal data class HomeFeedCardLayout(
    val coverAspectRatio: Float,
    val outerPaddingDp: Int,
    val itemSpacingDp: Int,
    val verticalItemSpacingDp: Int = itemSpacingDp,
    val storyCardHorizontalPaddingDp: Int,
    val compactMetadata: Boolean
)

/** 粉版双列列表框（偏高）。CDN 16:9 源图会左右裁。 */
internal const val HOME_FEED_OFFICIAL_COVER_ASPECT_RATIO = 4f / 3f

/** PiliPlus `Style.aspectRatio`：16:10。 */
internal const val HOME_FEED_PILIPLUS_COVER_ASPECT_RATIO = 16f / 10f

/** 与投稿/CDN 源同比例，标准封面几乎不裁。 */
internal const val HOME_FEED_FULL_COVER_ASPECT_RATIO = 16f / 9f

/** @deprecated 使用 [HOME_FEED_FULL_COVER_ASPECT_RATIO] */
internal const val HOME_FEED_CURRENT_COVER_ASPECT_RATIO = HOME_FEED_FULL_COVER_ASPECT_RATIO

/**
 * 解析首页/相关推荐等视频卡封面框比例。
 */
internal fun resolveHomeFeedCoverAspectRatio(style: HomeFeedCardStyle): Float {
    return when (style) {
        HomeFeedCardStyle.CURRENT -> HOME_FEED_FULL_COVER_ASPECT_RATIO
        HomeFeedCardStyle.OFFICIAL -> HOME_FEED_OFFICIAL_COVER_ASPECT_RATIO
        HomeFeedCardStyle.PILIPLUS -> HOME_FEED_PILIPLUS_COVER_ASPECT_RATIO
    }
}

internal fun resolveHomeFeedCardLayout(style: HomeFeedCardStyle): HomeFeedCardLayout {
    val coverAspectRatio = resolveHomeFeedCoverAspectRatio(style)
    return when (style) {
        HomeFeedCardStyle.CURRENT -> HomeFeedCardLayout(
            coverAspectRatio = coverAspectRatio,
            outerPaddingDp = 8,
            itemSpacingDp = 8,
            verticalItemSpacingDp = 8,
            storyCardHorizontalPaddingDp = 16,
            compactMetadata = false
        )

        HomeFeedCardStyle.OFFICIAL -> HomeFeedCardLayout(
            coverAspectRatio = coverAspectRatio,
            outerPaddingDp = 4,
            itemSpacingDp = 4,
            verticalItemSpacingDp = 6,
            storyCardHorizontalPaddingDp = 0,
            compactMetadata = true
        )

        HomeFeedCardStyle.PILIPLUS -> HomeFeedCardLayout(
            // 间距对齐 PiliPlus Style.cardSpace / safeSpace 系：8dp 卡间距
            coverAspectRatio = coverAspectRatio,
            outerPaddingDp = 8,
            itemSpacingDp = 8,
            verticalItemSpacingDp = 8,
            storyCardHorizontalPaddingDp = 8,
            compactMetadata = true
        )
    }
}
