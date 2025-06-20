package com.example.atry

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import com.example.atry.ui.theme.TryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                setCameraContent()
            }
        }

        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                setCameraContent()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun setCameraContent() {
        setContent {
            TryTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CameraPreviewWithOverlay()
                }
            }
        }
    }
}

@Composable
fun CameraPreviewWithOverlay() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    var lineHeight by remember { mutableStateOf(320f) } // in dp
    var animationDuration by remember { mutableStateOf(1200f) } // in ms

    val sliderOffset = 174.dp

    LaunchedEffect(Unit) {
        cameraProvider = cameraProviderFuture.get()
    }

    LaunchedEffect(cameraSelector, cameraProvider) {
        cameraProvider?.let { provider ->
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        else
                            CameraSelector.DEFAULT_BACK_CAMERA
                    }
                )
            }
    ) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        var showInstruction by remember { mutableStateOf(true) }
        var showGuidance by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(3000)
            showInstruction = false
            showGuidance = true
            delay(5000)
            showGuidance = false
        }

        AnimatedVisibility(
            visible = showInstruction,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
        ) {
            Text(
                text = "Double tap to change the camera view",
                color = Color.White,
                modifier = Modifier
                    .padding(horizontal = 50.dp, vertical = 50.dp)
            )
        }

        AnimatedVisibility(
            visible = showGuidance,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp) // Distance from the bottom edge
        ) {
            Text(
                text = "Modify the speed first then adjust the height to see results.",
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
        }

        // Center overlay: vertical line and circles
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 450.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val lineHeightDp = lineHeight.dp

            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(lineHeightDp)
                    .background(Color.White)
            )

            LightLineAnimation(
                lineHeight = lineHeightDp,
                animationDuration = animationDuration.toInt()
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.height(lineHeightDp)
            ) {
                CircleOverlay()
                CircleOverlay()
            }
        }

        // Height Slider (left side)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = sliderOffset)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Height",
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Box(
                modifier = Modifier
                    .height(300.dp)       // track thickness after rotation
                    .width(250.dp),      // becomes height after rotation
                contentAlignment = Alignment.Center
            ) {
                Slider(
                    value = lineHeight,
                    onValueChange = { lineHeight = it },
                    valueRange = 100f..600f,
                    modifier = Modifier
                        .fillMaxWidth() // because width is the long side before rotation
                        .rotate(-90f)
                )
            }
            Text(
                text = "${lineHeight.toInt()}",
                color = Color.White,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        // Speed Slider (right side)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = -sliderOffset)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Speed",
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Box(
                modifier = Modifier
                    .height(300.dp)
                    .width(250.dp),
                contentAlignment = Alignment.Center
            ) {
                Slider(
                    value = animationDuration,
                    onValueChange = { animationDuration = it },
                    valueRange = 400f..5000f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .rotate(-90f)
                )
            }
            Text(
                text = "${(animationDuration / 1000f).format(2)} s",
                color = Color.White,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

// Helper extension
private fun Float.format(digits: Int) = "%.${digits}f".format(this)



@Composable
fun LightLineAnimation(lineHeight: Dp, animationDuration: Int) {
    val infiniteTransition = rememberInfiniteTransition()
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = with(LocalDensity.current) { lineHeight.toPx() - 40f },
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .width(6.dp)
            .height(40.dp)
            .offset(y = with(LocalDensity.current) { animatedOffset.toDp() })
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Green)
    )
}

@Composable
fun CircleOverlay() {
    Box(
        modifier = Modifier
            .size(100.dp)
            .border(3.dp, Color.White, shape = androidx.compose.foundation.shape.CircleShape)
    )
}
