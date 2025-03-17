package com.example.cameras.ui

import android.content.Context
import android.media.MediaActionSound
import android.util.Log
import android.net.Uri
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalLifecycleOwner
import kotlin.math.roundToInt
import java.io.File
import java.util.concurrent.Executor
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.example.cameras.camera.setupCameraComponents
import com.example.cameras.utils.createPhotoFile
import com.example.cameras.image_recognition.processImageAndRecognize
import com.example.cameras.view.CameraUiState

/**
 * カメラのプレビューのサイズを計算
 */
private fun calculatePreviewSize(windowSize: IntSize): IntSize {
    val windowWidth = windowSize.width
    val windowHeight = windowSize.height

    val targetAspectRatio = 3f / 4f  // 縦方向のアプリでは3:4がよい

    // 画面幅に基づいてプレビューの高さを計算
    val previewWidth = windowWidth
    val previewHeight = (windowWidth / targetAspectRatio).roundToInt()
    
    // 高さが画面を超える場合は調整
    val finalHeight = if (previewHeight > windowHeight) {
        windowHeight
    } else {
        previewHeight
    }

    Log.d("CameraPreview", "Window size: $windowWidth x $windowHeight")
    Log.d("CameraPreview", "Preview size: $previewWidth x $finalHeight")

    return IntSize(previewWidth, finalHeight)
}

/**
 * 撮影処理を実行
 */
@OptIn(ExperimentalPermissionsApi::class)
private fun takePhoto(
    imageCapture: ImageCapture,
    outputDirectory: File,
    executor: Executor,
    setCapturedMsg: (Uri) -> Unit,
    setRecognitionMsg: (String) -> Unit
) {
    // Ensure the image capture use case is initialized
    val imageCaptureUseCase = imageCapture ?: return
    
    // Create output file with timestamp format
    val photoFile = createPhotoFile(outputDirectory)
    
    // Create output options for the capture
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    
    // Execute the capture process
    imageCaptureUseCase.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                // Process the saved image
                processImageAndRecognize(photoFile, setCapturedMsg, setRecognitionMsg)
            }
            
            override fun onError(e: ImageCaptureException) {
                Log.e("Camera", "Photo capture failed: ${e.message}", e)
            }
        }
    )
}

/**
  * カメラプレビューのサイズと表示領域を設定する
  */
@Composable
private fun configureCameraPreview(context: Context): Pair<IntSize, PreviewView> {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    
    // 画面サイズからプレビューサイズを計算
    val previewSize = remember(density, configuration) {
        val windowSize = with(density) {
            IntSize(
                width = configuration.screenWidthDp.dp.toPx().toInt(),
                height = configuration.screenHeightDp.dp.toPx().toInt(),
            )
        }
        calculatePreviewSize(windowSize)
    }
    
    // プレビュービューを作成
    val previewView = remember { 
        PreviewView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                previewSize.width,
                previewSize.height
            )
            // スケールタイプを CENTER_INSIDE に変更して、プレビューがコンテナに収まるようにする
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }
    
    return Pair(previewSize, previewView)
}

@Composable
fun CameraView(
    outputDirectory: File,
    executor: Executor,
    sound: MediaActionSound
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // カメラプレビューのサイズとビューを設定
    val (previewSize, previewView) = configureCameraPreview(context)
    
    // カメラコンポーネントを設定
    val (preview, cameraSelector, imageCapture, useCaseGroup) = setupCameraComponents(previewSize, previewView)
    
    // カメラをライフサイクルにバインド
    CameraLifecycle(lifecycleOwner, cameraSelector, useCaseGroup, context)

    // アプリのメイン画面を表示
    CameraUI(
        previewSize = previewSize,
        previewView = previewView,
        imageCapture = imageCapture,
        outputDirectory = outputDirectory,
        executor = executor,
        sound = sound
    )
}

// ProcessCameraProviderのインスタンスを返す
private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { cameraProvider ->
            cameraProvider.addListener({
                continuation.resume(cameraProvider.get())
            }, ContextCompat.getMainExecutor(this))
        }
    }


/**
  * カメラをライフサイクルにバインドする
  */
@Composable
private fun CameraLifecycle(
    lifecycleOwner: LifecycleOwner,
    cameraSelector: CameraSelector,
    useCaseGroup: UseCaseGroup,
    context: Context
) {
    LaunchedEffect(cameraSelector) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            useCaseGroup
        )
    }
}

/**
  * メインの画面
  */
@Composable
private fun CameraUI(
    previewSize: IntSize,
    previewView: PreviewView,
    imageCapture: ImageCapture,
    outputDirectory: File,
    executor: Executor,
    sound: MediaActionSound
) {
    val uiState = remember { CameraUiState() }
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier.fillMaxSize()
    ) {
        // カメラプレビュー - 明示的にサイズを指定してボックス内に配置
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(previewSize.width.dp, previewSize.height.dp)
                .background(Color.Black),  // プレビュー外の領域を黒にする
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 認識結果表示エリア
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
        ) {
            RecognitionResultDisplay(
                getRecognitionMsg = uiState::getRecognitionMsg,
                previewSize = previewSize
            )
        }
        
        // 撮影ボタン
        CaptureButton(
            onCapture = {
                sound.play(MediaActionSound.SHUTTER_CLICK)
                takePhoto(
                    imageCapture = imageCapture,
                    outputDirectory = outputDirectory,
                    executor = executor,
                    setCapturedMsg = uiState::setCapturedMsg,
                    setRecognitionMsg = uiState::setRecognitionMsg
                )
            }
        )
        
        // 撮影ファイルパス表示エリア（無くても良い）
        // CapturedPathDisplay(getCapturedMsg = uiState::getCapturedMsg)
    }
}

/**
  * 認識結果表示エリア
  */
@Composable
private fun RecognitionResultDisplay(
    getRecognitionMsg: () -> String,
    previewSize: IntSize
) {
    Box(
        contentAlignment = Alignment.TopStart,
        modifier = Modifier
            .fillMaxWidth()  // 横幅いっぱいに広げる
            .height((previewSize.height/10).dp)
            .background(Color.Gray)
            .padding(0.dp, 20.dp, 0.dp, 0.dp)
    ) {
        Text(
            text = getRecognitionMsg(),
            fontSize = 20.sp,
            color = Color.White  // テキストの視認性向上
        )
    }
}

/**
  * 撮影ボタン
  */
@Composable
private fun CaptureButton(onCapture: () -> Unit) {
    IconButton(
        modifier = Modifier
            .size(150.dp)
            .padding(5.dp)
            .border(1.dp, Color.White),
        onClick = onCapture,
        content = {
            Icon(
                imageVector = Icons.Rounded.AddCircle,
                contentDescription = "Image Capture",
                tint = Color.Gray,
                modifier = Modifier
                    .size(200.dp)
                    .padding(30.dp)
                    .border(5.dp, Color.Gray)
            )
        }
    )
}

/**
  * 撮影ファイルパス表示エリア
  */
@Composable
private fun CapturedPathDisplay(getCapturedMsg: () -> String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Gray.copy(alpha = 0.8f))
            .padding(8.dp)
    ) {
        Text(
            text = getCapturedMsg(),
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}