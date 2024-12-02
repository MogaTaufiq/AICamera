package com.aicamera

import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import com.aicamera.ImageClassifier
import com.google.mlkit.vision.common.InputImage

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }
    var isScanning by remember { mutableStateOf(false) }
    var classificationResult by remember { mutableStateOf("") }
    var points by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // Box for layout
    Box(modifier = Modifier.fillMaxSize()) {
        // Preview View for the CameraX
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Scan Button
        Button(
            onClick = { isScanning = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text(text = "Scan")
        }

        // Scanning Overlay
        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .wrapContentSize(Alignment.Center)
            ) {
                Text(text = "Scanning...", color = Color.White, fontSize = 24.sp)
            }
        }

        // Display classification results
        if (classificationResult.isNotEmpty()) {
            Text(
                text = "Classification: $classificationResult\nPoints: $points",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                color = Color.White,
                fontSize = 20.sp
            )
        }
    }

    // Camera binding logic
    LaunchedEffect(Unit) {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        preview.setSurfaceProvider(previewView.surfaceProvider)

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(Dispatchers.IO.asExecutor()) { imageProxy ->
            if (isScanning) {
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    coroutineScope.launch {
                        classifyImage(inputImage) { result, score ->
                            classificationResult = result
                            points = score
                            isScanning = false
                        }
                    }
                }
                imageProxy.close()
            }
        }

        try {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
        } catch (exc: Exception) {
            exc.printStackTrace()
        }
    }
}

