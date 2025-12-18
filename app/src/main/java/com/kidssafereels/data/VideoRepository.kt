package com.kidssafereels.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Repository for fetching videos from the configured source
 */
class VideoRepository {
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        // Disable caching to always get fresh video list
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .build()
            chain.proceed(request)
        }
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://gist.githubusercontent.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val apiService = retrofit.create(ApiService::class.java)
    
    /**
     * Fetches videos from the configured GitHub Gist URL
     * 
     * ⚠️ IMPORTANT: Replace this URL with your own GitHub Gist raw URL
     * 
     * How to get your URL:
     * 1. Go to https://gist.github.com
     * 2. Create a new gist with filename: videos.json
     * 3. Add your video list in JSON format
     * 4. Click "Create secret gist" or "Create public gist"
     * 5. Click the "Raw" button
     * 6. Copy the URL and paste it below
     */
    suspend fun fetchVideos(): Result<List<Video>> {
        return try {
            // ========================================================
            // 🔴 CONFIGURE YOUR VIDEO LIST URL HERE 🔴
            // ========================================================
            // Replace this URL with your GitHub Gist raw URL
            // Example: https://gist.githubusercontent.com/YOUR_USERNAME/GIST_ID/raw/videos.json
            
            // IMPORTANT: Remove the commit hash to always get latest version!
            val gistUrl = "https://gist.githubusercontent.com/GowthamGanapathi/543bd8d52be0f15baefd529f50af5765/raw/videos.json"
            
            // ========================================================
            
            val videos = apiService.getVideos(gistUrl)
            Result.success(videos)
        } catch (e: Exception) {
            // If network fails, return demo videos for testing
            Result.success(getDemoVideos())
        }
    }
    
    /**
     * Demo videos for testing when network is unavailable
     * Uses YouTube Shorts as examples
     */
    private fun getDemoVideos(): List<Video> {
        return listOf(
            Video(
                id = "demo-1",
                title = "🔤 Learn ABC",
                url = "https://youtube.com/shorts/baomxpKyoNs",
                description = "Fun alphabet song!"
            ),
            Video(
                id = "demo-2",
                title = "🔢 Counting 1-10",
                url = "https://youtube.com/shorts/hjIBu2z0isU",
                description = "Learn to count!"
            )
        )
    }
}

