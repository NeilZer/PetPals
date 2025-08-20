import SwiftUI
import Foundation
import UIKit
import UserNotifications
import FirebaseAuth

// MARK: - App Constants
struct AppConstants {
    // Firestore
    static let usersCollection = "users"
    static let postsCollection = "posts"
    static let commentsCollection = "comments"

    // Storage (מותאם לכללים החדשים)
    static let profileImagesPath = "profileImages"
    static let postImagesPath    = "postImages"

    // Limits
    static let maxImageSize: CGFloat = 1024
    static let imageCompressionQuality: CGFloat = 0.85
    static let maxPostTextLength = 500
    static let maxCommentTextLength = 200

    // Animations
    static let shortAnimation = 0.2
    static let mediumAnimation = 0.4
    static let longAnimation = 0.6
}

// MARK: - Utilities
extension String {
    var trimmed: String { trimmingCharacters(in: .whitespacesAndNewlines) }

    var isValidEmail: Bool {
        let emailRegEx = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        return NSPredicate(format:"SELF MATCHES %@", emailRegEx).evaluate(with: self)
    }

    func truncated(to length: Int) -> String {
        count > length ? String(prefix(length)) + "..." : self
    }
}

extension UIImage {
    func resized(to size: CGSize) -> UIImage? {
        UIGraphicsBeginImageContextWithOptions(size, false, 0)
        defer { UIGraphicsEndImageContext() }
        draw(in: CGRect(origin: .zero, size: size))
        return UIGraphicsGetImageFromCurrentImageContext()
    }

    func resizedToFit(maxSize: CGFloat) -> UIImage? {
        guard max(size.width, size.height) > maxSize else { return self }
        let ratio = min(maxSize / size.width, maxSize / size.height)
        return resized(to: CGSize(width: size.width * ratio, height: size.height * ratio))
    }
}

// MARK: - Loading / Errors
enum LoadingState: Equatable {
    case idle, loading, loaded, error(String)

    var isLoading: Bool { if case .loading = self { return true } else { return false } }
    var errorMessage: String? { if case .error(let m) = self { return m } else { return nil } }
}

enum PetPalsError: LocalizedError {
    case networkError, authenticationRequired, invalidData, uploadFailed, permissionDenied, custom(String)

    var errorDescription: String? {
        switch self {
        case .networkError: return "בעיה בחיבור לאינטרנט"
        case .authenticationRequired: return "נדרשת התחברות למערכת"
        case .invalidData: return "מידע לא תקין"
        case .uploadFailed: return "העלאה נכשלה"
        case .permissionDenied: return "אין הרשאות מתאימות"
        case .custom(let msg): return msg
        }
    }
}

// MARK: - Haptics
struct HapticFeedback {
    static func impact(_ style: UIImpactFeedbackGenerator.FeedbackStyle = .medium) {
        UIImpactFeedbackGenerator(style: style).impactOccurred()
    }
    static func notification(_ type: UINotificationFeedbackGenerator.FeedbackType) {
        UINotificationFeedbackGenerator().notificationOccurred(type)
    }
    static func selection() { UISelectionFeedbackGenerator().selectionChanged() }
}

// MARK: - Image Cache (אחוד – זיכרון + דיסק)
final class ImageCache: ObservableObject {
    static let shared = ImageCache()

    private let mem = NSCache<NSString, UIImage>()
    private let fm = FileManager.default

    private init() {
        mem.countLimit = 150
        mem.totalCostLimit = 60 * 1024 * 1024
    }

    func image(for key: String) -> UIImage? {
        if let img = mem.object(forKey: key as NSString) { return img }
        let url = diskURL(for: key)
        guard let data = try? Data(contentsOf: url), let img = UIImage(data: data) else { return nil }
        mem.setObject(img, forKey: key as NSString)
        return img
    }

    func set(_ image: UIImage, for key: String) {
        mem.setObject(image, forKey: key as NSString)
        if let data = image.jpegData(compressionQuality: 0.85) {
            try? data.write(to: diskURL(for: key), options: .atomic)
        }
    }

    func clear() {
        mem.removeAllObjects()
        let dir = documentsDir()
        if let items = try? fm.contentsOfDirectory(at: dir, includingPropertiesForKeys: nil) {
            for url in items where url.lastPathComponent.hasPrefix("imgcache_") { try? fm.removeItem(at: url) }
        }
    }

    private func diskURL(for key: String) -> URL {
        documentsDir().appendingPathComponent("imgcache_\(key.hashValue).jpg")
    }
    private func documentsDir() -> URL {
        fm.urls(for: .documentDirectory, in: .userDomainMask)[0]
    }
}

// MARK: - CachedImage (תחליף תקין ל-CachedAsyncImage)
final class ImageLoader: ObservableObject {
    @Published var image: UIImage?
    private var task: URLSessionDataTask?

    func load(from url: URL?) {
        guard let url else { return }
        let key = url.absoluteString
        if let cached = ImageCache.shared.image(for: key) {
            image = cached
            return
        }
        task?.cancel()
        task = URLSession.shared.dataTask(with: url) { data, _, _ in
            guard let data, let img = UIImage(data: data) else { return }
            ImageCache.shared.set(img, for: key)
            DispatchQueue.main.async { self.image = img }
        }
        task?.resume()
    }

    deinit { task?.cancel() }
}

struct CachedImage<Placeholder: View>: View {
    let url: URL?
    var contentMode: ContentMode = .fill
    @ViewBuilder var placeholder: () -> Placeholder

    @StateObject private var loader = ImageLoader()

    var body: some View {
        Group {
            if let ui = loader.image {
                Image(uiImage: ui).resizable().aspectRatio(contentMode: contentMode)
            } else {
                placeholder()
            }
        }
        .onAppear { loader.load(from: url) }
    }
}

// MARK: - Validation
struct ValidationHelper {
    static func validatePetName(_ name: String) -> String? {
        let t = name.trimmed
        if t.isEmpty { return "נא להכניס שם לחיית המחמד" }
        if t.count < 2 { return "השם חייב להכיל לפחות 2 תווים" }
        if t.count > 30 { return "השם לא יכול להכיל יותר מ-30 תווים" }
        return nil
    }

    static func validatePostText(_ text: String) -> String? {
        let t = text.trimmed
        if t.isEmpty { return "נא להכניס תוכן לפוסט" }
        if t.count > AppConstants.maxPostTextLength { return "הפוסט לא יכול להכיל יותר מ-\(AppConstants.maxPostTextLength) תווים" }
        return nil
    }

    static func validateCommentText(_ text: String) -> String? {
        let t = text.trimmed
        if t.isEmpty { return "נא להכניס תוכן לתגובה" }
        if t.count > AppConstants.maxCommentTextLength { return "התגובה לא יכולה להכיל יותר מ-\(AppConstants.maxCommentTextLength) תווים" }
        return nil
    }

    static func validateEmail(_ email: String) -> String? {
        let t = email.trimmed
        if t.isEmpty { return "נא להכניס כתובת אימייל" }
        if !t.isValidEmail { return "כתובת אימייל לא תקינה" }
        return nil
    }

    static func validatePassword(_ password: String) -> String? {
        if password.isEmpty { return "נא להכניס סיסמה" }
        if password.count < 6 { return "הסיסמה חייבת להכיל לפחות 6 תווים" }
        return nil
    }
}

// MARK: - Modifiers
struct LoadingModifier: ViewModifier {
    let isLoading: Bool
    func body(content: Content) -> some View {
        content
            .disabled(isLoading)
            .overlay {
                if isLoading {
                    Color.black.opacity(0.3)
                        .overlay { ProgressView().progressViewStyle(.circular).scaleEffect(1.2) }
                }
            }
    }
}
struct ShakeEffect: GeometryEffect {
    var amount: CGFloat = 10
    var shakesPerUnit = 3
    var animatableData: CGFloat
    func effectValue(size: CGSize) -> ProjectionTransform {
        ProjectionTransform(CGAffineTransform(translationX: amount * sin(animatableData * .pi * CGFloat(shakesPerUnit)), y: 0))
    }
}
extension View {
    func loading(_ isLoading: Bool) -> some View { modifier(LoadingModifier(isLoading: isLoading)) }
    func shake(with attempts: Int) -> some View { modifier(ShakeEffect(animatableData: CGFloat(attempts))) }
}

// MARK: - Analytics (אחוד)
final class AnalyticsManager {
    static let shared = AnalyticsManager()
    private init() {}

    func logEvent(_ name: String, parameters: [String: Any]? = nil) {
        #if DEBUG
        print("📊 Analytics Event: \(name) - \(parameters ?? [:])")
        #endif
        // Firebase Analytics? -> Analytics.logEvent(name, parameters: parameters)
    }

    struct Event {
        static func postCreated(hasImage: Bool, hasLocation: Bool) -> (String, [String: Any]) {
            ("post_created", ["has_image": hasImage, "has_location": hasLocation])
        }
        static let postLiked = ("post_liked", [:] as [String: Any])
        static let commentAdded = ("comment_added", [:] as [String: Any])
        static let profileUpdated = ("profile_updated", [:] as [String: Any])
    }
}

// MARK: - Timers & Performance
/// טיימר פשוט למדידות – כדי לא להתנגש בשם הישן
final class PerfTimer {
    static let shared = PerfTimer()
    private var startTimes: [String: CFTimeInterval] = [:]
    private init() {}

    func start(_ key: String) { startTimes[key] = CACurrentMediaTime() }
    func stop(_ key: String) {
        guard let t = startTimes[key] else { return }
        print("⏱️ \(key): \(String(format: "%.3f", CACurrentMediaTime() - t))s")
        startTimes.removeValue(forKey: key)
    }
}

/// מנהל מצב ביצועים (ללא שימוש ב-mach_* כדי למנוע תלויות)
final class PerformanceModeManager: ObservableObject {
    @Published var isLowPerformanceMode = false
    func enable() { isLowPerformanceMode = true }
    func disable() { isLowPerformanceMode = false }
}

// MARK: - Offline Queue
final class OfflineManager: ObservableObject {
    @Published var isOnline = true
    @Published private(set) var pending: [PendingAction] = []

    struct PendingAction: Identifiable, Codable {
        let id = UUID()
        let type: ActionType
        let data: Data
        let timestamp: Date
        enum ActionType: String, Codable { case newPost, likePost, addComment, updateProfile }
    }

    func add(_ action: PendingAction) {
        pending.append(action); persist()
    }
    func processAll(_ handler: (PendingAction) -> Void) {
        pending.forEach(handler)
        pending.removeAll(); persist()
    }

    private func persist() {
        if let data = try? JSONEncoder().encode(pending) {
            UserDefaults.standard.set(data, forKey: "pendingActions")
        }
    }
    private func restore() {
        if let data = UserDefaults.standard.data(forKey: "pendingActions"),
           let arr = try? JSONDecoder().decode([PendingAction].self, from: data) {
            pending = arr
        }
    }
    init() { restore() }
}

// MARK: - Notifications
final class NotificationManager: NSObject, ObservableObject, UNUserNotificationCenterDelegate {
    @Published var hasPermission = false

    override init() {
        super.init()
        UNUserNotificationCenter.current().delegate = self
    }

    func requestPermission() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { [weak self] granted, _ in
            DispatchQueue.main.async { self?.hasPermission = granted }
        }
    }

    func scheduleLocal(title: String, body: String, in seconds: TimeInterval) {
        let content = UNMutableNotificationContent()
        content.title = title; content.body = body; content.sound = .default
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: seconds, repeats: false)
        let req = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: trigger)
        UNUserNotificationCenter.current().add(req)
    }

    // Foreground presentation
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.alert, .badge, .sound])
    }
}

// MARK: - Date Formatters
extension DateFormatter {
    static let shared: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale.current
        return f
    }()
    static let shortDate: DateFormatter = {
        let f = DateFormatter.shared
        f.dateStyle = .short; f.timeStyle = .none
        return f
    }()
    static let shortTime: DateFormatter = {
        let f = DateFormatter.shared
        f.dateStyle = .none; f.timeStyle = .short
        return f
    }()
}
