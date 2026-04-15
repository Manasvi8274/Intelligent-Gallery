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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPerson(person: PersonEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaces(faces: List<FaceEntity>): List<Long>

    @Query("SELECT * FROM images ORDER BY capturedAtEpochMs DESC")
    suspend fun getAllImages(): List<ImageEntity>

    @Query("SELECT COUNT(*) FROM images")
    suspend fun getImageCount(): Int

    @Query("SELECT * FROM people")
    suspend fun getAllPeople(): List<PersonEntity>

    @Query("SELECT * FROM images WHERE mediaStoreId = :mediaStoreId LIMIT 1")
    suspend fun findImageByMediaStoreId(mediaStoreId: Long): ImageEntity?

    @Query("SELECT * FROM images WHERE faceScanDone = 0 ORDER BY capturedAtEpochMs DESC LIMIT :limit")
    suspend fun getPendingFaceScanImages(limit: Int): List<ImageEntity>

    @Query("SELECT * FROM faces WHERE imageId = :imageId")
    suspend fun getFacesByImageId(imageId: Long): List<FaceEntity>

    @Query(
        """
        SELECT
            f.id AS faceId,
            f.imageId AS imageId,
            i.contentUri AS contentUri,
            f.faceIndex AS faceIndex,
            f.bboxLeft AS bboxLeft,
            f.bboxTop AS bboxTop,
            f.bboxRight AS bboxRight,
            f.bboxBottom AS bboxBottom
        FROM faces f
        INNER JOIN images i ON i.id = f.imageId
        WHERE f.personId IS NULL
        ORDER BY i.capturedAtEpochMs DESC, f.faceIndex ASC
        LIMIT 1
        """
    )
    suspend fun getNextUnknownFace(): UnknownFaceRow?

    @Query("SELECT * FROM faces WHERE personId IS NOT NULL")
    suspend fun getKnownFaces(): List<FaceEntity>

    @Query("SELECT * FROM faces WHERE personId IS NULL")
    suspend fun getUnknownFaces(): List<FaceEntity>

    @Query("SELECT * FROM faces WHERE id = :faceId LIMIT 1")
    suspend fun getFaceById(faceId: Long): FaceEntity?

    @Query("UPDATE faces SET personId = :personId WHERE id = :faceId")
    suspend fun updateFacePerson(faceId: Long, personId: Long)

    @Query("UPDATE faces SET personId = :personId WHERE id IN (:faceIds)")
    suspend fun updateFacesPerson(faceIds: List<Long>, personId: Long)

    @Query("UPDATE images SET peopleIds = :peopleIdsCsv WHERE id = :imageId")
    suspend fun updateImagePeople(imageId: Long, peopleIdsCsv: String)

    @Query("UPDATE images SET faceScanDone = 1 WHERE id = :imageId")
    suspend fun markImageFaceScanDone(imageId: Long)

    @Query("SELECT * FROM people WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findPersonByName(name: String): PersonEntity?

    @Query("DELETE FROM images")
    suspend fun clearImages()
}
