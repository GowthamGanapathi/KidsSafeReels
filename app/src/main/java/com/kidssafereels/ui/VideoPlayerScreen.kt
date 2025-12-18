@file:OptIn(ExperimentalFoundationApi::class)

package com.kidssafereels.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
                        message = "No videos found!",
                        onRetry = { viewModel.refresh() }
                    )
                } else {
                    VideoPlayerWithButtons(videos = state.videos, viewModel = viewModel)
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
 * Convert any YouTube URL to Shorts format
 */
fun getYouTubeShortsUrl(url: String): String {
    val patterns = listOf(
        Regex("youtube\\.com/shorts/([a-zA-Z0-9_-]+)"),
        Regex("youtu\\.be/([a-zA-Z0-9_-]+)"),
        Regex("youtube\\.com/watch\\?v=([a-zA-Z0-9_-]+)"),
        Regex("youtube\\.com/embed/([a-zA-Z0-9_-]+)")
    )
    for (pattern in patterns) {
        pattern.find(url)?.let { 
            val videoId = it.groupValues[1]
            return "https://m.youtube.com/shorts/$videoId"
        }
    }
    return url
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoPlayerWithButtons(
    videos: List<Video>,
    viewModel: VideoViewModel
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    
    // Create WebView once and reuse
    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            
            // Enable cookies
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
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                // Mobile user agent
                userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            }
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    isLoading = false
                }
                
                // Block navigation to other videos
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val requestUrl = request?.url?.toString() ?: return true
                    // Only allow the initial shorts URL, block all other navigation
                    return !requestUrl.contains("/shorts/")
                }
            }
            
            webChromeClient = WebChromeClient()
            setBackgroundColor(android.graphics.Color.BLACK)
        }
    }
    
    // Load video when index changes
    LaunchedEffect(currentIndex) {
        isLoading = true
        viewModel.setCurrentVideoIndex(currentIndex)
        val url = getYouTubeShortsUrl(videos[currentIndex].url)
        webView.loadUrl(url)
    }
    
    // Cleanup
    DisposableEffect(Unit) {
        onDispose { webView.destroy() }
    }
    
    val currentVideo = videos.getOrNull(currentIndex) ?: return
    
    fun goNext() {
        currentIndex = (currentIndex + 1) % videos.size
    }
    
    fun goPrev() {
        currentIndex = if (currentIndex - 1 < 0) videos.size - 1 else currentIndex - 1
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // WebView
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )
        
        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
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
                .padding(top = 45.dp, bottom = 30.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🌟 Kids Safe Reels 🌟",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // PREV button - Left side
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
                .size(65.dp)
                .clip(CircleShape)
                .background(KidsPurple)
                .clickable { goPrev() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous",
                tint = Color.White,
                modifier = Modifier.size(45.dp)
            )
        }
        
        // NEXT button - Right side
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .size(65.dp)
                .clip(CircleShape)
                .background(KidsPink)
                .clickable { goNext() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier.size(45.dp)
            )
        }
        
        // Bottom bar with video info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Title
            Text(
                text = currentVideo.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Description
            currentVideo.description?.let { desc ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Dots and counter
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val colors = listOf(KidsPurple, KidsPink, KidsBlue, KidsGreen, KidsOrange, KidsYellow)
                repeat(minOf(videos.size, 10)) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentIndex) 14.dp else 10.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == currentIndex) colors[index % colors.size]
                                else Color.White.copy(alpha = 0.3f)
                            )
                            .clickable { currentIndex = index }
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = "${currentIndex + 1} / ${videos.size}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(30.dp))
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
