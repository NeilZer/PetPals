//
//  AppConstants.swift
//  PetPals
//
//  Created by mymac on 10/08/2025.
//


import SwiftUI
import Foundation
import SwiftUICore

// MARK: - App Constants
struct AppConstants {
    // Firebase Collections
    static let usersCollection = "users"
    static let postsCollection = "posts"
    static let commentsCollection = "comments"
    
    // Storage Paths
    static let profileImagesPath = "profileImages"
    static let postImagesPath = "posts"
    
    // Limits
    static let maxImageSize: CGFloat = 1024 // Max width/height for uploaded images
    static let imageCompressionQuality: CGFloat = 0.85
    static let maxPostTextLength = 500
    static let maxCommentTextLength = 200
    
    // Animation Durations
    static let shortAnimation = 0.2
    static let mediumAnimation = 0.4
    static let longAnimation = 0.6
}

// MARK: - Utility Extensions
extension String {
    var trimmed: String {
        return self.trimmingCharacters(in: .whitespacesAndNewlines)
    }
    
    var isValidEmail: Bool {
        let emailRegEx = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        let emailPred = NSPredicate(format:"SELF MATCHES %@", emailRegEx)
        return emailPred.evaluate(with: self)
    }
    
    func truncated(to length: Int) -> String {
        return self.count > length ? String(self.prefix(length)) + "..." : self
    }
}

extension UIImage {
    func resized(to size: CGSize) -> UIImage? {
        UIGraphicsBeginImageContextWithOptions(size, false, 0.0)
        defer { UIGraphicsEndImageContext() }
        draw(in: CGRect(origin: .zero, size: size))
        return UIGraphicsGetImageFromCurrentImageContext()
    }
    
    func resizedToFit(maxSize: CGFloat) -> UIImage? {
        let ratio = min(maxSize / size.width, maxSize / size.height)
        let newSize = CGSize(width: size.width * ratio, height: size.height * ratio)
        return resized(to: newSize)
    }
}

// MARK: - Loading States
enum LoadingState: Equatable {
    case idle
    case loading
    case loaded
    case error(String)
    
    var isLoading: Bool {
        if case .loading = self { return true }
        return false
    }
    
    var errorMessage: String? {
        if case .error(let message) = self { return message }
        return nil
    }
}

// MARK: - Error Handling
enum PetPalsError: LocalizedError {
    case networkError
    case authenticationRequired
    case invalidData
    case uploadFailed
    case permissionDenied
    case custom(String)
    
    var errorDescription: String? {
        switch self {
        case .networkError:
            return "בעיה בחיבור לאינטרנט"
        case .authenticationRequired:
            return "נדרשת התחברות למערכת"
        case .invalidData:
            return "מידע לא תקין"
        case .uploadFailed:
            return "העלאה נכשלה"
        case .permissionDenied:
            return "אין הרשאות מתאימות"
        case .custom(let message):
            return message
        }
    }
}

// MARK: - Haptic Feedback Helper
struct HapticFeedback {
    static func impact(_ style: UIImpactFeedbackGenerator.FeedbackStyle = .medium) {
        let generator = UIImpactFeedbackGenerator(style: style)
        generator.impactOccurred()
    }
    
    static func notification(_ type: UINotificationFeedbackGenerator.FeedbackType) {
        let generator = UINotificationFeedbackGenerator()
        generator.notificationOccurred(type)
    }
    
    static func selection() {
        let generator = UISelectionFeedbackGenerator()
        generator.selectionChanged()
    }
}

// MARK: - Image Cache Manager
final class ImageCacheManager: ObservableObject {
    static let shared = ImageCacheManager()
    private let cache = NSCache<NSString, UIImage>()
    
    private init() {
        cache.countLimit = 100 // Limit number of cached images
        cache.totalCostLimit = 50 * 1024 * 1024 // 50MB limit
    }
    
    func getImage(for key: String) -> UIImage? {
        return cache.object(forKey: NSString(string: key))
    }
    
    func setImage(_ image: UIImage, for key: String) {
        cache.setObject(image, forKey: NSString(string: key))
    }
    
    func removeImage(for key: String) {
        cache.removeObject(forKey: NSString(string: key))
    }
    
    func clearCache() {
        cache.removeAllObjects()
    }
}

// MARK: - Validation Helpers
struct ValidationHelper {
    static func validatePetName(_ name: String) -> String? {
        let trimmed = name.trimmed
        if trimmed.isEmpty {
            return "נא להכניס שם לחיית המחמד"
        }
        if trimmed.count < 2 {
            return "השם חייב להכיל לפחות 2 תווים"
        }
        if trimmed.count > 30 {
            return "השם לא יכול להכיל יותר מ-30 תווים"
        }
        return nil
    }
    
    static func validatePostText(_ text: String) -> String? {
        let trimmed = text.trimmed
        if trimmed.isEmpty {
            return "נא להכניס תוכן לפוסט"
        }
        if trimmed.count > AppConstants.maxPostTextLength {
            return "הפוסט לא יכול להכיל יותר מ-\(AppConstants.maxPostTextLength) תווים"
        }
        return nil
    }
    
    static func validateCommentText(_ text: String) -> String? {
        let trimmed = text.trimmed
        if trimmed.isEmpty {
            return "נא להכניס תוכן לתגובה"
        }
        if trimmed.count > AppConstants.maxCommentTextLength {
            return "התגובה לא יכולה להכיל יותר מ-\(AppConstants.maxCommentTextLength) תווים"
        }
        return nil
    }
    
    static func validateEmail(_ email: String) -> String? {
        let trimmed = email.trimmed
        if trimmed.isEmpty {
            return "נא להכניס כתובת אימייל"
        }
        if !trimmed.isValidEmail {
            return "כתובת אימייל לא תקינה"
        }
        return nil
    }
    
    static func validatePassword(_ password: String) -> String? {
        if password.isEmpty {
            return "נא להכניס סיסמה"
        }
        if password.count < 6 {
            return "הסיסמה חייבת להכיל לפחות 6 תווים"
        }
        return nil
    }
}

// MARK: - Custom Modifiers
struct LoadingModifier: ViewModifier {
    let isLoading: Bool
    
    func body(content: Content) -> some View {
        content
            .disabled(isLoading)
            .overlay {
                if isLoading {
                    Color.black.opacity(0.3)
                        .overlay {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                .scaleEffect(1.2)
                        }
                }
            }
    }
}

struct ShakeEffect: GeometryEffect {
    var amount: CGFloat = 10
    var shakesPerUnit = 3
    var animatableData: CGFloat

    func effectValue(size: CGSize) -> ProjectionTransform {
        ProjectionTransform(CGAffineTransform(translationX:
            amount * sin(animatableData * .pi * CGFloat(shakesPerUnit)),
            y: 0))
    }
}

// MARK: - Extension for View Modifiers
extension View {
    func loading(_ isLoading: Bool) -> some View {
        modifier(LoadingModifier(isLoading: isLoading))
    }
    
    func shake(with attempts: Int) -> some View {
        modifier(ShakeEffect(animatableData: CGFloat(attempts)))
    }
    
}

// MARK: - Analytics Helper (Placeholder)
struct AnalyticsHelper {
    static func logEvent(_ event: AnalyticsEvent) {
        // Implement Firebase Analytics or other analytics service
        print("Analytics Event: \(event.name) - \(event.parameters)")
    }
}

struct AnalyticsEvent {
    let name: String
    let parameters: [String: Any]
    
    static func postCreated(hasImage: Bool, hasLocation: Bool) -> AnalyticsEvent {
        return AnalyticsEvent(
            name: "post_created",
            parameters: [
                "has_image": hasImage,
                "has_location": hasLocation
            ]
        )
    }
    
    static func postLiked() -> AnalyticsEvent {
        return AnalyticsEvent(name: "post_liked", parameters: [:])
    }
    
    static func commentAdded() -> AnalyticsEvent {
        return AnalyticsEvent(name: "comment_added", parameters: [:])
    }
    
    static func profileUpdated() -> AnalyticsEvent {
        return AnalyticsEvent(name: "profile_updated", parameters: [:])
    }
}

// MARK: - Performance Monitor
final class PerformanceMonitor {
    static let shared = PerformanceMonitor()
    private var startTimes: [String: CFTimeInterval] = [:]
    
    private init() {}
    
    func startTimer(for operation: String) {
        startTimes[operation] = CACurrentMediaTime()
    }
    
    func endTimer(for operation: String) {
        guard let startTime = startTimes[operation] else { return }
        let duration = CACurrentMediaTime() - startTime
        print("⏱️ \(operation): \(String(format: "%.3f", duration))s")
        startTimes.removeValue(forKey: operation)
    }
}

// MARK: - Debug Helper
struct DebugHelper {
    static func log(_ message: String, file: String = #file, function: String = #function, line: Int = #line) {
        #if DEBUG
        let fileName = URL(fileURLWithPath: file).lastPathComponent
        print("🐾 [\(fileName):\(line)] \(function) - \(message)")
        #endif
    }
    
    static func logError(_ error: Error, file: String = #file, function: String = #function, line: Int = #line) {
        #if DEBUG
        let fileName = URL(fileURLWithPath: file).lastPathComponent
        print("❌ [\(fileName):\(line)] \(function) - ERROR: \(error.localizedDescription)")
        #endif
    }
}

// MARK: - Date Formatters
extension DateFormatter {
    static let shared: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "he")
        return formatter
    }()
    
    static let shortDate: DateFormatter = {
        let formatter = DateFormatter.shared
        formatter.dateStyle = .short
        formatter.timeStyle = .none
        return formatter
    }()
    
    static let shortTime: DateFormatter = {
        let formatter = DateFormatter.shared
        formatter.dateStyle = .none
        formatter.timeStyle = .short
        return formatter
    }()
    
    static let relative: RelativeDateTimeFormatter = {
        let formatter = RelativeDateTimeFormatter()
        formatter.locale = Locale(identifier: "he")
        formatter.unitsStyle = .abbreviated
        return formatter
    }()
}
