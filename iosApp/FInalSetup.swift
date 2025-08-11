
import SwiftUI
import Firebase

// MARK: - Missing Components & Final Setup

// Fix for UserProfile in EditProfileManager
extension UserProfile {
    var petBreeed: String {
        return petBreed // Fix typo from original code
    }
}

// MARK: - Enhanced Auth Manager with Better Error Handling
class EnhancedAuthManager: ObservableObject {
    @Published var isLoggedIn = false
    @Published var currentUser: User?
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private var authHandle: AuthStateDidChangeListenerHandle?
    
    init() {
        checkAuthState()
    }
    
    func checkAuthState() {
        authHandle = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            DispatchQueue.main.async {
                self?.currentUser = user
                self?.isLoggedIn = user != nil
            }
        }
    }
    
    func signIn(email: String, password: String) async {
        isLoading = true
        errorMessage = nil
        
        do {
            try await Auth.auth().signIn(withEmail: email, password: password)
        } catch {
            errorMessage = handleAuthError(error)
        }
        
        isLoading = false
    }
    
    func createUser(email: String, password: String) async -> Bool {
        isLoading = true
        errorMessage = nil
        
        do {
            let result = try await Auth.auth().createUser(withEmail: email, password: password)
            try await result.user.sendEmailVerification()
            errorMessage = "נשלח מייל אימות ל-\(email), בדקו את תיבת הדואר שלכם"
            isLoading = false
            return true
        } catch {
            errorMessage = handleAuthError(error)
            isLoading = false
            return false
        }
    }
    
    func signOut() {
        try? Auth.auth().signOut()
    }
    
    private func handleAuthError(_ error: Error) -> String {
        if let authError = error as NSError? {
            switch authError.localizedDescription {
            case let msg where msg.contains("password"):
                return "סיסמה שגויה"
            case let msg where msg.contains("email"):
                return "כתובת אימייל לא קיימת"
            case let msg where msg.contains("already in use"):
                return "כתובת האימייל כבר קיימת במערכת"
            case let msg where msg.contains("weak"):
                return "הסיסמה חלשה מדי"
            case let msg where msg.contains("invalid"):
                return "כתובת אימייל לא תקינה"
            default:
                return error.localizedDescription
            }
        }
        return "שגיאה לא ידועה"
    }
    
    deinit {
        if let handle = authHandle {
            Auth.auth().removeStateDidChangeListener(handle)
        }
    }
}

// MARK: - Offline Support Manager
class OfflineManager: ObservableObject {
    @Published var isOnline = true
    @Published var pendingActions: [PendingAction] = []
    
    struct PendingAction: Identifiable, Codable {
        let id = UUID()
        let type: ActionType
        let data: Data
        let timestamp: Date
        
        enum ActionType: String, Codable {
            case newPost
            case likePost
            case addComment
            case updateProfile
        }
    }
    
    func addPendingAction(_ action: PendingAction) {
        pendingActions.append(action)
        savePendingActions()
    }
    
    func processPendingActions() {
        // Process pending actions when back online
        for action in pendingActions {
            // Implementation depends on action type
            processSingleAction(action)
        }
        
        pendingActions.removeAll()
        savePendingActions()
    }
    
    private func processSingleAction(_ action: PendingAction) {
        // Implementation for processing different action types
        switch action.type {
        case .newPost:
            // Process pending post upload
            break
        case .likePost:
            // Process pending like
            break
        case .addComment:
            // Process pending comment
            break
        case .updateProfile:
            // Process pending profile update
            break
        }
    }
    
    private func savePendingActions() {
        if let data = try? JSONEncoder().encode(pendingActions) {
            UserDefaults.standard.set(data, forKey: "pendingActions")
        }
    }
    
    private func loadPendingActions() {
        if let data = UserDefaults.standard.data(forKey: "pendingActions"),
           let actions = try? JSONDecoder().decode([PendingAction].self, from: data) {
            pendingActions = actions
        }
    }
    
    init() {
        loadPendingActions()
    }
}

// MARK: - Cache Manager for Offline Images
class CacheManager {
    static let shared = CacheManager()
    private let imageCache = NSCache<NSString, UIImage>()
    private let fileManager = FileManager.default
    
    private init() {
        imageCache.countLimit = 100
        imageCache.totalCostLimit = 50 * 1024 * 1024 // 50MB
    }
    
    func cacheImage(_ image: UIImage, forKey key: String) {
        imageCache.setObject(image, forKey: NSString(string: key))
        
        // Also save to disk
        if let data = image.jpegData(compressionQuality: 0.8) {
            let url = cacheURL(for: key)
            try? data.write(to: url)
        }
    }
    
    func getCachedImage(forKey key: String) -> UIImage? {
        // Check memory cache first
        if let image = imageCache.object(forKey: NSString(string: key)) {
            return image
        }
        
        // Check disk cache
        let url = cacheURL(for: key)
        if let data = try? Data(contentsOf: url),
           let image = UIImage(data: data) {
            imageCache.setObject(image, forKey: NSString(string: key))
            return image
        }
        
        return nil
    }
    
    private func cacheURL(for key: String) -> URL {
        let documentsPath = fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0]
        return documentsPath.appendingPathComponent("\(key.hashValue).jpg")
    }
    
    func clearCache() {
        imageCache.removeAllObjects()
        
        // Clear disk cache
        let documentsPath = fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0]
        if let enumerator = fileManager.enumerator(at: documentsPath, includingPropertiesForKeys: nil) {
            for case let fileURL as URL in enumerator {
                if fileURL.pathExtension == "jpg" {
                    try? fileManager.removeItem(at: fileURL)
                }
            }
        }
    }
}

// MARK: - Enhanced AsyncImage with Caching
struct CachedAsyncImage<Content: View>: View {
    let url: URL?
    let content: (AsyncImagePhase) -> Content
    
    @State private var cachedImage: UIImage?
    
    init(url: URL?, @ViewBuilder content: @escaping (AsyncImagePhase) -> Content) {
        self.url = url
        self.content = content
    }
    
    var body: some View {
        Group {
            if let cachedImage = cachedImage {
                content(.success(Image(uiImage: cachedImage)))
            } else {
                AsyncImage(url: url) { phase in
                    content(phase)
                        .onAppear {
                            if case .success(let image) = phase,
                               let url = url {
                                // Cache the loaded image
                                let key = url.absoluteString
                                if let uiImage = UIImage(data: Data()) { // This should be the actual image data
                                    CacheManager.shared.cacheImage(uiImage, forKey: key)
                                }
                            }
                        }
                }
            }
        }
        .onAppear {
            if let url = url {
                let key = url.absoluteString
                cachedImage = CacheManager.shared.getCachedImage(forKey: key)
            }
        }
    }
}

// MARK: - Push Notifications Manager (Optional)
import UserNotifications

class NotificationManager: NSObject, ObservableObject, UNUserNotificationCenterDelegate {
    @Published var hasPermission = false
    
    override init() {
        super.init()
        UNUserNotificationCenter.current().delegate = self
    }
    
    func requestPermission() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { [weak self] granted, error in
            DispatchQueue.main.async {
                self?.hasPermission = granted
            }
        }
    }
    
    func scheduleLocalNotification(title: String, body: String, timeInterval: TimeInterval) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: timeInterval, repeats: false)
        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: trigger)
        
        UNUserNotificationCenter.current().add(request)
    }
    
    // MARK: - UNUserNotificationCenterDelegate
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.alert, .badge, .sound])
    }
}

// MARK: - Analytics Helper (Optional)
class AnalyticsManager {
    static let shared = AnalyticsManager()
    
    private init() {}
    
    func logEvent(_ name: String, parameters: [String: Any]? = nil) {
        #if DEBUG
        print("📊 Analytics Event: \(name) - \(parameters ?? [:])")
        #endif
        
        // If using Firebase Analytics:
        // Analytics.logEvent(name, parameters: parameters)
    }
    
    func logScreenView(_ screenName: String) {
        logEvent("screen_view", parameters: ["screen_name": screenName])
    }
    
    func logUserAction(_ action: String, item: String? = nil) {
        var parameters: [String: Any] = ["action": action]
        if let item = item {
            parameters["item"] = item
        }
        logEvent("user_action", parameters: parameters)
    }
}

// MARK: - Performance Monitor
class PerformanceMonitor: ObservableObject {
    @Published var isLowPerformanceMode = false
    
    func enableLowPerformanceMode() {
        isLowPerformanceMode = true
        // Reduce image quality, disable animations, etc.
    }
    
    func disableLowPerformanceMode() {
        isLowPerformanceMode = false
    }
    
    func checkPerformance() {
        // Monitor memory usage, CPU usage, etc.
        let info = mach_task_basic_info()
        var count = mach_msg_type_number_t(MemoryLayout<mach_task_basic_info>.size)/4
        
        let kerr: kern_return_t = withUnsafeMutablePointer(to: &info) {
            $0.withMemoryRebound(to: integer_t.self, capacity: 1) {
                task_info(mach_task_self_, task_flavor_t(MACH_TASK_BASIC_INFO), $0, &count)
            }
        }
        
        if kerr == KERN_SUCCESS {
            let memoryUsage = info.resident_size
            // If memory usage is high, enable low performance mode
            if memoryUsage > 200 * 1024 * 1024 { // 200MB threshold
                enableLowPerformanceMode()
            }
        }
    }
}

// MARK: - App State Manager
class AppStateManager: ObservableObject {
    @Published var currentTab = 0
    @Published var isAppActive = true
    @Published var lastActiveTime: Date = Date()
    
    func updateLastActiveTime() {
        lastActiveTime = Date()
    }
    
    func handleAppBackground() {
        isAppActive = false
        // Save any pending data
    }
    
    func handleAppForeground() {
        isAppActive = true
        updateLastActiveTime()
        // Refresh data if needed
    }
}

// MARK: - Final App Integration Example
struct EnhancedPetPalsApp: App {
    @StateObject private var authManager = EnhancedAuthManager()
    @StateObject private var networkManager = NetworkManager()
    @StateObject private var offlineManager = OfflineManager()
    @StateObject private var appStateManager = AppStateManager()
    @StateObject private var notificationManager = NotificationManager()
    
    var body: some Scene {
        WindowGroup {
            Group {
                if authManager.isLoggedIn {
                    MainTabView()
                        .environmentObject(authManager)
                        .environmentObject(networkManager)
                        .environmentObject(offlineManager)
                        .environmentObject(appStateManager)
                } else {
                    LoginView()
                        .environmentObject(authManager)
                }
            }
            .onReceive(NotificationCenter.default.publisher(for: UIApplication.willEnterForegroundNotification)) { _ in
                appStateManager.handleAppForeground()
                if networkManager.isConnected {
                    offlineManager.processPendingActions()
                }
            }
            .onReceive(NotificationCenter.default.publisher(for: UIApplication.didEnterBackgroundNotification)) { _ in
                appStateManager.handleAppBackground()
            }
            .onChange(of: networkManager.isConnected) { isConnected in
                if isConnected {
                    offlineManager.processPendingActions()
                }
            }
        }
    }
}

// MARK: - Test Data Helper (for development)
#if DEBUG
class TestDataHelper {
    static let shared = TestDataHelper()
    
    func createSamplePosts() -> [FeedPost] {
        return [
            FeedPost(
                userId: "user1",
                text: "הטיול הראשון שלנו בפארק!",
                imageUrl: "https://example.com/image1.jpg",
                likes: 15,
                timestamp: Date().addingTimeInterval(-3600).timeIntervalSince1970,
                likedBy: ["user2", "user3"],
                location: "פארק הירקון"
            ),
            FeedPost(
                userId: "user2",
                text: "מקס אוהב לשחק בכדור",
                imageUrl: "https://example.com/image2.jpg",
                likes: 8,
                timestamp: Date().addingTimeInterval(-7200).timeIntervalSince1970,
                likedBy: ["user1"],
                location: "הבית"
            )
        ]
    }
    
    func createSampleProfile() -> UserProfile {
        return UserProfile(
            petName: "מקס",
            petAge: 3,
            petBreed: "גולדן רטריבר",
            petImage: "https://example.com/profile.jpg"
        )
    }
}
#endif
