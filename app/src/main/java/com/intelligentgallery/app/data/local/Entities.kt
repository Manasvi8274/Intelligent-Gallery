package com.intelligentgallery.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "images",
    indices = [Index("capturedAtEpochMs"), Index("normalizedPlace"), Index("occasion")]
)
data class ImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaStoreId: Long,
    val contentUri: String,
    val capturedAtEpochMs: Long,
    val latitude: Double?,
    val longitude: Double?,
    val normalizedPlace: String?,
    val nearestLandmark: String?,
    val occasion: String?,
    val peopleIds: List<Long>,
    val aiEnrichedAtEpochMs: Long?,
    val faceScanDone: Boolean
)

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val aliases: List<String>,
    val embeddingProfilePath: String?
)

@Entity(
    tableName = "faces",
    indices = [Index("imageId"), Index("personId")]
)
data class FaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imageId: Long,
    val personId: Long?,
    val faceIndex: Int,
    val bboxLeft: Int,
    val bboxTop: Int,
    val bboxRight: Int,
    val bboxBottom: Int,
    val signature: List<Float>
)

data class UnknownFaceRow(
    val faceId: Long,
    val imageId: Long,
    val contentUri: String,
    val faceIndex: Int,
    val bboxLeft: Int,
    val bboxTop: Int,
    val bboxRight: Int,
    val bboxBottom: Int
)
