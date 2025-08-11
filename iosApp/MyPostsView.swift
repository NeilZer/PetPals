
import SwiftUI
import FirebaseAuth
import FirebaseFirestore
import FirebaseStorage

struct MyPostsView: View {
    @StateObject private var postsManager = MyPostsManager()
    @Environment(\.dismiss) private var dismiss
    @State private var selectedSortOption = SortOption.newest
    @State private var showNewPost = false
    
    enum SortOption: String, CaseIterable {
        case newest = "החדשים ביותר"
        case oldest = "הישנים ביותר"
        case mostLiked = "הכי אהובים"
        
        var systemImage: String {
            switch self {
            case .newest: return "calendar.badge.clock"
            case .oldest: return "clock.arrow.circlepath"
            case .mostLiked: return "heart.fill"
            }
        }
    }
    
    var body: some View {
        NavigationStack {
            ZStack {
                VStack {
                    if postsManager.isLoading && postsManager.myPosts.isEmpty {
                        // Loading state
                        VStack(spacing: 16) {
                            ProgressView()
                                .scaleEffect(1.2)
                            Text("טוען את הפוסטים שלך...")
                                .foregroundColor(.secondary)
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else if postsManager.myPosts.isEmpty {
                        // Empty state with create post option
                        VStack(spacing: 20) {
                            Text("🐾")
                                .font(.system(size: 60))
                            
                            Text("עדיין לא פרסמת פוסטים")
                                .font(.title2)
                                .fontWeight(.bold)
                                .multilineTextAlignment(.center)
                            
                            Text("התחל לשתף את הרגעים המיוחדים עם חיית המחמד שלך")
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal)
                            
                            Button("צור פוסט ראשון") {
                                showNewPost = true
                            }
                            .buttonStyle(ProfileButtonStyle(color: .logoBrown))
                            .padding(.horizontal, 40)
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else {
                        // Content
                        VStack(spacing: 0) {
                            // Header with stats and sort
                            headerView
                            
                            // Posts list
                            ScrollView {
                                LazyVStack(spacing: 16) {
                                    ForEach(sortedPosts) { post in
                                        MyPostCard(
                                            post: post,
                                            onDeleted: {
                                                postsManager.removePost(post.id)
                                            }
                                        )
                                    }
                                }
                                .padding()
                            }
                            .refreshable {
                                postsManager.loadMyPosts()
                            }
                        }
                    }
                }
                
                // Floating Action Button - מוצג תמיד כשיש פוסטים
                if !postsManager.myPosts.isEmpty {
                    VStack {
                        Spacer()
                        HStack {
                            Spacer()
                            Button {
                                showNewPost = true
                            } label: {
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
            .navigationTitle("הפוסטים שלי")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("סגור") { dismiss() }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    HStack {
                        // כפתור יצירת פוסט בטולבר
                        Button {
                            showNewPost = true
                        } label: {
                            Image(systemName: "plus")
                                .foregroundColor(.logoBrown)
                        }
                        
                        Menu {
                            ForEach(SortOption.allCases, id: \.self) { option in
                                Button {
                                    selectedSortOption = option
                                } label: {
                                    Label(option.rawValue, systemImage: option.systemImage)
                                }
                            }
                        } label: {
                            Image(systemName: "arrow.up.arrow.down")
                        }
                    }
                }
            }
        }
        .onAppear {
            postsManager.loadMyPosts()
        }
        .sheet(isPresented: $showNewPost) {
            NewPostView()
                .onDisappear {
                    // רענן את הפוסטים אחרי יצירת פוסט חדש
                    postsManager.loadMyPosts()
                }
        }
    }
    func timeAgo(_ date: Date) -> String {
        let seconds = Int(Date().timeIntervalSince(date))
        let m = seconds / 60, h = m / 60, d = h / 24
        switch true {
        case seconds < 60: return "לפני \(max(1, seconds)) שנ'"
        case m < 60:      return "לפני \(m) דק'"
        case h < 24:      return "לפני \(h) שע'"
        default:          return "לפני \(d) ימ'"
        }
    }

    private var headerView: some View {
        VStack(spacing: 12) {
            // Stats summary
            HStack(spacing: 20) {
                StatSummaryItem(
                    title: "פוסטים",
                    value: "\(postsManager.myPosts.count)",
                    color: .pink
                )
                
                StatSummaryItem(
                    title: "לייקים כולל",
                    value: "\(postsManager.totalLikes)",
                    color: .red
                )
                
                StatSummaryItem(
                    title: "ממוצע לייקים",
                    value: postsManager.myPosts.isEmpty ? "0" : String(format: "%.1f", Double(postsManager.totalLikes) / Double(postsManager.myPosts.count)),
                    color: .purple
                )
            }
            .padding(.horizontal)
            
            Divider()
            
            // Sort indicator
            HStack {
                Image(systemName: selectedSortOption.systemImage)
                    .foregroundColor(.logoBrown)
                Text("מסודר לפי: \(selectedSortOption.rawValue)")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Spacer()
                
                // Quick create button in header
                Button("פוסט חדש") {
                    showNewPost = true
                }
                .font(.caption)
                .foregroundColor(.logoBrown)
            }
            .padding(.horizontal)
        }
        .padding(.vertical, 8)
        .background(Color(.systemGroupedBackground))
    }
    
    private var sortedPosts: [FeedPost] {
        switch selectedSortOption {
        case .newest:
            return postsManager.myPosts.sorted { $0.timestamp > $1.timestamp }
        case .oldest:
            return postsManager.myPosts.sorted { $0.timestamp < $1.timestamp }
        case .mostLiked:
            return postsManager.myPosts.sorted { $0.likes > $1.likes }
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
            Text(value)
                .font(.title3)
                .fontWeight(.bold)
                .foregroundColor(color)
            
            Text(title)
                .font(.caption2)
                .foregroundColor(.secondary)
        }
    }
}

// MARK: - My Post Card
struct MyPostCard: View {
    let post: FeedPost
    let onDeleted: () -> Void
    
    @State private var showDeleteConfirm = false
    @State private var showEditPost = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header with date and actions
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(timeAgo(post.timestamp))
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    if !post.location.isEmpty {
                        HStack(spacing: 4) {
                            Image(systemName: "location.fill")
                                .font(.caption2)
                            Text(post.location)
                                .font(.caption2)
                        }
                        .foregroundColor(.secondary)
                    }
                }
                
                Spacer()
                
                // Action buttons
                HStack(spacing: 8) {
                    Button {
                        HapticFeedback.selection()
                        showEditPost = true
                    } label: {
                        Image(systemName: "pencil")
                            .foregroundColor(.logoBrown)
                    }
                    
                    Button {
                        HapticFeedback.impact(.light)
                        showDeleteConfirm = true
                    } label: {
                        Image(systemName: "trash")
                            .foregroundColor(.red)
                    }
                }
            }
            
            // Image
            if !post.imageUrl.isEmpty, let url = URL(string: post.imageUrl) {
                AsyncImage(url: url) { image in
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                } placeholder: {
                    Rectangle()
                        .fill(Color.gray.opacity(0.3))
                        .overlay {
                            ProgressView()
                        }
                }
                .frame(height: 200)
                .clipped()
                .cornerRadius(8)
            }
            
            // Text content
            if !post.text.isEmpty {
                Text(post.text)
                    .font(.body)
                    .lineLimit(nil)
            }
            
            // Stats
            HStack(spacing: 16) {
                HStack(spacing: 4) {
                    Image(systemName: "heart.fill")
                        .foregroundColor(.red)
                        .font(.caption)
                    Text("\(post.likes)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                // Comments count (placeholder - you'd need to load this)
                HStack(spacing: 4) {
                    Image(systemName: "message.fill")
                        .foregroundColor(.blue)
                        .font(.caption)
                    Text("--")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                // Performance indicator
                let performance = getPerformanceLevel(likes: post.likes)
                HStack(spacing: 4) {
                    Text(performance.emoji)
                    Text(performance.text)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(radius: 2)
        .confirmationDialog(
            "מחק פוסט",
            isPresented: $showDeleteConfirm,
            titleVisibility: .visible
        ) {
            Button("מחק", role: .destructive) {
                HapticFeedback.notification(.warning)
                deletePost()
            }
            Button("ביטול", role: .cancel) { }
        } message: {
            Text("האם אתה בטוח שברצונך למחוק את הפוסט? פעולה זו לא ניתנת לביטול.")
        }
        .sheet(isPresented: $showEditPost) {
            EditPostView(post: post) {
                // Refresh posts after edit
            }
        }
    }
    private func getPerformanceLevel(likes: Int) -> (emoji: String, text: String) {
        switch likes {
        case 0: return ("😐", "אין לייקים")
        case 1...5: return ("👍", "מתחיל טוב")
        case 6...15: return ("🔥", "פופולרי")
        case 16...30: return ("⭐", "כוכב")
        default: return ("🚀", "ויראלי!")
        }
    }
    
    private func deletePost() {
        onDeleted() // מוריד מקומית מהרשימה
        
        PostDeletionService.delete(post: post) { result in
            switch result {
            case .success:
                HapticFeedback.notification(.success)
            case .failure(let error):
                print("Error deleting post: \(error)")
                HapticFeedback.notification(.error)
            }
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
                    // Current image (read-only)
                    if !post.imageUrl.isEmpty, let url = URL(string: post.imageUrl) {
                        VStack(alignment: .leading) {
                            Text("תמונה נוכחית")
                                .font(.headline)
                                .padding(.horizontal)
                            
                            AsyncImage(url: url) { image in
                                image
                                    .resizable()
                                    .aspectRatio(contentMode: .fill)
                            } placeholder: {
                                Rectangle()
                                    .fill(Color.gray.opacity(0.3))
                                    .overlay { ProgressView() }
                            }
                            .frame(height: 200)
                            .clipped()
                            .cornerRadius(8)
                            .padding(.horizontal)
                        }
                    }
                    
                    // Text editor
                    VStack(alignment: .leading, spacing: 8) {
                        Text("תוכן הפוסט")
                            .font(.headline)
                            .padding(.horizontal)
                        
                        TextEditor(text: $editedText)
                            .frame(minHeight: 120)
                            .padding(8)
                            .background(Color.gray.opacity(0.1))
                            .cornerRadius(8)
                            .padding(.horizontal)
                    }
                    
                    // Error message
                    if let errorMessage {
                        Text(errorMessage)
                            .foregroundColor(.red)
                            .font(.caption)
                            .padding(.horizontal)
                    }
                    
                    Spacer()
                }
                .navigationTitle("ערוך פוסט")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button("ביטול") { dismiss() }
                    }
                    
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button("שמור") {
                            savePost()
                        }
                        .disabled(isLoading || editedText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    }
                }
            }
            .onAppear {
                editedText = post.text
            }
        }
        
        private func savePost() {
            let trimmedText = editedText.trimmingCharacters(in: .whitespacesAndNewlines)
            
            guard !trimmedText.isEmpty else {
                errorMessage = "נא להכניס תוכן לפוסט"
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
                        errorMessage = "שגיאה בשמירת הפוסט: \(error.localizedDescription)"
                    } else {
                        onSaved()
                        dismiss()
                    }
                }
            }
        }
    }
    

}
