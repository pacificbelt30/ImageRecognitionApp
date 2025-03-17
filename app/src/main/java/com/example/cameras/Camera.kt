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