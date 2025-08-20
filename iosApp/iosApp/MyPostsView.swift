import SwiftUI
import FirebaseAuth
import FirebaseFirestore
import FirebaseStorage

struct MyPostsView: View {
    @StateObject private var postsManager = MyPostsManager()
    @Environment(\.dismiss) private var dismiss
    @State private var selectedSortOption = SortOption.newest
    @State private var showNewPost = false

    // דיאלוג מחיקה
    @State private var postIdPendingDelete: String? = nil

    enum SortOption: String, CaseIterable {
        case newest, oldest, mostLiked

        var systemImage: String {
            switch self {
            case .newest:    return "calendar.badge.clock"
            case .oldest:    return "clock.arrow.circlepath"
            case .mostLiked: return "heart.fill"
            }
        }
        var titleKey: LocalizedStringKey {
            switch self {
            case .newest: "myposts.sort.newest"
            case .oldest: "myposts.sort.oldest"
            case .mostLiked: "myposts.sort.mostLiked"
            }
        }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                VStack {
                    if postsManager.isLoading && postsManager.myPosts.isEmpty {
                        VStack(spacing: 16) {
                            ProgressView().scaleEffect(1.2)
                            Text("myposts.loading").foregroundColor(.secondary)
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        
                    } else if postsManager.myPosts.isEmpty {
                        VStack(spacing: 20) {
                            Text("🐾").font(.system(size: 60))
                            Text("myposts.empty.title")
                                .font(.title2).fontWeight(.bold)
                                .multilineTextAlignment(.center)
                            Text("myposts.empty.subtitle")
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal)
                            Button("myposts.empty.cta") { showNewPost = true }
                                .buttonStyle(ProfileButtonStyle(color: .logoBrown))
                                .padding(.horizontal, 40)
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        
                    } else {
                        VStack(spacing: 0) {
                            headerView
                            ScrollView {
                                LazyVStack(spacing: 16) {
                                    ForEach(sortedPosts) { post in
                                        MyPostCard(
                                            post: post,
                                            onDeleted: { postsManager.removePost(post.id) },
                                            onAskDelete: { p in
                                                postIdPendingDelete = p.id
                                            }
                                        )
                                    }
                                }
                                .padding()
                            }
                            .refreshable { postsManager.loadMyPosts() }
                        }
                    }
                }
                
                if !postsManager.myPosts.isEmpty {
                    VStack {
                        Spacer()
                        HStack {
                            Spacer()
                            Button { showNewPost = true } label: {
                                Image(systemName: "plus")
                                    .font(.title2)
                                    .foregroundColor(.white)
                                    .frame(width: 56, height: 56)
                                    .background(Color.logoBrown)
                                    .clipShape(Circle())
                                    .shadow(radius: 4)
                            }
                            .padding(.trailing, 16)
                            .padding(.bottom, 16)
                        }
                    }
                }
            }
            .navigationTitle(Text("myposts.title"))
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("common.close") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    HStack {
                        Button { showNewPost = true } label: {
                            Image(systemName: "plus").foregroundColor(.logoBrown)
                        }
                        Menu {
                            ForEach(SortOption.allCases, id: \.self) { option in
                                Button { selectedSortOption = option } label: {
                                    Label { Text(option.titleKey) } icon: { Image(systemName: option.systemImage) }
                                }
                            }
                        } label: { Image(systemName: "arrow.up.arrow.down") }
                    }
                }
            }
        }
        .onAppear { postsManager.loadMyPosts() }
        .sheet(isPresented: $showNewPost) {
            NewPostView().onDisappear { postsManager.loadMyPosts() }
        }
        // דיאלוג מחיקה
        .confirmationDialog(
            String(localized: "myposts.delete.title"),
            isPresented: Binding(
                get: { postIdPendingDelete != nil },
                set: { if !$0 { postIdPendingDelete = nil } }
            ),
            titleVisibility: .visible
        ) {
            Button("common.delete", role: .destructive) {
                guard let id = postIdPendingDelete,
                      let p = postsManager.myPosts.first(where: { $0.id == id }) else { return }
                postsManager.removePost(p.id)
                
                PostDeletionService.delete(post: p) { res in
                    switch res {
                    case .success:
                        HapticFeedback.notification(.success)
                    case .failure(let e):
                        print("Error deleting post: \(e)")
                        HapticFeedback.notification(.error)
                    }
                }
                postIdPendingDelete = nil
            }
            Button("common.cancel", role: .cancel) { postIdPendingDelete = nil }
        } message: {
            Text("myposts.delete.message")
        }
    }

    private var headerView: some View {
        VStack(spacing: 12) {
            HStack(spacing: 20) {
                StatSummaryItem(title: String(localized: "myposts.stat.posts"),
                                value: "\(postsManager.myPosts.count)",
                                color: .pink)
                StatSummaryItem(title: String(localized: "myposts.stat.total_likes"),
                                value: "\(postsManager.totalLikes)",
                                color: .red)
                StatSummaryItem(title: String(localized: "myposts.stat.avg_likes"),
                                value: postsManager.myPosts.isEmpty
                                       ? "0"
                                       : String(format: "%.1f",
                                                Double(postsManager.totalLikes) / Double(postsManager.myPosts.count)),
                                color: .purple)
            }
            .padding(.horizontal)
            Divider()
            HStack {
                Image(systemName: selectedSortOption.systemImage).foregroundColor(.logoBrown)
                Text("myposts.sorted_by")
                    .font(.caption).foregroundColor(.secondary)
                Text(selectedSortOption.titleKey)
                    .font(.caption).foregroundColor(.secondary)
                Spacer()
                Button("myposts.new_post") { showNewPost = true }
                    .font(.caption).foregroundColor(.logoBrown)
            }
            .padding(.horizontal)
        }
        .padding(.vertical, 8)
        .background(Color(.systemGroupedBackground))
    }

    private var sortedPosts: [FeedPost] {
        switch selectedSortOption {
        case .newest:    return postsManager.myPosts.sorted { $0.timestamp > $1.timestamp }
        case .oldest:    return postsManager.myPosts.sorted { $0.timestamp < $1.timestamp }
        case .mostLiked: return postsManager.myPosts.sorted { $0.likes > $1.likes }
        }
    }
}

// MARK: - Stat Summary Item
struct StatSummaryItem: View {
    let title: String
    let value: String
    let color: Color

    var body: some View {
        VStack(spacing: 4) {
            Text(value).font(.title3).fontWeight(.bold).foregroundColor(color)
            Text(title).font(.caption2).foregroundColor(.secondary)
        }
        .frame(height: 80)
        .frame(maxWidth: .infinity)
        .background(color.opacity(0.1))
        .cornerRadius(12)
    }
}

// MARK: - My Post Card
struct MyPostCard: View {
    let post: FeedPost
    let onDeleted: () -> Void
    var onAskDelete: (FeedPost) -> Void = { _ in }
    
    @State private var showEditPost = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header with date and actions
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(timeAgo(from: Date(timeIntervalSince1970: post.timestamp)))
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    if !post.location.isEmpty {
                        HStack(spacing: 4) {
                            Image(systemName: "location.fill").font(.caption2)
                            Text(post.location).font(.caption2)
                        }
                        .foregroundColor(.secondary)
                    }
                }
                
                Spacer()
                
                HStack(spacing: 8) {
                    Button {
                        HapticFeedback.selection()
                        showEditPost = true
                    } label: {
                        Image(systemName: "pencil").foregroundColor(.logoBrown)
                    }
                    
                    Button {
                        HapticFeedback.impact(.light)
                        onAskDelete(post)
                    } label: {
                        Image(systemName: "trash").foregroundColor(.red)
                    }
                    .buttonStyle(.plain)
                    .contentShape(Rectangle())
                }
            }
            
            // Image
            if !post.imageUrl.isEmpty, let url = URL(string: post.imageUrl) {
                AsyncImage(url: url) { image in
                    image.resizable().aspectRatio(contentMode: .fill)
                } placeholder: {
                    Rectangle().fill(Color.gray.opacity(0.3)).overlay { ProgressView() }
                }
                .frame(height: 200).clipped().cornerRadius(8)
            }
            
            // Text content
            if !post.text.isEmpty {
                Text(post.text).font(.body)
            }
            
            // Stats
            HStack(spacing: 16) {
                HStack(spacing: 4) {
                    Image(systemName: "heart.fill").foregroundColor(.red).font(.caption)
                    Text("\(post.likes)").font(.caption).foregroundColor(.secondary)
                }
                HStack(spacing: 4) {
                    Image(systemName: "message.fill").foregroundColor(.blue).font(.caption)
                    Text("--").font(.caption).foregroundColor(.secondary)
                }
                Spacer()
                let performance = performanceLevel(likes: post.likes)
                HStack(spacing: 4) {
                    Text(performance.emoji)
                    Text(performance.titleKey).font(.caption2).foregroundColor(.secondary)
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(radius: 2)
        .sheet(isPresented: $showEditPost) {
            EditPostView(post: post) { }
        }
    }
    
    // לייבלים יחסיים לפי השפה במכשיר
    private func timeAgo(from date: Date) -> String {
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .short
        formatter.locale = Locale.current
        return formatter.localizedString(for: date, relativeTo: Date())
    }

    private func performanceLevel(likes: Int) -> (emoji: String, titleKey: LocalizedStringKey) {
        switch likes {
        case 0: return ("😐", "myposts.performance.none")
        case 1...5: return ("👍", "myposts.performance.starting")
        case 6...15: return ("🔥", "myposts.performance.popular")
        case 16...30: return ("⭐", "myposts.performance.star")
        default: return ("🚀", "myposts.performance.viral")
        }
    }

    // MARK: - Edit Post View
    struct EditPostView: View {
        let post: FeedPost
        let onSaved: () -> Void
        
        @Environment(\.dismiss) private var dismiss
        @State private var editedText: String = ""
        @State private var isLoading = false
        @State private var errorMessage: String?
        
        var body: some View {
            NavigationStack {
                VStack(spacing: 20) {
                    if !post.imageUrl.isEmpty, let url = URL(string: post.imageUrl) {
                        VStack(alignment: .leading) {
                            Text("editpost.current_image").font(.headline).padding(.horizontal)
                            AsyncImage(url: url) { image in
                                image.resizable().aspectRatio(contentMode: .fill)
                            } placeholder: {
                                Rectangle().fill(Color.gray.opacity(0.3)).overlay { ProgressView() }
                            }
                            .frame(height: 200).clipped().cornerRadius(8).padding(.horizontal)
                        }
                    }
                    VStack(alignment: .leading, spacing: 8) {
                        Text("editpost.post_content").font(.headline).padding(.horizontal)
                        TextEditor(text: $editedText)
                            .frame(minHeight: 120)
                            .padding(8)
                            .background(Color.gray.opacity(0.1))
                            .cornerRadius(8)
                            .padding(.horizontal)
                    }
                    if let errorMessage {
                        Text(errorMessage).foregroundColor(.red).font(.caption).padding(.horizontal)
                    }
                    Spacer()
                }
                .navigationTitle(Text("editpost.title"))
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button("common.cancel") { dismiss() }
                    }
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button("common.save") { savePost() }
                            .disabled(isLoading || editedText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    }
                }
            }
            .onAppear { editedText = post.text }
        }
        
        private func savePost() {
            let trimmedText = editedText.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !trimmedText.isEmpty else {
                errorMessage = String(localized: "errors.post.empty")
                return
            }
            isLoading = true
            errorMessage = nil
            
            let db = Firestore.firestore()
            db.collection("posts").document(post.id).updateData([
                "text": trimmedText
            ]) { error in
                DispatchQueue.main.async {
                    isLoading = false
                    if let error = error {
                        errorMessage = String(localized: "errors.post.save_failed") + ": \(error.localizedDescription)"
                    } else {
                        onSaved()
                        dismiss()
                    }
                }
            }
        }
    }
}
