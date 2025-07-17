package com.yodgorbek.jellyapp.presentation.camera

import android.Manifest
import android.content.pm.PackageManager
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = koinViewModel(),
    navController: NavController
) {
    val context = LocalContext.current

    val frontSurfaceView = remember { SurfaceView(context) }
    val backSurfaceView = remember { SurfaceView(context) }

    var isFrontSurfaceReady by remember { mutableStateOf(false) }
    var isBackSurfaceReady by remember { mutableStateOf(false) }
    var isInitDone by remember { mutableStateOf(false) }

    val isRecording by viewModel.isRecording.collectAsState()
    val elapsed by viewModel.elapsedSeconds.collectAsState()
    val isFrontCameraReady by viewModel.isFrontCameraReady.collectAsState()
    val isBackCameraReady by viewModel.isBackCameraReady.collectAsState()

    val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.all { it.value }
        if (allGranted) {
            if (isFrontSurfaceReady && isBackSurfaceReady && !isInitDone) {
                isInitDone = true
                viewModel.initCameras(context, frontSurfaceView.holder, backSurfaceView.holder)
            }
        } else {
            Toast.makeText(context, "Camera and Audio permissions are required", Toast.LENGTH_LONG).show()
        }
    }

    // Attach surface callbacks
    DisposableEffect(Unit) {
        val frontCallback = object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                isFrontSurfaceReady = true
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                isFrontSurfaceReady = false
            }
        }
        val backCallback = object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                isBackSurfaceReady = true
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                isBackSurfaceReady = false
            }
        }

        frontSurfaceView.holder.addCallback(frontCallback)
        backSurfaceView.holder.addCallback(backCallback)

        onDispose {
            frontSurfaceView.holder.removeCallback(frontCallback)
            backSurfaceView.holder.removeCallback(backCallback)
        }
    }

    // Launch effect to init camera once surfaces and permissions are ready
    LaunchedEffect(isFrontSurfaceReady, isBackSurfaceReady) {
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted && isFrontSurfaceReady && isBackSurfaceReady && !isInitDone) {
            isInitDone = true
            viewModel.initCameras(context, frontSurfaceView.holder, backSurfaceView.holder)
        } else if (!allGranted) {
            permissionLauncher.launch(permissions)
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎥 Dual Camera",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    Text(
                        text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }

                // Camera previews
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    CameraPreviewBox(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        previewView = frontSurfaceView,
                        label = "Front Cam",
                        resolution = "1920x1080",
                        fps = 30,
                        isRecording = isRecording,
                        elapsedSeconds = elapsed
                    )
                    CameraPreviewBox(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        previewView = backSurfaceView,
                        label = "Back Cam",
                        resolution = "1920x1080",
                        fps = 30,
                        isRecording = isRecording,
                        elapsedSeconds = elapsed
                    )
                }
            }

            // Record button
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                IconButton(
                    onClick = {
                        val hasAllPermissions = permissions.all {
                            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                        }

                        if (!isFrontCameraReady || !isBackCameraReady) {
                            Toast.makeText(context, "Cameras not ready", Toast.LENGTH_SHORT).show()
                        } else if (hasAllPermissions) {
                            viewModel.startRecording(context) {
                                navController.navigate("gallery")
                            }
                        } else {
                            permissionLauncher.launch(permissions)
                        }
                    },
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .size(72.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isRecording) Color.Red else Color.Gray,
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun CameraPreviewBox(
    modifier: Modifier,
    previewView: SurfaceView,
    label: String,
    resolution: String,
    fps: Int,
    isRecording: Boolean,
    elapsedSeconds: Int
) {
    Box(
        modifier = modifier
            .padding(6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.DarkGray)
    ) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        Text(
            text = "$label: $resolution @ ${fps}fps",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        )

        if (isRecording) {
            Text(
                text = "REC 00:${elapsedSeconds.toString().padStart(2, '0')}",
                color = Color.Red,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp)
            )
        }
    }
}
