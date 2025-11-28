package com.example.claudeapp.data.imgbb

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ImgBBApiService {
    @FormUrlEncoded
    @POST("1/upload")
    suspend fun uploadImage(
        @Field("key") apiKey: String,
        @Field("image") base64Image: String,
        @Field("name") name: String? = null,
        @Field("expiration") expiration: Int? = null
    ): Response<ImgBBResponse>
}

data class ImgBBResponse(
    val data: ImgBBData?,
    val success: Boolean,
    val status: Int
)

data class ImgBBData(
    val id: String,
    val title: String,
    val url_viewer: String,
    val url: String,
    val display_url: String,
    val width: String,
    val height: String,
    val size: String,
    val time: String,
    val expiration: String,
    val image: ImgBBImage,
    val thumb: ImgBBThumb,
    val medium: ImgBBMedium,
    val delete_url: String
)

data class ImgBBImage(
    val filename: String,
    val name: String,
    val mime: String,
    val extension: String,
    val url: String
)

data class ImgBBThumb(
    val filename: String,
    val name: String,
    val mime: String,
    val extension: String,
    val url: String
)

data class ImgBBMedium(
    val filename: String,
    val name: String,
    val mime: String,
    val extension: String,
    val url: String
)
