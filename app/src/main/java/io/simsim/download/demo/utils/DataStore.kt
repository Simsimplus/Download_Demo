package io.simsim.download.demo.utils

import splitties.preferences.Preferences

object DataStore : Preferences("download") {
    var lastDownloadId by longPref("lastDownloadId", -1)
}
