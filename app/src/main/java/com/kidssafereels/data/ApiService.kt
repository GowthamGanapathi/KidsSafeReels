package com.kidssafereels.data

import retrofit2.http.GET
import retrofit2.http.Url

/**
 * API Service for fetching video list from GitHub Gist
 */
interface ApiService {
    
    /**
     * Fetches the video list from a raw GitHub Gist URL
     * @param url The full URL to the raw gist JSON file
     */
    @GET
    suspend fun getVideos(@Url url: String): List<Video>
}

