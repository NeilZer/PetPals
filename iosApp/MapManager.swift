import Foundation
import SwiftUI
import CoreLocation
import FirebaseFirestore

@MainActor
final class MapManager: ObservableObject {
    /// כל הפוסטים עם מיקום (מטמון אחרון)
    private var allLocationPosts: [FeedPost] = []
    /// הפוסטים המסוננים להצגה במפה
    @Published var locationPosts: [LocationPost] = []
    @Published var isLoading = false

    private let db = Firestore.firestore()
    private var listener: ListenerRegistration?

    // פילטר נוכחי
    private var currentCenter: CLLocationCoordinate2D?
    private var currentRadiusKm: Double = 20


    /// מאזין בזמן אמת לפוסטים האחרונים, ומסנן בצד לקוח לפי רדיוס.
    func startListening(near center: CLLocationCoordinate2D, withinKm radiusKm: Double) {
        currentCenter = center
        currentRadiusKm = radiusKm

        // אם כבר יש מאזין – נבטל כדי למנוע כפילויות
        stopListening()
        isLoading = true

        // שימי לב: Firestore לא תומך "exists" על GeoPoint, לכן מאזינים לפוסטים האחרונים
        // ומסננים לקליינט רק מי שיש לו location תקף.
        listener = db.collection("posts")
            .order(by: "timestamp", descending: true)
            .limit(to: 400) // אפשר להגדיל/להקטין לפי נפח האפליקציה
            .addSnapshotListener { [weak self] snap, err in
                guard let self = self else { return }
                if let err = err {
                    print("MapManager listener error:", err)
                    Task { @MainActor in self.isLoading = false }
                    return
                }

                let docs = snap?.documents ?? []
                let posts: [FeedPost] = docs.compactMap { doc in
                    let p = FeedPost(id: doc.documentID, dict: doc.data())
                    // רק אם יש קואורדינטות אמיתיות
                    if let c = p.coordinate, !(c.latitude == 0 && c.longitude == 0) {
                        return p
                    }
                    return nil
                }

                Task { @MainActor in
                    self.allLocationPosts = posts
                    self.applyFilter()
                    self.isLoading = false
                }
            }
    }

    func stopListening() {
        listener?.remove()
        listener = nil
    }

    /// עדכון רדיוס/מרכז קיים (ללא יצירת listener חדש)
    func updateFilter(center: CLLocationCoordinate2D?, radiusKm: Double) {
        if let c = center { currentCenter = c }
        currentRadiusKm = radiusKm
        applyFilter()
    }

    /// ריענון ידני – אם יש center ידוע אבל אין מאזין, נאתחל מאזין.
    func refreshIfNeeded() {
        guard let c = currentCenter else { return }
        if listener == nil { startListening(near: c, withinKm: currentRadiusKm) }
        else { applyFilter() }
    }

    // MARK: - Filtering
    private func applyFilter() {
        // אם אין center – נציג פשוט את הפוסטים האחרונים עם מיקום (ללא סינון רדיוס)
        guard let c = currentCenter else {
            // נטען שמות/פרופילים ואז נמפה ל-LocationPost
            loadUserProfilesForPosts(allLocationPosts)
            return
        }

        let me = CLLocation(latitude: c.latitude, longitude: c.longitude)
        let filtered = allLocationPosts.filter {
            guard let cc = $0.coordinate else { return false }
            let loc = CLLocation(latitude: cc.latitude, longitude: cc.longitude)
            return loc.distance(from: me) <= currentRadiusKm * 1000.0
        }

        loadUserProfilesForPosts(filtered)
    }

    private func loadUserProfilesForPosts(_ posts: [FeedPost]) {
        let group = DispatchGroup()
        var out: [LocationPost] = []
        out.reserveCapacity(posts.count)

        for p in posts {
            group.enter()
            db.collection("users").document(p.userId).getDocument { doc, _ in
                defer { group.leave() }

                var petName = "משתמש"
                if let data = doc?.data() {
                    let profile = UserProfile(dict: data)
                    if !profile.petName.isEmpty { petName = profile.petName }
                }
                out.append(LocationPost(from: p, petName: petName))
            }
        }

        group.notify(queue: .main) {
            // מיון מהחדש לישן
            self.locationPosts = out.sorted { $0.timestamp > $1.timestamp }
        }
    }
}
