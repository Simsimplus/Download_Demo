package io.simsim.download.demo.utils

import android.os.Build
import android.os.FileObserver
import androidx.annotation.RequiresApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import splitties.init.appCtx
import java.io.File

@RequiresApi(Build.VERSION_CODES.Q)
class CacheFileObserver(private val file: File, val onChange: (event: Int) -> Unit) :
    FileObserver(appCtx.cacheDir, ALL_EVENTS) {
    override fun onEvent(event: Int, path: String?) {
        if (path == file.name) {
            onChange(event)
        }
    }
}

fun observeCacheFileExist(file: File) = callbackFlow {
    send(file.exists())
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val observer = CacheFileObserver(file) { event: Int ->
            when (event) {
                FileObserver.CREATE, FileObserver.MODIFY, FileObserver.MOVED_TO -> trySend(true)
                FileObserver.DELETE, FileObserver.DELETE_SELF, FileObserver.MOVE_SELF -> trySend(
                    false
                )
            }
        }

        observer.startWatching()
        this.awaitClose {
            observer.stopWatching()
        }
    }
    awaitClose {
    }
}.distinctUntilChanged()
