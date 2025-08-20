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

        // 1) מחיקת תמונה (לא קריטי – גם אם נכשל ממשיכים)
        func deleteImageIfNeeded(_ next: @escaping () -> Void) {
            guard !post.imageUrl.isEmpty else { next(); return }

            // ניסיון ראשון: URL מלא (https / gs)
            let tryDeleteByURL: (@escaping () -> Void) -> Void = { done in
                let storageRef = Storage.storage().reference(forURL: post.imageUrl)
                storageRef.delete { _ in done() } // לא עוצרים על שגיאה – ממשיכים הלאה
            }

            // ניסיון שני (נפילת־חסד): הנתיב החדש postImages/{uid}/{postId}.jpg
            let tryDeleteByKnownPath: (@escaping () -> Void) -> Void = { done in
                let pathRef = Storage.storage().reference(withPath: "postImages/\(post.userId)/\(post.id).jpg")
                pathRef.delete { _ in done() } // גם כאן, לא חוסמים אם יש כשל
            }

            // מריצים: קודם URL, ואז fallback לנתיב הידוע
            tryDeleteByURL {
                tryDeleteByKnownPath {
                    next()
                }
            }
        }

        // 2) מחיקת תגובות – בבאטצ'ים של 300 כדי לא לחצות את מגבלת 500
        func deleteCommentsPaged(_ next: @escaping () -> Void) {
            func deleteOnePage(completion: @escaping (Error?) -> Void) {
                postRef.collection("comments")
                    .limit(to: 300)
                    .getDocuments { snapshot, err in
                        if let err = err { completion(err); return }
                        let docs = snapshot?.documents ?? []
                        guard !docs.isEmpty else { completion(nil); return }

                        let batch = db.batch()
                        docs.forEach { batch.deleteDocument($0.reference) }
                        batch.commit { batchErr in
                            if let batchErr = batchErr { completion(batchErr) }
                            else { deleteOnePage(completion: completion) } // המשך לעמוד הבא
                        }
                    }
            }

            deleteOnePage { _ in next() } // גם אם הייתה שגיאה – ממשיכים למחיקת המסמך
        }

        // 3) מחיקת הפוסט עצמו
        func deletePostDoc() {
            postRef.delete { err in
                if let err = err { onComplete(.failure(err)) }
                else { onComplete(.success(())) }
            }
        }

        // Pipeline: תמונה → תגובות → מסמך
        deleteImageIfNeeded {
            deleteCommentsPaged {
                deletePostDoc()
            }
        }
    }
}
