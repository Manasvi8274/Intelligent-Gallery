package com.intelligentgallery.app.data.repo

import com.intelligentgallery.app.ai.AiApi
import com.intelligentgallery.app.ai.ParseQueryRequest
import com.intelligentgallery.app.data.local.GalleryDao
import com.intelligentgallery.app.data.local.ImageEntity
import com.intelligentgallery.app.data.model.GalleryImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GalleryRepository(
    private val dao: GalleryDao,
    private val aiApi: AiApi
) {
    suspend fun seedDemoData() = withContext(Dispatchers.IO) {
        if (dao.getAllImages().isNotEmpty()) return@withContext

        dao.upsertPeople(
            listOf(
                com.intelligentgallery.app.data.local.PersonEntity(
                    id = 1,
                    name = "myself",
                    aliases = listOf("me", "i"),
                    embeddingProfilePath = null
                ),
                com.intelligentgallery.app.data.local.PersonEntity(
                    id = 2,
                    name = "rohan",
                    aliases = emptyList(),
                    embeddingProfilePath = null
                ),
                com.intelligentgallery.app.data.local.PersonEntity(
                    id = 3,
                    name = "shrey",
                    aliases = emptyList(),
                    embeddingProfilePath = null
                ),
                com.intelligentgallery.app.data.local.PersonEntity(
                    id = 4,
                    name = "family",
                    aliases = listOf("parents"),
                    embeddingProfilePath = null
                )
            )
        )

        val now = System.currentTimeMillis()
        val samples = listOf(
            ImageEntity(
                mediaStoreId = 1,
                contentUri = "content://sample/1",
                capturedAtEpochMs = now - 5L * 24 * 60 * 60 * 1000,
                latitude = 30.7520,
                longitude = 76.8057,
                normalizedPlace = "Rock Garden, Chandigarh",
                nearestLandmark = "Rock Garden",
                occasion = "road trip",
                peopleIds = listOf(1, 2, 3),
                aiEnrichedAtEpochMs = now
            ),
            ImageEntity(
                mediaStoreId = 2,
                contentUri = "content://sample/2",
                capturedAtEpochMs = now - 40L * 24 * 60 * 60 * 1000,
                latitude = 30.7333,
                longitude = 76.7794,
                normalizedPlace = "Sector 17, Chandigarh",
                nearestLandmark = "Plaza",
                occasion = "family function",
                peopleIds = listOf(1, 4),
                aiEnrichedAtEpochMs = now
            )
        )
        samples.forEach { dao.upsertImage(it) }
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

    suspend fun search(rawQuery: String): List<GalleryImage> = withContext(Dispatchers.IO) {
        val parsed = aiApi.parseQuery(ParseQueryRequest(query = rawQuery))
        val allImages = dao.getAllImages()
        val allPeople = dao.getAllPeople()
        val nameToId = allPeople.associate { it.name.lowercase() to it.id }

        val includeIds = parsed.people.mapNotNull { nameToId[it.lowercase()] }.toSet()
        val excludeIds = parsed.excludePeople.mapNotNull { nameToId[it.lowercase()] }.toSet()

        allImages
            .asSequence()
            .filter { image ->
                includeIds.all { requiredId -> image.peopleIds.contains(requiredId) } &&
                    excludeIds.none { excludedId -> image.peopleIds.contains(excludedId) } &&
                    parsed.place?.let { p ->
                        val n = image.normalizedPlace.orEmpty().lowercase()
                        val l = image.nearestLandmark.orEmpty().lowercase()
                        n.contains(p.lowercase()) || l.contains(p.lowercase())
                    } != false &&
                    parsed.occasion?.let { o ->
                        image.occasion.orEmpty().lowercase().contains(o.lowercase())
                    } != false &&
                    parsed.startEpochMs?.let { image.capturedAtEpochMs >= it } != false &&
                    parsed.endEpochMs?.let { image.capturedAtEpochMs <= it } != false
            }
            .map {
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
            .toList()
    }
}
