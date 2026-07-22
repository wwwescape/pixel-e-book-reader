package com.wwwescape.pixelebookreader.ui.screens.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wwwescape.pixelebookreader.data.backup.BackupRepository
import com.wwwescape.pixelebookreader.data.backup.ImportMode
import com.wwwescape.pixelebookreader.data.backup.ImportSummary
import com.wwwescape.pixelebookreader.data.settings.AppSettings
import com.wwwescape.pixelebookreader.data.settings.SettingsRepository
import com.wwwescape.pixelebookreader.data.settings.ThemeContrast
import com.wwwescape.pixelebookreader.data.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val settings: StateFlow<AppSettings> = SettingsRepository.settingsFlow(application)
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { SettingsRepository.setThemeMode(getApplication(), mode) }

    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { SettingsRepository.setDynamicColor(getApplication(), enabled) }

    fun setColorThemeId(id: String?) = viewModelScope.launch { SettingsRepository.setColorThemeId(getApplication(), id) }

    fun setThemeContrast(contrast: ThemeContrast) = viewModelScope.launch { SettingsRepository.setThemeContrast(getApplication(), contrast) }

    fun setPureDark(enabled: Boolean) = viewModelScope.launch { SettingsRepository.setPureDark(getApplication(), enabled) }

    fun setAbsoluteDark(enabled: Boolean) = viewModelScope.launch { SettingsRepository.setAbsoluteDark(getApplication(), enabled) }

    fun setShowStartScreen(enabled: Boolean) = viewModelScope.launch { SettingsRepository.setShowStartScreen(getApplication(), enabled) }

    fun setDoublePressBackToExit(enabled: Boolean) = viewModelScope.launch { SettingsRepository.setDoublePressBackToExit(getApplication(), enabled) }

    fun exportBackup(destination: Uri, onResult: (Result<Unit>) -> Unit) = viewModelScope.launch {
        onResult(BackupRepository.export(getApplication(), destination))
    }

    fun importBackup(source: Uri, mode: ImportMode, onResult: (Result<ImportSummary>) -> Unit) = viewModelScope.launch {
        onResult(BackupRepository.import(getApplication(), source, mode))
    }
}
