package com.example.cameras

import kotlinx.serialization.Serializable

@Serializable
data class RecognizeObjects (
    val result: List<Object>
)

@Serializable
data class Object (
    val objectName: String,
    val reliability: Double
)