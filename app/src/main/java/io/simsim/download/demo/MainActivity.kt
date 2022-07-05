package io.simsim.download.demo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.URLUtil
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.liulishuo.filedownloader.FileDownloader
import io.simsim.download.demo.ui.theme.DownloadDemoTheme
import io.simsim.download.demo.utils.NetworkConnectionMonitor
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import splitties.permissions.requestPermission
import splitties.snackbar.snackForever
import java.io.File

class MainActivity : AppCompatActivity() {
    private val vm: DownloadViewModel by viewModels()
    private var snack: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileDownloader.setup(this)
        cacheDir.deleteRecursively()
        lifecycleScope.launch {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        setContent {
            Content(vm = vm)
        }
        lifecycleScope.launch {
            NetworkConnectionMonitor.connectionFlow.collectLatest {
                if (!it) {
                    snack = window.decorView.findViewById<ViewGroup>(android.R.id.content)
                        .snackForever("网络无连接")
                } else {
                    snack?.dismiss()
                }
            }
        }
    }


    companion object {
        const val downloadUrl =
            "https://dldir1v6.qq.com/foxmail/qqmail_android_6.3.4.10153505.551_0.apk"
    }
}

@Composable
fun Content(
    vm: DownloadViewModel,
) = LocalContext.current.run {
    val uiState by vm.uiState.collectAsState()
    val buttonEnable by NetworkConnectionMonitor.connectionFlow.collectAsState(initial = false)
    var url by rememberSaveable {
        mutableStateOf(MainActivity.downloadUrl)
    }
    val fileName = URLUtil.guessFileName(url, null, null)
    val downloadPathFD = File(cacheDir, fileName).also {
        kotlin.runCatching {
            it.delete()
        }
    }.path
    val focusRequester = remember {
        FocusRequester()
    }
    DownloadDemoTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(
                    8.dp,
                    Alignment.CenterVertically
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    modifier = Modifier.focusRequester(focusRequester),
                    readOnly = !buttonEnable,
                    value = url,
                    onValueChange = {
                        url = it
                    },
                    singleLine = true,
                    trailingIcon = {
                        if (url != MainActivity.downloadUrl) {
                            IconButton(onClick = { url = MainActivity.downloadUrl }) {
                                Icon(
                                    imageVector = Icons.Rounded.Undo,
                                    contentDescription = "roll back"
                                )
                            }
                        }
                    }
                )
                OutlinedButton(enabled = buttonEnable, onClick = {
                    vm.downloadWithFD(url, downloadPathFD)
                }) {
                    Text(text = "应用内下载")
                }
                OutlinedButton(enabled = buttonEnable, onClick = {
                    Intent().apply {
                        data = Uri.parse(url)
                        action = Intent.ACTION_VIEW
                    }.also {
                        startActivity(Intent.createChooser(it, "浏览器下载"))
                    }
                }) {
                    Text(text = "浏览器下载")
                }
                OutlinedButton(enabled = buttonEnable, onClick = {
                    vm.downloadWithDM(url)
                }) {
                    Text(text = "系统下载器下载")
                }
            }
            InfoDialog(uiState = uiState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoDialog(
    uiState: UiState,
) {
    val ctx = LocalContext.current
    var showing by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(uiState) {
        showing = uiState != UiState.None
    }
    if (showing) {
        when (uiState) {
            is UiState.Downloading -> {
                Dialog(onDismissRequest = {}) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val progress = uiState.progress.coerceIn(0f, 1f)
                            Text(text = "${progress.times(100f).toInt()}%")
                            LinearProgressIndicator(
                                progress = progress
                            )
                        }
                    }
                }

            }
            is UiState.Failure -> Dialog(onDismissRequest = { showing = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = "下载失败[${uiState.throwable.localizedMessage}]"
                        )
                    }

                }
            }
            UiState.None -> {}
            is UiState.Success -> Dialog(onDismissRequest = { showing = false }) {
                LaunchedEffect(uiState) {
                    installApk(uiState.uri, ctx)
                }
                AlertDialog(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    onDismissRequest = { showing = false },
                    text = {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(
                                modifier = Modifier.align(Alignment.Center),
                                text = "下载成功",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { installApk(uiState.uri, ctx) }) {
                            Text(text = "安装")
                        }
                    }
                )
            }
            UiState.Pause -> {
                Dialog(onDismissRequest = { showing = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(modifier = Modifier.align(Alignment.Center), text = "下载暂停")
                        }
                    }
                }
            }
        }

    }
}


private fun installApk(
    uri: Uri,
    ctx: Context
) {
    val apkUri = if (URLUtil.isContentUrl(uri.toString())) {
        val tmp = File(ctx.cacheDir, "tmp.apk")
        val tmpUri =
            FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", tmp)
        ctx.contentResolver.openInputStream(uri)?.use { inputStream ->
            tmp.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        tmpUri
    } else {
        FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", uri.toFile())
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = apkUri
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        ctx.startActivity(intent)
    } else {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }
}