import SwiftUI
import PhotosUI
import FirebaseAuth
import FirebaseFirestore
import FirebaseStorage

// MARK: - Profile
struct ProfileView: View {
    @EnvironmentObject var authManager: AuthManager
    @StateObject private var profileManager = ProfileManager()
    @State private var showingEditProfile = false
    @State private var showingMyPosts = false
    @State private var showingSettings = false
    
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
                        Button("profile.my_posts") { showingMyPosts = true }
                            .buttonStyle(ProfileButtonStyle(color: .logoBrown))
                        
                        Button("profile.settings") { showingSettings = true }
                            .buttonStyle(ProfileButtonStyle(color: .petPalsBlue))
                        
                        Button("profile.sign_out") { authManager.signOut() }
                            .buttonStyle(ProfileButtonStyle(color: .red))
                    }
                    
                    Spacer(minLength: 100)
                }
                .padding()
            }
            .navigationTitle(Text("profile.title"))
            .refreshable { await profileManager.refreshData() }
        }
        .onAppear { profileManager.loadUserProfile() }
        .sheet(isPresented: $showingEditProfile) {
            EditProfileView().environmentObject(profileManager)
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
                        .overlay { ProgressView() }
                } else if let profile = profile,
                          !profile.petImage.isEmpty,
                          let url = URL(string: profile.petImage) {
                    AsyncImage(url: url) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Circle().fill(Color.gray.opacity(0.3)).overlay { Text("🐾").font(.system(size: 40)) }
                    }
                    .frame(width: 120, height: 120)
                    .clipShape(Circle())
                    .overlay { Circle().stroke(Color.logoBrown, lineWidth: 3) }
                } else {
                    Circle()
                        .fill(Color.logoBrown.opacity(0.1))
                        .frame(width: 120, height: 120)
                        .overlay { Text("🐾").font(.system(size: 40)).foregroundColor(.logoBrown) }
                        .overlay { Circle().stroke(Color.logoBrown, lineWidth: 3) }
                }
            }
            
            // Pet Details
            VStack(spacing: 8) {
                Text(profile?.petName.isEmpty == false ? profile!.petName : NSLocalizedString("profile.pick_name", comment: ""))
                    .font(.title2).fontWeight(.bold).foregroundColor(.logoBrown)
                
                if let profile = profile {
                    HStack {
                        if profile.petAge > 0 {
                            // לשיפור: אפשר להוסיף stringsdict לריבוי
                            Label("\(profile.petAge) \(NSLocalizedString("profile.years", comment: ""))", systemImage: "calendar")
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
            Button("profile.edit", action: onEditTapped)
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
            Text("profile.stats.title").font(.headline).fontWeight(.bold)
            
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 2), spacing: 12) {
                StatItemCard(title: "profile.stats.posts",   value: "\(stats.totalPosts)",         icon: "camera.fill",        color: .pink)
                StatItemCard(title: "profile.stats.likes",   value: "\(stats.totalLikes)",         icon: "heart.fill",         color: .red)
                StatItemCard(title: "profile.stats.comments",value: "\(stats.totalComments)",      icon: "message.fill",       color: .blue)
                StatItemCard(title: "profile.stats.days_in_month", value: "\(stats.activeDaysThisMonth)", icon: "calendar.badge.clock", color: .green)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(radius: 2)
    }
}

struct StatItemCard: View {
    let title: LocalizedStringKey
    let value: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon).font(.title2).foregroundColor(color)
            Text(value).font(.title3).fontWeight(.bold)
            Text(title).font(.caption).foregroundColor(.secondary)
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
                                    .resizable().aspectRatio(contentMode: .fill)
                                    .frame(width: 120, height: 120)
                                    .clipShape(Circle())
                                    .overlay { Circle().stroke(Color.logoBrown, lineWidth: 3) }
                            } else if let profile = profileManager.userProfile,
                                      !profile.petImage.isEmpty,
                                      let url = URL(string: profile.petImage) {
                                AsyncImage(url: url) { image in
                                    image.resizable().aspectRatio(contentMode: .fill)
                                } placeholder: {
                                    Circle().fill(Color.gray.opacity(0.3))
                                }
                                .frame(width: 120, height: 120)
                                .clipShape(Circle())
                                .overlay { Circle().stroke(Color.logoBrown, lineWidth: 3) }
                            } else {
                                Circle()
                                    .fill(Color.logoBrown.opacity(0.1))
                                    .frame(width: 120, height: 120)
                                    .overlay { Text("🐾").font(.system(size: 40)).foregroundColor(.logoBrown) }
                                    .overlay { Circle().stroke(Color.logoBrown, lineWidth: 3) }
                            }
                        }
                        
                        PhotosPicker(selection: $selectedItem, matching: .images) {
                            Text("profile.change_photo").foregroundColor(.logoBrown)
                        }
                    }
                    
                    // Form Fields
                    VStack(spacing: 16) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("profile.pet_name").font(.headline)
                            TextField(LocalizedStringKey("profile.pet_name.placeholder"), text: $petName)
                                .textFieldStyle(PetPalsTextFieldStyle())
                        }
                        
                        VStack(alignment: .leading, spacing: 8) {
                            Text("profile.age").font(.headline)
                            HStack {
                                Stepper(value: $petAge, in: 0...30) {
                                    Text("\(petAge) \(NSLocalizedString("profile.years", comment: ""))")
                                }
                            }
                            .padding()
                            .background(Color.white)
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.logoBrown, lineWidth: 1))
                            .cornerRadius(12)
                        }
                        
                        VStack(alignment: .leading, spacing: 8) {
                            Text("profile.breed").font(.headline)
                            TextField(LocalizedStringKey("profile.breed.placeholder"), text: $petBreed)
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
                    Button("common.save_changes") { saveProfile() }
                        .buttonStyle(ProfileButtonStyle(color: .logoBrown))
                        .disabled(isLoading || petName.isEmpty)
                }
                .padding()
            }
            .navigationTitle(Text("profile.edit.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("common.cancel") { dismiss() }
                }
            }
        }
        .onAppear { loadCurrentProfile() }
        .onChange(of: selectedItem) { newItem in
            Task {
                if let newItem,
                   let data = try? await newItem.loadTransferable(type: Data.self),
                   let image = UIImage(data: data) {
                    await MainActor.run { selectedImage = image }
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
            errorMessage = NSLocalizedString("profile.error.enter_pet_name", comment: "")
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
                await MainActor.run { isLoading = false; dismiss() }
            } catch {
                await MainActor.run {
                    isLoading = false
                    errorMessage = NSLocalizedString("profile.error.save_failed", comment: "") + ": \(error.localizedDescription)"
                }
            }
        }
    }
}

// MARK: - Profile Manager (ללא שינוי מהותי)
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
                if let data = doc?.data() { self.userProfile = UserProfile(dict: data) }
                self.loadUserStats()
            }
        }
    }
    
    private func loadUserStats() {
        guard let userId = Auth.auth().currentUser?.uid else { return }
        db.collection("posts").whereField("userId", isEqualTo: userId)
            .getDocuments { [weak self] snapshot, _ in
                DispatchQueue.main.async {
                    guard let self = self, let documents = snapshot?.documents else { return }
                    let posts = documents.count
                    let likes = documents.compactMap { $0.data()["likes"] as? Int }.reduce(0, +)
                    var totalComments = 0
                    let group = DispatchGroup()
                    for doc in documents {
                        group.enter()
                        doc.reference.collection("comments")
                            .whereField("userId", isEqualTo: userId)
                            .getDocuments { snap, _ in
                                totalComments += snap?.documents.count ?? 0
                                group.leave()
                            }
                    }
                    group.notify(queue: .main) {
                        let cal = Calendar.current
                        let thisMonth = documents.filter { d in
                            if let ts = d.data()["timestamp"] as? Double {
                                let date = Date(timeIntervalSince1970: ts)
                                let m = cal.component(.month, from: date)
                                let y = cal.component(.year, from: date)
                                let cm = cal.component(.month, from: Date())
                                let cy = cal.component(.year, from: Date())
                                return m == cm && y == cy
                            }
                            return false
                        }
                        let days = Set(thisMonth.compactMap { d in
                            if let ts = d.data()["timestamp"] as? Double {
                                let date = Date(timeIntervalSince1970: ts)
                                return cal.component(.day, from: date)
                            }
                            return nil
                        })
                        self.userStats = ProfileStats(
                            totalPosts: posts,
                            totalLikes: likes,
                            totalComments: totalComments,
                            activeDaysThisMonth: days.count
                        )
                    }
                }
            }
    }
    
    func updateProfile(petName: String, petAge: Int, petBreed: String, newImage: UIImage?) async throws {
        guard let userId = Auth.auth().currentUser?.uid else { return }
        var imageUrl = userProfile?.petImage ?? ""
        if let newImage = newImage {
            imageUrl = try await uploadImage(newImage, userId: userId)
        }
        let updatedProfile = UserProfile(petName: petName, petAge: petAge, petBreed: petBreed, petImage: imageUrl)
        try await db.collection("users").document(userId).setData(updatedProfile.asDict, merge: true)
        self.userProfile = updatedProfile
    }
    
    private func uploadImage(_ image: UIImage, userId: String) async throws -> String {
        guard let data = image.jpegData(compressionQuality: 0.8) else {
            throw NSError(domain: "ImageError", code: 0, userInfo: [NSLocalizedDescriptionKey: "Failed to convert image"])
        }
        let filename = "profileImages/\(userId)/profile_\(Int(Date().timeIntervalSince1970)).jpg"
        let ref = Storage.storage().reference().child(filename)
        let meta = StorageMetadata(); meta.contentType = "image/jpeg"
        _ = try await ref.putDataAsync(data, metadata: meta)
        let url = try await ref.downloadURL()
        return url.absoluteString
    }
    
    func refreshData() async { await MainActor.run { loadUserProfile() } }
}

// MARK: - Profile Stats Model
struct ProfileStats {
    var totalPosts = 0
    var totalLikes = 0
    var totalComments = 0
    var activeDaysThisMonth = 0
}

// MARK: - Settings
struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var lang: LanguageManager
    @AppStorage("selectedLanguage") private var selectedLanguage = "he"
    @AppStorage("enableNotifications") private var enableNotifications = true
    @AppStorage("enableLocation") private var enableLocation = true
    @State private var showLanguageSelection = false
    
    // מציג את שם השפה הנוכחית – לוקליזבילי
    private var languageDisplayName: LocalizedStringKey {
        selectedLanguage == "he" ? "language.hebrew" : "language.english"
    }
    
    var body: some View {
        NavigationStack {
            List {
                Section(header: Text("general.title")) {
                    HStack {
                        Image(systemName: "globe").foregroundColor(.blue).frame(width: 25)
                        Text("settings.language")
                        Spacer()
                        Button(languageDisplayName) { showLanguageSelection = true }
                            .foregroundColor(.secondary)
                    }
                    
                    HStack {
                        Image(systemName: "bell.fill").foregroundColor(.orange).frame(width: 25)
                        Text("settings.notifications")
                        Spacer()
                        Toggle("", isOn: $enableNotifications)
                    }
                    
                    HStack {
                        Image(systemName: "location.fill").foregroundColor(.green).frame(width: 25)
                        Text("settings.location_services")
                        Spacer()
                        Toggle("", isOn: $enableLocation)
                    }
                }
                
                Section(header: Text("support.title")) {
                    Button {
                        if let url = URL(string: "mailto:support@petpals.com") {
                            UIApplication.shared.open(url)
                        }
                    } label: {
                        HStack {
                            Image(systemName: "envelope.fill").foregroundColor(.blue).frame(width: 25)
                            Text("support.contact").foregroundColor(.primary)
                            Spacer()
                            Image(systemName: "chevron.right").foregroundColor(.secondary).font(.caption)
                        }
                    }
                    
                    Button {
                        if let url = URL(string: "itms-apps://itunes.apple.com/app/id123456789") {
                            UIApplication.shared.open(url)
                        }
                    } label: {
                        HStack {
                            Image(systemName: "star.fill").foregroundColor(.yellow).frame(width: 25)
                            Text("support.rate_app").foregroundColor(.primary)
                            Spacer()
                            Image(systemName: "chevron.right").foregroundColor(.secondary).font(.caption)
                        }
                    }
                }
                
                Section(header: Text("info.title")) {
                    HStack {
                        Image(systemName: "info.circle.fill").foregroundColor(.blue).frame(width: 25)
                        Text("info.app_version")
                        Spacer()
                        Text("1.0.0").foregroundColor(.secondary)
                    }
                }
            }
            .navigationTitle(Text("settings.title"))
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("common.close") { dismiss() }
                }
            }
        }
        .confirmationDialog(Text("settings.choose_language"), isPresented: $showLanguageSelection) {
            Button("language.hebrew")  { selectedLanguage = "he"; lang.set("he") }
            Button("language.english") { selectedLanguage = "en"; lang.set("en") }
            Button("common.cancel", role: .cancel) { }
        }
    }
}
