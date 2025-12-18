package com.kidssafereels.data

import com.google.gson.annotations.SerializedName

/**
 * Data class representing a video in the curated list
 */
data class Video(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("url")
    val url: String,
    
    @SerializedName("thumbnail")
    val thumbnail: String? = null,
    
    @SerializedName("description")
    val description: String? = null
)

