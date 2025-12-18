package com.kidssafereels

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kidssafereels.ui.VideoPlayerScreen
import com.kidssafereels.ui.theme.KidsSafeReelsTheme
import com.kidssafereels.viewmodel.VideoViewModel

class MainActivity : ComponentActivity() {
    
    private var videoViewModel: VideoViewModel? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            KidsSafeReelsTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    val viewModel: VideoViewModel = viewModel()
                    videoViewModel = viewModel
                    VideoPlayerScreen(viewModel = viewModel)
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh videos when app comes back to foreground
        // This ensures newly added videos are fetched
        videoViewModel?.refresh()
    }
}

