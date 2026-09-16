# Mobile Apps Completion Plan

**Дата:** 28 January 2026  
**Длительность:** 1-2 месяца (4-8 недель)  
**Приоритет:** Высокий  
**Статус:** В реализации

---

## 🎯 Цель

Завершение разработки полнофункциональных мобильных приложений для iOS и Android с нативной интеграцией всех возможностей IP-CSS.

---

## 📱 iOS Приложение (SwiftUI) - 4-5 недель

### Неделя 1: Проект и архитектура

#### Структура проекта
```
platforms/client-ios/
├── IP-CSS/
│   ├── App/
│   │   ├── IP_CSSApp.swift
│   │   ├── AppDelegate.swift
│   │   └── Scenes.swift
│   ├── Core/
│   │   ├── Network/
│   │   │   ├── APIClient.swift
│   │   │   ├── WebSocketManager.swift
│   │   │   └── Endpoints.swift
│   │   ├── Database/
│   │   │   ├── CoreDataStack.swift
│   │   │   └── Entities/
│   │   └── Utils/
│   ├── Domain/
│   │   ├── Models/
│   │   ├── UseCases/
│   │   └── Repositories/
│   ├── UI/
│   │   ├── Views/
│   │   ├── ViewModels/
│   │   ├── Components/
│   │   └── Themes/
│   ├── Resources/
│   │   ├── Assets.xcassets
│   │   ├── Localizable.strings
│   │   └── Info.plist
│   └── Tests/
├── IP-CSS.xcodeproj
├── Podfile / Package.swift
└── README.md
```

#### MVVM архитектура
```swift
// Domain/Models/Camera.swift
struct Camera: Identifiable, Codable {
    let id: String
    let name: String
    let rtspUrl: String
    let status: CameraStatus
    let isRecording: Bool
    let ipAddress: String?
}

// Domain/UseCases/CameraUseCase.swift
protocol CameraUseCase {
    func getCameras() async -> Result<[Camera], Error>
    func getCameraById(_ id: String) async -> Result<Camera, Error>
    func addCamera(_ camera: Camera) async -> Result<Void, Error>
    func deleteCamera(_ id: String) async -> Result<Void, Error>
}
```

### Неделя 2: Основные экраны

#### Home Screen
```swift
// UI/Views/Home/HomeView.swift
struct HomeView: View {
    @StateObject private var viewModel = HomeViewModel()
    
    var body: some View {
        NavigationView {
            VStack {
                StatsRow(
                    online: viewModel.onlineCount,
                    offline: viewModel.offlineCount,
                    recording: viewModel.recordingCount
                )
                
                SearchBar(text: $viewModel.searchQuery)
                
                CameraGridView(cameras: viewModel.filteredCameras) { camera in
                    viewModel.selectCamera(camera)
                }
            }
            .navigationTitle("IP-CSS")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: viewModel.openSettings) {
                        Image(systemName: "gear")
                    }
                }
            }
        }
    }
}
```

#### Camera View Screen
```swift
// UI/Views/Camera/CameraView.swift
struct CameraView: View {
    @ObservedObject var viewModel: CameraViewModel
    @State private var showPTZ = false
    
    var body: some View {
        VStack {
            VideoPlayerView(streamUrl: viewModel.streamUrl)
                .aspectRatio(16/9, contentMode: .fit)
                .overlay(
                    LiveIndicator(isRecording: viewModel.isRecording)
                )
            
            ControlBar(
                onRecord: viewModel.toggleRecording,
                onScreenshot: viewModel.takeScreenshot,
                onPTZ: { showPTZ = true },
                onSettings: viewModel.openSettings
            )
            
            if showPTZ {
                PTZControlsView(
                    onDirection: viewModel.movePTZ,
                    onZoom: viewModel.zoomPTZ,
                    onPreset: viewModel.loadPreset
                )
            }
        }
    }
}
```

#### Video Player (AVFoundation)
```swift
// UI/Components/VideoPlayer/AVPlayerView.swift
struct AVPlayerView: UIViewRepresentable {
    let streamUrl: String
    
    func makeUIView(context: Context) -> PlayerUIView {
        let view = PlayerUIView()
        view.setupPlayer(url: streamUrl)
        return view
    }
    
    func updateUIView(_ uiView: PlayerUIView, context: Context) {
        uiView.updateStream(url: streamUrl)
    }
}

class PlayerUIView: UIView {
    private var player: AVPlayer?
    private var playerLayer: AVPlayerLayer?
    
    func setupPlayer(url: String) {
        guard let url = URL(string: url) else { return }
        player = AVPlayer(url: url)
        playerLayer = AVPlayerLayer(player: player)
        playerLayer?.videoGravity = .resizeAspect
        layer.addSublayer(playerLayer!)
        player?.play()
    }
}
```

### Неделя 3: PTZ и дополнительные функции

#### PTZ Controls
```swift
// UI/Components/PTZ/PTZControlsView.swift
struct PTZControlsView: View {
    let onDirection: (PTZDirection) -> Void
    let onZoom: (PTZZoom) -> Void
    let onPreset: (Int) -> Void
    
    var body: some View {
        VStack(spacing: 16) {
            // Direction pad
            DirectionPad(onDirection: onDirection)
            
            // Zoom controls
            HStack {
                ZoomButton(action: { onZoom(.in) }) {
                    Image(systemName: "plus.magnifyingglass")
                }
                ZoomButton(action: { onZoom(.out) }) {
                    Image(systemName: "minus.magnifyingglass")
                }
            }
            
            // Presets
            PresetRow(presets: 1...5, onSelect: onPreset)
        }
        .padding()
        .background(Color.gray.opacity(0.2))
        .cornerRadius(16)
    }
}
```

### Неделя 4: Уведомления и Push

#### Push Notifications
```swift
// Core/Notifications/PushNotificationManager.swift
class PushNotificationManager: NSObject, ObservableObject {
    static let shared = PushNotificationManager()
    
    func requestAuthorization() async -> Bool {
        let authOptions: UNAuthorizationOptions = [.alert, .badge, .sound]
        let result = try? await UNUserNotificationCenter.current()
            .requestAuthorization(options: authOptions)
        return result ?? false
    }
    
    func registerForRemoteNotifications() {
        DispatchQueue.main.async {
            UIApplication.shared.registerForRemoteNotifications()
        }
    }
    
    func handleNotification(_ notification: UNNotification) {
        // Process notification content
    }
}

// AppDelegate integration
extension AppDelegate: UNUserNotificationCenterDelegate {
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: 
                                @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .sound, .badge])
    }
}
```

### Неделя 5: Biometric Auth и полировка

#### Face ID / Touch ID
```swift
// Core/Security/BiometricAuth.swift
class BiometricAuthenticator {
    enum BiometricError: Error {
        case notAvailable
        case notEnrolled
        case failed
    }
    
    func authenticate() async -> Result<Void, BiometricError> {
        let context = LAContext()
        var error: NSError?
        
        guard context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, 
                                        error: &error) else {
            return .failure(.notAvailable)
        }
        
        return await withCheckedContinuation { continuation in
            context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, 
                                   localizedReason: "Authenticate to access IP-CSS") 
            { success, error in
                if success {
                    continuation.resume(returning: .success(()))
                } else {
                    continuation.resume(returning: .failure(.failed))
                }
            }
        }
    }
}
```

---

## 🤖 Android Полировка - 2-3 недели

### Неделя 1: UI/UX улучшения

#### Material 3 Theme
```kotlin
// androidApp/src/main/kotlin/com/company/ipcamera/android/ui/theme/Theme.kt
@Composable
fun IPCSSTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

### Неделя 2: Offline режим

#### Room Database
```kotlin
// androidApp/src/main/kotlin/com/company/ipcamera/android/data/local/CameraDao.kt
@Dao
interface CameraDao {
    @Query("SELECT * FROM cameras")
    suspend fun getAllCameras(): List<CameraEntity>
    
    @Query("SELECT * FROM cameras WHERE id = :id")
    suspend fun getCameraById(id: String): CameraEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCamera(camera: CameraEntity)
    
    @Delete
    suspend fun deleteCamera(camera: CameraEntity)
}

@Database(entities = [CameraEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cameraDao(): CameraDao
}
```

### Неделя 3: Кэширование и оптимизация

#### Coil Image Loading
```kotlin
// androidApp/src/main/kotlin/com/company/ipcamera/android/ui/components/CameraThumbnail.kt
@Composable
fun CameraThumbnail(
    camera: Camera,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(camera.thumbnailUrl)
            .crossfade(true)
            .build(),
        contentDescription = camera.name,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}
```

---

## 📊 Метрики успеха

| Метрика | iOS | Android |
|---------|-----|---------|
| App Store rating | 4.5+ | 4.5+ |
| Crash-free users | 99.5% | 99.5% |
| Launch time | <2s | <2s |
| App size | <50 MB | <50 MB |
| Offline support | ✅ | ✅ |
| Push notifications | ✅ | ✅ |
| Biometric auth | ✅ | ✅ |

---

## 📅 Timeline

```
Неделя 1-2: iOS проект и архитектура
Неделя 3-4: iOS основные экраны
Неделя 5: iOS Push и Biometric
Неделя 6-7: Android полировка
Неделя 8: Тестирование и релиз
```

---

## 🎯 Acceptance Criteria

### iOS:
- ✅ Home Screen со списком камер
- ✅ Camera View с видео плеером
- ✅ PTZ управление
- ✅ Timeline просмотр
- ✅ Settings экран
- ✅ Push уведомления
- ✅ Face ID / Touch ID
- ✅ Offline режим

### Android:
- ✅ Material 3 дизайн
- ✅ Offline режим (Room)
- ✅ Кэширование изображений
- ✅ Push уведомления (FCM)
- ✅ Fingerprint auth
- ✅ Оптимизация производительности

---

**Статус:** В реализации  
**Следующий шаг:** iOS project setup
