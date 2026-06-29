package com.android.purebilibili.feature.plugin.js

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.purebilibili.core.plugin.js.BiliPaiJsMediaItem
import com.android.purebilibili.core.plugin.js.BiliPaiJsModule
import com.android.purebilibili.core.plugin.js.BiliPaiJsPluginInstallStore
import com.android.purebilibili.core.plugin.js.BiliPaiJsRuntime
import com.android.purebilibili.core.plugin.js.ExternalMediaLaunchStore
import com.android.purebilibili.core.plugin.js.InstalledBiliPaiJsPlugin
import com.android.purebilibili.core.plugin.js.isPlayable
import com.android.purebilibili.core.plugin.js.resolveBiliPaiJsMediaStreams
import com.android.purebilibili.core.ui.rememberAppBackIcon
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiliPaiJsPluginContentScreen(
    pluginId: String,
    onBack: () -> Unit,
    onPlayMedia: (String) -> Unit
) {
    val context = LocalContext.current
    val store = remember(context) { BiliPaiJsPluginInstallStore.createDefault(context) }
    val runtime = remember(context) { BiliPaiJsRuntime(context) }
    val scope = rememberCoroutineScope()
    var installed by remember(pluginId) {
        mutableStateOf(store.listInstalledPlugins().firstOrNull { it.manifest.id == pluginId })
    }
    var selectedModule by remember(installed) {
        mutableStateOf(installed?.manifest?.modules?.firstOrNull())
    }
    val paramValues = remember(selectedModule) { mutableStateMapOf<String, String>() }
    var items by remember { mutableStateOf<List<BiliPaiJsMediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadModule(module: BiliPaiJsModule) {
        val current = installed ?: return
        selectedModule = module
        isLoading = true
        errorMessage = null
        scope.launch {
            val result = runtime.loadModuleItems(
                installed = current,
                module = module,
                paramsJson = buildParamsJson(module, paramValues)
            )
            isLoading = false
            result.onSuccess { loadedItems ->
                items = loadedItems
                errorMessage = if (loadedItems.isEmpty()) "插件没有返回媒体内容" else null
            }.onFailure { error ->
                items = emptyList()
                errorMessage = error.message ?: "插件内容加载失败"
            }
        }
    }

    LaunchedEffect(selectedModule, installed?.enabled) {
        val module = selectedModule
        val current = installed
        if (module != null && current?.enabled == true) {
            loadModule(module)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(installed?.manifest?.title ?: "JS 插件内容") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(rememberAppBackIcon(), contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        val current = installed
        when {
            current == null -> EmptyState(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                text = "插件不存在或已删除"
            )
            !current.enabled -> EmptyState(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                text = "插件已禁用，请先在插件中心启用"
            )
            else -> PluginContent(
                modifier = Modifier.padding(padding),
                installed = current,
                selectedModule = selectedModule,
                paramValues = paramValues,
                mediaItems = items,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onSelectModule = { module -> loadModule(module) },
                onReload = { selectedModule?.let(::loadModule) },
                onPlayItem = { item ->
                    val streams = resolveBiliPaiJsMediaStreams(item)
                    if (streams.isNotEmpty()) {
                        onPlayMedia(
                            ExternalMediaLaunchStore.put(
                                title = item.title,
                                coverUrl = item.coverUrl,
                                streams = streams
                            )
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun PluginContent(
    modifier: Modifier,
    installed: InstalledBiliPaiJsPlugin,
    selectedModule: BiliPaiJsModule?,
    paramValues: MutableMap<String, String>,
    mediaItems: List<BiliPaiJsMediaItem>,
    isLoading: Boolean,
    errorMessage: String?,
    onSelectModule: (BiliPaiJsModule) -> Unit,
    onReload: () -> Unit,
    onPlayItem: (BiliPaiJsMediaItem) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = installed.manifest.description.ifBlank { "${installed.manifest.id} · v${installed.manifest.version}" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                installed.manifest.modules.forEach { module ->
                    FilterChip(
                        selected = module == selectedModule,
                        onClick = { onSelectModule(module) },
                        label = { Text(module.title) }
                    )
                }
            }
        }
        selectedModule?.params?.takeIf { it.isNotEmpty() }?.let { params ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    params.forEach { param ->
                        OutlinedTextField(
                            value = paramValues[param.name] ?: param.defaultValue,
                            onValueChange = { paramValues[param.name] = it },
                            label = { Text(param.title) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    AssistChip(onClick = onReload, label = { Text("重新加载") })
                }
            }
        }
        if (isLoading) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("正在加载插件内容")
                }
            }
        }
        errorMessage?.let { message ->
            item {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        items(flattenMediaItems(mediaItems), key = { it.id }) { item ->
            BiliPaiJsMediaItemRow(
                item = item,
                onPlay = { onPlayItem(item) }
            )
        }
    }
}

@Composable
private fun BiliPaiJsMediaItemRow(
    item: BiliPaiJsMediaItem,
    onPlay: () -> Unit
) {
    val streams = remember(item) { resolveBiliPaiJsMediaStreams(item) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = item.isPlayable, onClick = onPlay),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.coverUrl ?: item.backdropUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(width = 96.dp, height = 56.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                if (streams.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "线路 ${streams.size} 条",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (item.isPlayable) {
                Text(
                    text = "播放",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier,
    text: String
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun buildParamsJson(
    module: BiliPaiJsModule,
    values: Map<String, String>
): String {
    return buildJsonObject {
        module.params.forEach { param ->
            put(param.name, JsonPrimitive(values[param.name] ?: param.defaultValue))
        }
    }.toString()
}

private fun flattenMediaItems(items: List<BiliPaiJsMediaItem>): List<BiliPaiJsMediaItem> {
    return buildList {
        items.forEach { item ->
            add(item)
            addAll(item.childItems)
        }
    }
}
