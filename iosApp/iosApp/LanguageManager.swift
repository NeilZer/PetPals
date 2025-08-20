import Foundation
import SwiftUI

final class LanguageManager: ObservableObject {
    @Published var current: String = UserDefaults.standard.string(forKey: "selectedLanguage") ?? "he"

    // ערכים לשימוש ב-Environment:
    var locale: Locale { Locale(identifier: current) }
    var layoutDirection: LayoutDirection { current == "he" ? .rightToLeft : .leftToRight }

    func set(_ code: String) {
        guard current != code else { return }
        current = code
        UserDefaults.standard.set(code, forKey: "selectedLanguage")
        Bundle.setLanguage(code) // אם את משתמשת ב-swizzle

        // אם את עדיין מאזינה לנוטיפיקציה כדי לרענן עץ ה-UI:
        NotificationCenter.default.post(name: Notification.Name.languageChanged, object: nil)
    }
}

extension Notification.Name {
    static let languageChanged = Notification.Name("app.languageChanged")
}
