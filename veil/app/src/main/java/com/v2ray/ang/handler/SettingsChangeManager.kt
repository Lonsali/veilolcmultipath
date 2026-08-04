package com.v2ray.ang.handler

import com.v2ray.ang.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow

object SettingsChangeManager {
    private val _restartService = MutableStateFlow(false)
    private val _setupGroupTab = MutableStateFlow(false)

    // Keys that affect only UI behavior and do not require core service restart.
    private val uiOnlyKeys = setOf(
        AppConfig.PREF_CONFIRM_REMOVE,
        AppConfig.PREF_DOUBLE_COLUMN_DISPLAY,
        AppConfig.PREF_GROUP_ALL_DISPLAY,
        AppConfig.PREF_LANGUAGE,
        AppConfig.PREF_UI_MODE_NIGHT,
        AppConfig.PREF_IS_BOOTED,
    )

    /**
     * Called when a setting value changes.
     * Triggers service restart if the key is not UI-only, and always refreshes UI tabs.
     */
    fun notifySettingChanged(key: String) {
        if (key !in uiOnlyKeys) {
            makeRestartService()
        }
        makeSetupGroupTab()
    }

    // Mark restartService as requiring a restart
    fun makeRestartService() {
        _restartService.value = true
    }

    // Read and clear the restartService flag
    fun consumeRestartService(): Boolean {
        val v = _restartService.value
        _restartService.value = false
        return v
    }

    // Mark reinitGroupTab as requiring tab reinitialization
    fun makeSetupGroupTab() {
        _setupGroupTab.value = true
    }

    // Read and clear the reinitGroupTab flag
    fun consumeSetupGroupTab(): Boolean {
        val v = _setupGroupTab.value
        _setupGroupTab.value = false
        return v
    }
}
