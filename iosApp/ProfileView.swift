import SwiftUI
import PhotosUI
import FirebaseAuth
import FirebaseFirestore
import FirebaseStorage

struct ProfileView: View {
    @EnvironmentObject var authManager: AuthManager
    @StateObject private var profileManager = ProfileManager()
    @State private var showingEditProfile = false
    @State private var showingMyPosts = false
    @State private var showingSettings = false
    @State private var selectedTab = 0
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // Header with pet info
                    ProfileHeaderView(
                        profile: profileManager.userProfile,
                        isLoading: profileManager.isLoading,
                        onEditTapped: { showingEditProfile = true }
                    )
                    
                    // Stats Cards
                    ProfileStatsView(stats: profileManager.userStats)
                    
                    // Action Buttons
                    VStack(spacing: 12) {
                        Button("הפוסטים שלי") {
                            showingMyPosts = true
                        }
                        .buttonStyle(ProfileButtonStyle(color: .logoBrown))
                        
                        Button("הגדרות") {
                            showingSettings = true
                        }
                        .buttonStyle(ProfileButtonStyle(color: .petPalsBlue))
                        
                        Button("התנתק") {
                            authManager.signOut()
                        }
                        .buttonStyle(ProfileButtonStyle(color: .red))
                    }
                    
                    Spacer(minLength: 100)
                }
                .padding()
            }
            .navigationTitle("הפרופיל שלי")
            .refreshable {
                await profileManager.refreshData()
            }
        }
        .onAppear {
            profileManager.loadUserProfile()
        }
        .sheet(isPresented: $showingEditProfile) {
            EditProfileView()
                .environmentObject(profileManager)
        }
        .sheet(isPresented: $showingMyPosts) {
            MyPostsView()
        }
        .sheet(isPresented: $showingSettings) {
            SettingsView()
        }
    }
}


// MARK: - Profile Header
struct ProfileHeaderView: View {
    let profile: UserProfile?
    let isLoading: Bool
    let onEditTapped: () -> Void
    
    var body: some View {
        VStack(spacing: 16) {
            // Pet Image
            ZStack {
                if isLoading {
                    Circle()
                        .fill(Color.gray.opacity(0.3))
                        .frame(width: 120, height: 120)
                        .overlay {
                            ProgressView()
                        }
                } else if let profile = profile,
                          !profile.petImage.isEmpty,
                          let url = URL(string: profile.petImage) {
                    AsyncImage(url: url) { image in
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Circle()
                            .fill(Color.gray.opacity(0.3))
                            .overlay {
                                Text("🐾")
                                    .font(.system(size: 40))
                            }
                    }
                    .frame(width: 120, height: 120)
                    .clipShape(Circle())
                    .overlay {
                        Circle()
                            .stroke(Color.logoBrown, lineWidth: 3)
                    }
                } else {
                    Circle()
                        .fill(Color.logoBrown.opacity(0.1))
                        .frame(width: 120, height: 120)
                        .overlay {
                            Text("🐾")
                                .font(.system(size: 40))
                                .foregroundColor(.logoBrown)
                        }
                        .overlay {
                            Circle()
                                .stroke(Color.logoBrown, lineWidth: 3)
                        }
                }
            }
            
            // Pet Details
            VStack(spacing: 8) {
                Text(profile?.petName ?? "בחר שם לחיית המחמד")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.logoBrown)
                
                if let profile = profile {
                    HStack {
                        if profile.petAge > 0 {
                            Label("\(profile.petAge) שנים", systemImage: "calendar")
                        }
                        
                        if !profile.petBreed.isEmpty {
                            Label(profile.petBreed, systemImage: "pawprint.fill")
                        }
                    }
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                }
            }
            
            // Edit Button
            Button("ערוך פרופיל", action: onEditTapped)
                .buttonStyle(ProfileButtonStyle(color: .logoBrown, isCompact: true))
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(radius: 2)
    }
}

// MARK: - Profile Stats
struct ProfileStatsView: View {
    let stats: ProfileStats
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("סטטיסטיקות")
                .font(.headline)
                .fontWeight(.bold)
            
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 2), spacing: 12) {
                StatItemCard(
                    title: "פוסטים",
                    value: "\(stats.totalPosts)",
                    icon: "camera.fill",
                    color: .pink
                )
                
                StatItemCard(
                    title: "לייקים",
                    value: "\(stats.totalLikes)",
                    icon: "heart.fill",
                    color: .red
                )
                
                StatItemCard(
                    title: "תגובות",
                    value: "\(stats.totalComments)",
                    icon: "message.fill",
                    color: .blue
                )
                
                StatItemCard(
                    title: "ימים בחודש",
                    value: "\(stats.activeDaysThisMonth)",
                    icon: "calendar.badge.clock",
                    color: .green
                )
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(radius: 2)
    }
}

struct StatItemCard: View {
    let title: String
    let value: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(color)
            
            Text(value)
                .font(.title3)
                .fontWeight(.bold)
            
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(height: 80)
        .frame(maxWidth: .infinity)
        .background(color.opacity(0.1))
        .cornerRadius(12)
    }
}

// MARK: - Custom Button Style
struct ProfileButtonStyle: ButtonStyle {
    let color: Color
    let isCompact: Bool
    
    init(color: Color, isCompact: Bool = false) {
        self.color = color
        self.isCompact = isCompact
    }
    
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .frame(maxWidth: .infinity)
            .frame(height: isCompact ? 40 : 50)
            .background(color)
            .foregroundColor(.white)
            .cornerRadius(12)
            .scaleEffect(configuration.isPressed ? 0.95 : 1.0)
            .animation(.easeInOut(duration: 0.1), value: configuration.isPressed)
    }
}

// MARK: - Edit Profile View
struct EditProfileView: View {
    @EnvironmentObject var profileManager: ProfileManager
    @Environment(\.dismiss) private var dismiss
    
    @State private var petName = ""
    @State private var petAge = 0
    @State private var petBreed = ""
    @State private var selectedImage: UIImage?
    @State private var selectedItem: PhotosPickerItem?
    @State private var isLoading = false
    @State private var errorMessage: String?
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // Profile Image Picker
                    VStack {
                        ZStack {
                            if let selectedImage {
                                Image(uiImage: selectedImage)
                                    .resizable()
                                    .aspectRatio(contentMode: .fill)
                                    .frame(width: 120, height: 120)
                                    .clipShape(Circle())
                                    .overlay {
                                        Circle()
                                            .stroke(Color.logoBrown, lineWidth: 3)
                                    }
                            } else if let profile = profileManager.userProfile,
                                      !profile.petImage.isEmpty,
                                      let url = URL(string: profile.petImage) {
                                AsyncImage(url: url) { image in
                                    image
                                        .resizable()
                                        .aspectRatio(contentMode: .fill)
                                } placeholder: {
                                    Circle()
                                        .fill(Color.gray.opacity(0.3))
                                }
                                .frame(width: 120, height: 120)
                                .clipShape(Circle())
                                .overlay {
                                    Circle()
                                        .stroke(Color.logoBrown, lineWidth: 3)
                                }
                            } else {
                                Circle()
                                    .fill(Color.logoBrown.opacity(0.1))
                                    .frame(width: 120, height: 120)
                                    .overlay {
                                        Text("🐾")
                                            .font(.system(size: 40))
                                            .foregroundColor(.logoBrown)
                                    }
                                    .overlay {
                                        Circle()
                                            .stroke(Color.logoBrown, lineWidth: 3)
                                    }
                            }
                        }
                        
                        PhotosPicker(
                            selection: $selectedItem,
                            matching: .images
                        ) {
                            Text("שנה תמונה")
                                .foregroundColor(.logoBrown)
                        }
                    }
                    
                    // Form Fields
                    VStack(spacing: 16) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("שם חיית המחמד")
                                .font(.headline)
                            TextField("הכנס שם", text: $petName)
                                .textFieldStyle(PetPalsTextFieldStyle())
                        }
                        
                        VStack(alignment: .leading, spacing: 8) {
                            Text("גיל")
                                .font(.headline)
                            HStack {
                                Stepper(value: $petAge, in: 0...30) {
                                    Text("\(petAge) שנים")
                                }
                            }
                            .padding()
                            .background(Color.white)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(Color.logoBrown, lineWidth: 1)
                            )
                            .cornerRadius(12)
                        }
                        
                        VStack(alignment: .leading, spacing: 8) {
                            Text("גזע")
                                .font(.headline)
                            TextField("הכנס גזע", text: $petBreed)
                                .textFieldStyle(PetPalsTextFieldStyle())
                        }
                    }
                    
                    // Error Message
                    if let errorMessage {
                        Text(errorMessage)
                            .foregroundColor(.red)
                            .font(.caption)
                            .padding()
                            .background(Color.red.opacity(0.1))
                            .cornerRadius(8)
                    }
                    
                    // Save Button
                    Button("שמור שינויים") {
                        saveProfile()
                    }
                    .buttonStyle(ProfileButtonStyle(color: .logoBrown))
                    .disabled(isLoading || petName.isEmpty)
                }
                .padding()
            }
            .navigationTitle("עריכת פרופיל")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("בטל") { dismiss() }
                }
            }
        }
        .onAppear {
            loadCurrentProfile()
        }
        .onChange(of: selectedItem) { newItem in
            Task {
                if let newItem,
                   let data = try? await newItem.loadTransferable(type: Data.self),
                   let image = UIImage(data: data) {
                    await MainActor.run {
                        selectedImage = image
                    }
                }
            }
        }
    }
    
    private func loadCurrentProfile() {
        if let profile = profileManager.userProfile {
            petName = profile.petName
            petAge = profile.petAge
            petBreed = profile.petBreed
        }
    }
    
    private func saveProfile() {
        guard !petName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            errorMessage = "נא להכניס שם לחיית המחמד"
            return
        }
        
        isLoading = true
        errorMessage = nil
        
        Task {
            do {
                try await profileManager.updateProfile(
                    petName: petName.trimmingCharacters(in: .whitespacesAndNewlines),
                    petAge: petAge,
                    petBreed: petBreed.trimmingCharacters(in: .whitespacesAndNewlines),
                    newImage: selectedImage
                )
                
                await MainActor.run {
                    isLoading = false
                    dismiss()
                }
            } catch {
                await MainActor.run {
                    isLoading = false
                    errorMessage = "שגיאה בשמירת הפרופיל: \(error.localizedDescription)"
                }
            }
        }
    }
}

// MARK: - Profile Manager
@MainActor
final class ProfileManager: ObservableObject {
    @Published var userProfile: UserProfile?
    @Published var userStats = ProfileStats()
    @Published var isLoading = false
    
    private let db = Firestore.firestore()
    
    func loadUserProfile() {
        guard let userId = Auth.auth().currentUser?.uid else { return }
        
        isLoading = true
        
        db.collection("users").document(userId).getDocument { [weak self] doc, error in
            DispatchQueue.main.async {
                guard let self = self else { return }
                self.isLoading = false
                
                if let error = error {
                    print("Error loading profile: \(error)")
                    return
                }
                
                if let data = doc?.data() {
                    self.userProfile = UserProfile(dict: data)
                }
                
                self.loadUserStats()
            }
        }
    }
    
    private func loadUserStats() {
        guard let userId = Auth.auth().currentUser?.uid else { return }
        
        // Load posts count and likes
        db.collection("posts")
            .whereField("userId", isEqualTo: userId)
            .getDocuments { [weak self] snapshot, error in
                DispatchQueue.main.async {
                    guard let self = self, let documents = snapshot?.documents else { return }
                    
                    let posts = documents.count
                    let likes = documents.compactMap { $0.data()["likes"] as? Int }.reduce(0, +)
                    
                    // Count comments made by user
                    var totalComments = 0
                    let group = DispatchGroup()
                    
                    for doc in documents {
                        group.enter()
                        doc.reference.collection("comments")
                            .whereField("userId", isEqualTo: userId)
                            .getDocuments { snapshot, _ in
                                totalComments += snapshot?.documents.count ?? 0
                                group.leave()
                            }
                    }
                    
                    group.notify(queue: .main) {
                        let calendar = Calendar.current
                        let currentMonth = calendar.component(.month, from: Date())
                        let currentYear = calendar.component(.year, from: Date())
                        
                        let thisMonthPosts = documents.filter { doc in
                            if let timestamp = doc.data()["timestamp"] as? Double {
                                let date = Date(timeIntervalSince1970: timestamp)
                                let postMonth = calendar.component(.month, from: date)
                                let postYear = calendar.component(.year, from: date)
                                return postMonth == currentMonth && postYear == currentYear
                            }
                            return false
                        }
                        
                        let uniqueDays = Set(thisMonthPosts.compactMap { doc in
                            if let timestamp = doc.data()["timestamp"] as? Double {
                                let date = Date(timeIntervalSince1970: timestamp)
                                return calendar.component(.day, from: date)
                            }
                            return nil
                        })
                        
                        self.userStats = ProfileStats(
                            totalPosts: posts,
                            totalLikes: likes,
                            totalComments: totalComments,
                            activeDaysThisMonth: uniqueDays.count
                        )
                    }
                }
            }
    }
    
    func updateProfile(petName: String, petAge: Int, petBreed: String, newImage: UIImage?) async throws {
        guard let userId = Auth.auth().currentUser?.uid else { return }
        
        var imageUrl = userProfile?.petImage ?? ""
        
        // Upload new image if provided
        if let newImage = newImage {
            imageUrl = try await uploadImage(newImage, userId: userId)
        }
        
        let updatedProfile = UserProfile(
            petName: petName,
            petAge: petAge,
            petBreed: petBreed,
            petImage: imageUrl
        )
        
        try await db.collection("users").document(userId).setData(updatedProfile.asDict, merge: true)
        
        self.userProfile = updatedProfile
    }
    
    private func uploadImage(_ image: UIImage, userId: String) async throws -> String {
        guard let imageData = image.jpegData(compressionQuality: 0.8) else {
            throw NSError(domain: "ImageError", code: 0, userInfo: [NSLocalizedDescriptionKey: "Failed to convert image"])
        }
        
        let filename = "profileImages/\(userId)/profile_\(Int(Date().timeIntervalSince1970)).jpg"
        let storageRef = Storage.storage().reference().child(filename)
        
        let metadata = StorageMetadata()
        metadata.contentType = "image/jpeg"
        
        _ = try await storageRef.putDataAsync(imageData, metadata: metadata)
        let downloadURL = try await storageRef.downloadURL()
        
        return downloadURL.absoluteString
    }
    
    func refreshData() async {
        await MainActor.run {
            loadUserProfile()
        }
    }
}
import Foundation

func changeAppLanguage(to languageCode: String) {
    UserDefaults.standard.set([languageCode], forKey: "AppleLanguages")
    UserDefaults.standard.set(languageCode, forKey: "selectedLanguage")
    UserDefaults.standard.synchronize()

    Bundle.setLanguage(languageCode)       // שינוי באנדל בזמן ריצה
    NotificationCenter.default.post(name: .languageChanged, object: nil) // ריענון מסכים
}

/// הרחבה לנוטיפיקציה
extension Notification.Name {
    static let languageChanged = Notification.Name("languageChanged")
}

// MARK: - Profile Stats Model
struct ProfileStats {
    var totalPosts = 0
    var totalLikes = 0
    var totalComments = 0
    var activeDaysThisMonth = 0
}
// MARK: - Settings View
struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss
    @AppStorage("selectedLanguage") private var selectedLanguage = "he"
    @AppStorage("enableNotifications") private var enableNotifications = true
    @AppStorage("enableLocation") private var enableLocation = true
    @State private var showLanguageSelection = false

    // שם ידידותי לשפה הנבחרת
    private var languageDisplayName: String {
        switch selectedLanguage {
        case "he": return "עברית"
        case "en": return "English"
        default:   return selectedLanguage
        }
    }

    var body: some View {
        NavigationStack {
            List {
                Section("כללי") {
                    HStack {
                        Image(systemName: "globe")
                            .foregroundColor(.blue)
                            .frame(width: 25)
                        Text("שפה")
                        Spacer()
                        Button(languageDisplayName) {
                            showLanguageSelection = true
                        }
                        .foregroundColor(.secondary)
                    }

                    HStack {
                        Image(systemName: "bell.fill")
                            .foregroundColor(.orange)
                            .frame(width: 25)
                        Text("התראות")
                        Spacer()
                        Toggle("", isOn: $enableNotifications)
                    }

                    HStack {
                        Image(systemName: "location.fill")
                            .foregroundColor(.green)
                            .frame(width: 25)
                        Text("שירותי מיקום")
                        Spacer()
                        Toggle("", isOn: $enableLocation)
                    }
                }

                Section("תמיכה") {
                    Button {
                        if let url = URL(string: "mailto:support@petpals.com") {
                            UIApplication.shared.open(url)
                        }
                    } label: {
                        HStack {
                            Image(systemName: "envelope.fill")
                                .foregroundColor(.blue)
                                .frame(width: 25)
                            Text("צור קשר")
                                .foregroundColor(.primary)
                            Spacer()
                            Image(systemName: "chevron.right")
                                .foregroundColor(.secondary)
                                .font(.caption)
                        }
                    }

                    Button {
                        if let url = URL(string: "itms-apps://itunes.apple.com/app/id123456789") {
                            UIApplication.shared.open(url)
                        }
                    } label: {
                        HStack {
                            Image(systemName: "star.fill")
                                .foregroundColor(.yellow)
                                .frame(width: 25)
                            Text("דרג את האפליקציה")
                                .foregroundColor(.primary)
                            Spacer()
                            Image(systemName: "chevron.right")
                                .foregroundColor(.secondary)
                                .font(.caption)
                        }
                    }
                }

                Section("מידע") {
                    HStack {
                        Image(systemName: "info.circle.fill")
                            .foregroundColor(.blue)
                            .frame(width: 25)
                        Text("גרסת אפליקציה")
                        Spacer()
                        Text("1.0.0")
                            .foregroundColor(.secondary)
                    }
                }
            }
            .navigationTitle("הגדרות")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("סגור") { dismiss() }
                }
            }
        }
        .confirmationDialog("בחר שפה", isPresented: $showLanguageSelection) {
            Button("עברית") {
                selectedLanguage = "he"
                changeAppLanguage(to: "he")
            }
            Button("English") {
                selectedLanguage = "en"
                changeAppLanguage(to: "en")
            }
           
            Button("ביטול", role: .cancel) { }
        }
    }
}
