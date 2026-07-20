package com.android.purebilibili.feature.settings

import com.android.purebilibili.navigation3.BiliPaiNavKey
import com.android.purebilibili.navigation3.BiliPaiNavRouteTransition

internal const val SETTINGS_ROUTE_BASE = "settings"
internal const val SETTINGS_CATEGORY_ROUTE_BASE = "settings_category"
internal const val SETTINGS_SEARCH_ROUTE_BASE = "settings_search"

internal val SETTINGS_SUBTREE_ROUTE_BASES: Set<String> = setOf(
    SETTINGS_ROUTE_BASE,
    SETTINGS_CATEGORY_ROUTE_BASE,
    SETTINGS_SEARCH_ROUTE_BASE,
    "appearance_settings",
    "icon_settings",
    "animation_settings",
    "playback_settings",
    "permission_settings",
    "plugins_settings",
    "js_plugin",
    "external_media",
    "bottom_bar_settings",
    "settings_share",
    "webdav_backup",
    "tips_settings",
    "open_source_licenses",
)

private val SETTINGS_DEPTH2_ROUTE_BASES: Set<String> = setOf(
    "appearance_settings",
    "playback_settings",
    "permission_settings",
    "plugins_settings",
    "bottom_bar_settings",
    "settings_share",
    "webdav_backup",
    "tips_settings",
    "open_source_licenses",
)

private val SETTINGS_DEPTH3_ROUTE_BASES: Set<String> = setOf(
    "icon_settings",
    "animation_settings",
    "js_plugin",
)

private val SETTINGS_DEPTH4_ROUTE_BASES: Set<String> = setOf(
    "external_media",
)

/** settings / search / category 可直达的二级页；动画页虽挂在外观下，搜索与快捷入口也可直达。 */
private val SETTINGS_DIRECT_REACH_FROM_ROOT_CHILDREN: Set<String> =
    SETTINGS_DEPTH2_ROUTE_BASES + setOf("animation_settings")

private val SETTINGS_DIRECT_REACH_PARENTS: Set<String> = setOf(
    SETTINGS_ROUTE_BASE,
    SETTINGS_SEARCH_ROUTE_BASE,
    SETTINGS_CATEGORY_ROUTE_BASE,
)

private val ROUTE_TO_CATEGORY: Map<String, SettingsRootCategory> = mapOf(
    "appearance_settings" to SettingsRootCategory.APPEARANCE_INTERACTION,
    "animation_settings" to SettingsRootCategory.APPEARANCE_INTERACTION,
    "icon_settings" to SettingsRootCategory.APPEARANCE_INTERACTION,
    "bottom_bar_settings" to SettingsRootCategory.APPEARANCE_INTERACTION,
    "playback_settings" to SettingsRootCategory.CONTENT_PLAYBACK,
    "permission_settings" to SettingsRootCategory.PRIVACY_STORAGE,
    "settings_share" to SettingsRootCategory.PRIVACY_STORAGE,
    "webdav_backup" to SettingsRootCategory.PRIVACY_STORAGE,
    "plugins_settings" to SettingsRootCategory.SYSTEM_ABOUT,
    "js_plugin" to SettingsRootCategory.SYSTEM_ABOUT,
    "external_media" to SettingsRootCategory.SYSTEM_ABOUT,
    "tips_settings" to SettingsRootCategory.SYSTEM_ABOUT,
    "open_source_licenses" to SettingsRootCategory.SYSTEM_ABOUT,
)

internal fun isSettingsSubtreeRoute(routeBase: String?): Boolean {
    val normalized = routeBase?.substringBefore("?")?.takeIf { it.isNotBlank() } ?: return false
    return normalized in SETTINGS_SUBTREE_ROUTE_BASES
}

internal fun resolveSettingsNavRouteBase(key: BiliPaiNavKey): String = key.routeBase

internal fun resolveSettingsNavDepth(routeBase: String?): Int {
    val normalized = routeBase?.substringBefore("?")?.takeIf { it.isNotBlank() } ?: return -1
    return when (normalized) {
        SETTINGS_ROUTE_BASE -> 0
        SETTINGS_CATEGORY_ROUTE_BASE,
        SETTINGS_SEARCH_ROUTE_BASE -> 1
        in SETTINGS_DEPTH2_ROUTE_BASES -> 2
        in SETTINGS_DEPTH3_ROUTE_BASES -> 3
        in SETTINGS_DEPTH4_ROUTE_BASES -> 4
        else -> -1
    }
}

internal fun resolveSettingsNavParentRoute(childRoute: String?): String? {
    val normalized = childRoute?.substringBefore("?")?.takeIf { it.isNotBlank() } ?: return null
    return when (normalized) {
        SETTINGS_ROUTE_BASE -> null
        SETTINGS_CATEGORY_ROUTE_BASE,
        SETTINGS_SEARCH_ROUTE_BASE -> SETTINGS_ROUTE_BASE
        in SETTINGS_DEPTH2_ROUTE_BASES -> SETTINGS_CATEGORY_ROUTE_BASE
        "icon_settings",
        "animation_settings" -> "appearance_settings"
        "js_plugin" -> "plugins_settings"
        "external_media" -> "js_plugin"
        else -> null
    }
}

internal fun resolveSettingsRootCategoryForRoute(routeBase: String?): SettingsRootCategory? {
    val normalized = routeBase?.substringBefore("?")?.takeIf { it.isNotBlank() } ?: return null
    return when (normalized) {
        SETTINGS_CATEGORY_ROUTE_BASE -> null
        in ROUTE_TO_CATEGORY -> ROUTE_TO_CATEGORY.getValue(normalized)
        else -> null
    }
}

internal fun resolveSettingsRootCategoryForNavKey(key: BiliPaiNavKey): SettingsRootCategory? {
    return when (key) {
        is BiliPaiNavKey.SettingsCategory -> key.category
        else -> resolveSettingsRootCategoryForRoute(key.routeBase)
    }
}

internal fun isSettingsNavHierarchyTransition(
    parentRoute: String?,
    childRoute: String?,
): Boolean {
    if (parentRoute == null || childRoute == null) return false
    if (!isSettingsSubtreeRoute(childRoute)) return false
    val normalizedParent = parentRoute.substringBefore("?")
    val normalizedChild = childRoute.substringBefore("?")
    if (resolveSettingsNavParentRoute(normalizedChild) == normalizedParent) {
        return true
    }
    // 设置根页 / 搜索 / 分类可直达二级子页（分栏、快捷入口、Nav3 push），需与 category 中转同等动画。
    if (normalizedChild in SETTINGS_DIRECT_REACH_FROM_ROOT_CHILDREN &&
        normalizedParent in SETTINGS_DIRECT_REACH_PARENTS
    ) {
        return true
    }
    return false
}

internal fun resolveSettingsNavRouteTransition(
    fromRoute: String?,
    toRoute: String?,
    forward: Boolean,
): BiliPaiNavRouteTransition? {
    if (forward) {
        if (!isSettingsNavHierarchyTransition(fromRoute, toRoute)) return null
        return BiliPaiNavRouteTransition.SETTINGS_IOS_PUSH_FORWARD
    }
    if (!isSettingsNavHierarchyTransition(toRoute, fromRoute)) return null
    return BiliPaiNavRouteTransition.SETTINGS_IOS_PUSH_POP
}

/**
 * 设置树 pop 的单一决策入口。Display / Entry / 预测返回 handler 都应读这里，
 * 避免 MainHost ↔ 底栏 Settings remap 不一致导致预览与提交动画分裂。
 */
internal fun resolveSettingsNavPopTransition(
    fromRoute: String?,
    toRoute: String?,
    activeMainHostRoute: String? = null,
): BiliPaiNavRouteTransition? {
    if (!isSettingsSubtreeRoute(fromRoute)) return null
    val normalizedTo = toRoute?.substringBefore("?")?.takeIf { it.isNotBlank() } ?: return null
    val normalizedActive = activeMainHostRoute?.substringBefore("?")?.takeIf { it.isNotBlank() }
    val effectiveParentRoute = if (
        normalizedTo == BiliPaiNavKey.MainHost.routeBase &&
        isSettingsSubtreeRoute(normalizedActive)
    ) {
        normalizedActive
    } else {
        normalizedTo
    }
    if (isSettingsNavHierarchyTransition(
            parentRoute = effectiveParentRoute,
            childRoute = fromRoute,
        )
    ) {
        return BiliPaiNavRouteTransition.SETTINGS_IOS_PUSH_POP
    }
    // 设置子树任意页直接回到 MainHost，且底栏仍在 Settings：统一走设置 iOS pop，
    // 覆盖非严格父子边（如搜索直达 animation 后一键返回）。
    if (normalizedTo == BiliPaiNavKey.MainHost.routeBase && isSettingsSubtreeRoute(normalizedActive)) {
        return BiliPaiNavRouteTransition.SETTINGS_IOS_PUSH_POP
    }
    return null
}

internal fun resolveSettingsNavPopTransition(
    fromKey: BiliPaiNavKey?,
    toKey: BiliPaiNavKey?,
    activeMainHostRoute: String? = null,
): BiliPaiNavRouteTransition? {
    if (fromKey == null || toKey == null) return null
    return resolveSettingsNavPopTransition(
        fromRoute = fromKey.routeBase,
        toRoute = toKey.routeBase,
        activeMainHostRoute = activeMainHostRoute,
    )
}

internal fun isSettingsNavPopTransition(
    fromKey: BiliPaiNavKey?,
    toKey: BiliPaiNavKey?,
    activeMainHostRoute: String? = null,
): Boolean {
    return resolveSettingsNavPopTransition(
        fromKey = fromKey,
        toKey = toKey,
        activeMainHostRoute = activeMainHostRoute,
    ) != null
}
