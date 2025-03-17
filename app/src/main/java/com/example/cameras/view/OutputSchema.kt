package com.example.cameras.view

import kotlinx.serialization.Serializable

@Serializable
public data class RecognizeObjects (
    val result: List<Object>
)

@Serializable
data class Object (
    val objectName: String,
    val reliability: Double
)

/**
 * JSON 形式の出力を人間が読みやすい形式に認識結果を整形
 */
public fun formatRecognitionResults(outputJson: RecognizeObjects): String {
    val builder = StringBuilder()
    outputJson.result.forEach { obj ->
        // Format reliability as percentage with two decimal places
        val reliabilityPercent = (obj.reliability * 100).toFloat()
        builder.append("・ ${obj.objectName}: ${reliabilityPercent}%\n")
    }
    return builder.toString()
}