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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.abs

/**
 * Main video player screen - Simple approach with navigation buttons
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
                SimpleVideoPlayer(
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
 * Simple video player with manual navigation buttons
 * More reliable than gesture-based navigation with WebView
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SimpleVideoPlayer(
    videos: List<Video>,
    viewModel: VideoViewModel
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    
    val currentVideo = videos[currentIndex]
    val youtubeUrl = remember(currentVideo.url) { 
        getYouTubeMobileUrl(currentVideo.url) 
    }
    
    // Update viewmodel
    LaunchedEffect(currentIndex) {
        viewModel.setCurrentVideoIndex(currentIndex)
    }
    
    // Cleanup WebView
    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
        }
    }
    
    fun goToNext() {
        if (videos.isNotEmpty()) {
            currentIndex = (currentIndex + 1) % videos.size
            isLoading = true
            webView?.loadUrl(getYouTubeMobileUrl(videos[currentIndex].url))
        }
    }
    
    fun goToPrevious() {
        if (videos.isNotEmpty()) {
            currentIndex = if (currentIndex - 1 < 0) videos.size - 1 else currentIndex - 1
            isLoading = true
            webView?.loadUrl(getYouTubeMobileUrl(videos[currentIndex].url))
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // YouTube WebView
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
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
                        databaseEnabled = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    }
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            
                            // Inject CSS to hide YouTube recommendations, comments, and other UI
                            // Also disable YouTube's internal swipe navigation
                            view?.evaluateJavascript(
                                """
                                (function() {
                                    // Create style element to hide unwanted YouTube elements
                                    var style = document.createElement('style');
                                    style.textContent = `
                                        /* Hide recommendations, related videos, comments */
                                        ytm-reel-shelf-renderer,
                                        ytm-shorts-shelf-renderer,
                                        ytm-related-videos-renderer,
                                        ytm-comments-section-renderer,
                                        ytm-comment-section-renderer,
                                        ytm-engagement-panel-section-list-renderer,
                                        .related-chips-slot-wrapper,
                                        .slim-owner-section,
                                        ytm-shorts-action-buttons,
                                        .reel-player-overlay-actions,
                                        .pivot-bar,
                                        ytm-pivot-bar-renderer,
                                        .shorts-title-channel,
                                        .reel-player-page-segment,
                                        ytm-shorts-reels-segment-renderer,
                                        [class*="related"],
                                        [class*="suggestion"],
                                        [class*="recommend"] {
                                            display: none !important;
                                        }
                                        
                                        /* Hide swipe indicators */
                                        .reel-player-navigation-up,
                                        .reel-player-navigation-down,
                                        .navigation-container {
                                            display: none !important;
                                        }
                                        
                                        /* Make video fullscreen */
                                        video {
                                            width: 100vw !important;
                                            height: 100vh !important;
                                            object-fit: cover !important;
                                        }
                                        
                                        /* Hide bottom bar */
                                        ytm-mobile-topbar-renderer,
                                        .mobile-topbar-header,
                                        .ytm-pivot-bar-renderer {
                                            display: none !important;
                                        }
                                    `;
                                    document.head.appendChild(style);
                                    
                                    // Auto-play and loop the video
                                    var video = document.querySelector('video');
                                    if (video) {
                                        video.play();
                                        video.loop = true;
                                        video.muted = false;
                                    }
                                    
                                    // Disable touch events on the page to prevent YouTube swipe
                                    document.body.style.overflow = 'hidden';
                                    document.body.style.touchAction = 'none';
                                })();
                                """.trimIndent(),
                                null
                            )
                        }
                        
                        override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                            // BLOCK ALL navigation - only allow the exact video we loaded
                            // This prevents YouTube from navigating to other videos
                            return true
                        }
                    }
                    
                    webChromeClient = WebChromeClient()
                    setBackgroundColor(android.graphics.Color.BLACK)
                    loadUrl(youtubeUrl)
                    webView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Left edge swipe zone for navigation
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(60.dp)
                .align(Alignment.CenterStart)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (abs(dragOffset) > 100) {
                                if (dragOffset < 0) goToNext() else goToPrevious()
                            }
                            dragOffset = 0f
                        },
                        onVerticalDrag = { _, dragAmount ->
                            dragOffset += dragAmount
                        }
                    )
                }
        )
        
        // Right edge swipe zone for navigation
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(60.dp)
                .align(Alignment.CenterEnd)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (abs(dragOffset) > 100) {
                                if (dragOffset < 0) goToNext() else goToPrevious()
                            }
                            dragOffset = 0f
                        },
                        onVerticalDrag = { _, dragAmount ->
                            dragOffset += dragAmount
                        }
                    )
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
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = KidsPink,
                    modifier = Modifier.size(40.dp),
                    strokeWidth = 4.dp
                )
            }
        }
        
        // Top bar with app name
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
                .padding(top = 40.dp, bottom = 20.dp)
        ) {
            Text(
                text = "🌟 Kids Safe Reels 🌟",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // Navigation buttons on right side
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Previous button
            IconButton(
                onClick = { goToPrevious() },
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(35.dp)
                )
            }
            
            // Next button
            IconButton(
                onClick = { goToNext() },
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(35.dp)
                )
            }
        }
        
        // Bottom info bar
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
                .padding(16.dp)
        ) {
            Column {
                // Video title
                Text(
                    text = currentVideo.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                if (currentVideo.description != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentVideo.description,
                        color = Color.White.copy(alpha = 0.7f),
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
                    repeat(minOf(videos.size, 10)) { index ->
                        val color = colors[index % colors.size]
                        Box(
                            modifier = Modifier
                                .size(if (index == currentIndex) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentIndex) color
                                    else Color.White.copy(alpha = 0.4f)
                                )
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "${currentIndex + 1} / ${videos.size}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

/**
 * Convert YouTube URL to mobile-friendly format
 */
fun getYouTubeMobileUrl(url: String): String {
    val patterns = listOf(
        Regex("youtube\\.com/shorts/([a-zA-Z0-9_-]+)"),
        Regex("youtu\\.be/([a-zA-Z0-9_-]+)"),
        Regex("youtube\\.com/watch\\?v=([a-zA-Z0-9_-]+)"),
        Regex("youtube\\.com/embed/([a-zA-Z0-9_-]+)")
    )
    
    for (pattern in patterns) {
        val match = pattern.find(url)
        if (match != null) {
            val videoId = match.groupValues[1]
            return "https://www.youtube.com/shorts/$videoId"
        }
    }
    return url
}

/**
 * Loading screen
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
            Text(text = "🎬", fontSize = 80.sp)
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
 * Error screen
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
            Text(text = "😢", fontSize = 80.sp)
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
