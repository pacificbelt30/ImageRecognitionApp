package com.example.cameras.view

import android.net.Uri
import androidx.compose.runtime.mutableStateOf

/**
  * カメラUI状態の管理クラス
  */
public class CameraUiState {
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