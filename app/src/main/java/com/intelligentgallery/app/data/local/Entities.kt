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
    val aiEnrichedAtEpochMs: Long?
)

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val aliases: List<String>,
    val embeddingProfilePath: String?
)
