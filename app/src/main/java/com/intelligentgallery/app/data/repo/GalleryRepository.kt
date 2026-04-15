package com.intelligentgallery.app.data.repo

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.provider.MediaStore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.intelligentgallery.app.data.local.FaceEntity
import com.intelligentgallery.app.data.local.GalleryDao
import com.intelligentgallery.app.data.local.ImageEntity
import com.intelligentgallery.app.data.local.PersonEntity
import com.intelligentgallery.app.data.model.GalleryImage
import com.intelligentgallery.app.data.model.PendingFaceLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class GalleryRepository(
    private val context: Context,
    private val dao: GalleryDao
) {
    private val detector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.03f)
            .build()
        FaceDetection.getClient(options)
    }

    data class ImportResult(val imported: Int, val totalInDb: Int, val hasMoreToImport: Boolean)
    data class FaceProcessResult(val processed: Int, val hasMoreToProcess: Boolean)

    suspend fun importNextImageBatch(batchSize: Int = 10): ImportResult = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        var imported = 0
        var hasMore = false

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            while (cursor.moveToNext()) {
                val mediaId = cursor.getLong(idCol)
                if (dao.findImageByMediaStoreId(mediaId) != null) continue
                val dateTaken = cursor.getLong(dateCol)
                val imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaId)
                dao.upsertImage(
                    ImageEntity(
                        mediaStoreId = mediaId,
                        contentUri = imageUri.toString(),
                        capturedAtEpochMs = dateTaken.takeIf { it > 0 } ?: System.currentTimeMillis(),
                        latitude = null,
                        longitude = null,
                        normalizedPlace = null,
                        nearestLandmark = null,
                        occasion = null,
                        peopleIds = emptyList(),
                        aiEnrichedAtEpochMs = null,
                        faceScanDone = false
                    )
                )
                imported += 1
                if (imported >= batchSize) {
                    hasMore = true
                    break
                }
            }
        }
        ImportResult(
            imported = imported,
            totalInDb = dao.getImageCount(),
            hasMoreToImport = hasMore
        )
    }

    suspend fun processPendingFacesBatch(limit: Int = 3): FaceProcessResult = withContext(Dispatchers.IO) {
        val pendingImages = dao.getPendingFaceScanImages(limit)
        if (pendingImages.isEmpty()) {
            return@withContext FaceProcessResult(processed = 0, hasMoreToProcess = false)
        }

        var processed = 0
        pendingImages.forEach { image ->
            val knownFaces = dao.getKnownFaces()
            runCatching {
                processFacesForImage(image, knownFaces)
            }.onFailure {
                // Avoid blocking queue forever on a problematic file.
                dao.markImageFaceScanDone(image.id)
            }
            processed += 1
        }
        val hasMore = dao.getPendingFaceScanImages(1).isNotEmpty()
        FaceProcessResult(processed = processed, hasMoreToProcess = hasMore)
    }

    private suspend fun processFacesForImage(image: ImageEntity, knownFaces: List<FaceEntity>) {
        val bitmap = decodeBitmap(Uri.parse(image.contentUri))
        if (bitmap == null) {
            dao.markImageFaceScanDone(image.id)
            return
        }

        val faces = detector.process(InputImage.fromBitmap(bitmap, 0)).await()
        val faceEntities = faces.mapIndexedNotNull { idx, face ->
            val crop = cropBitmap(bitmap, face.boundingBox) ?: return@mapIndexedNotNull null
            val signature = computeSignature(crop)
            val matchedPersonId = findBestPersonMatch(signature, knownFaces)
            FaceEntity(
                imageId = image.id,
                personId = matchedPersonId,
                faceIndex = idx + 1,
                bboxLeft = face.boundingBox.left,
                bboxTop = face.boundingBox.top,
                bboxRight = face.boundingBox.right,
                bboxBottom = face.boundingBox.bottom,
                signature = signature
            )
        }
        if (faceEntities.isNotEmpty()) {
            dao.insertFaces(faceEntities)
        }
        refreshImagePeople(image.id)
        dao.markImageFaceScanDone(image.id)
    }

    suspend fun getAllImages(): List<GalleryImage> = withContext(Dispatchers.IO) {
        dao.getAllImages().map {
            GalleryImage(
                id = it.id,
                contentUri = it.contentUri,
                capturedAtEpochMs = it.capturedAtEpochMs,
                place = it.normalizedPlace,
                nearestLandmark = it.nearestLandmark,
                occasion = it.occasion,
                peopleIds = it.peopleIds
            )
        }
    }

    suspend fun getTotalImageCount(): Int = withContext(Dispatchers.IO) { dao.getImageCount() }

    suspend fun getNextPendingFace(): PendingFaceLabel? = withContext(Dispatchers.IO) {
        val row = dao.getNextUnknownFace() ?: return@withContext null
        val bitmap = decodeBitmap(Uri.parse(row.contentUri)) ?: return@withContext null
        val crop = cropBitmap(
            bitmap,
            Rect(row.bboxLeft, row.bboxTop, row.bboxRight, row.bboxBottom)
        ) ?: return@withContext null
        PendingFaceLabel(
            faceId = row.faceId,
            imageId = row.imageId,
            imageUri = row.contentUri,
            personNumber = row.faceIndex,
            cropBitmap = crop
        )
    }

    suspend fun saveFaceLabel(faceId: Long, personName: String) = withContext(Dispatchers.IO) {
        val face = dao.getFaceById(faceId) ?: return@withContext
        val cleanName = personName.trim()
        if (cleanName.isBlank()) return@withContext

        val existing = dao.findPersonByName(cleanName)
        val personId = existing?.id ?: dao.upsertPerson(
            PersonEntity(
                name = cleanName,
                aliases = emptyList(),
                embeddingProfilePath = null
            )
        )
        dao.updateFacePerson(faceId, personId)

        val unknownFaces = dao.getUnknownFaces()
        val similarUnknownIds = unknownFaces
            .filter { cosineSimilarity(it.signature, face.signature) >= 0.95f }
            .map { it.id }
        if (similarUnknownIds.isNotEmpty()) {
            dao.updateFacesPerson(similarUnknownIds, personId)
        }

        refreshImagePeople(face.imageId)
        similarUnknownIds.forEach { unknownId ->
            dao.getFaceById(unknownId)?.let { refreshImagePeople(it.imageId) }
        }
    }

    private suspend fun refreshImagePeople(imageId: Long) {
        val ids = dao.getFacesByImageId(imageId).mapNotNull { it.personId }.distinct()
        dao.updateImagePeople(imageId, ids.joinToString(","))
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        return context.contentResolver.openInputStream(uri)?.use { input ->
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inSampleSize = 2
            }
            BitmapFactory.decodeStream(input, null, options)
        }
    }

    private fun cropBitmap(source: Bitmap, rect: Rect): Bitmap? {
        if (source.width <= 0 || source.height <= 0) return null
        val left = max(0, rect.left)
        val top = max(0, rect.top)
        val right = min(source.width, rect.right)
        val bottom = min(source.height, rect.bottom)
        val width = right - left
        val height = bottom - top
        if (width <= 2 || height <= 2) return null
        return Bitmap.createBitmap(source, left, top, width, height)
    }

    private fun computeSignature(faceCrop: Bitmap): List<Float> {
        val resized = Bitmap.createScaledBitmap(faceCrop, 16, 16, true)
        val pixels = IntArray(16 * 16)
        resized.getPixels(pixels, 0, 16, 0, 0, 16, 16)
        return pixels.map { pixel ->
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            ((0.299f * r) + (0.587f * g) + (0.114f * b)) / 255f
        }
    }

    private fun findBestPersonMatch(signature: List<Float>, knownFaces: List<FaceEntity>): Long? {
        var bestScore = -1f
        var bestPersonId: Long? = null
        for (knownFace in knownFaces) {
            val personId = knownFace.personId ?: continue
            val score = cosineSimilarity(signature, knownFace.signature)
            if (score > bestScore) {
                bestScore = score
                bestPersonId = personId
            }
        }
        return if (bestScore >= 0.93f) bestPersonId else null
    }

    private fun cosineSimilarity(a: List<Float>, b: List<Float>): Float {
        if (a.isEmpty() || b.isEmpty() || a.size != b.size) return 0f
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        if (normA == 0f || normB == 0f) return 0f
        return (dot / (sqrt(normA) * sqrt(normB))).coerceIn(-1f, 1f)
    }
}
