package com.intelligentgallery.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GalleryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertImage(image: ImageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPeople(people: List<PersonEntity>): List<Long>

    @Query("SELECT * FROM images ORDER BY capturedAtEpochMs DESC")
    suspend fun getAllImages(): List<ImageEntity>

    @Query("SELECT * FROM people")
    suspend fun getAllPeople(): List<PersonEntity>

    @Query("DELETE FROM images")
    suspend fun clearImages()
}
