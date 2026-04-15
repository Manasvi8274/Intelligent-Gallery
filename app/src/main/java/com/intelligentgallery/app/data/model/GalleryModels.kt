package com.intelligentgallery.app.data.model

import android.graphics.Bitmap

data class GalleryImage(
    val id: Long,
    val contentUri: String,
    val capturedAtEpochMs: Long,
    val place: String?,
    val nearestLandmark: String?,
    val occasion: String?,
    val peopleIds: List<Long>
)

data class SearchQuery(
    val peopleNames: List<String>,
    val place: String?,
    val occasion: String?,
    val startEpochMs: Long?,
    val endEpochMs: Long?,
    val excludePeopleNames: List<String>
)

data class PendingFaceLabel(
    val faceId: Long,
    val imageId: Long,
    val imageUri: String,
    val personNumber: Int,
    val cropBitmap: Bitmap
)
