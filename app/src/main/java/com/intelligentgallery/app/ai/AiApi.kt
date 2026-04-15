package com.intelligentgallery.app.ai

import retrofit2.http.Body
import retrofit2.http.POST

interface AiApi {
    @POST("enrich_image")
    suspend fun enrichImage(@Body request: EnrichImageRequest): EnrichImageResponse

    @POST("parse_query")
    suspend fun parseQuery(@Body request: ParseQueryRequest): ParsedQueryResponse
}
