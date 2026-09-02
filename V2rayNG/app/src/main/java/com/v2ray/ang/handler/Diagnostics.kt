package com.v2ray.ang.handler

import android.content.Context
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreNativeManager
import com.v2ray.ang.util.DeviceInfo
import com.v2ray.ang.util.HttpUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Сводка о состоянии приложения - то, что нужно поддержке, чтобы понять «не работает».
 *
 * Собирается в одном месте, а не набирается по экрану: строки на виду и текст, который
 * уходит в переписку, обязаны совпадать. Разъедутся - и разбираться будут по неверным
 * сведениям.
 *
 * Сеть здесь не трогается. Настоящий адрес до и после туннеля - вопрос отдельный, он
 * требует запроса наружу и ответа, которого может не быть; сводка же должна собираться
 * мгновенно и работать, даже когда не работает ничего.
 *
 * Секретов в сводке нет намеренно. Имя сервера и протокол - да, адрес, порт и ключи -
 * нет: этот текст человек пересылает в переписку, и превращать его в утечку доступа
 * нельзя. Что за сервер, по имени понятно и так.
 */
object Diagnostics {

    /** Строка сводки: слева подпись, справа значение. */
    data class Entry(val label: String, val value: String)

    private const val NEVER = "-"

    /**
     * Что происходит прямо сейчас: режим, сервер, подписки, DNS, маршрутизация.
     *
     * Отдельно от сведений о приложении и устройстве, потому что меняется по ходу дела,
     * а те - нет.
     */
    fun state(context: Context): List<Entry> = buildList {
        add(Entry(context.getString(R.string.title_info_mode), mode()))
        add(Entry(context.getString(R.string.title_lockdown_status), lockdown(context)))
        add(Entry(context.getString(R.string.title_info_selected_server), selectedServer(context)))
        add(Entry(context.getString(R.string.title_info_subscriptions), subscriptions(context)))
        add(Entry(context.getString(R.string.title_info_last_update), lastUpdate()))
        add(Entry(context.getString(R.string.title_info_dns), dns()))
        add(Entry(context.getString(R.string.title_info_routing), routing()))
        add(Entry(context.getString(R.string.title_info_per_app), perApp(context)))
    }

    /** Сведения о самом приложении и об устройстве - те же, что показаны строками выше. */
    fun about(context: Context): List<Entry> = listOf(
        Entry(context.getString(R.string.title_info_app_version),
            "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"),
        Entry(context.getString(R.string.title_info_commit), BuildConfig.GIT_COMMIT),
        Entry(context.getString(R.string.title_info_build),
            "${BuildConfig.DISTRIBUTION} / ${BuildConfig.BUILD_TYPE}"),
        Entry(context.getString(R.string.title_info_core_version), CoreNativeManager.getLibVersion()),
        Entry(context.getString(R.string.title_info_user_agent), HttpUtil.defaultUserAgent()),
        Entry(context.getString(R.string.title_info_android_version),
            "${DeviceInfo.osVersion} (API ${DeviceInfo.sdkInt})"),
        Entry(context.getString(R.string.title_info_device_model), DeviceInfo.model),
        Entry(context.getString(R.string.title_info_device_abi), DeviceInfo.abi),
        Entry(context.getString(R.string.title_info_hwid), DeviceInfo.hwid())
    )

    /** Вся сводка одним текстом - её и просят переслать. */
    fun asText(context: Context): String =
        (state(context) + about(context)).joinToString("\n") { "${it.label}: ${it.value}" }

    private fun mode(): String =
        MmkvManager.decodeSettingsString(AppConfig.PREF_MODE, AppConfig.VPN).orEmpty()

    private fun lockdown(context: Context): String {
        val status = LockdownStatus.remembered() ?: return NEVER
        return context.getString(
            when {
                status.isSealed -> R.string.lockdown_sealed
                status.alwaysOn -> R.string.lockdown_no_block
                else -> R.string.lockdown_off
            }
        )
    }

    /**
     * Имя и протокол выбранного сервера. Адреса и ключей здесь нет: сводку пересылают.
     */
    private fun selectedServer(context: Context): String {
        val guid = MmkvManager.getSelectServer().orEmpty()
        if (guid.isEmpty()) return context.getString(R.string.title_info_none)
        val profile = MmkvManager.decodeServerConfig(guid) ?: return context.getString(R.string.title_info_none)
        val name = profile.remarks.ifBlank { context.getString(R.string.title_info_none) }
        return "$name (${profile.configType.name})"
    }

    private fun subscriptions(context: Context): String {
        val subs = MmkvManager.decodeSubscriptions()
        if (subs.isEmpty()) return context.getString(R.string.title_info_none)
        val enabled = subs.count { it.subscription.enabled }
        return "${subs.size} / $enabled"
    }

    /** Самое свежее обновление среди всех подписок: по нему видно, живёт ли обновление вообще. */
    private fun lastUpdate(): String {
        val latest = MmkvManager.decodeSubscriptions()
            .map { cache -> cache.subscription.lastUpdated }
            .filter { time -> time > 0 }
            .maxOrNull() ?: return NEVER
        return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(latest))
    }

    private fun dns(): String {
        val remote = MmkvManager.decodeSettingsString(AppConfig.PREF_REMOTE_DNS).orEmpty()
        val domestic = MmkvManager.decodeSettingsString(AppConfig.PREF_DOMESTIC_DNS).orEmpty()
        val local = MmkvManager.decodeSettingsBool(AppConfig.PREF_LOCAL_DNS_ENABLED)
        val fake = MmkvManager.decodeSettingsBool(AppConfig.PREF_FAKE_DNS_ENABLED)
        return "$remote | $domestic | local=$local fake=$fake"
    }

    private fun routing(): String {
        val strategy = MmkvManager.decodeSettingsString(AppConfig.PREF_ROUTING_DOMAIN_STRATEGY).orEmpty()
        val sniffing = MmkvManager.decodeSettingsBool(AppConfig.PREF_SNIFFING_ENABLED, true)
        return "$strategy | sniffing=$sniffing"
    }

    private fun perApp(context: Context): String {
        if (!MmkvManager.decodeSettingsBool(AppConfig.PREF_PER_APP_PROXY)) {
            return context.getString(R.string.title_info_off)
        }
        val count = MmkvManager.decodeSettingsStringSet(AppConfig.PREF_PER_APP_PROXY_SET)?.size ?: 0
        val bypass = MmkvManager.decodeSettingsBool(AppConfig.PREF_BYPASS_APPS)
        val mode = context.getString(
            if (bypass) R.string.title_info_per_app_bypass else R.string.title_info_per_app_only
        )
        return "$mode: $count"
    }
}
