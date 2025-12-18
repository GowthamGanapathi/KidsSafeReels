@file:OptIn(ExperimentalFoundationApi::class)

package com.kidssafereels.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

/**
 * Main video player screen with vertical swipe navigation
 */
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
            is VideoUiState.Loading -> {
                LoadingScreen()
            }
            is VideoUiState.Success -> {
                VideoReelsPager(
                    videos = state.videos,
                    viewModel = viewModel
                )
            }
            is VideoUiState.Error -> {
                ErrorScreen(
                    message = state.message,
                    onRetry = { viewModel.refresh() }
                )
            }
        }
    }
}

/**
 * Vertical pager for swiping through videos like TikTok/Reels
 */
@Composable
fun VideoReelsPager(
    videos: List<Video>,
    viewModel: VideoViewModel
) {
    val pagerState = rememberPagerState(pageCount = { videos.size })
    
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setCurrentVideoIndex(pagerState.currentPage)
    }
    
    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        VideoPage(
            video = videos[page],
            isCurrentPage = page == pagerState.currentPage,
            pageIndex = page,
            totalPages = videos.size
        )
    }
}

/**
 * Extract YouTube video ID from various URL formats
 */
fun extractYouTubeVideoId(url: String): String? {
    val patterns = listOf(
        // youtube.com/shorts/VIDEO_ID
        Regex("youtube\\.com/shorts/([a-zA-Z0-9_-]+)"),
        // youtu.be/VIDEO_ID
        Regex("youtu\\.be/([a-zA-Z0-9_-]+)"),
        // youtube.com/watch?v=VIDEO_ID
        Regex("youtube\\.com/watch\\?v=([a-zA-Z0-9_-]+)"),
        // youtube.com/embed/VIDEO_ID
        Regex("youtube\\.com/embed/([a-zA-Z0-9_-]+)"),
        // youtube.com/v/VIDEO_ID
        Regex("youtube\\.com/v/([a-zA-Z0-9_-]+)")
    )
    
    for (pattern in patterns) {
        val match = pattern.find(url)
        if (match != null) {
            return match.groupValues[1]
        }
    }
    return null
}

/**
 * Generate HTML for embedded YouTube player optimized for kids
 */
fun generateYouTubePlayerHtml(videoId: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }
                html, body {
                    width: 100%;
                    height: 100%;
                    background-color: #000;
                    overflow: hidden;
                }
                .video-container {
                    position: fixed;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 100%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    background-color: #000;
                }
                iframe {
                    width: 100%;
                    height: 100%;
                    border: none;
                }
            </style>
        </head>
        <body>
            <div class="video-container">
                <iframe 
                    src="https://www.youtube.com/embed/$videoId?autoplay=1&loop=1&playlist=$videoId&controls=1&modestbranding=1&rel=0&showinfo=0&fs=0&playsinline=1&mute=0&enablejsapi=1"
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                    allowfullscreen>
                </iframe>
            </div>
        </body>
        </html>
    """.trimIndent()
}

/**
 * Single video page with YouTube player and overlay
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoPage(
    video: Video,
    isCurrentPage: Boolean,
    pageIndex: Int,
    totalPages: Int
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    
    val videoId = remember(video.url) { 
        extractYouTubeVideoId(video.url) 
    }
    
    // Remember WebView to control playback
    var webView by remember { mutableStateOf<WebView?>(null) }
    
    // Control playback based on current page
    LaunchedEffect(isCurrentPage) {
        webView?.let { wv ->
            if (isCurrentPage) {
                // Resume video
                wv.onResume()
                wv.evaluateJavascript(
                    "document.querySelector('iframe').contentWindow.postMessage('{\"event\":\"command\",\"func\":\"playVideo\",\"args\":\"\"}', '*');",
                    null
                )
            } else {
                // Pause video
                wv.evaluateJavascript(
                    "document.querySelector('iframe').contentWindow.postMessage('{\"event\":\"command\",\"func\":\"pauseVideo\",\"args\":\"\"}', '*');",
                    null
                )
                wv.onPause()
            }
        }
    }
    
    // Cleanup WebView
    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (videoId != null) {
            // YouTube WebView Player
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
                            cacheMode = WebSettings.LOAD_DEFAULT
                            setSupportZoom(false)
                            builtInZoomControls = false
                            displayZoomControls = false
                        }
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                            }
                        }
                        
                        webChromeClient = WebChromeClient()
                        
                        setBackgroundColor(android.graphics.Color.BLACK)
                        
                        loadDataWithBaseURL(
                            "https://www.youtube.com",
                            generateYouTubePlayerHtml(videoId),
                            "text/html",
                            "UTF-8",
                            null
                        )
                        
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { wv ->
                    webView = wv
                }
            )
        } else {
            // Invalid URL message
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⚠️",
                        fontSize = 60.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Invalid YouTube URL",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = video.url,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        }
        
        // Loading indicator
        AnimatedVisibility(
            visible = isLoading && videoId != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            CircularProgressIndicator(
                color = KidsPink,
                modifier = Modifier.size(60.dp),
                strokeWidth = 4.dp
            )
        }
        
        // Bottom gradient overlay with video info
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                // Video title with fun styling
                Text(
                    text = video.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                if (video.description != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = video.description,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Page indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val colors = listOf(KidsPurple, KidsPink, KidsBlue, KidsGreen, KidsOrange, KidsYellow)
                    repeat(minOf(totalPages, 10)) { index ->
                        val color = colors[index % colors.size]
                        Box(
                            modifier = Modifier
                                .size(if (index == pageIndex) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == pageIndex) color
                                    else Color.White.copy(alpha = 0.4f)
                                )
                        )
                    }
                    if (totalPages > 10) {
                        Text(
                            text = " +${totalPages - 10}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
        
        // Top gradient for status bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        // App title at top
        Text(
            text = "🌟 Kids Safe Reels 🌟",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 45.dp)
        )
        
        // Swipe hint for first video
        if (pageIndex == 0 && totalPages > 1) {
            Text(
                text = "⬆️ Swipe up for more",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )
        }
    }
}

/**
 * Loading screen with fun animation
 */
@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(KidsPurple, KidsPink, KidsOrange)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎬",
                fontSize = 80.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(50.dp),
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Loading Fun Videos...",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Error screen with retry option
 */
@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "😢",
                fontSize = 80.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Oops!",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(30.dp))
            IconButton(
                onClick = onRetry,
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(KidsPink)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Retry",
                    tint = Color.White,
                    modifier = Modifier.size(35.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Tap to try again",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        }
    }
}
