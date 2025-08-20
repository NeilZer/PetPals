//
//  MyPostsManager.swift
//  PetPals
//
//  Created by mymac on 11/08/2025.
//


import Foundation
import FirebaseAuth
import FirebaseFirestore

@MainActor
final class MyPostsManager: ObservableObject {
    @Published var myPosts: [FeedPost] = []
    @Published var isLoading = false
    @Published var totalLikes = 0
    private let db = Firestore.firestore()

    func loadMyPosts() {
        guard let userId = Auth.auth().currentUser?.uid else { return }
        isLoading = true
        db.collection("posts")
            .whereField("userId", isEqualTo: userId)
            .order(by: "timestamp", descending: true)
            .getDocuments { [weak self] snapshot, error in
                DispatchQueue.main.async {
                    guard let self = self else { return }
                    self.isLoading = false
                    if let error = error {
                        print("Error loading my posts: \(error)")
                        return
                    }
                    self.myPosts = snapshot?.documents.map { FeedPost(id: $0.documentID, dict: $0.data()) } ?? []
                    self.totalLikes = self.myPosts.reduce(0) { $0 + $1.likes }
                }
            }
    }

    func removePost(_ postId: String) {
        myPosts.removeAll { $0.id == postId }
        totalLikes = myPosts.reduce(0) { $0 + $1.likes }
    }
}
