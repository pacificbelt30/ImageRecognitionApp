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

class MainActivity : ComponentActivity() {
    // 【シャッター音】メディア音の宣言
    private lateinit var sound: MediaActionSound

    // 保存先ディレクトリ関連 ----------------------------------------------------------------------
    private lateinit var outputDirectory: File

    private fun getOutputDirectory(): File {
        // Scoped storage(対象範囲別ストレージ)
        val outDir = getExternalFilesDir(null)?.path.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (outDir != null && outDir.exists()) outDir else filesDir
    }

    // 撮影後のファイルパス表示関連 ----------------------------------------------------------------------
    private lateinit var photoUri: Uri
    private var capturedMsg = mutableStateOf("")
    private var recognitionMsg = mutableStateOf("")

    private fun setCapturedMsg(uri: Uri) {
        photoUri = uri

        val msg = photoUri.toString()
        val msgTemp = msg.replace("file:///storage/emulated/0/", "内部ストレージ：")
        val msg2 = msgTemp.replace("%20", " ")

        capturedMsg.value = msg2
    }

    private fun getCapturedMsg(): String {
        return capturedMsg.value
    }

    private fun getRecognitionMsg(): String {
        return "認識結果: \n" + recognitionMsg.value
    }

    private fun setRecognitionMsg(s: String) {
        recognitionMsg.value = s
    }

    // Executorフレームワーク(並行処理ユーティリティ)のインタフェース
    private lateinit var cameraExecutor: ExecutorService

    // ライフサイクル：Activity破棄時
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        sound.release() // 【シャッター音】シャッター音インスタンス解放
    }

    // ライフサイクル：Activity生成時
    override fun onCreate(savedInstanceState: Bundle?) {
        sound = MediaActionSound() // 【シャッター音】メディア音のインスタンス生成
        sound.load(MediaActionSound.SHUTTER_CLICK) // 【シャッター音】シャッター音のロード

        outputDirectory = getOutputDirectory() // 保存先ディレクトリ
        cameraExecutor = Executors.newSingleThreadExecutor() // 単一のワーカースレッドExecutor(takePictureで撮影する時に渡す)

        super.onCreate(savedInstanceState)
        setContent {
            MainCamera(
                outputDirectory = outputDirectory,
                executor = cameraExecutor,
                setCapturedMsg = ::setCapturedMsg,
                getCapturedMsg = ::getCapturedMsg,
                getRecognitionMsg = ::getRecognitionMsg,
                setRecognitionMsg = ::setRecognitionMsg,
                sound = sound // 【シャッター音】
            )
        }
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
    // ImageCaptureユースケースを安定させる
    val imageCapture = imageCapture ?: return

    // ファイル名フォーマットはタイムスタンプ
    val filenameFormat = "yyyy-MM-dd-HH-mm-ss-SSS"
    // 保存先ファイルのオブジェクト
    val photoFile = File(
        outputDirectory,
        SimpleDateFormat(filenameFormat, Locale.JAPAN).format(System.currentTimeMillis()) + ".jpg"
    )
    // キャプチャした画像を保存する為の出力オプション。
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    // キャプチャの実行
    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            // 結果はImageCapture.OnImageSavedCallbackでコールバックされる
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                // 成功時の処理
                // println("Photo capture Succeeded: ${output.savedUri}")
                val savedUri = Uri.fromFile(photoFile)
                setCapturedMsg(savedUri)
                val path = savedUri.getPath()
                if (path == null)
                    return
                val source = ImageDecoder.createSource(File(path))
                val bitmap = ImageDecoder.decodeBitmap(source)
                CoroutineScope(Dispatchers.Main).launch {
                    val gem = Gemini()
//                    gem.InitContent(bitmap)
                    val output = gem.GetStructuredContent(bitmap)
                    val output_json = Json.decodeFromString<RecognizeObjects>(output)
                    var result = ""
                    output_json.result.forEach{ obj->
                        result += "・ " + obj.objectName + ": " + obj.reliability*100 + "%\n"
                    }
                    Log.d("GEMINI", result)
                    setRecognitionMsg(result)
                }
            }
            override fun onError(e: ImageCaptureException) {
                // 失敗時の処理
                // println("Photo capture Error: {$e}")
            }
        })
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
    setCapturedMsg: (Uri) -> Unit,
    getCapturedMsg: () -> String,
    getRecognitionMsg: () -> String,
    setRecognitionMsg: (String) -> Unit,
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
                setCapturedMsg = setCapturedMsg,
                getCapturedMsg = getCapturedMsg,
                getRecognitionMsg = getRecognitionMsg,
                setRecognitionMsg = setRecognitionMsg,
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
    setCapturedMsg: (Uri) -> Unit,
    getCapturedMsg: () -> String,
    getRecognitionMsg: () -> String,
    setRecognitionMsg: (String) -> Unit,
    sound: MediaActionSound // 【シャッター音】
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Previewユースケース
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val previewSize =
        remember(density, configuration) {
            val windowSize =
                with(density) {
                    IntSize(
                        width = configuration.screenWidthDp.dp.toPx().toInt(),
                        height = configuration.screenHeightDp.dp.toPx().toInt(),
                    )
                }
            Log.d("SIZEZ", windowSize.toString())
            calculatePreviewSize(windowSize) // 未実装
        }
    val previewView = remember { PreviewView(context).apply {
        layoutParams =
            android.view.ViewGroup.LayoutParams(
                previewSize.width,
                previewSize.height,
            )
    } }
    Log.d("SIZE", previewSize.width.toString()+", "+previewSize.height.toString())
    previewView.scaleType = PreviewView.ScaleType.FIT_CENTER
    val rs = ResolutionSelector.Builder()
//        .setResolutionStrategy(ResolutionStrategy(Size(1080, 1920), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
//        .setResolutionStrategy(ResolutionStrategy(Size(108, 192), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
        .setAspectRatioStrategy(AspectRatioStrategy(AspectRatio.RATIO_4_3, AspectRatioStrategy.FALLBACK_RULE_AUTO))
//        .setAspectRatioStrategy(AspectRatioStrategy(AspectRatio.RATIO_16_9, AspectRatioStrategy.FALLBACK_RULE_AUTO))
        .build()
    val preview = androidx.camera.core.Preview.Builder()
        .setResolutionSelector(rs)
        .build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
    preview.targetRotation = Surface.ROTATION_0

    // カメラの選択
    // 「背面カメラ」選択の例
    val lensFacing = CameraSelector.LENS_FACING_BACK
    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(lensFacing)
        .build()

    // ImageCaptureユースケース
    val imageCapture: ImageCapture = remember { ImageCapture.Builder().build() }

    val viewPort =  ViewPort.Builder(Rational(previewSize.height, previewSize.width), Surface.ROTATION_0).setScaleType(ViewPort.FILL_CENTER).build()
    val useCaseGroup = UseCaseGroup.Builder()
        .addUseCase(preview)
        .addUseCase(imageCapture)
        .setViewPort(viewPort)
        .build()

    LaunchedEffect(lensFacing) {
        val cameraProvider = context.getCameraProvider()

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
//            preview,
//            imageCapture,
            useCaseGroup
        )
    }

    // ファインダー
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier.fillMaxSize()
    ) {
        // 撮影後の保存先パスの表示
        Text(getRecognitionMsg(), fontSize = 20.sp,
            modifier = Modifier.background(
                Color.Gray
            )
                .size(previewSize.width.dp, (previewSize.height/10).dp)
                .padding(0.dp, 20.dp, 0.dp, 0.dp)
                .align(Alignment.TopCenter)
        )
        // ファインダー
        AndroidView({ previewView },
//            modifier = Modifier.fillMaxSize()
            modifier = Modifier.align(Alignment.Center)
        )

        IconButton(
            modifier = Modifier
                .size(150.dp)
                .padding(5.dp)
                .border(1.dp, Color.White),
            onClick = {
                sound.play(MediaActionSound.SHUTTER_CLICK) //【シャッター音】シャッター音を鳴らす
                takePhoto(
                    imageCapture = imageCapture,
                    outputDirectory = outputDirectory,
                    executor = executor,
                    setCapturedMsg = setCapturedMsg,
                    setRecognitionMsg = setRecognitionMsg,
                )
            },
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
        // 撮影後の保存先パスの表示
        Text(getCapturedMsg(),
            modifier = Modifier.background(
                Color.Gray
            )
        )
    }
}