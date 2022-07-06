package io.simsim.download.demo.utils

import android.os.Build
import android.os.FileObserver
import androidx.annotation.RequiresApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File


@RequiresApi(Build.VERSION_CODES.Q)
class CacheFileObserver(private val file: File, val onChange: (event: Int, path: String?) -> Unit) :
    FileObserver(file, FileObserver.ALL_EVENTS) {
    override fun onEvent(event: Int, path: String?) {
        onChange(event, path)
    }
}

fun observeCacheFileExist(file: File) = callbackFlow {
    send(file.exists())
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val observer = CacheFileObserver(file) { event: Int, _: String? ->
            when (event) {
                FileObserver.CREATE, FileObserver.MODIFY -> trySend(true)
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

