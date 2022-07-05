package io.simsim.download.demo

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.URLUtil
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.liulishuo.filedownloader.FileDownloader
import io.simsim.download.demo.ui.theme.DownloadDemoTheme
import kotlinx.coroutines.launch
import splitties.permissions.requestPermission
import java.io.File

class MainActivity : FragmentActivity() {
    private val vm: DownloadViewModel by viewModels()
    private val fileName = URLUtil.guessFileName(downloadUrl, null, null)
    private val downloadPathFD: String
        get() = File(cacheDir, fileName).also {
            kotlin.runCatching {
                it.delete()
            }
        }.path

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileDownloader.setup(this)
        cacheDir.deleteRecursively()
        lifecycleScope.launch {
            requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        setContent {
            DownloadDemoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by vm.uiState.collectAsState()
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(
                            8.dp,
                            Alignment.CenterVertically
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = downloadUrl,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true
                        )
                        OutlinedButton(onClick = {
                            vm.downloadWithFD(downloadUrl, downloadPathFD)
                        }) {
                            Text(text = "应用内下载")
                        }
                        OutlinedButton(onClick = {
                            Intent().apply {
                                data = Uri.parse(downloadUrl)
                                action = Intent.ACTION_VIEW
                            }.also {
                                startActivity(Intent.createChooser(it, "浏览器下载"))
                            }
                        }) {
                            Text(text = "浏览器下载")
                        }
                        OutlinedButton(onClick = {
                            vm.downloadWithDM(downloadUrl)
                        }) {
                            Text(text = "系统下载器下载")
                        }
                    }
                    InfoDialog(uiState = uiState)
                }
            }
        }
    }


    companion object {
        const val downloadUrl =
            "https://dldir1v6.qq.com/foxmail/qqmail_android_6.3.4.10153505.551_0.apk"
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
                            .heightIn(min = 100.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
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
                        .heightIn(min = 100.dp)
                ) {
                    Text(text = "下载失败[${uiState.throwable.localizedMessage}]")
                }
            }
            UiState.None -> {}
            is UiState.Success -> Dialog(onDismissRequest = { showing = false }) {
                LaunchedEffect(uiState) {
                    val uri = uiState.uri
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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp)
                ) {
                    Text(text = "下载成功")
                }
            }
            UiState.Pause -> {
                Dialog(onDismissRequest = { showing = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp)
                    ) {
                        Text(text = "下载暂停")
                    }
                }
            }
        }

    }
}