package io.simsim.download.demo

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liulishuo.filedownloader.BaseDownloadTask
import com.liulishuo.filedownloader.FileDownloadListener
import com.liulishuo.filedownloader.FileDownloader
import io.simsim.download.demo.utils.DataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import splitties.init.appCtx
import splitties.systemservices.downloadManager
import splitties.toast.UnreliableToastApi
import splitties.toast.toast


class DownloadViewModel : ViewModel() {
    val uiState = MutableStateFlow<UiState>(UiState.None)

    fun downloadWithFD(url: String, path: String) = viewModelScope.launch {
        val task = FileDownloader.getImpl().create(url).setPath(path).setListener(
            CustomFileDownloadListener(path)
        )
        task.start()
    }

    @OptIn(UnreliableToastApi::class)
    @SuppressLint("Range")
    fun downloadWithDM(url: String) = viewModelScope.launch {
        val fileName = URLUtil.guessFileName(url, null, null)
        val mime = MimeTypeMap.getFileExtensionFromUrl(url)
        uiState.value = UiState.None

        val downloadRequest = DownloadManager.Request(
            url.toUri()
        ).setDescription(fileName)
            .setTitle(fileName)
            .setMimeType(mime)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        val downloadId = downloadManager.enqueue(downloadRequest)
        DataStore.lastDownloadId = downloadId
        appCtx.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
                if (downloadId == id) {
                    toast("download complete")
                }
            }
        }, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        var isDownloadFinished = false
        while (!isDownloadFinished) {
            delay(500)
            downloadManager.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
                if (cursor.moveToFirst()) {
                    when (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                        DownloadManager.STATUS_RUNNING -> {
                            val totalBytes =
                                cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            if (totalBytes > 0) {
                                val downloadedBytes =
                                    cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                                uiState.value =
                                    UiState.Downloading(downloadedBytes / totalBytes.toFloat())
                            }
                        }
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            val dest = downloadManager.getUriForDownloadedFile(downloadId)
                            uiState.value = UiState.Success(dest)
                            isDownloadFinished = true
                        }
                        DownloadManager.STATUS_PAUSED -> {
                            uiState.value = UiState.Pause
                        }
                        DownloadManager.STATUS_FAILED -> {
                            val reason =
                                cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
                            uiState.value = UiState.Failure(Throwable(reason))
                            isDownloadFinished = true
                        }
                        DownloadManager.STATUS_PENDING -> {
                            uiState.value = UiState.Downloading(0f)
                        }
                    }
                }
            }
        }

    }

    private inner class CustomFileDownloadListener(val destPath: String) : FileDownloadListener() {
        override fun pending(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
            uiState.value = UiState.Downloading(0f)
        }

        override fun progress(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
            uiState.value = UiState.Downloading(soFarBytes / totalBytes.toFloat())
        }

        override fun completed(task: BaseDownloadTask?) {
            uiState.value = UiState.Success(destPath.toUri())
        }

        override fun paused(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
            uiState.value = UiState.Pause
        }

        override fun error(task: BaseDownloadTask?, e: Throwable?) {
            uiState.value = UiState.Failure(e ?: Throwable(""))
        }

        override fun warn(task: BaseDownloadTask?) {
        }

    }
}

sealed class UiState {
    object None : UiState()
    class Downloading(val progress: Float) : UiState()
    object Pause : UiState()
    class Failure(val throwable: Throwable) : UiState()
    class Success(val uri: Uri) : UiState()
}