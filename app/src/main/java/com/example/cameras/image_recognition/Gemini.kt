package com.example.cameras.image_recognition

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.FunctionType
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.ai.client.generativeai.type.GenerationConfig
import com.example.cameras.BuildConfig
import com.example.cameras.view.formatRecognitionResults
import com.example.cameras.view.RecognizeObjects
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

class Gemini {

    private val modelName = "gemini-2.0-flash"
    private val apiKey = BuildConfig.apiKey

    private fun createGenerativeModel(config: GenerationConfig? = null): GenerativeModel {
        return GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            generationConfig = config
        )
    }

    private fun createInputContent(prompt: String, input: Bitmap): Content {
        return content {
            text(prompt)
            image(input)
        }
    }

    suspend fun GetStructuredContent(input: Bitmap): String {
        val prompt = "If input images, describe about provided image."
        val config = generationConfig {
            responseMimeType = "application/json"
            responseSchema = Schema(
                name = "objects",
                description = "list of objects",
                type = FunctionType.ARRAY,
                items = Schema(
                    name = "object",
                    description = "A object in the image.",
                    type = FunctionType.OBJECT,
                    properties = mapOf(
                        "objectName" to Schema(
                            name = "objectName",
                            description = "Name of the object",
                            type = FunctionType.STRING,
                            nullable = false
                        ),
                        "reliability" to Schema(
                            name = "reliability",
                            description = "Reliability of objects in the image",
                            type = FunctionType.NUMBER,
                            nullable = false
                        ),
                    ),
                    required = listOf("objectName", "reliability")
                ),
            )
        }
        val generativeModel = createGenerativeModel(config)
        val inputContent = createInputContent(prompt, input)
        val response = generativeModel.generateContent(inputContent)
        val responseText = response.text
        return if (responseText != null) {
            Log.d("GEMINI", "response = $responseText")
            "{ \"result\": ${responseText.trimIndent()} }"
        } else {
            Log.d("GEMINI", "Response is NULL")
            ""
        }
    }
}

/**
 * Gemini APIを使用して、カメラで撮影された画像を対象に画像認識を実行します。
 */
public fun processAndRecognizeImage(
    photoFile: File, 
    setCapturedMsg: (Uri) -> Unit,
    setRecognitionMsg: (String) -> Unit
) {
    val savedUri = Uri.fromFile(photoFile)
    setCapturedMsg(savedUri)
    val path = savedUri.path ?: return
    
    try {
        val source = ImageDecoder.createSource(File(path))
        val bitmap = ImageDecoder.decodeBitmap(source)
        
        recognizeImageContents(bitmap, setRecognitionMsg)
    } catch (e: Exception) {
        Log.e("Camera", "Failed to process image: ${e.message}", e)
    }
}

private fun recognizeImageContents(bitmap: Bitmap, setRecognitionMsg: (String) -> Unit) {
    CoroutineScope(Dispatchers.Main).launch {
        try {
            val gem = Gemini()
            val output = gem.GetStructuredContent(bitmap)
            
            // JSONデータをパースして、認識結果を取得
            val outputJson = Json.decodeFromString<RecognizeObjects>(output)
            
            // 認識結果をフォーマット
            val result = formatRecognitionResults(outputJson)
            
            Log.d("GEMINI", result)
            setRecognitionMsg(result)
        } catch (e: Exception) {
            Log.e("GEMINI", "Recognition failed: ${e.message}", e)
            setRecognitionMsg("画像認識に失敗しました。")
        }
    }
}
