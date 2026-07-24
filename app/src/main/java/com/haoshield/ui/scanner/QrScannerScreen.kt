package com.haoshield.ui.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.haoshield.domain.model.ShieldScanResult
import com.haoshield.ui.components.HaoBackLink
import com.haoshield.ui.theme.HaoMotion
import com.haoshield.ui.theme.HaoTheme

@Composable
fun QrScannerScreen(
    onFinished: (ShieldScanResult) -> Unit,
    onCancel: () -> Unit,
    viewModel: QrScannerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionRequested by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        permissionRequested = true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is QrScannerEvent.Finished -> onFinished(event.result)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = HaoTheme.spacing.screenH, vertical = HaoTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HaoBackLink(
            onClick = onCancel,
            modifier = Modifier.align(Alignment.Start),
            label = "Cancel",
        )

        Text(
            text = "Scan your Shield",
            modifier = Modifier.align(Alignment.Start),
            style = HaoTheme.type.display,
            color = HaoTheme.colors.ink,
        )

        Text(
            text = "Hold your printed Hǎo Shield in the frame.",
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = HaoTheme.spacing.sm, bottom = HaoTheme.spacing.lg),
            style = HaoTheme.type.body,
            color = HaoTheme.colors.inkSoft,
        )

        if (hasCameraPermission) {
            CameraPreview(
                onQrDetected = viewModel::onQrDetected,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(HaoTheme.shapes.card),
            )
        } else {
            CameraPermissionRationale(
                permissionRequested = permissionRequested,
                onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
        }

        uiState.hint?.let { hint ->
            Text(
                text = hint,
                modifier = Modifier.padding(top = HaoTheme.spacing.lg),
                style = HaoTheme.type.body,
                color = HaoTheme.colors.inkSoft,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CameraPreview(
    onQrDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraController = remember { LifecycleCameraController(context) }
    val barcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build(),
        )
    }

    DisposableEffect(lifecycleOwner) {
        val executor = ContextCompat.getMainExecutor(context)
        val analyzer = androidx.camera.mlkit.vision.MlKitAnalyzer(
            listOf(barcodeScanner),
            COORDINATE_SYSTEM_ORIGINAL,
            executor,
        ) { result ->
            val value = result?.getValue(barcodeScanner)?.firstOrNull()?.rawValue
            if (value != null) onQrDetected(value)
        }
        cameraController.setImageAnalysisAnalyzer(executor, analyzer)
        cameraController.bindToLifecycle(lifecycleOwner)

        onDispose {
            cameraController.unbind()
            barcodeScanner.close()
        }
    }

    val breathTransition = rememberInfiniteTransition(label = "reticleBreath")
    val breathAlpha by breathTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(HaoMotion.BREATH, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "reticleAlpha",
    )
    val reticleColor = HaoTheme.colors.paper

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    controller = cameraController
                }
            },
        )

        // Corner-bracket scan reticle, breathing gently above the preview. Draw-only —
        // it never intercepts touches.
        Spacer(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    val arm = 24.dp.toPx()
                    val inset = 24.dp.toPx()
                    val stroke = 2.dp.toPx()
                    val color = reticleColor.copy(alpha = breathAlpha)
                    val left = inset
                    val top = inset
                    val right = size.width - inset
                    val bottom = size.height - inset

                    // Top-left
                    drawLine(color, Offset(left, top), Offset(left + arm, top), stroke)
                    drawLine(color, Offset(left, top), Offset(left, top + arm), stroke)
                    // Top-right
                    drawLine(color, Offset(right - arm, top), Offset(right, top), stroke)
                    drawLine(color, Offset(right, top), Offset(right, top + arm), stroke)
                    // Bottom-left
                    drawLine(color, Offset(left, bottom - arm), Offset(left, bottom), stroke)
                    drawLine(color, Offset(left, bottom), Offset(left + arm, bottom), stroke)
                    // Bottom-right
                    drawLine(color, Offset(right, bottom - arm), Offset(right, bottom), stroke)
                    drawLine(color, Offset(right - arm, bottom), Offset(right, bottom), stroke)
                },
        )
    }
}

@Composable
private fun CameraPermissionRationale(
    permissionRequested: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "The camera is only used to recognize your printed Shield. " +
                "Nothing is recorded or sent anywhere.",
            style = HaoTheme.type.body,
            color = HaoTheme.colors.inkSoft,
            textAlign = TextAlign.Center,
        )
        Box(modifier = Modifier.padding(top = HaoTheme.spacing.md)) {
            if (permissionRequested) {
                TextButton(onClick = onOpenSettings) {
                    Text(text = "Open settings", color = HaoTheme.colors.ink)
                }
            } else {
                TextButton(onClick = onRequest) {
                    Text(text = "Allow camera", color = HaoTheme.colors.ink)
                }
            }
        }
    }
}
