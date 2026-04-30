package com.mednavigator.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mednavigator.app.services.ModelDownloadManager
import com.mednavigator.app.services.ModelDownloadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ModelDownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val manager = ModelDownloadManager(application)

    private val _downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.NotDownloaded)
    val downloadState: StateFlow<ModelDownloadState> = _downloadState

    init {
        manager.refreshState()
        viewModelScope.launch {
            manager.downloadState.collect { state ->
                _downloadState.value = state
            }
        }
        if (_downloadState.value !is ModelDownloadState.Downloaded) {
            manager.startDownload()
        }
    }

    fun startDownload() {
        manager.startDownload()
    }

    fun isDownloaded(): Boolean {
        return _downloadState.value is ModelDownloadState.Downloaded
    }
}
