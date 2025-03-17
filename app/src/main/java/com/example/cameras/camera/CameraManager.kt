package com.example.cameras.camera

import android.media.MediaActionSound
import androidx.activity.ComponentActivity
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.example.cameras.utils.getOutputDirectory

/**
  * カメラの初期化と管理を担当するクラス
  */
public class CameraManager {
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

/**
  * カメラコンポーネントの初期化
  */
public fun initializeCamera(ComponentActivity: ComponentActivity, cameraManager: CameraManager) {
    // 保存先ディレクトリの設定
    cameraManager.outputDirectory = getOutputDirectory(ComponentActivity)
    
    // サウンドの初期化
    cameraManager.initializeSound()
    
    // Executorの初期化
    cameraManager.initializeExecutor()
}
