package com.example.cameras

import android.graphics.Bitmap
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.FunctionType
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.ai.client.generativeai.type.GenerationConfig

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
