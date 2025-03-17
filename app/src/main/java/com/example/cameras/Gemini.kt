package com.example.cameras

import android.graphics.Bitmap
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.FunctionType
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig

class Gemini {
    suspend fun InitContent(input: Bitmap) {
        val generativeModel =
            GenerativeModel(
                // Specify a Gemini model appropriate for your use case
                modelName = "gemini-1.5-flash",
                // Access your API key as a Build Configuration variable (see "Set up your API key" above)
                apiKey = BuildConfig.apiKey)

        val prompt = "If input images, describe about provided image."

        val inputContent = content {
            text(prompt)
            image(input)
        }
        val response = generativeModel.generateContent(inputContent)
        val response_text = response.text
        if (response_text != null)
            Log.d("GEMINI", response_text)
        else
            Log.d("GEMINI", "Response is NULL")
    }
//    https://ai.google.dev/gemini-api/docs/get-started/tutorial?lang=android&hl=ja#generate-text-from-text-and-image-input

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
        val generativeModel =
            GenerativeModel(
                // Specify a Gemini model appropriate for your use case
                modelName = "gemini-1.5-flash",
                // Access your API key as a Build Configuration variable (see "Set up your API key" above)
                apiKey = BuildConfig.apiKey,
                generationConfig = config
            )


        val inputContent = content {
            text(prompt)
            image(input)
        }
        val response = generativeModel.generateContent(inputContent)
        val response_text = response.text
        if (response_text != null) {
            Log.d("GEMINI", "response = "+response_text)
            return "{ \"result\": " + response_text.trimIndent() + " }"
        }
        else {
            Log.d("GEMINI", "Response is NULL")
            return ""
        }
    }
}
