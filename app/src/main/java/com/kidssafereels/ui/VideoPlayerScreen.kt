@file:OptIn(ExperimentalFoundationApi::class)

package com.kidssafereels.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kidssafereels.data.Video
import com.kidssafereels.ui.theme.KidsBlue
import com.kidssafereels.ui.theme.KidsGreen
import com.kidssafereels.ui.theme.KidsOrange
import com.kidssafereels.ui.theme.KidsPink
import com.kidssafereels.ui.theme.KidsPurple
import com.kidssafereels.ui.theme.KidsYellow
import com.kidssafereels.viewmodel.VideoUiState
import com.kidssafereels.viewmodel.VideoViewModel

@Composable
fun VideoPlayerScreen(
    viewModel: VideoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val state = uiState) {
            is VideoUiState.Loading -> LoadingScreen()
            is VideoUiState.Success -> {
                if (state.videos.isEmpty()) {
                    ErrorScreen(
                        message = "No videos found. Add videos to your gist!",
                        onRetry = { viewModel.refresh() }
                    )
                } else {
                    SimpleVideoPlayer(videos = state.videos, viewModel = viewModel)
                }
            }
            is VideoUiState.Error -> ErrorScreen(
                message = state.message,
                onRetry = { viewModel.refresh() }
            )
        }
    }
}

/**
 * Extract YouTube video ID from URL
 */
fun extractVideoId(url: String): String? {
    val patterns = listOf(
        Regex("youtube\\.com/shorts/([a-zA-Z0-9_-]+)"),
        Regex("youtu\\.be/([a-zA-Z0-9_-]+)"),
        Regex("youtube\\.com/watch\\?v=([a-zA-Z0-9_-]+)"),
        Regex("youtube\\.com/embed/([a-zA-Z0-9_-]+)")
    )
    for (pattern in patterns) {
        pattern.find(url)?.let { return it.groupValues[1] }
    }
    return null
}

/**
 * Generate HTML with YouTube IFrame Player - clean, no recommendations
 */
fun generatePlayerHtml(videoId: String): String {
    return """
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        html, body { 
            width: 100%; 
            height: 100%; 
            background: #000; 
            overflow: hidden;
        }
        #player {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            width: 100%;
            height: 100%;
        }
        iframe {
            width: 100%;
            height: 100%;
            border: none;
        }
    </style>
</head>
<body>
    <div id="player">
        <iframe 
            src="https://www.youtube.com/embed/$videoId?autoplay=1&mute=0&loop=1&playlist=$videoId&controls=1&modestbranding=1&rel=0&fs=0&playsinline=1&showinfo=0&iv_load_policy=3&disablekb=1"
            allow="autoplay; encrypted-media"
            allowfullscreen>
        </iframe>
    </div>
</body>
</html>
    """.trimIndent()
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SimpleVideoPlayer(
    videos: List<Video>,
    viewModel: VideoViewModel
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var key by remember { mutableIntStateOf(0) } // Force WebView recreation
    
    val currentVideo = videos.getOrNull(currentIndex) ?: return
    val videoId = remember(currentVideo.url) { extractVideoId(currentVideo.url) }
    
    LaunchedEffect(currentIndex) {
        viewModel.setCurrentVideoIndex(currentIndex)
    }
    
    DisposableEffect(Unit) {
        onDispose { webViewInstance?.destroy() }
    }
    
    fun navigateTo(index: Int) {
        val newIndex = when {
            index < 0 -> videos.size - 1
            index >= videos.size -> 0
            else -> index
        }
        currentIndex = newIndex
        isLoading = true
        key++ // Force WebView to recreate with new video
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // WebView with YouTube embed
        if (videoId != null) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            cacheMode = WebSettings.LOAD_NO_CACHE
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                            }
                        }
                        webChromeClient = WebChromeClient()
                        setBackgroundColor(android.graphics.Color.BLACK)
                        loadDataWithBaseURL(
                            "https://www.youtube.com",
                            generatePlayerHtml(videoId),
                            "text/html",
                            "UTF-8",
                            null
                        )
                        webViewInstance = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    // When key changes, reload with new video
                    val newVideoId = extractVideoId(videos.getOrNull(currentIndex)?.url ?: "")
                    if (newVideoId != null) {
                        view.loadDataWithBaseURL(
                            "https://www.youtube.com",
                            generatePlayerHtml(newVideoId),
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                }
            )
        } else {
            // Invalid video URL
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚠️ Invalid video URL",
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
        }
        
        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = KidsPink, strokeWidth = 4.dp)
            }
        }
        
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                    )
                )
                .padding(top = 40.dp, bottom = 30.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🌟 Kids Safe Reels 🌟",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Navigation buttons - LEFT side (Previous)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .size(60.dp)
                .clip(CircleShape)
                .background(KidsPurple.copy(alpha = 0.8f))
                .clickable { navigateTo(currentIndex - 1) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = "Previous",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
        
        // Navigation buttons - RIGHT side (Next)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .size(60.dp)
                .clip(CircleShape)
                .background(KidsPink.copy(alpha = 0.8f))
                .clickable { navigateTo(currentIndex + 1) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
        
        // Bottom info bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                    )
                )
                .padding(16.dp)
        ) {
            Text(
                text = currentVideo.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            currentVideo.description?.let { desc ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Video counter and dots
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val colors = listOf(KidsPurple, KidsPink, KidsBlue, KidsGreen, KidsOrange, KidsYellow)
                repeat(minOf(videos.size, 8)) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentIndex) 12.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == currentIndex) colors[index % colors.size]
                                else Color.White.copy(alpha = 0.3f)
                            )
                            .clickable { navigateTo(index) }
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "${currentIndex + 1} of ${videos.size}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(KidsPurple, KidsPink, KidsOrange))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🎬", fontSize = 80.sp)
            Spacer(modifier = Modifier.height(20.dp))
            CircularProgressIndicator(color = Color.White, strokeWidth = 4.dp)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Loading Videos...",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = "😢", fontSize = 80.sp)
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Oops!", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(30.dp))
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(KidsPink)
                    .clickable { onRetry() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Retry",
                    tint = Color.White,
                    modifier = Modifier.size(35.dp)
                )
            }
        }
    }
}
