package com.piku.app.data.model

import com.google.gson.annotations.SerializedName

data class AvatarUploadResponse(
    val mensaje: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null
)
