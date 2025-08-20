// MainAppStructure.swift
import SwiftUI
import FirebaseAuth
import Combine
import Foundation

struct MainAppStructure: View {
    @EnvironmentObject var authManager: AuthManager
    @State private var refreshToken = UUID()

    var body: some View {
        Group {
            if authManager.isLoggedIn { MainTabView() }
            else { LoginView() }
        }
        .id(refreshToken)
        .onReceive(NotificationCenter.default.publisher(for: Notification.Name.languageChanged)) { _ in
            refreshToken = UUID() // מרענן את כל ההיררכיה
        }
        .onAppear {
            let lang = UserDefaults.standard.string(forKey: "selectedLanguage") ?? "he"
            Bundle.setLanguage(lang)
        }
    }
}

struct MainTabView: View {
    @EnvironmentObject var authManager: AuthManager
    @State private var selectedTab = 0
    
    var body: some View {
        TabView(selection: $selectedTab) {
            FeedView()
                .tabItem { Image(systemName: "house.fill"); Text("בית") }
                .tag(0)

            MapView()
                .tabItem { Image(systemName: "map.fill"); Text("מפה") }
                .tag(1)

            ProfileView()
                .tabItem { Image(systemName: "person.crop.circle.fill"); Text("פרופיל") }
                .tag(2)

            StatisticsView()
                .tabItem { Image(systemName: "chart.bar.fill"); Text("נתונים") }
                .tag(3)
        }
        .accentColor(.logoBrown)
    }
}

extension Color {
    static let logoBrown = Color(red: 0.6, green: 0.4, blue: 0.2)
    static let logoBackground = Color(red: 0.98, green: 0.97, blue: 0.95)
    static let petPalsBlue = Color(red: 0.2, green: 0.6, blue: 0.8)
    static let petPalsGreen = Color(red: 0.3, green: 0.7, blue: 0.4)
}
