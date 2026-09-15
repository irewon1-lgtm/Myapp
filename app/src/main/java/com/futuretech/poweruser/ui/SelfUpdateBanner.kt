package com.futuretech.poweruser.ui

import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuretech.poweruser.BuildConfig
import com.futuretech.poweruser.update.AppUpdateManager
import kotlinx.coroutines.launch
import java.io.File

private val UpdateBg = Color(0xFF07121C)
private val UpdateCyan = Color(0xFF6BE7FF)
private val UpdateViolet = Color(0xFF9B8CFF)
private val UpdateGreen = Color(0xFF59F2AE)
private val UpdateRed = Color(0xFFFF7A90)
private val UpdateMuted = Color(0xFFA7B4C8)

private enum class UpdateUiState {
    CHECKING,
    HIDDEN,
    AVAILABLE,
    DOWNLOADING,
    NEEDS_PERMISSION,
    INSTALL_READY,
    FAILED
}

/**
 * Global update strip. It stays invisible while the installed version is current.
 * When a newer successful GitHub Release exists it appears above the app and turns
 * the whole update flow into a one-tap download + Android install confirmation.
 */
@Composable
fun SelfUpdateBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(UpdateUiState.CHECKING) }
    var updateInfo by remember { mutableStateOf<AppUpdateManager.UpdateInfo?>(null) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }
    var detail by remember { mutableStateOf("") }

    fun refresh() {
        scope.launch {
            state = UpdateUiState.CHECKING
            when (val result = AppUpdateManager.checkForUpdate()) {
                is AppUpdateManager.CheckResult.Available -> {
                    updateInfo = result.info
                    detail = "v${BuildConfig.VERSION_NAME} → v${result.info.versionName}"
                    state = UpdateUiState.AVAILABLE
                }
                AppUpdateManager.CheckResult.UpToDate -> {
                    updateInfo = null
                    state = UpdateUiState.HIDDEN
                }
                is AppUpdateManager.CheckResult.Unavailable -> {
                    detail = result.reason
                    state = UpdateUiState.HIDDEN
                }
            }
        }
    }

    fun launchInstallerIfPossible() {
        val file = downloadedApk ?: return
        if (AppUpdateManager.canRequestPackageInstalls(context)) {
            state = if (AppUpdateManager.launchInstaller(context, file)) {
                UpdateUiState.INSTALL_READY
            } else {
                UpdateUiState.FAILED
            }
        } else {
            state = UpdateUiState.NEEDS_PERMISSION
        }
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        launchInstallerIfPossible()
    }

    fun startUpdate() {
        val info = updateInfo ?: return
        scope.launch {
            state = UpdateUiState.DOWNLOADING
            val file = AppUpdateManager.downloadUpdate(context, info)
            if (file == null) {
                detail = "APK 다운로드 실패"
                state = UpdateUiState.FAILED
                return@launch
            }
            downloadedApk = file
            if (AppUpdateManager.canRequestPackageInstalls(context)) {
                state = if (AppUpdateManager.launchInstaller(context, file)) {
                    UpdateUiState.INSTALL_READY
                } else {
                    UpdateUiState.FAILED
                }
            } else {
                state = UpdateUiState.NEEDS_PERMISSION
                AppUpdateManager.installPermissionIntent(context)?.let { intent ->
                    permissionLauncher.launch(intent)
                }
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    if (state == UpdateUiState.HIDDEN || state == UpdateUiState.CHECKING) return

    val info = updateInfo
    val accent = when (state) {
        UpdateUiState.AVAILABLE -> UpdateCyan
        UpdateUiState.DOWNLOADING -> UpdateViolet
        UpdateUiState.NEEDS_PERMISSION -> UpdateViolet
        UpdateUiState.INSTALL_READY -> UpdateGreen
        UpdateUiState.FAILED -> UpdateRed
        else -> UpdateCyan
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("self_update_banner"),
        color = UpdateBg,
        border = BorderStroke(1.dp, accent.copy(alpha = .35f)),
        tonalElevation = 0.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(accent.copy(alpha = .10f), Color.Transparent)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = accent.copy(alpha = .13f),
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = .35f))
            ) {
                Text(
                    "↻ UPDATE",
                    Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    fontSize = 9.sp,
                    color = accent,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = .8.sp
                )
            }

            Column(Modifier.weight(1f)) {
                Text(
                    when (state) {
                        UpdateUiState.AVAILABLE -> "새 AI CODING OS 버전이 준비됐습니다"
                        UpdateUiState.DOWNLOADING -> "검증된 APK 다운로드 중"
                        UpdateUiState.NEEDS_PERMISSION -> "설치 권한을 한 번만 허용하세요"
                        UpdateUiState.INSTALL_READY -> "Android 설치 화면에서 업데이트를 확인하세요"
                        UpdateUiState.FAILED -> "업데이트 연결에 실패했습니다"
                        else -> "업데이트"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    when (state) {
                        UpdateUiState.DOWNLOADING -> "v${info?.versionName ?: "latest"} · 다운로드 완료 후 설치 화면 자동 실행"
                        UpdateUiState.NEEDS_PERMISSION -> "이 권한은 향후 APK 업데이트 설치에 필요합니다"
                        UpdateUiState.INSTALL_READY -> "다음부터는 새 버전을 앱이 자동 감지합니다"
                        UpdateUiState.FAILED -> detail.ifBlank { "네트워크 상태를 확인하고 다시 시도하세요" }
                        else -> detail
                    },
                    fontSize = 10.sp,
                    color = UpdateMuted
                )
            }

            Button(
                onClick = {
                    when (state) {
                        UpdateUiState.AVAILABLE -> startUpdate()
                        UpdateUiState.NEEDS_PERMISSION -> {
                            val intent = AppUpdateManager.installPermissionIntent(context)
                            if (intent != null) permissionLauncher.launch(intent) else launchInstallerIfPossible()
                        }
                        UpdateUiState.INSTALL_READY -> launchInstallerIfPossible()
                        UpdateUiState.FAILED -> refresh()
                        else -> Unit
                    }
                },
                enabled = state != UpdateUiState.DOWNLOADING,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color(0xFF061018),
                    disabledContainerColor = accent.copy(alpha = .35f),
                    disabledContentColor = Color(0xFF061018)
                ),
                contentPadding = PaddingValues(horizontal = 13.dp, vertical = 7.dp)
            ) {
                Text(
                    when (state) {
                        UpdateUiState.AVAILABLE -> "UPDATE v${info?.versionName ?: ""}"
                        UpdateUiState.DOWNLOADING -> "DOWNLOADING…"
                        UpdateUiState.NEEDS_PERMISSION -> "권한 열기"
                        UpdateUiState.INSTALL_READY -> "설치 다시 열기"
                        UpdateUiState.FAILED -> "RETRY"
                        else -> "UPDATE"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
