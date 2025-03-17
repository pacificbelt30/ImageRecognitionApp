package com.example.cameras.camera

import android.util.Rational
import android.view.Surface
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

/**
 * カメラコンポーネントデータクラス
 */
public data class CameraComponents(
    val preview: androidx.camera.core.Preview,
    val cameraSelector: CameraSelector,
    val imageCapture: ImageCapture,
    val useCaseGroup: UseCaseGroup
)

/**
 * カメラコンポーネント（Preview, CameraSelector, ImageCapture）を設定
 */
public fun setupCameraComponents(
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
        .setTargetRotation(Surface.ROTATION_90)  // 明示的に回転を設定
        .build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
    preview.targetRotation = Surface.ROTATION_0

    // カメラセレクターを設定（背面カメラ）
    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .build()

    // 画像キャプチャユースケースを設定
    val imageCapture = ImageCapture.Builder()
        .setResolutionSelector(resolutionSelector)  // 同じ解像度セレクターを使用
        .setTargetRotation(Surface.ROTATION_0)      // 同じ回転を使用
        .build()

    // ビューポートとユースケースグループを設定
    val viewPort = ViewPort.Builder(
        // Rational(previewSize.height, previewSize.width),
        Rational(previewSize.width, previewSize.height), // 幅と高さを正しく指定
        Surface.ROTATION_0
    ).setScaleType(ViewPort.FILL_CENTER).build()
    
    val useCaseGroup = UseCaseGroup.Builder()
        .addUseCase(preview)
        .addUseCase(imageCapture)
        .setViewPort(viewPort)
        .build()

    return CameraComponents(preview, cameraSelector, imageCapture, useCaseGroup)
}
