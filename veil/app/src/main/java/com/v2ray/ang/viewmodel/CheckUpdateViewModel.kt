package com.v2ray.ang.viewmodel

import android.app.Application
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.dto.CheckUpdateResult
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.UpdateCheckerManager
import com.v2ray.ang.ui.base.BaseViewModel
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CheckStatus { IDLE, CHECKING, UP_TO_DATE, UPDATE_AVAILABLE, ERROR }

class CheckUpdateViewModel(application: Application) : BaseViewModel(application) {

    private val _checkPreRelease = MutableStateFlow(
        MmkvManager.decodeSettingsBool(AppConfig.PREF_CHECK_UPDATE_PRE_RELEASE, false)
    )
    val checkPreRelease: StateFlow<Boolean> = _checkPreRelease.asStateFlow()

    private val _autoCheckUpdate = MutableStateFlow(
        MmkvManager.decodeSettingsBool(AppConfig.PREF_AUTO_CHECK_UPDATE, true)
    )
    val autoCheckUpdate: StateFlow<Boolean> = _autoCheckUpdate.asStateFlow()

    private val _updateResult = MutableStateFlow<CheckUpdateResult?>(null)
    val updateResult: StateFlow<CheckUpdateResult?> = _updateResult.asStateFlow()

    private val _status = MutableStateFlow(CheckStatus.IDLE)
    val status: StateFlow<CheckStatus> = _status.asStateFlow()

    fun toggleCheckPreRelease(enabled: Boolean) {
        _checkPreRelease.value = enabled
        MmkvManager.encodeSettings(AppConfig.PREF_CHECK_UPDATE_PRE_RELEASE, enabled)
    }

    fun toggleAutoCheckUpdate(enabled: Boolean) {
        _autoCheckUpdate.value = enabled
        MmkvManager.encodeSettings(AppConfig.PREF_AUTO_CHECK_UPDATE, enabled)
    }

    fun checkForUpdates() {
        launchLoading {
            _status.value = CheckStatus.CHECKING
            try {
                val result = UpdateCheckerManager.checkForUpdate(_checkPreRelease.value)
                if (result.hasUpdate) {
                    _updateResult.value = result
                    _status.value = CheckStatus.UPDATE_AVAILABLE
                } else {
                    _updateResult.value = null
                    _status.value = CheckStatus.UP_TO_DATE
                }
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to check for updates: ${e.message}")
                _status.value = CheckStatus.ERROR
                if (e.message == null) {
                    toastError(R.string.toast_failure)
                } else {
                    toastError(e.message.orEmpty())
                }
            }
        }
    }
}
