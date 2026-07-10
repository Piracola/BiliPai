# 听视频音乐资料页与液态播放器设计

## 目标

在不新增播放 Service、数据库迁移或第三方依赖的前提下，把 B 站现有内容投影成一套 Apple Music 风格的“听视频”体验：

- 收藏夹对应播放列表。
- 视频合集对应专辑。
- UP 主对应歌手。
- “听视频”成为主底栏入口，并沿用现有底栏液态指示器和页面滑动。
- 播放器在紧凑屏提供清晰可见的“播放 / 歌词”液态分段栏，指示器与横向分页同步。
- 歌词在普通 LRC、逐字歌词、翻译、罗马音、重复时间、长间奏、偏移和快速跳转等场景下保持确定且可测试的对齐行为。

首版是 B 站数据的只读音乐视图，不建立独立音乐资料库，不导入歌单，也不改变收藏夹本身。

## 已选方案

采用“只读资料投影 + 现有播放队列”方案。

不直接把收藏页改名为音乐页，因为收藏页缺少专辑、歌手和播放器分页语义；也不建立持久化音乐数据库，因为那会引入双向同步、过期数据和迁移成本。新的音乐资料模型只存在于 `ListenVideoViewModel` 的内存状态中，原始数据仍由 `FavoriteRepository` 提供，播放仍由 `PlaylistManager`、`PlayerViewModel`、ExoPlayer 和现有媒体会话完成。

## 顶层导航与默认迁移

新增顶层路由 `listen_video`、Navigation3 键 `BiliPaiNavKey.ListenVideo` 和 `BottomNavItem.LISTEN_VIDEO`。

“听视频”是 MainHost `HorizontalPager` 的真实页面，不是弹窗或二级页面。点击底栏时继续调用 `MainBottomPagerState.animateToPage`，因此内容横向滑入，现有液态指示器按页索引滑入“听视频”项。系统返回键从该页返回首页，重选该项触发滚动到顶或刷新，行为与其他顶层页一致。

默认底栏顺序调整为：

1. 首页
2. 动态
3. 历史
4. 听视频
5. 我的

已有用户采用一次性兼容策略：

- 已保存底栏少于五项且从未显式配置 `LISTEN_VIDEO` 时，将“听视频”插入“我的”之前。
- 已保存五项时不替换任何项目，“听视频”出现在底栏管理的可用项目中。
- 用户之后主动隐藏“听视频”时不再自动加入。
- 底栏仍保持最多五项。

设置页、预览、MD3/iOS 图标、颜色配置、字符串资源、侧栏和 Navigation3 映射都认识新项目。

## 音乐资料模型

新增只读领域模型：

```kotlin
data class ListenVideoTrack(
    val bvid: String,
    val cid: Long,
    val title: String,
    val coverUrl: String,
    val durationMs: Long,
    val artistId: Long,
    val artistName: String,
    val artistAvatarUrl: String
)

data class ListenVideoPlaylist(
    val mediaId: Long,
    val title: String,
    val coverUrl: String,
    val trackCount: Int,
    val source: FavFolderSource
)

data class ListenVideoAlbum(
    val seasonId: Long,
    val ownerMid: Long,
    val title: String,
    val coverUrl: String,
    val trackCount: Int,
    val artistName: String
)

data class ListenVideoArtist(
    val mid: Long,
    val name: String,
    val avatarUrl: String,
    val tracks: List<ListenVideoTrack>
)
```

稳定身份规则：播放列表使用 `mediaId`，专辑使用 `seasonId`，歌手使用 UP 主 `mid`，歌曲使用 `bvid`。缺失 BVID、合集 ID 或 UP 主 MID 的对象不进入对应聚合视图；它们仍可在原收藏页使用。

## 数据加载与映射

### 播放列表

登录后先加载用户创建的收藏夹。每个有效 `FavFolder` 映射为一个播放列表，封面、标题和数量直接使用收藏夹元数据。点击播放列表后按页加载其全部视频资源；单页失败保留已加载内容并提供重试。

### 专辑

专辑来源有两类：

- 用户收藏的 `type == 21` 合集收藏夹。
- 普通收藏夹内容中 `FavoriteData.type == 21` 的视频合集资源。

两类结果按 `seasonId` 去重。点击专辑后使用 `FavoriteRepository.getFavoriteSeasonList` 分页读取合集曲目。合集拥有者映射为专辑歌手。

### 歌手

歌手由已索引曲目的 `upper.mid` 聚合。进入“歌手”页时开始构建收藏索引：最多三个收藏夹并发，每个收藏夹内部串行翻页，避免瞬间发出大量请求。索引过程中持续显示已发现的歌手和进度；取消页面或切换账号会取消旧任务。相同歌曲在多个收藏夹中出现时按 BVID 去重。

### 状态与错误

`ListenVideoUiState` 是不可变状态，包含登录状态、三个资料列表、当前分段、索引进度、局部错误和刷新状态。收藏夹目录失败显示整页重试；单个文件夹或合集失败只标记对应项目，不阻断其他内容。未登录时显示登录引导，不发起收藏请求。

## 播放数据流

从播放列表、专辑或歌手打开歌曲时：

1. 将当前容器内有效歌曲映射为 `PlaylistItem`。
2. 以所点歌曲的 BVID 解析起始索引。
3. 调用 `PlaylistManager.setExternalPlaylist(..., source = FAVORITE)`。
4. 导航到 `BiliPaiNavKey.VideoDetail(startAudio = true)`；`PlayerViewModel` 继续负责 DASH 音轨、CID 解析、MediaSession、通知、PIP、定时关闭和切歌。

不创建第二个播放器，不改变 AU 音频的 `MiniPlayerManager` 所有权。

## “听视频”资料页界面

页面顶部是标题、当前播放摘要和刷新操作。其下使用现有 `BottomBarLiquidSegmentedControl` 展示“播放列表 / 专辑 / 歌手”。分段控件和内容 `HorizontalPager` 双向同步：点击或拖动控件切页，页面滑动时指示器连续跟随 `currentPage + currentPageOffsetFraction`。

紧凑屏使用一列卡片；宽屏使用自适应网格。卡片触控目标不小于 48dp，深浅主题均使用高对比文本。真实折射不安全或视觉效果关闭时，分段栏退化为带描边的半透明高对比表面，但仍保留可见的滑动指示器。

播放器与资料页共享现有调色板背景逻辑，但资料卡片不做循环背景采样。

## 播放器液态底栏

紧凑播放器仍保留两页：播放页和歌词页。在系统导航栏上方新增“播放 / 歌词”液态分段栏：

- 指示器位置读取播放器 Pager 的连续位置，因此手势拖动时同步移动。
- 点击分段使用可取消的分页动画。
- 分段栏始终可见，队列弹层打开时除外。
- 队列、播放模式、视频、合集、定时关闭和 PIP 收入一条更清晰的玻璃功能栏；不再只依赖低对比横向文字胶囊。
- 宽屏双栏不需要“播放 / 歌词”切页，但保留同样的玻璃功能栏。
- PIP 继续只显示封面与必要播放状态。

液态材质使用现有 BottomBar/Backdrop 策略。安全渲染路径才启用实时折射；其他设备使用白色高光、边框和高对比半透明底色，确保“玻璃层”在 Android 33、36、暗色背景和关闭特效时仍可辨认。

## 歌词时间轴规范化

解析完成后增加纯 Kotlin 规范化步骤：

1. 丢弃负时间、空文本且无逐字片段的损坏行。
2. 按开始时间稳定排序。
3. 同一时间且同一文本的行去重；同一时间的不同主歌词合并为一个多行歌词块，使它们同时高亮。
4. 翻译和罗马音优先精确时间匹配；没有精确匹配时，使用 650ms 内最近且尚未占用的主歌词行，距离相同选择更早的主歌词。
5. 行结束时间取有效显式结束时间、下一行开始时间和逐字片段结束时间中的合理边界；结束时间不得早于开始时间。
6. 逐字片段按时间排序，重叠片段截断到下一片段开始，缺失结束时间使用下一片段或行结束时间。

播放位置先减去用户偏移，再解析活动歌词：

- 第一行之前没有活动歌词。
- 普通行保持活动到下一行开始。
- 有明确结束时间且后面存在长间奏时，结束后进入无活动歌词状态。
- 重叠区间选择开始时间最晚的歌词块。
- 最后一行只保持到其结束时间，不无限高亮。
- 快速前后拖动直接二分查找规范化时间轴，不依赖上一帧状态。

逐字高亮同时检查 `startTimeMs <= position < endTimeMs`，超出行范围时不继续点亮后续字。

## 歌词滚动与视觉焦点

移除当前固定 `-160` 像素滚动偏移。歌词列表使用 dp 定义的顶部焦点区，并由密度转换为像素；紧凑竖屏焦点位于可视区域约 30%，横屏与宽屏位于约 38%。当前行变化时滚动到焦点区，减少动画时立即定位。

用户手动拖动歌词列表后暂停自动跟随 3 秒；点击歌词跳转后立即恢复跟随。当前行保持清晰，相邻和远端行继续使用现有渐进高斯模糊。无活动歌词的间奏阶段保留上一个可见位置，但所有行使用非焦点样式。

偏移调整继续限制在 ±10 秒并持久化到当前歌词缓存。

## 测试与验证

新增或更新以下定向测试：

- 底栏默认迁移、已有五项保护、路由映射、Navigation3 内容角色、MainHost Pager 页和指示器目标。
- 收藏夹到播放列表、`type == 21` 到专辑、UP 主到歌手、BVID 去重、缺失身份过滤和外部播放队列起始索引。
- 未登录、目录失败、单文件夹失败、取消旧索引和部分结果保留。
- 播放器分段指示器连续位置、紧凑/宽屏/PIP 布局和玻璃降级策略。
- 歌词乱序、重复时间、多主歌词、近似翻译、重叠片段、长间奏、最后一行结束、正负偏移、快速 seek 和密度无关滚动偏移。

验证顺序：

1. 运行新增资料映射、歌词和导航策略测试。
2. 运行现有 AudioMode、MiniPlayer、PlaylistManager、BottomBar 和 Navigation3 定向测试。
3. 执行 `./gradlew :app:compileDebugKotlin`。
4. 有设备时验证底栏滑入、收藏夹播放、合集切歌、歌手聚合、锁屏通知、歌词快进/回退、Android 33/36、暗色和横屏/平板。

不执行 APK 打包任务。

## 非目标

- 不新增音乐首页推荐流、下载、导入歌单或独立音乐资料库。
- 不修改 B 站收藏夹内容，不提供本地编辑专辑或歌手元数据。
- 不新增 Gradle 依赖，不新增 Room 表。
- 不改变 AU 音频和视频音轨各自的数据加载所有权。
