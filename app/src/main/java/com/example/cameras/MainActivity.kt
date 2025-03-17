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
    private val cameraManager = CameraManager()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeCamera(this, cameraManager)
        initializeUI()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraManager.release()
    }
    
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
