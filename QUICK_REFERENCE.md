# PlayTime Quick Reference 🎵

## 🚀 INSTANT COMMANDS

### Build & Install
```powershell
cd D:\Projects\PlayTime_Native
.\gradlew installDebug
```

### Launch App
```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb -s R58N72BVFVD shell am start -n com.eplaytime.app/.MainActivity
```

### Grant Permissions
```powershell
& $adb -s R58N72BVFVD shell pm grant com.eplaytime.app android.permission.READ_MEDIA_AUDIO
& $adb -s R58N72BVFVD shell pm grant com.eplaytime.app android.permission.POST_NOTIFICATIONS
```

### View Logs
```powershell
.\debug_app.ps1
# OR
& $adb -s R58N72BVFVD logcat | Select-String "PlayTime|AlarmReceiver|MusicService"
```

---

## 📁 PROJECT STRUCTURE

```
app/src/main/java/com/eplaytime/app/
├── MainActivity.kt                     # Entry point
├── PlayTimeApplication.kt              # Hilt app
│
├── data/
│   ├── model/
│   │   ├── Song.kt                     # Song data class
│   │   └── ScheduledTask.kt            # Alarm data class
│   ├── dao/
│   │   └── ScheduledTaskDao.kt         # Room DAO
│   ├── database/
│   │   └── PlayTimeDatabase.kt         # Room DB
│   └── repository/
│       └── MusicRepository.kt          # Media scanner
│
├── ui/
│   ├── screens/
│   │   ├── HomeScreen.kt               # Song list
│   │   └── SchedulerScreen.kt          # Alarm UI ⭐
│   ├── components/
│   │   └── MiniPlayer.kt               # Bottom player
│   ├── viewmodel/
│   │   ├── MusicViewModel.kt           # Music state
│   │   └── SchedulerViewModel.kt       # Alarm state ⭐
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
│
├── service/
│   └── MusicService.kt                 # ExoPlayer service ⭐
│
├── receiver/
│   ├── AlarmReceiver.kt                # Alarm trigger ⭐
│   └── BootReceiver.kt                 # Boot handler ⭐
│
└── util/
    └── AlarmScheduler.kt               # Alarm manager ⭐

⭐ = USP Feature Components
```

---

## 🎯 KEY FILES TO KNOW

### Core Logic
- `MusicViewModel.kt` - Playback control
- `MusicService.kt` - Background player
- `MusicRepository.kt` - Song scanner

### USP Feature
- `SchedulerScreen.kt` - Alarm UI
- `SchedulerViewModel.kt` - Alarm logic
- `AlarmScheduler.kt` - Scheduling
- `AlarmReceiver.kt` - Wake & play
- `BootReceiver.kt` - Persistence

### UI
- `HomeScreen.kt` - Main screen
- `MiniPlayer.kt` - Bottom player
- `Theme.kt` - OLED dark theme

---

## 🎨 DESIGN TOKENS

### Colors
```kotlin
Background     = #121212  // OLED Black
Accent         = #1DB954  // Spotify Green
TextPrimary    = #FFFFFF  // White
TextSecondary  = #FFFFFF60 // White 60%
Glass          = #00000070 // Black 70%
GlassBorder    = #FFFFFF20 // White 20%
```

### Typography
```kotlin
Font Family    = Oswald (Google Fonts)
Title          = Bold, 28sp
Subtitle       = Medium, 16sp
Body           = Normal, 14sp
Caption        = Normal, 12sp
```

### Spacing
```kotlin
XS  = 4dp
S   = 8dp
M   = 12dp
L   = 16dp
XL  = 24dp
XXL = 32dp
```

---

## 🔧 COMMON TASKS

### Add New Screen
1. Create `ui/screens/YourScreen.kt`
2. Add `@Composable fun YourScreen()`
3. Use Hilt ViewModel: `hiltViewModel()`
4. Apply theme: `PlayTimeTheme { }`

### Add New Database Entity
1. Create model in `data/model/`
2. Add `@Entity` annotation
3. Create DAO in `data/dao/`
4. Update `PlayTimeDatabase.kt`
5. Increment database version

### Add Dependency
1. Edit `app/build.gradle.kts`
2. Add to dependencies block
3. Sync Gradle
4. Import in code

---

## 🐛 TROUBLESHOOTING

### App won't install
```powershell
.\gradlew clean
.\gradlew installDebug
```

### No songs showing
- Grant READ_MEDIA_AUDIO permission
- Check device has music files
- View logs for errors

### Alarms not firing
- Check SCHEDULE_EXACT_ALARM permission
- Verify alarm is enabled (green switch)
- Check battery optimization settings
- View alarm list: `adb shell dumpsys alarm`

### Service crashes
```powershell
# Check logs
& $adb logcat *:E
# Filter for crashes
& $adb logcat | Select-String "FATAL|Exception"
```

---

## 📊 BUILD VARIANTS

### Debug (Current)
- Package: `com.eplaytime.app`
- Debuggable: Yes
- Minified: No
- APK: `app/build/outputs/apk/debug/`

### Release (Future)
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(...)
        signingConfig = signingConfigs.release
    }
}
```

---

## 🎁 BONUS FEATURES TO ADD

### Easy Wins:
- [ ] Dark/Light theme toggle
- [ ] Recently played list
- [ ] Favorites/Liked songs
- [ ] Sleep timer
- [ ] Crossfade between tracks

### Medium Complexity:
- [ ] Equalizer
- [ ] Lyrics display
- [ ] Queue management
- [ ] Multiple alarm support
- [ ] Custom repeat patterns

### Advanced:
- [ ] Online radio streaming
- [ ] Podcast support
- [ ] Car mode UI
- [ ] Widget
- [ ] Wear OS companion

---

## 📱 DEVICE INFO

**Current Test Device:**
- Model: SM-M315F (Samsung Galaxy M31)
- Android: 12
- Status: Connected
- ADB ID: R58N72BVFVD

---

## 🎓 LEARNING RESOURCES

### Android Docs:
- [Media3](https://developer.android.com/guide/topics/media/media3)
- [AlarmManager](https://developer.android.com/training/scheduling/alarms)
- [Room](https://developer.android.com/training/data-storage/room)
- [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)

### Compose:
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material 3](https://m3.material.io/)
- [State Management](https://developer.android.com/jetpack/compose/state)

---

## ✅ CHECKLIST BEFORE RELEASE

### Code Quality:
- [ ] Remove debug logs
- [ ] Add ProGuard rules
- [ ] Optimize images
- [ ] Enable R8 shrinking
- [ ] Add analytics (optional)

### Testing:
- [ ] Test on Android 7-14
- [ ] Test on different screen sizes
- [ ] Test with no internet
- [ ] Test with no songs
- [ ] Battery drain analysis

### Legal:
- [ ] Privacy policy
- [ ] Open source licenses
- [ ] App store description
- [ ] Screenshots
- [ ] Feature graphic

---

**Quick Start:** Just run `.\gradlew installDebug` and you're ready! 🚀
