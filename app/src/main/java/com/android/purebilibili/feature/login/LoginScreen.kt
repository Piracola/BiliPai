package com.android.purebilibili.feature.login

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

enum class LoginMethod {
    TV_QR,
    PASSWORD,
    SMS,
    COOKIE_IMPORT
}

internal fun resolveAvailableLoginMethods(): List<LoginMethod> = LoginMethod.entries

internal fun resolveQrLoginReason(): String {
    return "推荐使用 TV 扫码登录，可获得更完整的播放登录态并解锁高画质播放能力。"
}

private sealed interface CaptchaRequest {
    data class Sms(val phone: String) : CaptchaRequest
    data class Password(val phone: String, val password: String) : CaptchaRequest
}

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: () -> Unit,
    onClose: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedMethod by rememberSaveable { mutableStateOf(LoginMethod.TV_QR) }
    var captchaRequest by remember { mutableStateOf<CaptchaRequest?>(null) }
    var captchaManager by remember { mutableStateOf<CaptchaManager?>(null) }
    val activity = LocalActivity.current

    LaunchedEffect(state) {
        if (state is LoginState.Success) onLoginSuccess()
    }

    LaunchedEffect(selectedMethod) {
        captchaRequest = null
        viewModel.stopPolling()
        if (selectedMethod == LoginMethod.TV_QR) {
            viewModel.loadTvQrCode()
        } else {
            viewModel.resetPhoneLogin()
        }
    }

    LaunchedEffect(state, captchaRequest, activity) {
        val request = captchaRequest ?: return@LaunchedEffect
        val captchaData = (state as? LoginState.CaptchaReady)?.captchaData ?: return@LaunchedEffect
        val hostActivity = activity ?: return@LaunchedEffect
        captchaManager?.destroy()
        captchaManager = CaptchaManager(hostActivity).also { manager ->
            manager.startCaptcha(
                gt = captchaData.geetest?.gt.orEmpty(),
                challenge = captchaData.geetest?.challenge.orEmpty(),
                onSuccess = { validate, seccode, challenge ->
                    viewModel.saveCaptchaResult(validate, seccode, challenge)
                    when (request) {
                        is CaptchaRequest.Sms -> viewModel.sendSmsCode(request.phone, 86)
                        is CaptchaRequest.Password -> viewModel.loginByPassword(request.phone, request.password)
                    }
                    captchaRequest = null
                },
                onFailed = { error ->
                    captchaRequest = null
                    viewModel.showLoginError(error)
                },
                onCancel = {
                    captchaRequest = null
                    viewModel.showLoginError("已取消安全验证")
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            captchaManager?.destroy()
            viewModel.stopPolling()
        }
    }

    LoginPage(
        state = state,
        selectedMethod = selectedMethod,
        onMethodSelected = { selectedMethod = it },
        onClose = onClose,
        onRefreshQr = viewModel::loadTvQrCode,
        onRequestSms = { phone ->
            captchaRequest = CaptchaRequest.Sms(phone)
            viewModel.getCaptcha()
        },
        onSubmitSms = viewModel::loginBySms,
        onRequestPassword = { phone, password ->
            captchaRequest = CaptchaRequest.Password(phone, password)
            viewModel.getCaptcha()
        },
        onImportCookie = viewModel::loginByCookie,
        onContinueWithStandardSession = viewModel::continueWithStandardSession,
        onAuthorizeHighQuality = { selectedMethod = LoginMethod.TV_QR }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LoginPage(
    state: LoginState,
    selectedMethod: LoginMethod,
    onMethodSelected: (LoginMethod) -> Unit,
    onClose: () -> Unit,
    onRefreshQr: () -> Unit,
    onRequestSms: (String) -> Unit,
    onSubmitSms: (Int) -> Unit,
    onRequestPassword: (String, String) -> Unit,
    onImportCookie: (String) -> Unit,
    onContinueWithStandardSession: () -> Unit,
    onAuthorizeHighQuality: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        androidx.compose.material3.Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("登录") },
                    actions = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Outlined.Close, contentDescription = "关闭登录")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                        .testTag("login_scroll_content"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        LoginHeader()
                    }
                    item {
                        LoginMethodTabs(
                            selectedMethod = selectedMethod,
                            onMethodSelected = onMethodSelected
                        )
                    }
                    item {
                        LoginStateMessage(state)
                    }
                    if (state is LoginState.HighQualityAuthorization) {
                        item {
                            HighQualityAuthorizationCard(
                                onContinue = onContinueWithStandardSession,
                                onAuthorize = onAuthorizeHighQuality
                            )
                        }
                    }
                    item {
                        when (selectedMethod) {
                            LoginMethod.TV_QR -> TvQrLoginContent(state, onRefreshQr)
                            LoginMethod.PASSWORD -> PasswordLoginContent(state, onRequestPassword)
                            LoginMethod.SMS -> SmsLoginContent(state, onRequestSms, onSubmitSms)
                            LoginMethod.COOKIE_IMPORT -> CookieImportContent(state, onImportCookie)
                        }
                    }
                    item {
                        Text(
                            text = "继续即表示你同意用户协议和隐私政策。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "登录 BiliPai", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "选择一种方式继续，你的观看进度和账号信息会同步到当前设备。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoginMethodTabs(
    selectedMethod: LoginMethod,
    onMethodSelected: (LoginMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = resolveAvailableLoginMethods().indexOf(selectedMethod),
        modifier = modifier.fillMaxWidth(),
        edgePadding = 0.dp
    ) {
        resolveAvailableLoginMethods().forEach { method ->
            Tab(
                selected = method == selectedMethod,
                onClick = { onMethodSelected(method) },
                text = { Text(loginMethodLabel(method)) },
                icon = { Icon(loginMethodIcon(method), contentDescription = null) }
            )
        }
    }
}

private fun loginMethodLabel(method: LoginMethod): String = when (method) {
    LoginMethod.TV_QR -> "扫码登录"
    LoginMethod.PASSWORD -> "密码登录"
    LoginMethod.SMS -> "短信登录"
    LoginMethod.COOKIE_IMPORT -> "Cookie 导入"
}

private fun loginMethodIcon(method: LoginMethod) = when (method) {
    LoginMethod.TV_QR -> Icons.Outlined.QrCode2
    LoginMethod.PASSWORD -> Icons.Outlined.Password
    LoginMethod.SMS -> Icons.Outlined.Phone
    LoginMethod.COOKIE_IMPORT -> Icons.Outlined.ContentPaste
}

@Composable
private fun LoginStateMessage(state: LoginState, modifier: Modifier = Modifier) {
    val message = (state as? LoginState.Error)?.msg ?: return
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun HighQualityAuthorizationCard(
    onContinue: () -> Unit,
    onAuthorize: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "基础登录已完成",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "当前登录未返回高画质播放凭据。扫码可补充 1080P60、4K、HDR 等画质所需授权。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Button(onClick = onAuthorize, modifier = Modifier.fillMaxWidth()) {
                Text("扫码授权高画质")
            }
            OutlinedButton(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                Text("稍后使用")
            }
        }
    }
}

@Composable
private fun TvQrLoginContent(
    state: LoginState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "扫码登录", style = MaterialTheme.typography.titleLarge)
            Text(
                text = resolveQrLoginReason(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            when (state) {
                is LoginState.QrCode, is LoginState.Scanned -> {
                    val bitmap = when (state) {
                        is LoginState.QrCode -> state.bitmap
                        is LoginState.Scanned -> state.bitmap
                    }
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "登录二维码",
                        modifier = Modifier.size(232.dp).testTag("login_qr_code")
                    )
                    if (state is LoginState.Scanned) {
                        Text(
                            text = "已扫码，请在手机上确认登录。",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                LoginState.Loading -> CircularProgressIndicator(modifier = Modifier.size(48.dp))
                else -> Icon(
                    imageVector = Icons.Outlined.QrCode2,
                    contentDescription = null,
                    modifier = Modifier.size(232.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("刷新二维码")
            }
        }
    }
}

@Composable
private fun PasswordLoginContent(
    state: LoginState,
    onSubmit: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val isLoading = state is LoginState.Loading || state is LoginState.CaptchaReady

    LoginFormCard(title = "密码登录", modifier = modifier) {
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.filter(Char::isDigit) },
            label = { Text("手机号") },
            leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "提交前需要完成安全验证。遇到风控时请改用扫码登录。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = { onSubmit(phone, password) },
            enabled = phone.isNotBlank() && password.isNotBlank() && !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("验证并登录")
        }
    }
}

@Composable
private fun SmsLoginContent(
    state: LoginState,
    onRequestCode: (String) -> Unit,
    onSubmitCode: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var phone by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    val codeSent = state is LoginState.SmsSent
    val isLoading = state is LoginState.Loading || state is LoginState.CaptchaReady

    LoginFormCard(title = "短信验证码登录", modifier = modifier) {
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.filter(Char::isDigit) },
            label = { Text("中国大陆手机号") },
            prefix = { Text("+86 ") },
            leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (codeSent || code.isNotEmpty()) {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.filter(Char::isDigit).take(6) },
                label = { Text("短信验证码") },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            text = "发送验证码前需要完成安全验证。验证码有效期与发送频率以服务端规则为准。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (codeSent || code.isNotEmpty()) {
            Button(
                onClick = { onSubmitCode(code.toIntOrNull() ?: 0) },
                enabled = code.length == 6 && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("登录")
            }
        } else {
            Button(
                onClick = { onRequestCode(phone) },
                enabled = phone.length >= 6 && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("获取验证码")
            }
        }
    }
}

@Composable
private fun CookieImportContent(
    state: LoginState,
    onImport: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var cookieHeader by rememberSaveable { mutableStateOf("") }
    val isLoading = state is LoginState.Loading

    LoginFormCard(title = "Cookie 导入", modifier = modifier) {
        Text(
            text = "粘贴浏览器中的完整 Cookie 字符串。导入前会先验证账号，验证失败不会覆盖当前登录状态。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = cookieHeader,
            onValueChange = { cookieHeader = it },
            label = { Text("Cookie") },
            leadingIcon = { Icon(Icons.Outlined.ContentPaste, contentDescription = null) },
            minLines = 5,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { onImport(cookieHeader) },
            enabled = cookieHeader.isNotBlank() && !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("验证并导入")
        }
    }
}

@Composable
private fun LoginFormCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}
