package com.v2ray.ang.ui.perappproxy

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.dto.AppInfo
import com.v2ray.ang.extension.toastSuccess
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.AppDivider
import com.v2ray.ang.ui.compose.AppListItem
import com.v2ray.ang.ui.compose.AppTopBar
import com.v2ray.ang.ui.compose.SettingsSwitchItem
import com.v2ray.ang.ui.compose.ItemDivider
import com.v2ray.ang.util.Utils
import androidx.compose.ui.graphics.Color
import com.v2ray.ang.ui.compose.GlassMenuShape
import com.v2ray.ang.ui.compose.glassPanel

class PerAppProxyActivity : BaseComponentActivity() {

    private val viewModel: PerAppProxyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadApps(this)
    }

    @Composable
    override fun ScreenContent() {
        val apps by viewModel.displayedApps.collectAsStateWithLifecycle()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        val blacklist by viewModel.blacklist.collectAsStateWithLifecycle()
        val perAppProxyEnabled by viewModel.perAppProxyEnabled.collectAsStateWithLifecycle()
        val bypassApps by viewModel.bypassApps.collectAsStateWithLifecycle()

        PerAppProxyScreen(
            apps = apps,
            isLoading = isLoading,
            blacklist = blacklist,
            perAppProxyEnabled = perAppProxyEnabled,
            bypassApps = bypassApps,
            onBackClick = { finish() },
            onPerAppProxyChanged = { viewModel.setPerAppProxyEnabled(it) },
            onBypassAppsChanged = { viewModel.setBypassAppsEnabled(it) },
            onToggleApp = { viewModel.toggle(it) },
            onSearch = { viewModel.filterApps(it) },
            onSelectAll = { viewModel.selectAll() },
            onInvertSelection = { viewModel.invertSelection() },
            onSelectProxyAuto = { viewModel.selectProxyAppAuto(this) },
            onImportProxyApp = {
                val content = Utils.getClipboard(applicationContext)
                viewModel.importProxyApp(content, this)
            },
            onExportProxyApp = {
                val export = viewModel.exportProxyApp()
                Utils.setClipboard(applicationContext, export)
                toastSuccess(R.string.toast_success)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerAppProxyScreen(
    apps: List<AppInfo>,
    isLoading: Boolean,
    blacklist: Set<String>,
    perAppProxyEnabled: Boolean,
    bypassApps: Boolean,
    onBackClick: () -> Unit,
    onPerAppProxyChanged: (Boolean) -> Unit,
    onBypassAppsChanged: (Boolean) -> Unit,
    onToggleApp: (String) -> Unit,
    onSearch: (String) -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onSelectProxyAuto: () -> Unit,
    onImportProxyApp: () -> Unit,
    onExportProxyApp: () -> Unit
) {
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(searchQuery) {
        onSearch(searchQuery)
    }

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.per_app_proxy_settings),
                onBackClick = onBackClick,
                isLoading = isLoading,
                isSearchActive = showSearch,
                searchQuery = searchQuery,
                onSearchQueryChange = { query ->
                    searchQuery = query
                },
                onSearchClose = {
                    searchQuery = ""
                    showSearch = false
                },
                searchPlaceholder = stringResource(R.string.menu_item_search),
                actions = {
                    if (!showSearch) {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(
                                painterResource(R.drawable.ic_search_24dp),
                                contentDescription = stringResource(R.string.menu_item_search)
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                painterResource(R.drawable.ic_more_vert_24dp),
                                contentDescription = null
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = Color.Transparent,
                            shadowElevation = 0.dp,
                            modifier = Modifier.glassPanel(GlassMenuShape)
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_item_select_all)) },
                                onClick = { showMenu = false; onSelectAll() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_item_invert_selection)) },
                                onClick = { showMenu = false; onInvertSelection() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_item_select_proxy_app)) },
                                onClick = { showMenu = false; onSelectProxyAuto() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_item_import_proxy_app)) },
                                onClick = { showMenu = false; onImportProxyApp() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_item_export_proxy_app)) },
                                onClick = { showMenu = false; onExportProxyApp() }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Оба переключателя стояли в одной строке рядом с подписями и кнопкой
            // справки. Ширины на это не хватало: подпись забирала строку целиком, а
            // тумблеру места не оставалось - он сжимался в полоску. Теперь это
            // обычные строки настроек, такие же, как везде в приложении
            SettingsSwitchItem(
                title = stringResource(R.string.per_app_proxy_settings_enable),
                checked = perAppProxyEnabled,
                onCheckedChange = onPerAppProxyChanged
            )
            SettingsSwitchItem(
                title = stringResource(R.string.switch_bypass_apps_mode),
                summary = stringResource(R.string.summary_pref_per_app_proxy),
                checked = bypassApps,
                onCheckedChange = onBypassAppsChanged
            )
            AppDivider()

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                items(items = apps, key = { it.packageName }) { app ->
                    val checked = blacklist.contains(app.packageName)
                    AppListItem(
                        appName = app.appName,
                        packageName = app.packageName,
                        icon = app.appIcon,
                        checked = checked,
                        onCheckedChange = { onToggleApp(app.packageName) }
                    )
                    ItemDivider()
                }
            }
        }
    }
}
