package com.intelligentgallery.app.ai

import kotlinx.serialization.Serializable

@Serializable
data class EnrichImageRequest(
    val imagePath: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val capturedAtEpochMs: Long? = null
)

@Serializable
data class FacePrediction(
    val personName: String,
    val confidence: Double
)

@Serializable
data class EnrichImageResponse(
    val people: List<FacePrediction> = emptyList(),
    val place: String? = null,
    val nearestLandmark: String? = null,
    val occasion: String? = null
)

@Serializable
data class ParseQueryRequest(
    val query: String
)

@Serializable
data class ParsedQueryResponse(
    val people: List<String> = emptyList(),
    val place: String? = null,
    val occasion: String? = null,
    val startEpochMs: Long? = null,
    val endEpochMs: Long? = null,
    val excludePeople: List<String> = emptyList()
)
