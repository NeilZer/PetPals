import SwiftUI
import FirebaseCore
import UserNotifications
import Network
import Combine

@main
struct PetPalsApp: App {
    @StateObject private var authManager = AuthManager()
    @StateObject private var networkManager = NetworkManager()
    @StateObject private var lang = LanguageManager()   // מנוע השפה (עם locale ו-layoutDirection)

    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    init() {
        // Firebase — הגדרה פעם אחת בלבד (Idempotent)
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
        }
        configureAppearance()
    }

    var body: some Scene {
        WindowGroup {
            MainAppStructure()
                .environmentObject(authManager)
                .environmentObject(networkManager)
                .environmentObject(lang)
                // החלפת שפה ו-RTL/LTR בזמן ריצה
                .environment(\.locale, lang.locale)
                .environment(\.layoutDirection, lang.layoutDirection)
                .overlay(alignment: .bottom) {
                    // באנר אינטרנט מנותק
                    if !networkManager.isConnected {
                        NetworkOfflineView()
                            .transition(.move(edge: .bottom).combined(with: .opacity))
                            .padding(.bottom, 8)
                            .animation(.easeInOut(duration: 0.25), value: networkManager.isConnected)
                    }
                }
        }
    }

    private func configureAppearance() {
        // UINavigationBar
        let nav = UINavigationBarAppearance()
        nav.configureWithOpaqueBackground()
        nav.backgroundColor = UIColor(Color.logoBackground)
        nav.titleTextAttributes = [
            .foregroundColor: UIColor(Color.logoBrown),
            .font: UIFont.systemFont(ofSize: 18, weight: .bold)
        ]
        nav.largeTitleTextAttributes = [
            .foregroundColor: UIColor(Color.logoBrown),
            .font: UIFont.systemFont(ofSize: 28, weight: .bold)
        ]
        UINavigationBar.appearance().standardAppearance = nav
        UINavigationBar.appearance().scrollEdgeAppearance = nav
        UINavigationBar.appearance().compactAppearance = nav

        // UITabBar
        let tab = UITabBarAppearance()
        tab.configureWithOpaqueBackground()
        tab.backgroundColor = UIColor(Color.logoBackground)
        UITabBar.appearance().standardAppearance = tab
        UITabBar.appearance().scrollEdgeAppearance = tab

        // Pull to refresh
        UIRefreshControl.appearance().tintColor = UIColor(Color.logoBrown)
    }
}

// MARK: - AppDelegate (התראות)
final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        // בקשת הרשאות להתראות
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
            if granted {
                DispatchQueue.main.async {
                    application.registerForRemoteNotifications()
                }
            } else if let error = error {
                print("Notification permission error: \(error)")
            }
        }
        return true
    }

    func application(_ application: UIApplication,
                     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        let token = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        print("Device Token: \(token)")
    }

    func application(_ application: UIApplication,
                     didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("Failed to register for remote notifications: \(error)")
    }
}

// MARK: - Network Manager
final class NetworkManager: ObservableObject {
    @Published var isConnected = true
    @Published var connectionType: ConnectionType = .wifi

    enum ConnectionType { case wifi, cellular, ethernet, other, none }

    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "NetworkManager.monitor", qos: .background)

    init() { startMonitoring() }

    private func startMonitoring() {
        monitor.pathUpdateHandler = { [weak self] path in
            DispatchQueue.main.async {
                self?.isConnected = (path.status == .satisfied)
                self?.updateConnectionType(path)
            }
        }
        monitor.start(queue: queue)
    }

    private func updateConnectionType(_ path: NWPath) {
        if path.usesInterfaceType(.wifi) { connectionType = .wifi }
        else if path.usesInterfaceType(.cellular) { connectionType = .cellular }
        else if path.usesInterfaceType(.wiredEthernet) { connectionType = .ethernet }
        else if path.status == .satisfied { connectionType = .other }
        else { connectionType = .none }
    }

    deinit { monitor.cancel() }
}

// MARK: - Offline Banner
struct NetworkOfflineView: View {
    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "wifi.slash")
                .foregroundColor(.white)
                .accessibilityHidden(true)

            // חשוב: שימוש ב־LocalizedStringKey, לא NSLocalizedString
            Text("network.offline")
                .font(.caption)
                .foregroundColor(.white)
                .accessibilityLabel(Text("network.offline"))
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(Color.red)
        .cornerRadius(20)
        .shadow(radius: 4)
        .padding(.horizontal, 16)
        .allowsHitTesting(false)
        .accessibilityElement(children: .combine)
    }
}
