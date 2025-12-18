@file:OptIn(ExperimentalFoundationApi::class)

package com.kidssafereels.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Supports infinite looping - goes back to first video after last
 */
@Composable
fun VideoReelsPager(
    videos: List<Video>,
    viewModel: VideoViewModel
) {
    // Use a large number to simulate infinite scroll
    val infinitePageCount = Int.MAX_VALUE / 2
    val startPage = infinitePageCount / 2 - (infinitePageCount / 2) % videos.size
    
    val pagerState = rememberPagerState(
        initialPage = startPage,
        pageCount = { infinitePageCount }
    )
    
    // Calculate actual video index using modulo
    val actualIndex = pagerState.currentPage % videos.size
    
    LaunchedEffect(actualIndex) {
        viewModel.setCurrentVideoIndex(actualIndex)
    }
    
    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        val videoIndex = page % videos.size
        VideoPage(
            video = videos[videoIndex],
            isCurrentPage = page == pagerState.currentPage,
            pageIndex = videoIndex,
            totalPages = videos.size
        )
    }
}

/**
 * Convert YouTube URL to mobile-friendly format
 */
fun getYouTubeMobileUrl(url: String): String {
    // Extract video ID from various formats
    val patterns = listOf(
        Regex("youtube\\.com/shorts/([a-zA-Z0-9_-]+)"),
        Regex("youtu\\.be/([a-zA-Z0-9_-]+)"),
        Regex("youtube\\.com/watch\\?v=([a-zA-Z0-9_-]+)"),
        Regex("youtube\\.com/embed/([a-zA-Z0-9_-]+)"),
        Regex("youtube\\.com/v/([a-zA-Z0-9_-]+)")
    )
    
    for (pattern in patterns) {
        val match = pattern.find(url)
        if (match != null) {
            val videoId = match.groupValues[1]
            // Use YouTube Shorts URL for better mobile experience
            return "https://www.youtube.com/shorts/$videoId"
        }
    }
    
    // If no pattern matches, return original URL
    return url
}

/**
 * Single video page with YouTube WebView
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
    
    val youtubeUrl = remember(video.url) { 
        getYouTubeMobileUrl(video.url) 
    }
    
    // Remember WebView to control playback
    var webView by remember { mutableStateOf<WebView?>(null) }
    
    // Control playback based on current page
    LaunchedEffect(isCurrentPage) {
        webView?.let { wv ->
            if (isCurrentPage) {
                wv.onResume()
                // Try to play the video
                wv.evaluateJavascript(
                    """
                    (function() {
                        var video = document.querySelector('video');
                        if (video) video.play();
                    })();
                    """.trimIndent(),
                    null
                )
            } else {
                // Pause video when not visible
                wv.evaluateJavascript(
                    """
                    (function() {
                        var video = document.querySelector('video');
                        if (video) video.pause();
                    })();
                    """.trimIndent(),
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
        // YouTube WebView - loads YouTube directly like a browser
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    // Enable cookies for YouTube
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    
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
                        allowContentAccess = true
                        allowFileAccess = true
                        databaseEnabled = true
                        javaScriptCanOpenWindowsAutomatically = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    }
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            
                            // Auto-play video after page loads
                            view?.evaluateJavascript(
                                """
                                (function() {
                                    var video = document.querySelector('video');
                                    if (video) {
                                        video.play();
                                        video.loop = true;
                                    }
                                })();
                                """.trimIndent(),
                                null
                            )
                        }
                        
                        override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                            // Stay within YouTube
                            val url = request?.url?.toString() ?: return false
                            return if (url.contains("youtube.com") || url.contains("youtu.be")) {
                                false // Allow YouTube navigation
                            } else {
                                true // Block other URLs
                            }
                        }
                    }
                    
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            if (newProgress > 80) {
                                isLoading = false
                            }
                        }
                    }
                    
                    setBackgroundColor(android.graphics.Color.BLACK)
                    
                    // Load the YouTube URL directly
                    loadUrl(youtubeUrl)
                    
                    webView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { wv ->
                webView = wv
            }
        )
        
        // Loading indicator
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = KidsPink,
                    modifier = Modifier.size(50.dp),
                    strokeWidth = 4.dp
                )
            }
        }
        
        // Top overlay with app name
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
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
        
        // Bottom overlay with video info
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
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column {
                // Video title
                Text(
                    text = video.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                if (video.description != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = video.description,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
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
                
                // Swipe hint (only show on first view)
                if (pageIndex == 0 && totalPages > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⬆️ Swipe to browse • Videos loop forever!",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
            }
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
