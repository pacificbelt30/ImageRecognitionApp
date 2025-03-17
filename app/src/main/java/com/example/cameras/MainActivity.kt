package com.example.cameras

import android.Manifest
import android.content.Context
import android.graphics.ImageDecoder
import android.media.MediaActionSound
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt

/**
 * カメラアプリケーションのメインアクティビティ
 * 
 * カメラの初期化、状態管理、UIの表示を担当
 */
class MainActivity : ComponentActivity() {
    // カメラ関連のコンポーネント
    private val cameraManager = CameraManager()
    
    // UI状態
    // private val uiState = CameraUiState()
    
    // ライフサイクル：Activity生成時
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // カメラの初期化
        initializeCamera()
        
        // UIの設定
        setupUserInterface()
    }
    
    // ライフサイクル：Activity破棄時
    override fun onDestroy() {
        super.onDestroy()
        cameraManager.release()
    }
    
    /**
     * カメラコンポーネントの初期化
     */
    private fun initializeCamera() {
        // 保存先ディレクトリの設定
        cameraManager.outputDirectory = getOutputDirectory()
        
        // サウンドの初期化
        cameraManager.initializeSound()
        
        // Executorの初期化
        cameraManager.initializeExecutor()
    }
    
    /**
     * アプリケーションのUIをセットアップ
     */
    private fun setupUserInterface() {
        setContent {
            MainCamera(
                outputDirectory = cameraManager.outputDirectory,
                executor = cameraManager.cameraExecutor,
                // uiState = uiState,
                // getCapturedMsg = uiState::getCapturedMsg,
                // setCapturedMsg = uiState::setCapturedMsg,
                // getRecognitionMsg = uiState::getRecognitionMsg,
                // setRecognitionMsg = uiState::setRecognitionMsg,
                sound = cameraManager.sound
            )
        }
    }
    
    /**
     * 保存先ディレクトリを取得
     */
    private fun getOutputDirectory(): File {
        // Scoped storage(対象範囲別ストレージ)
        val outDir = getExternalFilesDir(null)?.path.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (outDir != null && outDir.exists()) outDir else filesDir
    }
    
    /**
     * カメラの初期化と管理を担当するクラス
     */
    private inner class CameraManager {
        // カメラ関連のコンポーネント
        lateinit var outputDirectory: File
        lateinit var cameraExecutor: ExecutorService
        lateinit var sound: MediaActionSound
        
        /**
         * サウンドの初期化
         */
        fun initializeSound() {
            sound = MediaActionSound()
            sound.load(MediaActionSound.SHUTTER_CLICK)
        }
        
        /**
         * Executorの初期化
         */
        fun initializeExecutor() {
            cameraExecutor = Executors.newSingleThreadExecutor()
        }
        
        /**
         * リソースの解放
         */
        fun release() {
            cameraExecutor.shutdown()
            sound.release()
        }
    }
    
}

/**
    * カメラUI状態の管理クラス
    */
private class CameraUiState {
    // 撮影後のファイルパス表示関連
    private var photoUri: Uri? = null
    private val capturedMsg = mutableStateOf("")
    private val recognitionMsg = mutableStateOf("")
    
    /**
        * 撮影されたイメージのパスメッセージを取得
        */
    fun getCapturedMsg(): String = capturedMsg.value
    
    /**
        * 撮影されたイメージのURIを設定し、表示用メッセージを更新
        */
    fun setCapturedMsg(uri: Uri) {
        photoUri = uri
        
        val msg = uri.toString()
        val msgTemp = msg.replace("file:///storage/emulated/0/", "内部ストレージ：")
        capturedMsg.value = msgTemp.replace("%20", " ")
    }
    
    /**
        * 認識結果のメッセージを取得
        */
    fun getRecognitionMsg(): String = "認識結果: \n${recognitionMsg.value}"
    
    /**
        * 認識結果のメッセージを設定
        */
    fun setRecognitionMsg(s: String) {
        recognitionMsg.value = s
    }
}

private fun calculatePreviewSize(windowSize: IntSize): IntSize {
    val windowWidth = windowSize.width
    val windowHeight = windowSize.height

    val aspectRatio = 3f / 4f

    val newWidth: Int
    val newHeight: Int

    if (windowWidth < windowHeight) {
        newWidth = windowWidth
        newHeight = (windowWidth * (1f / aspectRatio)).roundToInt()
    } else {
        newWidth = (windowHeight * (1f / aspectRatio)).roundToInt()
        newHeight = windowHeight
    }
    Log.d("CAL", newWidth.toString())
    Log.d("CAL", windowWidth.toString())

    return IntSize(newWidth, newHeight)
}


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
 * Creates a photo file with timestamp-based name
 */
private fun createPhotoFile(outputDirectory: File): File {
    val filenameFormat = "yyyy-MM-dd-HH-mm-ss-SSS"
    val timestamp = SimpleDateFormat(filenameFormat, Locale.JAPAN).format(System.currentTimeMillis())
    return File(outputDirectory, "$timestamp.jpg")
}

/**
 * Processes the captured image and performs object recognition
 */
private fun processImageAndRecognize(
    photoFile: File, 
    setCapturedMsg: (Uri) -> Unit,
    setRecognitionMsg: (String) -> Unit
) {
    val savedUri = Uri.fromFile(photoFile)
    
    // Update UI with file path
    setCapturedMsg(savedUri)
    
    // Process the image for recognition
    val path = savedUri.path ?: return
    
    try {
        // Decode the bitmap from the saved file
        val source = ImageDecoder.createSource(File(path))
        val bitmap = ImageDecoder.decodeBitmap(source)
        
        // Launch image recognition in a coroutine
        recognizeImageContents(bitmap, setRecognitionMsg)
    } catch (e: Exception) {
        Log.e("Camera", "Failed to process image: ${e.message}", e)
    }
}

/**
 * Performs image recognition using Gemini API
 */
private fun recognizeImageContents(bitmap: Bitmap, setRecognitionMsg: (String) -> Unit) {
    CoroutineScope(Dispatchers.Main).launch {
        try {
            // Initialize Gemini and process the image
            val gem = Gemini()
            val output = gem.GetStructuredContent(bitmap)
            
            // Parse the JSON response
            val outputJson = Json.decodeFromString<RecognizeObjects>(output)
            
            // Format the results for display
            val result = formatRecognitionResults(outputJson)
            
            // Update the UI with the formatted results
            Log.d("GEMINI", result)
            setRecognitionMsg(result)
        } catch (e: Exception) {
            Log.e("GEMINI", "Recognition failed: ${e.message}", e)
            setRecognitionMsg("画像認識に失敗しました。")
        }
    }
}

/**
 * Formats recognition results as a human-readable string
 */
private fun formatRecognitionResults(outputJson: RecognizeObjects): String {
    val builder = StringBuilder()
    outputJson.result.forEach { obj ->
        // Format reliability as percentage with two decimal places
        val reliabilityPercent = (obj.reliability * 100).toFloat()
        builder.append("・ ${obj.objectName}: ${reliabilityPercent}%\n")
    }
    return builder.toString()
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainCamera(
    outputDirectory: File,
    executor: Executor,
    // uiState: CameraUiState,
    // getCapturedMsg: () -> String,
    // setCapturedMsg: (Uri) -> Unit,
    // getRecognitionMsg: () -> String,
    // setRecognitionMsg: (String) -> Unit,
    sound: MediaActionSound // 【シャッター音】
) {
    // 必要な権限を定義
    val permissionList = mutableListOf(Manifest.permission.CAMERA)
    // Android 9 Pie以下では「WRITE_EXTERNAL_STORAGE」の権限も必要
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    // multiplePermissionsStateのインスタンスを生成
    val multiplePermissionsState = rememberMultiplePermissionsState(permissionList)

    when {
        // 全ての権限取得済みの場合
        multiplePermissionsState.allPermissionsGranted -> {
            Log.d("BuildUI", "CreateCameraView");
            CameraView(
                outputDirectory = outputDirectory,
                executor = executor,
                // uiState = uiState,
                // getCapturedMsg = getCapturedMsg,
                // setCapturedMsg = setCapturedMsg,
                // getRecognitionMsg = getRecognitionMsg,
                // setRecognitionMsg = setRecognitionMsg,
                sound = sound // 【シャッター音】
            )
        }
        // 1度、拒否した事がある場合
        multiplePermissionsState.shouldShowRationale -> {
            Log.d("BuildUI", "Resume");
            Column {
                Text("許可を与えてください(本来、1度、拒否された場合の説明も表示)")
                Button(onClick = { multiplePermissionsState.launchMultiplePermissionRequest() }) {
                    Text("ボタン")
                }
            }
        }
        // それ以外(権限確認が未だなど)の場合
        else -> {
            Log.d("BuildUI", "Else");
            Column {
                Text("許可を与えてください")
                Button(onClick = { multiplePermissionsState.launchMultiplePermissionRequest() }) {
                    Text("ボタン")
                }
            }
        }
    }
}


@Composable
fun CameraView(
    outputDirectory: File,
    executor: Executor,
    // uiState: CameraUiState,
    // getCapturedMsg: () -> String,
    // setCapturedMsg: (Uri) -> Unit,
    // getRecognitionMsg: () -> String,
    // setRecognitionMsg: (String) -> Unit,
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

    // カメラUIを表示
    CameraUI(
        previewSize = previewSize,
        previewView = previewView,
        imageCapture = imageCapture,
        outputDirectory = outputDirectory,
        executor = executor,
        // uiState = uiState,
        // getCapturedMsg = getCapturedMsg,
        // setCapturedMsg = setCapturedMsg,
        // getRecognitionMsg = getRecognitionMsg,
        // setRecognitionMsg = setRecognitionMsg,
        sound = sound
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
        Log.d("SIZEZ", windowSize.toString())
        calculatePreviewSize(windowSize)
    }
    
    // プレビュービューを作成
    val previewView = remember { 
        PreviewView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                previewSize.width,
                previewSize.height,
            )
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }
    
    Log.d("SIZE", "${previewSize.width}, ${previewSize.height}")
    
    return Pair(previewSize, previewView)
}

/**
 * カメラコンポーネント（Preview, CameraSelector, ImageCapture）を設定
 */
private fun setupCameraComponents(
    previewSize: IntSize,
    previewView: PreviewView
): CameraComponents {
    // 解像度セレクターを設定
    val resolutionSelector = ResolutionSelector.Builder()
        .setAspectRatioStrategy(AspectRatioStrategy(AspectRatio.RATIO_4_3, AspectRatioStrategy.FALLBACK_RULE_AUTO))
        .build()
    
    // プレビューを設定
    val preview = androidx.camera.core.Preview.Builder()
        .setResolutionSelector(resolutionSelector)
        .build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
    preview.targetRotation = Surface.ROTATION_0

    // カメラセレクターを設定（背面カメラ）
    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .build()

    // 画像キャプチャユースケースを設定
    val imageCapture = ImageCapture.Builder().build()

    // ビューポートとユースケースグループを設定
    val viewPort = ViewPort.Builder(
        Rational(previewSize.height, previewSize.width),
        Surface.ROTATION_0
    ).setScaleType(ViewPort.FILL_CENTER).build()
    
    val useCaseGroup = UseCaseGroup.Builder()
        .addUseCase(preview)
        .addUseCase(imageCapture)
        .setViewPort(viewPort)
        .build()

    return CameraComponents(preview, cameraSelector, imageCapture, useCaseGroup)
}

/**
 * カメラコンポーネントデータクラス
 */
private data class CameraComponents(
    val preview: androidx.camera.core.Preview,
    val cameraSelector: CameraSelector,
    val imageCapture: ImageCapture,
    val useCaseGroup: UseCaseGroup
)

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
 * カメラUIを表示する
 */
@Composable
private fun CameraUI(
    previewSize: IntSize,
    previewView: PreviewView,
    imageCapture: ImageCapture,
    outputDirectory: File,
    executor: Executor,
    // uiState: CameraUiState,
    // getCapturedMsg: () -> String,
    // setCapturedMsg: (Uri) -> Unit,
    // getRecognitionMsg: () -> String,
    // setRecognitionMsg: (String) -> Unit,
    sound: MediaActionSound
) {
    val uiState = remember { CameraViewModel() }
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier.fillMaxSize()
    ) {
        // 認識結果表示エリア
        RecognitionResultDisplay(
            getRecognitionMsg = getRecognitionMsg,
            previewSize = previewSize
        )
        
        // カメラプレビュー
        AndroidView(
            factory = { previewView },
            modifier = Modifier.align(Alignment.Center)
        )
        
        // 撮影ボタン
        CaptureButton(
            onCapture = {
                sound.play(MediaActionSound.SHUTTER_CLICK)
                takePhoto(
                    imageCapture = imageCapture,
                    outputDirectory = outputDirectory,
                    executor = executor,
                    setCapturedMsg = uiState.setCapturedMsg,
                    setRecognitionMsg = uiState.setRecognitionMsg
                )
            }
        )
        
        // 撮影ファイルパス表示エリア
        CapturedPathDisplay(getCapturedMsg = uiState.getCapturedMsg)
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
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .size(previewSize.width.dp, (previewSize.height/10).dp)
            .align(Alignment.TopCenter),
    ) {
        Text(
            text = getRecognitionMsg(),
            fontSize = 20.sp,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Gray)
                // .size(previewSize.width.dp, (previewSize.height/10).dp)
                .padding(0.dp, 20.dp, 0.dp, 0.dp)
                // .align(Alignment.TopCenter)
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
    Text(
        text = getCapturedMsg(),
        modifier = Modifier.background(Color.Gray)
    )
}

/**
 * カメラアプリの状態管理を担当するViewModel
 */
class CameraViewModel : ViewModel() {
    // UI状態
    private var photoUri = mutableStateOf<Uri?>(null)
    val capturedMsg = mutableStateOf("")
    val recognitionMsg = mutableStateOf("")
    
    /**
     * 撮影されたイメージのURIを設定し、表示用メッセージを更新
     */
    fun setCapturedMsg(uri: Uri) {
        photoUri.value = uri
        
        val msg = uri.toString()
        val msgTemp = msg.replace("file:///storage/emulated/0/", "内部ストレージ：")
        capturedMsg.value = msgTemp.replace("%20", " ")
    }
    
    /**
     * 認識結果のメッセージを設定
     */
    fun setRecognitionMsg(s: String) {
        recognitionMsg.value = s
    }
    
    /**
     * 認識結果のメッセージを表示用にフォーマット
     */
    fun getFormattedRecognitionMsg(): String = "認識結果: \n${recognitionMsg.value}"
    
    /**
     * 画像認識処理を実行
     */
    fun processImage(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Initialize Gemini and process the image
                val gem = Gemini()
                val output = gem.GetStructuredContent(bitmap)
                
                // Parse the JSON response
                val outputJson = Json.decodeFromString<RecognizeObjects>(output)
                
                // Format the results for display
                val result = formatRecognitionResults(outputJson)
                
                // Update the UI with the formatted results
                launch(Dispatchers.Main) {
                    Log.d("GEMINI", result)
                    recognitionMsg.value = result
                }
            } catch (e: Exception) {
                Log.e("GEMINI", "Recognition failed: ${e.message}", e)
                launch(Dispatchers.Main) {
                    recognitionMsg.value = "画像認識に失敗しました。"
                }
            }
        }
    }
    
    /**
     * 認識結果を人間が読める形式にフォーマット
     */
    private fun formatRecognitionResults(outputJson: RecognizeObjects): String {
        val builder = StringBuilder()
        outputJson.result.forEach { obj ->
            // Format reliability as percentage with two decimal places
            val reliabilityPercent = (obj.reliability * 100).toFloat()
            builder.append("・ ${obj.objectName}: ${reliabilityPercent}%\n")
        }
        return builder.toString()
    }
}