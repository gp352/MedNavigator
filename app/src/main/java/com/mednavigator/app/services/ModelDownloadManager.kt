package com.mednavigator.app.services

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.mednavigator.app.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class ModelDownloadState {
    object NotDownloaded : ModelDownloadState()
    data class Downloading(
        val progress: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : ModelDownloadState()
    object Downloaded : ModelDownloadState()
    data class Failed(val message: String) : ModelDownloadState()
}

class ModelDownloadManager(private val context: Context) {

    companion object {
        private const val TAG = "ModelDownloadManager"
        private const val POLL_DELAY_MS = 400L
    }

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.NotDownloaded)
    val downloadState: StateFlow<ModelDownloadState> = _downloadState

    private var downloadId: Long? = null

    fun getModelFile(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val modelsDir = File(dir, "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        return File(modelsDir, Constants.MODEL_FILENAME)
    }

    fun refreshState() {
        val file = getModelFile()
        if (file.exists() && file.length() > 0) {
            _downloadState.value = ModelDownloadState.Downloaded
        } else if (_downloadState.value !is ModelDownloadState.Downloading) {
            _downloadState.value = ModelDownloadState.NotDownloaded
        }
    }

    fun startDownload() {
        refreshState()
        if (_downloadState.value is ModelDownloadState.Downloaded || _downloadState.value is ModelDownloadState.Downloading) {
            return
        }

        val modelUrl = Constants.MODEL_DOWNLOAD_URL
        if (modelUrl.isBlank()) {
            _downloadState.value = ModelDownloadState.Failed("Model URL not configured")
            return
        }

        val destinationFile = getModelFile()
        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        val request = DownloadManager.Request(Uri.parse(modelUrl))
            .setTitle("Downloading MedNavigator AI model")
            .setDescription("Preparing on-device voice model")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationUri(Uri.fromFile(destinationFile))

        downloadId = downloadManager.enqueue(request)
        _downloadState.value = ModelDownloadState.Downloading(progress = 0, bytesDownloaded = 0, totalBytes = -1)

        scope.launch {
            monitorDownload()
        }
    }

    private suspend fun monitorDownload() {
        val id = downloadId ?: return
        val query = DownloadManager.Query().setFilterById(id)
        var done = false
        while (!done) {
            val cursor: Cursor = downloadManager.query(query)
            if (cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        _downloadState.value = ModelDownloadState.Downloaded
                        done = true
                    }
                    DownloadManager.STATUS_FAILED -> {
                        val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                        Log.e(TAG, "Download failed: reason=$reason")
                        _downloadState.value = ModelDownloadState.Failed("Download failed (reason: $reason)")
                        done = true
                    }
                    else -> {
                        val progress = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                        _downloadState.value = ModelDownloadState.Downloading(progress, downloaded, total)
                    }
                }
            }
            cursor.close()
            if (!done) {
                delay(POLL_DELAY_MS)
            }
        }
    }
}
