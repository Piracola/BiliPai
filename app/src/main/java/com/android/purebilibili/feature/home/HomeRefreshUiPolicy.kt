package com.android.purebilibili.feature.home

internal fun shouldHandleRefreshNewItemsEvent(
    refreshKey: Long,
    handledKey: Long
): Boolean {
    if (refreshKey <= 0L) return false
    return refreshKey > handledKey
}

/**
 * 分区/热门综合等分页流：手动刷新时翻到下一页，避免永远拉 pn=1 看起来没换视频。
 */
internal fun resolvePagedFeedPageToFetch(
    isLoadMore: Boolean,
    isManualRefresh: Boolean,
    currentPageIndex: Int,
    advanceOnManualRefresh: Boolean
): Int = when {
    isLoadMore -> currentPageIndex + 1
    isManualRefresh && advanceOnManualRefresh -> (currentPageIndex + 1).coerceAtLeast(1)
    else -> 1
}

internal fun shouldAdvancePagedFeedOnManualRefresh(
    category: HomeCategory,
    popularSubCategory: PopularSubCategory
): Boolean = when (category) {
    HomeCategory.RECOMMEND,
    HomeCategory.FOLLOW,
    HomeCategory.LIVE -> false
    HomeCategory.POPULAR -> popularSubCategory == PopularSubCategory.COMPREHENSIVE
    else -> category.tid > 0
}

internal fun resolvePagedFeedPageIndexAfterFetch(
    isLoadMore: Boolean,
    isManualRefresh: Boolean,
    advanceOnManualRefresh: Boolean,
    pageToFetch: Int,
    incomingCount: Int,
    previousPageIndex: Int
): Int = when {
    isLoadMore -> if (incomingCount > 0) previousPageIndex + 1 else previousPageIndex
    isManualRefresh && advanceOnManualRefresh -> {
        if (incomingCount > 0) pageToFetch else 0
    }
    else -> 1
}

internal fun shouldFallbackFollowIncrementalRefreshToFull(
    isManualRefresh: Boolean,
    isLoadMore: Boolean,
    incrementalRefreshEnabled: Boolean,
    addedCount: Int
): Boolean = isManualRefresh &&
    !isLoadMore &&
    incrementalRefreshEnabled &&
    addedCount <= 0

/**
 * 关注流整表刷新时，只统计旧列表里没有的条目，避免把整页数量误报成“新增”。
 */
internal fun resolveFollowRefreshAddedCount(
    previousKeys: Set<String>,
    refreshedKeys: List<String>
): Int = refreshedKeys.count { it !in previousKeys }

internal fun shouldShowRecommendOldContentDivider(
    currentCategory: HomeCategory,
    refreshNewItemsKey: Long,
    revealedRefreshKey: Long,
    anchorBvid: String?,
    oldContentStartIndex: Int?
): Boolean {
    if (currentCategory != HomeCategory.RECOMMEND) return false
    if (refreshNewItemsKey <= 0L || revealedRefreshKey != refreshNewItemsKey) return false
    return !anchorBvid.isNullOrBlank() || (oldContentStartIndex != null && oldContentStartIndex > 0)
}
