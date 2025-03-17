package com.example.cameras

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.cameras.camera.CameraManager
import com.example.cameras.camera.initializeCamera
import com.example.cameras.ui.MainUI

/**
 * カメラアプリケーションのメインアクティビティ
 */
class MainActivity : ComponentActivity() {
    // カメラ関連のコンポーネント
    private val cameraManager = CameraManager()
    
    // ライフサイクル：Activity生成時
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // カメラの初期化
        initializeCamera(this, cameraManager)
        
        // UIの設定
        initializeUI()
    }
    
    // ライフサイクル：Activity破棄時
    override fun onDestroy() {
        super.onDestroy()
        cameraManager.release()
    }
    
    /**
     * アプリケーションのUIをセットアップ
     */
    private fun initializeUI() {
        setContent {
            MainUI(
                outputDirectory = cameraManager.outputDirectory,
                executor = cameraManager.cameraExecutor,
                sound = cameraManager.sound
            )
        }
    }
}
