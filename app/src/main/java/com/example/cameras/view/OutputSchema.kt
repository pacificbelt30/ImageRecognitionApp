package com.example.cameras.view

import kotlinx.serialization.Serializable

/**
 * 認識結果の JSON スキーマ(Objectのリスト)
 */
@Serializable
public data class RecognizeObjects (
    val result: List<Object>
)

/**
 * 1つのオブジェクトの認識結果を表す JSON スキーマ
 */
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
        // 信頼性の値を小数点以下2桁まででフォーマット
        val reliabilityPercent = (obj.reliability * 100).toFloat()
        builder.append("・ ${obj.objectName}: ${reliabilityPercent}%\n")
    }
    return builder.toString()
}