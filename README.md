# 🌟 Kids Safe Reels

A safe, curated video player for kids. Show only the educational and fun YouTube videos you choose - no algorithms, no surprises!

![Platform](https://img.shields.io/badge/Platform-Android-green)
![Min SDK](https://img.shields.io/badge/Min%20SDK-24%20(Android%207.0)-blue)

## ✨ Features

- 📱 **TikTok/Reels-style vertical swipe** - Kids can easily swipe through videos
- 🎥 **YouTube support** - Works with YouTube Shorts and regular YouTube videos
- 🔒 **You control the content** - Only shows videos you've curated
- 🔄 **Easy updates** - Add new videos without app updates
- 📶 **Syncs on app open** - New videos appear automatically
- 🎨 **Kid-friendly UI** - Colorful, fun design
- ▶️ **Auto-play & loop** - Videos play automatically

## 🚀 Quick Setup Guide

### Step 1: Create Your Video List on GitHub Gist

1. Go to [https://gist.github.com](https://gist.github.com) (create a GitHub account if needed)

2. Click **"+"** to create a new gist

3. **Filename:** Enter `videos.json`

4. **Content:** Paste this template and customize with your YouTube videos:

```json
[
  {
    "id": "1",
    "title": "🔤 Learn ABC",
    "url": "https://youtube.com/shorts/baomxpKyoNs",
    "description": "Fun alphabet song!"
  },
  {
    "id": "2",
    "title": "🔢 Counting 1-10",
    "url": "https://youtube.com/shorts/hjIBu2z0isU",
    "description": "Learn to count!"
  }
]
```

5. Click **"Create secret gist"** (or public if you prefer)

6. Click the **"Raw"** button to get the raw URL

7. Copy the URL - it looks like: `https://gist.githubusercontent.com/YOUR_USERNAME/GIST_ID/raw/videos.json`

### Step 2: Configure the App

Open `app/src/main/java/com/kidssafereels/data/VideoRepository.kt` and find this section (around line 51):

```kotlin
val gistUrl = "https://gist.githubusercontent.com/YOUR_USERNAME/GIST_ID/raw/videos.json"
```

Replace with your Gist URL from Step 1.

### Step 3: Build & Install

1. Open the project in **Android Studio**
2. Connect your Android device or start an emulator
3. Click **Run** ▶️

## 📝 Managing Your Video List

### Adding New Videos

1. Go to your GitHub Gist
2. Click **"Edit"**
3. Add new videos **at the TOP** of the list:

```json
[
  {
    "id": "new-1",
    "title": "🆕 New Fun Video",
    "url": "https://youtube.com/shorts/NEW_VIDEO_ID",
    "description": "Just added!"
  },
  // ... existing videos below ...
]
```

4. Click **"Update secret gist"**
5. Close and reopen the app - new videos appear at the top!

### Supported YouTube URL Formats

All these formats work:

| Format | Example |
|--------|---------|
| YouTube Shorts | `https://youtube.com/shorts/VIDEO_ID` |
| Regular YouTube | `https://www.youtube.com/watch?v=VIDEO_ID` |
| Short URL | `https://youtu.be/VIDEO_ID` |
| Embed URL | `https://www.youtube.com/embed/VIDEO_ID` |

### Video JSON Format

Each video needs these fields:

| Field | Required | Description |
|-------|----------|-------------|
| `id` | Yes | Unique identifier (any string) |
| `title` | Yes | Video title shown on screen |
| `url` | Yes | YouTube URL (any format above) |
| `description` | No | Optional description text |

## 🏗️ Project Structure

```
KidsSafeReels/
├── app/src/main/java/com/kidssafereels/
│   ├── MainActivity.kt          # App entry point
│   ├── data/
│   │   ├── Video.kt              # Video data model
│   │   ├── ApiService.kt         # Network API interface
│   │   └── VideoRepository.kt    # 🔴 Configure your URL here!
│   ├── viewmodel/
│   │   └── VideoViewModel.kt     # Business logic
│   └── ui/
│       ├── VideoPlayerScreen.kt  # Main UI with YouTube player
│       └── theme/Theme.kt        # Colors & theming
└── sample_videos.json            # Template for your video list
```

## 🔧 Customization

### Change App Colors

Edit `app/src/main/java/com/kidssafereels/ui/theme/Theme.kt`:

```kotlin
val KidsPurple = Color(0xFF9C27B0)  // Primary color
val KidsPink = Color(0xFFE91E63)    // Accent color
// ... change any color you like!
```

### Change App Name

Edit `app/src/main/res/values/strings.xml`:

```xml
<string name="app_name">Your App Name</string>
```

## 📱 Requirements

- Android 7.0 (API 24) or higher
- Internet connection for streaming YouTube videos

## 🛡️ Privacy

- No user data collection
- No analytics or tracking
- No third-party ads
- Content is 100% controlled by you

## 💡 Tips

1. **Keep videos short** - Kids have short attention spans, YouTube Shorts are perfect!
2. **Use emojis in titles** - Makes it more fun and engaging
3. **Add new videos at the top** - They appear first when app opens
4. **Preview videos first** - Make sure content is appropriate before adding

## 🤝 Contributing

Feel free to submit issues and pull requests!

## 📄 License

MIT License - feel free to use and modify!

---

Made with ❤️ for keeping kids safe while learning
