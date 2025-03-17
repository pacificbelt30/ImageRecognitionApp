# ImageRecognitionApp

Gemini AIを活用した画像認識機能を備えたAndroidカメラアプリ。

## 概要

このアプリケーションはCameraXを使用して画像を撮影し、Google Gemini AIと連携して撮影した画像内のオブジェクトを分析・特定します。認識結果はカメラプレビュー画面に直接表示されます。

## 機能

- カメラプレビューと画像撮影機能
- Gemini AIを使用したリアルタイムオブジェクト認識
- 信頼度（%）付きの認識結果表示
- カメラアクセスの権限管理

## セットアップ要件

### 前提条件
- Android Studio Electric Eel (2023.1.1)以降
- 最小SDK 35のAndroid SDK
- Kotlin 2.0.0以降
- Gradle 8.9または互換性のあるバージョン

### Gemini API キーの設定
**重要**: このアプリケーションを動作させるには、Google Gemini APIキーが必要です。

1. [Google AI Studio](https://aistudio.google.com/)からGemini APIキーを取得
2. プロジェクトルートに`local.properties`ファイルを作成または編集
3. 以下の形式でAPIキーを追加:
   ```
   apiKey="YOUR_GEMINI_API_KEY_HERE"
   ```

## プロジェクトのビルド

1. リポジトリをクローン
2. 上記の説明に従って`local.properties`にGemini APIキーを追加
3. Gradleファイルを同期
4. Androidデバイス上でアプリケーションをビルド・実行

## プロジェクト構造

- `MainActivity.kt` - アプリケーションのメインエントリーポイント
- `camera/` - カメラの初期化と管理
- `image_recognition/` - 画像分析のためのGemini API連携
- `ui/` - カメラインターフェース用のComposeUIコンポーネント
- `utils/` - ファイル操作のユーティリティ関数
- `view/` - UIステート管理とデータモデル

## 依存関係
- CameraX - カメラ機能用
- Jetpack Compose - モダンなUI実装用
- Google Generative AI - Gemini API連携用
- Kotlin coroutines - 非同期タスク用
- Accompanist permissions - カメラ権限管理用
- Kotlin serialization - JSON解析用

## ライセンス
MIT ライセンス