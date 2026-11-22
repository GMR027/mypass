package app.mypass

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

@Composable
fun ScannerScreen(state: UiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> permissionGranted = granted }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenHeader(
            title = stringResource(if (state.scanForPass) R.string.scan_pass else R.string.scan_title),
            onBack = viewModel::backFromScan,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MpStatusBanner(state.status, state.error)

            if (!permissionGranted) {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MpIconTile(icon = Icons.Filled.Info, size = 56)
                        Text(
                            text = stringResource(R.string.camera_permission),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MpColors.OnSurfaceVariant,
                        )
                        MpButton(
                            text = stringResource(R.string.allow_camera),
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                CameraCapture(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    forPass = state.scanForPass,
                    onCaptured = viewModel::addScan,
                    onError = { message -> viewModel.showError(message) },
                )
            }
        }
    }
}

@Composable
private fun CameraCapture(
    modifier: Modifier = Modifier,
    forPass: Boolean = false,
    onCaptured: (File) -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var capturing by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, previewView) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        val listener = Runnable {
            runCatching {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    capture,
                )
                imageCapture = capture
            }.onFailure {
                onError(it.message ?: context.getString(R.string.error_open_camera))
            }
        }
        providerFuture.addListener(listener, executor)

        onDispose {
            runCatching { providerFuture.get().unbindAll() }
            imageCapture = null
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(Color.Black),
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.82f)
                    .aspectRatio(1.35f)
                    .border(
                        width = 2.dp,
                        color = Color.White.copy(alpha = 0.85f),
                        shape = MaterialTheme.shapes.medium,
                    ),
            )
            Text(
                text = stringResource(R.string.scanner_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(18.dp),
            )
        }

        MpButton(
            text = stringResource(
                when {
                    capturing -> R.string.capturing
                    forPass -> R.string.capture_pass
                    else -> R.string.capture_document
                },
            ),
            onClick = {
                val capture = imageCapture ?: return@MpButton
                capturing = true
                val file = File(context.cacheDir, "scan-${System.currentTimeMillis()}.jpg")
                val options = ImageCapture.OutputFileOptions.Builder(file).build()
                capture.takePicture(
                    options,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            capturing = false
                            onCaptured(file)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            capturing = false
                            onError(exception.message ?: context.getString(R.string.error_take_photo))
                        }
                    },
                )
            },
            enabled = imageCapture != null && !capturing,
            loading = capturing,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
