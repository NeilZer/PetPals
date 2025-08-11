//
//  PetPalsApp.swift
//  PetPals
//
//  Created by mymac on 10/08/2025.
//


import SwiftUI
import FirebaseCore

@main
struct PetPalsApp: App {
    @StateObject private var authManager = AuthManager()
    @StateObject private var networkManager = NetworkManager()
    
    // Register app delegate for additional configuration
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    
    init() {
        // Configure Firebase
        FirebaseApp.configure()
        
        // Configure app appearance
        configureAppearance()
    }
    
    var body: some Scene {
        WindowGroup {
            MainAppStructure()
                .environmentObject(authManager)
                .environmentObject(networkManager)
                .onAppear {
                    // Force Hebrew RTL layout if needed
                    if Locale.current.language.languageCode?.identifier == "he" {
                        UIView.appearance().semanticContentAttribute = .forceRightToLeft
                    }
                }
                .overlay {
                    // Network connectivity indicator
                    if !networkManager.isConnected {
                        NetworkOfflineView()
                    }
                }
        }
    }
    
    private func configureAppearance() {
        // Configure navigation bar appearance
        let appearance = UINavigationBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor(Color.logoBackground)
        appearance.titleTextAttributes = [
            .foregroundColor: UIColor(Color.logoBrown),
            .font: UIFont.systemFont(ofSize: 18, weight: .bold)
        ]
        appearance.largeTitleTextAttributes = [
            .foregroundColor: UIColor(Color.logoBrown),
            .font: UIFont.systemFont(ofSize: 28, weight: .bold)
        ]
        
        UINavigationBar.appearance().standardAppearance = appearance
        UINavigationBar.appearance().scrollEdgeAppearance = appearance
        UINavigationBar.appearance().compactAppearance = appearance
        
        // Configure tab bar appearance
        let tabAppearance = UITabBarAppearance()
        tabAppearance.configureWithOpaqueBackground()
        tabAppearance.backgroundColor = UIColor(Color.logoBackground)
        
        UITabBar.appearance().standardAppearance = tabAppearance
        UITabBar.appearance().scrollEdgeAppearance = tabAppearance
        
        // Configure other UI elements
        UIRefreshControl.appearance().tintColor = UIColor(Color.logoBrown)
    }
}

// MARK: - App Delegate
final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        
        // Configure Firebase (already done in App init, but kept for safety)
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
        }
        
        // Request notification permissions
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
            if granted {
                print("Notification permission granted")
                DispatchQueue.main.async {
                    application.registerForRemoteNotifications()
                }
            } else if let error = error {
                print("Notification permission error: \(error)")
            }
        }
        
        return true
    }
    
    // Handle remote notifications
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        // Convert device token to string if needed for Firebase
        let tokenParts = deviceToken.map { data in String(format: "%02.2hhx", data) }
        let token = tokenParts.joined()
        print("Device Token: \(token)")
    }
    
    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("Failed to register for remote notifications: \(error)")
    }
}

// MARK: - Network Manager
import Network
import Combine

final class NetworkManager: ObservableObject {
    @Published var isConnected = true
    @Published var connectionType: ConnectionType = .wifi
    
    enum ConnectionType {
        case wifi
        case cellular
        case ethernet
        case other
        case none
    }
    
    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "NetworkManager")
    
    init() {
        startMonitoring()
    }
    
    private func startMonitoring() {
        monitor.pathUpdateHandler = { [weak self] path in
            DispatchQueue.main.async {
                self?.isConnected = path.status == .satisfied
                self?.updateConnectionType(path)
            }
        }
        monitor.start(queue: queue)
    }
    
    private func updateConnectionType(_ path: NWPath) {
        if path.usesInterfaceType(.wifi) {
            connectionType = .wifi
        } else if path.usesInterfaceType(.cellular) {
            connectionType = .cellular
        } else if path.usesInterfaceType(.wiredEthernet) {
            connectionType = .ethernet
        } else if path.status == .satisfied {
            connectionType = .other
        } else {
            connectionType = .none
        }
    }
    
    deinit {
        monitor.cancel()
    }
}

// MARK: - Network Offline View
struct NetworkOfflineView: View {
    var body: some View {
        VStack {
            Spacer()
            
            HStack {
                Image(systemName: "wifi.slash")
                    .foregroundColor(.white)
                Text("אין חיבור לאינטרנט")
                    .font(.caption)
                    .foregroundColor(.white)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .background(Color.red)
            .cornerRadius(20)
            .shadow(radius: 4)
            
            Spacer()
                .frame(height: 100) // Above tab bar
        }
        .frame(maxWidth: .infinity)
        .allowsHitTesting(false) // Don't block touches
    }
}