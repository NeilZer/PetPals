import Foundation
import SwiftUI
import CoreLocation
import FirebaseFirestore

@MainActor
final class MapManager: ObservableObject {
    @Published var locationPosts: [LocationPost] = []
    @Published var isLoading = false

    private let db = Firestore.firestore()

    /// טען פוסטים עם מיקום וסנן לפי רדיוס סביב center (אם קיים).
    func loadLocationPosts(near center: CLLocationCoordinate2D?, withinKm radiusKm: Double) {
        isLoading = true

        db.collection("posts")
            .order(by: "timestamp", descending: true)
            .limit(to: 200)
            .getDocuments { [weak self] snapshot, error in
                guard let self = self else { return }

                if let error = error {
                    print("Error loading location posts:", error)
                    DispatchQueue.main.async { self.isLoading = false }
                    return
                }

                let docs = snapshot?.documents ?? []

                // ממירים ל-FeedPost ורק אם יש GeoPoint תקף
                let posts: [FeedPost] = docs.compactMap { doc in
                    let p = FeedPost(id: doc.documentID, dict: doc.data())
                    if let c = p.coordinate, !(c.latitude == 0 && c.longitude == 0) {
                        return p
                    }
                    return nil
                }

                // סינון לפי רדיוס (אם יש center)
                let filtered: [FeedPost] = {
                    guard let c = center else { return posts }
                    let me = CLLocation(latitude: c.latitude, longitude: c.longitude)
                    return posts.filter {
                        guard let cc = $0.coordinate else { return false }
                        let loc = CLLocation(latitude: cc.latitude, longitude: cc.longitude)
                        return loc.distance(from: me) <= radiusKm * 1000.0
                    }
                }()

                self.loadUserProfilesForPosts(filtered)
            }
    }

    private func loadUserProfilesForPosts(_ posts: [FeedPost]) {
        let group = DispatchGroup()
        var out: [LocationPost] = []

        for p in posts {
            group.enter()
            Firestore.firestore().collection("users")
                .document(p.userId).getDocument { doc, _ in
                    defer { group.leave() }

                    var petName = "משתמש לא ידוע"
                    if let data = doc?.data() {
                        let profile = UserProfile(dict: data)
                        if !profile.petName.isEmpty { petName = profile.petName }
                    }
                    out.append(LocationPost(from: p, petName: petName))
                }
        }

        group.notify(queue: .main) {
            self.locationPosts = out.sorted { $0.timestamp > $1.timestamp }
            self.isLoading = false
        }
    }
}
