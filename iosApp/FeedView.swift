import CoreLocation
import SwiftUI
import FirebaseAuth
import FirebaseFirestore
import FirebaseStorage

// MARK: - Feed
struct FeedView: View {
    @StateObject private var feedManager = FeedManager()
    @State private var showNewPost = false

    // דיאלוג מחיקה מנוהל ברמת המסך
    @State private var postPendingDelete: FeedPost? = nil

    var body: some View {
        NavigationStack {
            ZStack {
                if feedManager.posts.isEmpty && !feedManager.isLoading {
                    VStack(spacing: 16) {
                        Text("🐾").font(.system(size: 48))
                        Text("ברוכים הבאים ל-PetPals!").font(.title2).bold()
                        Text("עדיין אין פוסטים בפיד").foregroundColor(.secondary)
                        Text("התחילו לשתף את הרגעים המיוחדים עם חיות המחמד שלכם!")
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                    }
                    .padding()
                } else if feedManager.isLoading && feedManager.posts.isEmpty {
                    ProgressView("טוען פוסטים...")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    ScrollView {
                        LazyVStack(spacing: 16) {
                            ForEach(feedManager.posts) { post in
                                FeedPostCard(
                                    post: post,
                                    onDeleted: { feedManager.removePostLocally(post.id) },
                                    onLikeChanged: { feedManager.refreshPosts() },
                                    onAskDelete: { p in postPendingDelete = p }   // ← בקשת מחיקה
                                )
                            }
                        }
                        .padding()
                    }
                    .refreshable { feedManager.refreshPosts() }
                }

                // FAB
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
            .navigationTitle("PetPals")
            .task { feedManager.refreshPosts() }
            .sheet(isPresented: $showNewPost) {
                NewPostView()
                    .onDisappear { feedManager.refreshPosts() }
            }
            // דיאלוג מחיקה – ברמת המסך
            .confirmationDialog(
                "למחוק את הפוסט?",
                isPresented: Binding(
                    get: { postPendingDelete != nil },
                    set: { if !$0 { postPendingDelete = nil } }
                ),
                titleVisibility: .visible
            ) {
                Button("מחק", role: .destructive) {
                    guard let p = postPendingDelete else { return }
                    // הסרה אופטימית מה־UI
                    feedManager.removePostLocally(p.id)

                    // מחיקה בענן
                    PostDeletionService.delete(post: p) { result in
                        switch result {
                        case .success:
                            HapticFeedback.notification(.success)
                        case .failure(let err):
                            print("Delete error: \(err)")
                            HapticFeedback.notification(.error)
                            // רשות: להחזיר את הפוסט אם תרצי
                        }
                    }
                    postPendingDelete = nil
                }
                Button("ביטול", role: .cancel) { postPendingDelete = nil }
            }
        }
    }
}

// MARK: - Post Card
struct FeedPostCard: View {
    let post: FeedPost
    var onDeleted: () -> Void = {}
    var onLikeChanged: () -> Void = {}
    var onAskDelete: (FeedPost) -> Void = { _ in }   // ← חדש

    @State private var comments: [Comment] = []
    @State private var newComment = ""
    @State private var showComments = false
    @State private var isLoadingComments = false
    @State private var userProfile: UserProfile?
    @State private var isLiked = false
    @State private var likesCount = 0
    @State private var heartScale: CGFloat = 1.0
    @State private var heartOpacity: Double = 0.0
    @State private var isTogglingLike = false

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header
            HStack {
                if let profile = userProfile,
                   !profile.petImage.isEmpty,
                   let url = URL(string: profile.petImage) {
                    AsyncImage(url: url) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Circle().fill(Color.gray.opacity(0.3)).overlay(Text("🐾"))
                    }
                    .frame(width: 40, height: 40)
                    .clipShape(Circle())
                } else {
                    Circle().fill(Color.gray.opacity(0.3))
                        .overlay(Text("🐾"))
                        .frame(width: 40, height: 40)
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text(userProfile?.petName ?? "משתמש")
                        .font(.headline)
                        .fontWeight(.bold)
                    HStack {
                        Text(timeAgo(post.timestamp))
                            .font(.caption)
                            .foregroundColor(.secondary)
                        if !post.location.isEmpty {
                            Text("•").font(.caption).foregroundColor(.secondary)
                            Text(post.location).font(.caption).foregroundColor(.secondary)
                        }
                    }
                }

                Spacer()

                if canDelete {
                    Button(role: .destructive) {
                        HapticFeedback.impact(.light)
                        onAskDelete(post)                   // ← פותח דיאלוג במסך
                    } label: {
                        Image(systemName: "trash")
                            .foregroundColor(.red)
                    }
                    .buttonStyle(.plain)                    // חשוב למניעת בליעות לחיצה
                    .contentShape(Rectangle())
                }
            }

            // Image + double-tap like animation
            if !post.imageUrl.isEmpty, let url = URL(string: post.imageUrl) {
                ZStack {
                    AsyncImage(url: url) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Rectangle()
                            .fill(Color.gray.opacity(0.3))
                            .frame(height: 250)
                            .overlay { ProgressView() }
                    }
                    .frame(height: 250)
                    .clipped()
                    .cornerRadius(8)
                    .onTapGesture(count: 2) { doubleTapLike() }

                    Image(systemName: "heart.fill")
                        .font(.system(size: 80, weight: .bold))
                        .foregroundColor(.white)
                        .shadow(color: .black.opacity(0.3), radius: 10)
                        .scaleEffect(heartScale)
                        .opacity(heartOpacity)
                        .animation(.spring(response: 0.4, dampingFraction: 0.6), value: heartScale)
                        .animation(.easeOut(duration: 1.0), value: heartOpacity)
                }
            }

            if !post.text.isEmpty {
                Text(post.text).font(.body)
            }

            // Actions
            HStack(spacing: 20) {
                Button(action: {
                    HapticFeedback.selection()
                    toggleLike()
                }) {
                    HStack(spacing: 4) {
                        Image(systemName: isLiked ? "heart.fill" : "heart")
                            .foregroundColor(isLiked ? .red : .gray)
                            .scaleEffect(isLiked ? 1.1 : 1.0)
                            .animation(.easeInOut(duration: 0.2), value: isLiked)
                        Text("\(likesCount)").font(.caption).foregroundColor(.gray)
                    }
                }
                .disabled(isTogglingLike)
                .scaleEffect(isTogglingLike ? 0.95 : 1.0)
                .animation(.easeInOut(duration: 0.1), value: isTogglingLike)

                Button {
                    HapticFeedback.selection()
                    showComments.toggle()
                    if showComments && comments.isEmpty { loadComments() }
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "message").foregroundColor(.gray)
                        Text("תגובות").font(.caption).foregroundColor(.gray)
                    }
                }

                Spacer()
            }

            // Comments
            if showComments {
                VStack(alignment: .leading, spacing: 8) {
                    Divider()
                    if isLoadingComments {
                        ProgressView().frame(maxWidth: .infinity)
                    } else if comments.isEmpty {
                        Text("אין תגובות עדיין").foregroundColor(.secondary)
                    } else {
                        ForEach(comments) { comment in
                            CommentRow(comment: comment, postId: post.id) { loadComments() }
                        }
                    }
                    HStack {
                        TextField("הוסף תגובה", text: $newComment)
                            .textFieldStyle(.roundedBorder)
                        Button("פרסם") {
                            HapticFeedback.impact(.light)
                            addComment()
                        }
                        .disabled(newComment.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                        .foregroundColor(.logoBrown)
                    }
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.06), radius: 2, x: 0, y: 1)
        .onAppear {
            loadUserProfile()
            loadLikeStatus()
        }
    }

    // MARK: - Actions
    private func doubleTapLike() {
        withAnimation(.spring(response: 0.4, dampingFraction: 0.6)) {
            heartScale = 1.2
            heartOpacity = 1.0
        }
        if !isLiked {
            toggleLike()
            HapticFeedback.impact(.heavy)
        } else {
            HapticFeedback.impact(.light)
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            withAnimation(.easeOut(duration: 0.8)) {
                heartScale = 1.0
                heartOpacity = 0.0
            }
        }
    }

    private func toggleLike() {
        guard let currentUserId = Auth.auth().currentUser?.uid else { return }
        guard !isTogglingLike else { return }

        isTogglingLike = true
        let db = Firestore.firestore()
        let postRef = db.collection("posts").document(post.id)

        let wasLiked = isLiked
        withAnimation(.easeInOut(duration: 0.2)) {
            if isLiked { isLiked = false; likesCount = max(0, likesCount - 1) }
            else       { isLiked = true;  likesCount += 1 }
        }

        db.runTransaction({ (transaction, errorPointer) -> Any? in
            let postDoc: DocumentSnapshot
            do { postDoc = try transaction.getDocument(postRef) }
            catch { errorPointer?.pointee = error as NSError; return nil }

            var likedBy = postDoc.data()?["likedBy"] as? [String] ?? []
            var likes = postDoc.data()?["likes"] as? Int ?? 0

            if let idx = likedBy.firstIndex(of: currentUserId) {
                likedBy.remove(at: idx); likes = max(0, likes - 1)
            } else {
                likedBy.append(currentUserId); likes += 1
            }

            transaction.updateData(["likedBy": likedBy, "likes": likes], forDocument: postRef)
            return nil
        }) { _, error in
            DispatchQueue.main.async {
                self.isTogglingLike = false
                if let error = error {
                    print("Like toggle error: \(error)")
                    withAnimation(.easeInOut(duration: 0.2)) {
                        self.isLiked = wasLiked
                        self.likesCount = wasLiked ? max(0, self.likesCount - 1) : self.likesCount + 1
                    }
                    HapticFeedback.notification(.error)
                } else {
                    HapticFeedback.notification(.success)
                    self.onLikeChanged()
                }
            }
        }
    }
    private var canDelete: Bool {
        guard let uid = Auth.auth().currentUser?.uid else { return false }
        return post.userId == uid
    }

    private func loadUserProfile() {
        let db = Firestore.firestore()
        db.collection("users").document(post.userId).getDocument { doc, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("Error loading user profile: \(error)")
                    return
                }
                if let data = doc?.data() {
                    let p = UserProfile(dict: data)
                    if p.petImage.hasPrefix("gs://") {
                        let ref = Storage.storage().reference(forURL: p.petImage)
                        ref.downloadURL { url, error in
                            if let error = error {
                                print("Error getting download URL: \(error)")
                                self.userProfile = p
                            } else if let url = url {
                                self.userProfile = UserProfile(
                                    petName: p.petName, petAge: p.petAge, petBreed: p.petBreed,
                                    petImage: url.absoluteString
                                )
                            } else {
                                self.userProfile = p
                            }
                        }
                    } else {
                        self.userProfile = p
                    }
                }
            }
        }
    }

    private func loadLikeStatus() {
        guard let currentUserId = Auth.auth().currentUser?.uid else { return }
        Firestore.firestore().collection("posts").document(post.id).getDocument { doc, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("Error loading like status: \(error)")
                    return
                }
                if let data = doc?.data() {
                    let likedBy = data["likedBy"] as? [String] ?? []
                    let likes = data["likes"] as? Int ?? 0
                    self.isLiked = likedBy.contains(currentUserId)
                    self.likesCount = likes
                }
            }
        }
    }

    private func addComment() {
        guard let userId = Auth.auth().currentUser?.uid else { return }
        let commentText = newComment.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !commentText.isEmpty else { return }

        let payload: [String: Any] = [
            "userId": userId,
            "text": commentText,
            "timestamp": Date().timeIntervalSince1970
        ]

        Firestore.firestore().collection("posts").document(post.id)
            .collection("comments").addDocument(data: payload) { error in
                DispatchQueue.main.async {
                    if let error = error {
                        print("Error adding comment: \(error)")
                        HapticFeedback.notification(.error)
                    } else {
                        self.newComment = ""
                        self.loadComments()
                        HapticFeedback.notification(.success)
                    }
                }
            }
    }

    private func loadComments() {
        isLoadingComments = true
        Firestore.firestore().collection("posts").document(post.id)
            .collection("comments").order(by: "timestamp", descending: false)
            .getDocuments { snap, error in
                DispatchQueue.main.async {
                    self.isLoadingComments = false
                    if let error = error {
                        print("Error loading comments: \(error)")
                        return
                    }
                    let docs = snap?.documents ?? []
                    self.comments = docs.map {
                        let d = $0.data()
                        return Comment(
                            id: $0.documentID,
                            userId: d["userId"] as? String ?? "",
                            text: d["text"] as? String ?? "",
                            timestamp: d["timestamp"] as? Double ?? Date().timeIntervalSince1970
                        )
                    }
                }
            }
    }
}

// MARK: - Comment Row
struct CommentRow: View {
    let comment: Comment
    let postId: String
    var onDeleted: () -> Void = {}
    @State private var userProfile: UserProfile?
    @State private var showDeleteConfirm = false

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            if let profile = userProfile,
               !profile.petImage.isEmpty,
               let url = URL(string: profile.petImage) {
                AsyncImage(url: url) { image in
                    image.resizable().aspectRatio(contentMode: .fill)
                } placeholder: {
                    Circle().fill(Color.gray.opacity(0.3)).overlay(Text("💬"))
                }
                .frame(width: 28, height: 28)
                .clipShape(Circle())
            } else {
                Circle().fill(Color.gray.opacity(0.3))
                    .overlay(Text("💬"))
                    .frame(width: 28, height: 28)
            }

            VStack(alignment: .leading, spacing: 2) {
                HStack {
                    Text(userProfile?.petName ?? "משתמש")
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(.logoBrown)
                    Spacer()
                    Text(timeAgo(comment.timestamp))
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                Text(comment.text).font(.caption)
            }

            Spacer()

            if comment.userId == Auth.auth().currentUser?.uid {
                Button(role: .destructive) {
                    HapticFeedback.impact(.light)
                    showDeleteConfirm = true
                } label: {
                    Image(systemName: "trash")
                        .font(.caption)
                        .foregroundColor(.red)
                }
            }
        }
        .onAppear { loadUserProfile() }
        .confirmationDialog("מחק תגובה", isPresented: $showDeleteConfirm) {
            Button("מחק", role: .destructive) {
                HapticFeedback.notification(.warning)
                deleteComment()
            }
            Button("ביטול", role: .cancel) { }
        }
    }

    private func loadUserProfile() {
        let db = Firestore.firestore()
        db.collection("users").document(comment.userId).getDocument { doc, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("Error loading comment user profile: \(error)")
                    return
                }
                if let data = doc?.data() {
                    let p = UserProfile(dict: data)
                    if p.petImage.hasPrefix("gs://") {
                        let ref = Storage.storage().reference(forURL: p.petImage)
                        ref.downloadURL { url, _ in
                            if let url = url {
                                self.userProfile = UserProfile(
                                    petName: p.petName, petAge: p.petAge, petBreed: p.petBreed,
                                    petImage: url.absoluteString
                                )
                            } else {
                                self.userProfile = p
                            }
                        }
                    } else {
                        self.userProfile = p
                    }
                }
            }
        }
    }

    private func deleteComment() {
        Firestore.firestore().collection("posts").document(postId)
            .collection("comments").document(comment.id).delete { error in
                if let error = error {
                    print("Error deleting comment: \(error)")
                    HapticFeedback.notification(.error)
                } else {
                    DispatchQueue.main.async {
                        self.onDeleted()
                        HapticFeedback.notification(.success)
                    }
                }
            }
    }
}

// MARK: - Feed Manager
final class FeedManager: ObservableObject {
    @Published var posts: [FeedPost] = []
    @Published var isLoading = false
    private let db = Firestore.firestore()

    func refreshPosts() {
        isLoading = true
        db.collection("posts")
            .order(by: "timestamp", descending: true)
            .getDocuments { [weak self] snap, error in
                DispatchQueue.main.async {
                    self?.isLoading = false
                    if let error = error {
                        print("Error loading posts: \(error)")
                        return
                    }
                    guard let docs = snap?.documents else { return }

                    self?.posts = docs.map { doc in
                        let data = doc.data()
                        var post = FeedPost(id: doc.documentID, dict: data)

                        if let gp = data["location"] as? GeoPoint {
                            post.coordinate = CLLocationCoordinate2D(latitude: gp.latitude, longitude: gp.longitude)
                            if let locationName = data["locationName"] as? String, !locationName.isEmpty {
                                post.location = "📍 \(locationName)"
                            } else {
                                post.location = "📍 מיקום"
                            }
                        }
                        return post
                    }
                }
            }
    }

    func removePostLocally(_ postId: String) {
        posts.removeAll { $0.id == postId }
    }
}
