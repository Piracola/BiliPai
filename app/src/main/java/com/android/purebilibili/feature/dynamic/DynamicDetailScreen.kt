package com.android.purebilibili.feature.dynamic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.imageLoader
import com.android.purebilibili.R
import com.android.purebilibili.core.ui.AdaptiveScaffold
import com.android.purebilibili.core.ui.AdaptiveTopAppBar
import com.android.purebilibili.core.util.responsiveContentWidth
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.repository.DynamicRepository
import com.android.purebilibili.feature.dynamic.components.DynamicCardV2
import com.android.purebilibili.feature.dynamic.components.DynamicInlineCommentComposer
import com.android.purebilibili.feature.dynamic.components.DynamicInlineCommentHeader
import com.android.purebilibili.feature.dynamic.components.DynamicSubReplyPreviewHost
import com.android.purebilibili.feature.dynamic.components.ImagePreviewDialog
import com.android.purebilibili.feature.dynamic.components.ImagePreviewTextContent
import com.android.purebilibili.feature.dynamic.components.dynamicInlineCommentItems
import com.android.purebilibili.feature.dynamic.components.RepostDialog
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

private sealed interface DynamicDetailUiState {
    data object Loading : DynamicDetailUiState
    data class Success(val item: DynamicItem) : DynamicDetailUiState
    data class Error(val message: String) : DynamicDetailUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicDetailScreen(
    dynamicId: String,
    openCommentRootRpid: Long = 0L,
    openCommentTargetRpid: Long = 0L,
    onBack: () -> Unit,
    onVideoClick: (String) -> Unit,
    onBangumiClick: (Long, Long) -> Unit = { _, _ -> },
    onUserClick: (Long) -> Unit,
    onArticleClick: (articleId: Long, title: String) -> Unit = { _, _ -> },
    onLiveClick: (roomId: Long, title: String, uname: String) -> Unit = { _, _, _ -> }
) {
    val interactionViewModel: DynamicViewModel = viewModel()
    var retryToken by rememberSaveable { mutableIntStateOf(0) }
    val screenTitle = stringResource(R.string.dynamic_detail_title)
    val backLabel = stringResource(R.string.common_back)
    val retryLabel = stringResource(R.string.common_retry)
    val loadFailedMessage = stringResource(R.string.dynamic_detail_load_failed)
    val uiState by produceState<DynamicDetailUiState>(
        initialValue = DynamicDetailUiState.Loading,
        key1 = dynamicId,
        key2 = retryToken
    ) {
        value = DynamicDetailUiState.Loading
        value = DynamicRepository.getDynamicDetail(dynamicId).fold(
            onSuccess = { item -> DynamicDetailUiState.Success(item) },
            onFailure = { error ->
                DynamicDetailUiState.Error(error.message ?: loadFailedMessage)
            }
        )
    }

    val context = LocalContext.current
    val gifImageLoader = context.imageLoader
    val likedDynamics by interactionViewModel.likedDynamics.collectAsStateWithLifecycle()
    val comments by interactionViewModel.comments.collectAsStateWithLifecycle()
    val commentsLoading by interactionViewModel.commentsLoading.collectAsStateWithLifecycle()
    val commentsLoadingMore by interactionViewModel.commentsLoadingMore.collectAsStateWithLifecycle()
    val commentTotalCount by interactionViewModel.commentTotalCount.collectAsStateWithLifecycle()
    val commentSortMode by interactionViewModel.dynamicCommentSortMode.collectAsStateWithLifecycle()
    val subReplyState by interactionViewModel.subReplyState.collectAsStateWithLifecycle()
    var showRepostDialog by remember { mutableStateOf<String?>(null) }
    val detailListState = rememberLazyListState()
    val detailScrollScope = rememberCoroutineScope()
    var showImagePreview by remember { mutableStateOf(false) }
    var previewImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var previewInitialIndex by remember { mutableIntStateOf(0) }
    var previewSourceRect by remember { mutableStateOf<Rect?>(null) }
    var previewTextContent by remember { mutableStateOf<ImagePreviewTextContent?>(null) }
    // Load comments once when detail loads (or when route targets a specific reply).
    var hasAutoOpenedComments by rememberSaveable(
        dynamicId,
        openCommentRootRpid,
        openCommentTargetRpid
    ) { mutableStateOf(false) }

    AdaptiveScaffold(
        topBar = {
            AdaptiveTopAppBar(
                title = screenTitle,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(rememberAppBackIcon(), contentDescription = backLabel)
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            DynamicDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    com.android.purebilibili.core.ui.CutePersonLoadingIndicator()
                }
            }

            is DynamicDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { retryToken++ }) {
                            Text(retryLabel)
                        }
                    }
                }
            }

            is DynamicDetailUiState.Success -> {
                LaunchedEffect(
                    state.item.id_str,
                    openCommentRootRpid,
                    openCommentTargetRpid
                ) {
                    if (shouldAutoOpenCommentsOnDynamicDetailEntry(hasAutoOpenedComments)) {
                        interactionViewModel.openCommentSheet(
                            item = state.item,
                            rootReplyId = openCommentRootRpid,
                            targetReplyId = openCommentTargetRpid
                        )
                        hasAutoOpenedComments = true
                    }
                }

                LaunchedEffect(
                    detailListState,
                    comments.size,
                    commentTotalCount,
                    commentsLoading,
                    commentsLoadingMore,
                ) {
                    snapshotFlow {
                        val lastVisibleIndex = detailListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                        val itemCount = detailListState.layoutInfo.totalItemsCount
                        itemCount > 0 && lastVisibleIndex >= itemCount - 4
                    }
                        .distinctUntilChanged()
                        .filter { it }
                        .collect {
                            if (comments.size < commentTotalCount && !commentsLoading && !commentsLoadingMore) {
                                interactionViewModel.loadMoreComments()
                            }
                        }
                }

                LazyColumn(
                    state = detailListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .responsiveContentWidth(maxWidth = resolveDynamicFeedMaxWidth()),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    item {
                        DynamicCardV2(
                            item = state.item,
                            onVideoClick = onVideoClick,
                            onBangumiClick = onBangumiClick,
                            onUserClick = onUserClick,
                            onArticleClick = onArticleClick,
                            onLiveClick = onLiveClick,
                            isDetail = true,
                            gifImageLoader = gifImageLoader,
                            onCommentClick = {
                                detailScrollScope.launch { detailListState.animateScrollToItem(1) }
                            },
                            onRepostClick = { showRepostDialog = it },
                            onLikeClick = { targetDynamicId ->
                                interactionViewModel.likeDynamic(targetDynamicId) { _, msg ->
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDeleteClick = { action ->
                                interactionViewModel.deleteDynamic(action) { success, msg ->
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                    if (success) onBack()
                                }
                            },
                            isLiked = likedDynamics.contains(state.item.id_str)
                        )
                    }
                    item(key = "dynamic_detail_comment_header") {
                        DynamicInlineCommentHeader(
                            totalCount = commentTotalCount,
                            sortMode = commentSortMode,
                            onSortModeChange = interactionViewModel::setDynamicCommentSortMode,
                        )
                    }
                    dynamicInlineCommentItems(
                        comments = comments,
                        isLoading = commentsLoading,
                        isLoadingMore = commentsLoadingMore,
                        onViewReplies = { reply -> interactionViewModel.openSubReply(reply) },
                        onImagePreview = { images, index, sourceRect, textContent ->
                            previewImages = images
                            previewInitialIndex = index
                            previewSourceRect = sourceRect
                            previewTextContent = textContent
                            showImagePreview = true
                        },
                    )
                    item(key = "dynamic_detail_comment_composer") {
                        DynamicInlineCommentComposer(
                            onPostComment = { message ->
                                interactionViewModel.postComment(state.item.id_str, message) { _, toastMessage ->
                                    android.widget.Toast.makeText(context, toastMessage, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                        )
                    }
                }

                DynamicSubReplyPreviewHost(
                    state = subReplyState,
                    onDismiss = interactionViewModel::closeSubReply,
                    onLoadMore = interactionViewModel::loadMoreSubReplies,
                )

                if (showImagePreview && previewImages.isNotEmpty()) {
                    ImagePreviewDialog(
                        images = previewImages,
                        initialIndex = previewInitialIndex,
                        sourceRect = previewSourceRect,
                        textContent = previewTextContent,
                        onDismiss = {
                            showImagePreview = false
                            previewTextContent = null
                        },
                    )
                }

                showRepostDialog?.let { repostDynamicId ->
                    RepostDialog(
                        onDismiss = { showRepostDialog = null },
                        onRepost = { content: String, onComplete: (Boolean) -> Unit ->
                            interactionViewModel.repostDynamic(repostDynamicId, content) { success, msg ->
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                if (success) showRepostDialog = null
                                onComplete(success)
                            }
                        }
                    )
                }
            }
        }
    }
}
