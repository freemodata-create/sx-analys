/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import android.annotation.SuppressLint
import android.accounts.AccountManager
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.metrolist.music.BuildConfig
import com.metrolist.music.constants.GoogleSignInEmailKey
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.YouTubeAccount
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.AccountChannelHandleKey
import com.metrolist.music.constants.AccountEmailKey
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.DataSyncIdKey
import com.metrolist.music.constants.InnerTubeAuthUserKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.VisitorDataKey
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.reportException
import com.metrolist.music.utils.safeDataStoreEdit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

private enum class LoginStage {
    Authenticating,
    CheckingAccounts,
    SelectingAccount,
    SwitchingAccount,
    Completing,
}

private data class AuthData(
    val cookie: String,
    val visitorData: String,
    val dataSyncId: String,
    val authUser: String,
)

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    isSwitchingChannel: Boolean = false,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val jsInterface = remember { LoginJsInterface() }
    var loginStage by remember { mutableStateOf(LoginStage.Authenticating) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var accounts by remember { mutableStateOf<List<YouTubeAccount>>(emptyList()) }
    var selectedAccount by remember { mutableStateOf<YouTubeAccount?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showIntro by remember { mutableStateOf(!isSwitchingChannel) }
    var loginHint by remember { mutableStateOf<String?>(null) }
    var pendingAccount by remember { mutableStateOf<String?>(null) }
    var pendingGoogle by remember { mutableStateOf<Pair<String, String>?>(null) }
    val accountChooser =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val name = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (result.resultCode == Activity.RESULT_OK && !name.isNullOrBlank()) {
                pendingAccount = name
            }
        }

    suspend fun extractAuthData(webView: WebView?): AuthData? {
        val view = webView ?: return null
        var hasAuthCookie = false
        var hasVisitorData = false
        repeat(20) {
            val cookie = CookieManager.getInstance().getCookie("https://music.youtube.com").orEmpty()
            val cookieMap = runCatching { parseCookieString(cookie) }.getOrDefault(emptyMap())
            val visitorDataDeferred = CompletableDeferred<String?>()
            val dataSyncIdDeferred = CompletableDeferred<String?>()
            val authUserDeferred = CompletableDeferred<String?>()
            jsInterface.onVisitorDataReceived = { visitorData ->
                if (!visitorDataDeferred.isCompleted) visitorDataDeferred.complete(visitorData)
            }
            jsInterface.onDataSyncIdReceived = { dataSyncId ->
                if (!dataSyncIdDeferred.isCompleted) dataSyncIdDeferred.complete(dataSyncId)
            }
            jsInterface.onAuthUserReceived = { authUser ->
                if (!authUserDeferred.isCompleted) authUserDeferred.complete(authUser)
            }

            view.loadUrl(
                "javascript:Android.onRetrieveVisitorData(" +
                    "window.yt&&window.yt.config_?window.yt.config_.VISITOR_DATA:null)",
            )
            view.loadUrl(
                "javascript:Android.onRetrieveDataSyncId(" +
                    "window.yt&&window.yt.config_?window.yt.config_.DATASYNC_ID:null)",
            )
            view.loadUrl(
                "javascript:Android.onRetrieveAuthUser(" +
                    "window.yt&&window.yt.config_?String(window.yt.config_.SESSION_INDEX||0):'0')",
            )

            val pageAuthData =
                withTimeoutOrNull(1_000) {
                    WebAuthData(
                        visitorData = visitorDataDeferred.await(),
                        dataSyncId = dataSyncIdDeferred.await(),
                        authUser = authUserDeferred.await().orEmpty(),
                    )
                }
            hasAuthCookie = "SAPISID" in cookieMap
            val visitorData = pageAuthData?.visitorData
            hasVisitorData = !visitorData.isNullOrBlank()
            if (hasAuthCookie && pageAuthData != null && !visitorData.isNullOrBlank()) {
                return AuthData(
                    cookie = cookie,
                    visitorData = visitorData,
                    dataSyncId = pageAuthData.dataSyncId.orEmpty().substringBefore("||"),
                    authUser = pageAuthData.authUser.filter(Char::isDigit).ifBlank { "0" },
                )
            }
            delay(500)
        }

        Timber.w(
            "Login: Timed out waiting for the YouTube Music session " +
                "(authCookie=$hasAuthCookie, visitorData=$hasVisitorData)",
        )
        return null
    }

    suspend fun finalizeLogin(authData: AuthData) {
        loginStage = LoginStage.Completing
        YouTube.cookie = authData.cookie
        YouTube.visitorData = authData.visitorData
        YouTube.dataSyncId = authData.dataSyncId
        YouTube.authUser = authData.authUser

        try {
            val accountInfo = YouTube.accountInfo().getOrThrow()
            val saved =
                withContext(Dispatchers.IO) {
                    context.safeDataStoreEdit { settings ->
                        settings[InnerTubeCookieKey] = authData.cookie
                        settings[VisitorDataKey] = authData.visitorData
                        settings[DataSyncIdKey] = authData.dataSyncId
                        settings[InnerTubeAuthUserKey] = authData.authUser
                        settings[AccountNameKey] = accountInfo.name
                        settings[AccountEmailKey] = accountInfo.email.orEmpty()
                        settings[AccountChannelHandleKey] = accountInfo.channelHandle.orEmpty()
                    }
                }
            check(saved) { "Failed to persist account data" }

            webViewRef?.apply {
                stopLoading()
                clearHistory()
                clearCache(true)
                clearFormData()
            }
            CookieManager.getInstance().flush()

            withContext(Dispatchers.Main) {
                context.packageManager
                    .getLaunchIntentForPackage(context.packageName)
                    ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) }
                    ?.let(context::startActivity)
                Runtime.getRuntime().exit(0)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (throwable: Exception) {
            Timber.e(throwable, "Login: Failed to validate or save the selected YouTube channel")
            reportException(throwable)
            errorMessage =
                context.getString(
                    R.string.login_failed_with_error,
                    throwable.message ?: context.getString(R.string.error_unknown),
                )
            loginStage = LoginStage.Authenticating
        }
    }

    suspend fun checkAvailableAccounts(authData: AuthData) {
        loginStage = LoginStage.CheckingAccounts
        YouTube.cookie = authData.cookie
        YouTube.visitorData = authData.visitorData
        YouTube.dataSyncId = authData.dataSyncId
        YouTube.authUser = authData.authUser

        val availableAccounts =
            YouTube.accountsList().getOrElse { throwable ->
                Timber.e(throwable, "Login: Failed to retrieve YouTube channels")
                if (isSwitchingChannel) {
                    errorMessage = context.getString(R.string.youtube_channels_failed)
                    loginStage = LoginStage.Authenticating
                    return
                }
                emptyList()
            }

        if (isSwitchingChannel && availableAccounts.isEmpty()) {
            errorMessage = context.getString(R.string.youtube_channels_failed)
            loginStage = LoginStage.Authenticating
        } else if (availableAccounts.size > 1) {
            val currentAccount =
                availableAccounts.firstOrNull { account ->
                    account.pageId == authData.dataSyncId ||
                        account.dataSyncId?.substringBefore("||") == authData.dataSyncId
                } ?: availableAccounts.firstOrNull { it.isSelected }
            accounts = availableAccounts.map { it.copy(isSelected = it == currentAccount) }
            selectedAccount = accounts.firstOrNull { it.isSelected } ?: accounts.first()
            loginStage = LoginStage.SelectingAccount
        } else {
            finalizeLogin(authData)
        }
    }

    fun handleAuthenticatedPage() {
        when (loginStage) {
            LoginStage.Authenticating,
            LoginStage.SwitchingAccount,
            -> {
                val stage = loginStage
                loginStage = LoginStage.CheckingAccounts
                coroutineScope.launch {
                    val authData =
                        extractAuthData(webViewRef) ?: run {
                            errorMessage = context.getString(R.string.authentication_data_extraction_failed)
                            loginStage = stage
                            return@launch
                        }
                    if (stage == LoginStage.SwitchingAccount) {
                        val selectedDataSyncId = selectedAccount?.dataSyncId?.substringBefore("||")
                        finalizeLogin(
                            authData.copy(
                                dataSyncId = selectedDataSyncId?.takeIf(String::isNotBlank) ?: authData.dataSyncId,
                            ),
                        )
                    } else {
                        checkAvailableAccounts(authData)
                    }
                }
            }

            else -> Unit
        }
    }

    fun launchAccountChooser() {
        runCatching {
            accountChooser.launch(
                AccountManager.newChooseAccountIntent(null, null, arrayOf("com.google"), null, null, null, null),
            )
        }.onFailure { showIntro = false }
    }

    fun startGoogleSignIn() {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
            launchAccountChooser()
            return
        }
        coroutineScope.launch {
            try {
                val request =
                    GetCredentialRequest
                        .Builder()
                        .addCredentialOption(GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build())
                        .build()
                val credential = CredentialManager.create(context).getCredential(context, request).credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val google = GoogleIdTokenCredential.createFrom(credential.data)
                    pendingGoogle = google.id to (google.displayName ?: google.id)
                } else {
                    errorMessage = "Akun Google tidak dapat dibaca"
                }
            } catch (e: GetCredentialCancellationException) {
                Timber.d("Google sign-in cancelled")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Google sign-in failed")
                errorMessage = e.message ?: "Login Google gagal"
            }
        }
    }

    Box(
        modifier =
            Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .fillMaxSize(),
    ) {
        if (!showIntro) AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { webViewContext ->
                WebView(webViewContext).apply {
                    webViewClient =
                        object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest,
                            ): Boolean {
                                val scheme = request.url.scheme
                                if (scheme == "http" || scheme == "https") return false

                                if (scheme == "intent") {
                                    runCatching {
                                        Intent.parseUri(request.url.toString(), Intent.URI_INTENT_SCHEME)
                                            .apply {
                                                addCategory(Intent.CATEGORY_BROWSABLE)
                                                component = null
                                                selector = null
                                            }.let(context::startActivity)
                                    }
                                }
                                return true
                            }

                            override fun onPageFinished(
                                view: WebView,
                                url: String?,
                            ) {
                                val pageUri = url?.let(Uri::parse)
                                if (pageUri?.scheme == "https" && pageUri.host == "music.youtube.com") {
                                    handleAuthenticatedPage()
                                }
                            }
                        }
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                    }
                    addJavascriptInterface(jsInterface, "Android")
                    webViewRef = this
                    if (isSwitchingChannel) {
                        restoreYouTubeCookies(YouTube.cookie)
                    }
                    loadUrl(
                        if (isSwitchingChannel) {
                            "https://music.youtube.com"
                        } else {
                            val hint = loginHint?.let { "&Email=" + Uri.encode(it) + "&login_hint=" + Uri.encode(it) }.orEmpty()
                            "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com$hint"
                        },
                    )
                }
            },
        )

        if (loginStage == LoginStage.CheckingAccounts || loginStage == LoginStage.Completing) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }

        if (!showIntro) TopAppBar(
            title = {
                Text(
                    stringResource(
                        if (isSwitchingChannel) R.string.switch_youtube_channel else R.string.login,
                    ),
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain,
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            },
        )

        if (showIntro) {
            LoginIntro(
                onChooseAccount = { startGoogleSignIn() },
                onOtherAccount = {
                    loginHint = null
                    showIntro = false
                },
                onBack = if (navController.previousBackStackEntry != null) ({ navController.navigateUp() }) else null,
            )
        }
    }

    if (loginStage == LoginStage.SelectingAccount) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.choose_youtube_channel)) },
            text = {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                ) {
                    Text(
                        text = stringResource(R.string.choose_youtube_channel_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                    ) {
                        accounts.forEach { account ->
                            YouTubeAccountPickerItem(
                                account = account,
                                isSelected = account == selectedAccount,
                                onClick = { selectedAccount = account },
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = selectedAccount?.let { it.isSelected || it.signinUrl != null } == true,
                    onClick = {
                        val account = selectedAccount ?: return@Button
                        if (account.isSelected) {
                            loginStage = LoginStage.Completing
                            coroutineScope.launch {
                                val authData =
                                    extractAuthData(webViewRef) ?: run {
                                        errorMessage = context.getString(R.string.authentication_data_extraction_failed)
                                        loginStage = LoginStage.Authenticating
                                        return@launch
                                    }
                                finalizeLogin(authData)
                            }
                        } else {
                            val signinUrl = account.signinUrl ?: return@Button
                            val absoluteUrl =
                                when {
                                    signinUrl.startsWith("http://") || signinUrl.startsWith("https://") -> signinUrl
                                    signinUrl.startsWith("/") -> "https://music.youtube.com$signinUrl"
                                    else -> "https://music.youtube.com/$signinUrl"
                                }
                            loginStage = LoginStage.SwitchingAccount
                            webViewRef?.loadUrl(absoluteUrl)
                        }
                    },
                ) {
                    Text(stringResource(R.string.continue_action))
                }
            },
        )
    }

    pendingGoogle?.let { (email, name) ->
        AlertDialog(
            onDismissRequest = { pendingGoogle = null },
            title = { Text(stringResource(R.string.sx_login_confirm_title)) },
            text = { Text(stringResource(R.string.sx_login_confirm_message, email)) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingGoogle = null
                        coroutineScope.launch {
                            context.safeDataStoreEdit {
                                it[GoogleSignInEmailKey] = email
                                it[AccountNameKey] = name
                                it[AccountEmailKey] = email
                            }
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.sx_login_confirm_agree))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingGoogle = null }) {
                    Text(stringResource(R.string.sx_login_confirm_cancel))
                }
            },
        )
    }

    pendingAccount?.let { account ->
        AlertDialog(
            onDismissRequest = { pendingAccount = null },
            title = { Text(stringResource(R.string.sx_login_confirm_title)) },
            text = { Text(stringResource(R.string.sx_login_confirm_message, account)) },
            confirmButton = {
                Button(
                    onClick = {
                        loginHint = account
                        pendingAccount = null
                        showIntro = false
                    },
                ) {
                    Text(stringResource(R.string.sx_login_confirm_agree))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAccount = null }) {
                    Text(stringResource(R.string.sx_login_confirm_cancel))
                }
            },
        )
    }

    errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text(stringResource(R.string.login_failed)) },
            text = { Text(error) },
            confirmButton = {
                Button(
                    onClick = {
                        errorMessage = null
                        webViewRef?.reload()
                    },
                ) {
                    Text(stringResource(R.string.retry))
                }
            },
        )
    }

    BackHandler(
        enabled = webViewRef?.canGoBack() == true && loginStage == LoginStage.Authenticating && errorMessage == null,
    ) {
        webViewRef?.goBack()
    }
}

@Composable
private fun YouTubeAccountPickerItem(
    account: YouTubeAccount,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (account.thumbnailUrl != null) {
                AsyncImage(
                    model = account.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.account),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                account.channelHandle?.let { channelHandle ->
                    Text(
                        text = channelHandle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                account.byline?.let { byline ->
                    Text(
                        text = byline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            RadioButton(selected = isSelected, onClick = null)
        }
    }
}

private class LoginJsInterface {
    var onVisitorDataReceived: ((String?) -> Unit)? = null
    var onDataSyncIdReceived: ((String?) -> Unit)? = null
    var onAuthUserReceived: ((String?) -> Unit)? = null

    @JavascriptInterface
    fun onRetrieveVisitorData(visitorData: String?) {
        onVisitorDataReceived?.invoke(visitorData)
    }

    @JavascriptInterface
    fun onRetrieveDataSyncId(dataSyncId: String?) {
        onDataSyncIdReceived?.invoke(dataSyncId)
    }

    @JavascriptInterface
    fun onRetrieveAuthUser(authUser: String?) {
        onAuthUserReceived?.invoke(authUser)
    }
}

private data class WebAuthData(
    val visitorData: String?,
    val dataSyncId: String?,
    val authUser: String,
)

private fun restoreYouTubeCookies(cookie: String?) {
    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    cookie
        ?.let(::parseCookieString)
        .orEmpty()
        .forEach { (name, value) ->
            cookieManager.setCookie(
                "https://music.youtube.com",
                "$name=$value; Domain=.youtube.com; Path=/; Secure",
            )
        }
    cookieManager.flush()
}

@Composable
private fun LoginIntro(
    onChooseAccount: () -> Unit,
    onOtherAccount: () -> Unit,
    onBack: (() -> Unit)?,
) {
    val dark = isSystemInDarkTheme()
    val background = if (dark) Color(0xFF262421) else Color(0xFFF5F1EA)
    val textColor = if (dark) Color(0xFFF2EDE4) else Color(0xFF1F1D1A)
    val accent = Color(0xFFD97757)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(background),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                    .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Box(
                modifier =
                    Modifier
                        .size(112.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF4A2A8C), Color(0xFF1E1042))),
                        ),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_spark_fg),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().scale(1.3f),
                )
            }
            Spacer(Modifier.height(36.dp))
            Text(
                text = stringResource(R.string.sx_login_title),
                color = textColor,
                fontFamily = FontFamily.Serif,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.sx_login_subtitle),
                color = textColor.copy(alpha = 0.65f),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1.2f))
            Button(
                onClick = onChooseAccount,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(
                    text = stringResource(R.string.sx_login_continue_google),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onOtherAccount) {
                Text(
                    text = stringResource(R.string.sx_login_other_account),
                    color = textColor.copy(alpha = 0.75f),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                onLongClick = onBack,
                modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current).padding(4.dp),
            ) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null, tint = textColor)
            }
        }
    }
}
