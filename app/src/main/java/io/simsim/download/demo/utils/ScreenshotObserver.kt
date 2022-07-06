package io.simsim.download.demo.utils

import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import splitties.init.appCtx
import splitties.mainhandler.mainHandler

object ScreenshotObserver {
    private val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL
        )
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
    val screenshotFlow = callbackFlow {
        send(false)
        val contentObserver = object : ContentObserver(mainHandler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                uri?.let {
                    trySend(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            queryRelativeDataColumn(uri)
                        } else {
                            queryDataColumn(uri)
                        }
                    )
                }
            }
        }
        appCtx.contentResolver.registerContentObserver(imageCollection, true, contentObserver)
        awaitClose {
            appCtx.contentResolver.unregisterContentObserver(contentObserver)
        }
    }

    private fun queryDataColumn(uri: Uri): Boolean {
        val projection = arrayOf(
            MediaStore.Images.Media.DATA
        )
        var isScreenshot = false
        return appCtx.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            while (cursor.moveToNext()) {
                val path = cursor.getString(dataColumn)
                isScreenshot = path.contains("screenshot", true)
            }
            isScreenshot
        } ?: false
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun queryRelativeDataColumn(uri: Uri): Boolean {
        val projection = arrayOf(
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH
        )
        return appCtx.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            var isScreenshot = false
            val relativePathColumn =
                cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            val displayNameColumn =
                cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val name = cursor.getString(displayNameColumn)
                val relativePath = cursor.getString(relativePathColumn)
                isScreenshot = name.contains("screenshot", true) or
                    relativePath.contains("screenshot", true)
            }
            isScreenshot
        } ?: false
    }
}
