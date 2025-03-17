package com.example.cameras.ui

import android.media.MediaActionSound
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
// import com.example.cameras.ui.CameraView
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@OptIn(ExperimentalPermissionsApi::class)
@Composable
public fun MainUI(
    outputDirectory: File,
    executor: Executor,
    sound: MediaActionSound
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
                sound = sound
            )
        }
        // 1度、拒否した事がある場合
        multiplePermissionsState.shouldShowRationale -> {
            Log.d("BuildUI", "Resume");
            Column {
                Text("許可を与えてください(本来、1度、拒否された場合の説明も表示)")
                Button(onClick = { multiplePermissionsState.launchMultiplePermissionRequest() }) {
                    Text("許可する")
                }
            }
        }
        // それ以外(権限確認が未など)の場合
        else -> {
            Log.d("BuildUI", "Else");
            Column {
                Text("許可を与えてください")
                Button(onClick = { multiplePermissionsState.launchMultiplePermissionRequest() }) {
                    Text("許可する")
                }
            }
        }
    }
}
