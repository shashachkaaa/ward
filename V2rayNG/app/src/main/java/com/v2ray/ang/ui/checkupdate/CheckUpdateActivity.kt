package com.v2ray.ang.ui.checkupdate

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.v2ray.ang.handler.AppUpdateInstaller
import com.v2ray.ang.handler.UpdateInstallState
import com.v2ray.ang.ui.compose.AppScreenScaffold
import com.v2ray.ang.ui.compose.AppSnackbarManager
import com.v2ray.ang.ui.compose.ReleaseNotesText
import com.v2ray.ang.ui.compose.GlassAlertDialog
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreNativeManager
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.SettingsMenuItem
import com.v2ray.ang.ui.compose.SettingsSwitchItem
import com.v2ray.ang.ui.compose.VersionInfoBlock
import com.v2ray.ang.util.Utils

class CheckUpdateActivity : BaseComponentActivity() {

    private val viewModel: CheckUpdateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun ScreenContent() {
        CheckUpdateScreen(viewModel = viewModel, onBackClick = { finish() })
    }
}

@Composable
fun CheckUpdateScreen(
    viewModel: CheckUpdateViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val checkPreRelease by viewModel.checkPreRelease.collectAsStateWithLifecycle()
    val autoCheck by viewModel.autoCheck.collectAsStateWithLifecycle()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsStateWithLifecycle()
    val updateResult by viewModel.updateResult.collectAsStateWithLifecycle()
    val installState by AppUpdateInstaller.state.collectAsStateWithLifecycle()

    val versionText = "v${BuildConfig.VERSION_NAME} (${CoreNativeManager.getLibVersion()})"

    LaunchedEffect(Unit) {
        viewModel.checkForUpdates()
    }

    // Установка запрещена системой - ведём в настройки, где её разрешают. Установка
    // закончилась - окно закрывать нечего, приложение уже перезапускается
    LaunchedEffect(installState) {
        when (installState) {
            is UpdateInstallState.NeedsPermission -> {
                AppSnackbarManager.show(context.getString(R.string.update_needs_permission))
                runCatching { context.startActivity(AppUpdateInstaller.permissionIntent(context)) }
                AppUpdateInstaller.reset()
            }

            is UpdateInstallState.Failed -> {
                AppSnackbarManager.show(context.getString(R.string.update_failed))
                AppUpdateInstaller.reset()
            }

            else -> Unit
        }
    }

    AppScreenScaffold(
        title = stringResource(R.string.update_check_for_update),
        onBackClick = onBackClick,
        isLoading = isLoading
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSwitchItem(
                icon = painterResource(R.drawable.ic_check_update_24dp),
                title = stringResource(R.string.update_auto_check),
                summary = stringResource(R.string.update_auto_check_summary),
                checked = autoCheck,
                onCheckedChange = { viewModel.toggleAutoCheck(it) }
            )
            SettingsSwitchItem(
                icon = painterResource(R.drawable.ic_source_code_24dp),
                title = stringResource(R.string.update_check_pre_release),
                checked = checkPreRelease,
                onCheckedChange = { viewModel.toggleCheckPreRelease(it) }
            )
            SettingsMenuItem(
                icon = painterResource(R.drawable.ic_check_update_24dp),
                title = stringResource(R.string.update_check_for_update),
                onClick = { viewModel.checkForUpdates() }
            )
            VersionInfoBlock(versionText = versionText)
        }
    }

    if (showUpdateDialog && updateResult != null) {
        val result = updateResult!!
        val busy = installState is UpdateInstallState.Downloading ||
                installState is UpdateInstallState.Installing

        GlassAlertDialog(
            onDismissRequest = { if (!busy) viewModel.dismissUpdateDialog() },
            title = { Text(stringResource(R.string.update_new_version_found, result.latestVersion ?: "")) },
            text = {
                Column {
                    ReleaseNotesText(result.releaseNotes.orEmpty())
                    // Пока идёт загрузка, окно остаётся открытым и показывает ход:
                    // иначе нажатие выглядело бы как «ничего не произошло»
                    if (busy) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when (val state = installState) {
                                is UpdateInstallState.Downloading ->
                                    stringResource(R.string.update_downloading, state.percent)

                                else -> stringResource(R.string.update_installing)
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                if (busy) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(18.dp)
                    )
                } else {
                    TextButton(onClick = {
                        // Ставим системным установщиком - тем же, что и автопроверка.
                        // Браузер остаётся запасным путём, если установщик не завёлся
                        viewModel.startUpdate(onFallback = { url -> Utils.openUri(context, url) })
                    }) {
                        Text(stringResource(R.string.update_now))
                    }
                }
            },
            dismissButton = {
                if (!busy) {
                    TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            }
        )
    }
}
