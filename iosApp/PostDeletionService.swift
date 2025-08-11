//
//  PostDeletionService.swift
//  PetPals
//
//  Created by mymac on 11/08/2025.
//


import Foundation
import FirebaseFirestore
import FirebaseStorage

struct PostDeletionService {
    static func delete(post: FeedPost, onComplete: @escaping (Result<Void, Error>) -> Void) {
        let db = Firestore.firestore()
        let postRef = db.collection("posts").document(post.id)

        // 1) מחיקת התמונה (אם יש)
        func deleteImageIfNeeded(_ next: @escaping () -> Void) {
            guard !post.imageUrl.isEmpty else { next(); return }
            let ref = Storage.storage().reference(forURL: post.imageUrl)
            ref.delete { _ in next() } // גם אם נכשל, נמשיך למחיקה בבסיס
        }

        // 2) מחיקת תגובות בבאטץ'
        func deleteComments(_ next: @escaping () -> Void) {
            postRef.collection("comments").getDocuments { snapshot, _ in
                let batch = db.batch()
                snapshot?.documents.forEach { batch.deleteDocument($0.reference) }
                batch.commit { _ in next() }
            }
        }

        // 3) מחיקת הפוסט עצמו
        func deletePostDoc() {
            postRef.delete { err in
                if let err = err { onComplete(.failure(err)) }
                else { onComplete(.success(())) }
            }
        }

        deleteImageIfNeeded {
            deleteComments {
                deletePostDoc()
            }
        }
    }
}
