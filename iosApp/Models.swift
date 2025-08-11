//
//  Models.swift
//  PetPals
//
//  Created by mymac on 10/08/2025.
//
import SwiftUI
import CoreLocation
import FirebaseFirestore
import Network
import FirebaseAuth

// MARK: - User Profile Model
struct UserProfile {
    var petName: String
    var petAge: Int
    var petBreed: String
    var petImage: String
    
    init(petName: String = "", petAge: Int = 0, petBreed: String = "", petImage: String = "") {
        self.petName = petName
        self.petAge = petAge
        self.petBreed = petBreed
        self.petImage = petImage
    }
    
    init(dict: [String: Any]) {
        self.petName = dict["petName"] as? String ?? ""
        self.petAge = dict["petAge"] as? Int ?? 0
        self.petBreed = dict["petBreed"] as? String ?? ""
        self.petImage = dict["petImage"] as? String ?? ""
    }
    
    var asDict: [String: Any] {
        return [
            "petName": petName,
            "petAge": petAge,
            "petBreed": petBreed,
            "petImage": petImage
        ]
    }
}

// MARK: - Feed Post Model
struct FeedPost: Identifiable, Equatable {
    let id: String
    let userId: String
    var text: String
    var imageUrl: String
    var likes: Int
    var timestamp: Double
    var likedBy: [String]
    var location: String
    var coordinate: CLLocationCoordinate2D?
    var locationName: String
    
    init(id: String = UUID().uuidString,
         userId: String = "",
         text: String = "",
         imageUrl: String = "",
         likes: Int = 0,
         timestamp: Double = Date().timeIntervalSince1970,
         likedBy: [String] = [],
         location: String = "",
         coordinate: CLLocationCoordinate2D? = nil,
         locationName: String = "") {
        self.id = id
        self.userId = userId
        self.text = text
        self.imageUrl = imageUrl
        self.likes = likes
        self.timestamp = timestamp
        self.likedBy = likedBy
        self.location = location
        self.coordinate = coordinate
        self.locationName = locationName
    }
    
    init(id: String, dict: [String: Any]) {
        self.id = id
        self.userId = dict["userId"] as? String ?? ""
        self.text = dict["text"] as? String ?? ""
        self.imageUrl = dict["imageUrl"] as? String ?? ""
        self.likes = dict["likes"] as? Int ?? 0
        self.timestamp = dict["timestamp"] as? Double ?? Date().timeIntervalSince1970
        self.likedBy = dict["likedBy"] as? [String] ?? []
        self.locationName = dict["locationName"] as? String ?? ""
        
        // Handle location data
        if let geoPoint = dict["location"] as? GeoPoint {
            self.coordinate = CLLocationCoordinate2D(latitude: geoPoint.latitude, longitude: geoPoint.longitude)
            self.location = locationName.isEmpty ? "📍 מיקום" : "📍 \(locationName)"
        } else {
            self.coordinate = nil
            self.location = ""
        }
    }
    
    static func == (lhs: FeedPost, rhs: FeedPost) -> Bool {
        return lhs.id == rhs.id
    }
}

// MARK: - Location Post Model
struct LocationPost: Identifiable {
    let id: String
    let userId: String
    let petName: String
    let text: String
    let imageUrl: String
    let timestamp: Double
    let coordinate: CLLocationCoordinate2D
    let locationName: String
    
    init(from feedPost: FeedPost, petName: String) {
        self.id = feedPost.id
        self.userId = feedPost.userId
        self.petName = petName
        self.text = feedPost.text
        self.imageUrl = feedPost.imageUrl
        self.timestamp = feedPost.timestamp
        self.coordinate = feedPost.coordinate ?? CLLocationCoordinate2D()
        self.locationName = feedPost.locationName
    }
}

// MARK: - Comment Model
struct Comment: Identifiable {
    let id: String
    let userId: String
    let text: String
    let timestamp: Double
    
    init(id: String = UUID().uuidString, userId: String, text: String, timestamp: Double = Date().timeIntervalSince1970) {
        self.id = id
        self.userId = userId
        self.text = text
        self.timestamp = timestamp
    }
}

// MARK: - Statistics Models
struct MonthlyStats: Identifiable {
    let id = UUID()
    let month: String
    let postsCount: Int
    let likesReceived: Int
    let commentsCount: Int
    let totalDistance: Double
    let activeDays: Int
}

struct Achievement: Identifiable {
    let id = UUID()
    let title: String
    let description: String
    let icon: String
    let color: AchievementColor
    let isUnlocked: Bool
}

enum AchievementColor {
    case bronze, silver, gold, platinum
    
    var swiftUIColor: Color {
        switch self {
        case .bronze: return .orange
        case .silver: return .gray
        case .gold: return .yellow
        case .platinum: return .purple
        }
    }
}

// MARK: - Statistics Manager
@MainActor
final class StatisticsManager: ObservableObject {
    @Published var totalPosts = 0
    @Published var totalLikes = 0
    @Published var totalComments = 0
    @Published var totalDistance: Double = 0
    @Published var activeDaysThisMonth = 0
    @Published var streakDays = 0
    @Published var averageLikesPerPost: Double = 0
    @Published var mostActiveDay = "אין נתונים"
    @Published var favoriteLocation = "אין נתונים"
    @Published var monthlyStats: [MonthlyStats] = []
    @Published var isLoading = false
    
    private let db = Firestore.firestore()
    
    func loadStatistics() {
        guard let userId = Auth.auth().currentUser?.uid else { return }
        
        isLoading = true
        
        // Load user posts
        db.collection("posts")
            .whereField("userId", isEqualTo: userId)
            .order(by: "timestamp", descending: true)
            .getDocuments { [weak self] snapshot, error in
                DispatchQueue.main.async {
                    guard let self = self else { return }
                    
                    if let error = error {
                        print("Error loading statistics: \(error)")
                        self.isLoading = false
                        return
                    }
                    
                    guard let documents = snapshot?.documents else {
                        self.isLoading = false
                        return
                    }
                    
                    let posts = documents.map { FeedPost(id: $0.documentID, dict: $0.data()) }
                    self.calculateStatistics(from: posts)
                    self.isLoading = false
                }
            }
    }
    
    private func calculateStatistics(from posts: [FeedPost]) {
        totalPosts = posts.count
        totalLikes = posts.reduce(0) { $0 + $1.likes }
        averageLikesPerPost = totalPosts > 0 ? Double(totalLikes) / Double(totalPosts) : 0
        
        // Calculate monthly stats
        calculateMonthlyStats(from: posts)
        
        // Calculate distance (mock calculation - in real app would use actual GPS data)
        totalDistance = Double(posts.count) * 2.5 // Average 2.5km per trip
        
        // Calculate active days this month
        let calendar = Calendar.current
        let currentMonth = calendar.component(.month, from: Date())
        let currentYear = calendar.component(.year, from: Date())
        
        let thisMonthPosts = posts.filter { post in
            let date = Date(timeIntervalSince1970: post.timestamp)
            let postMonth = calendar.component(.month, from: date)
            let postYear = calendar.component(.year, from: date)
            return postMonth == currentMonth && postYear == currentYear
        }
        
        let uniqueDays = Set(thisMonthPosts.map { post in
            let date = Date(timeIntervalSince1970: post.timestamp)
            return calendar.component(.day, from: date)
        })
        
        activeDaysThisMonth = uniqueDays.count
        
        // Calculate most active day (mock data)
        let dayFormatter = DateFormatter()
        dayFormatter.locale = Locale(identifier: "he")
        dayFormatter.dateFormat = "EEEE"
        mostActiveDay = thisMonthPosts.isEmpty ? "אין נתונים" : dayFormatter.string(from: Date())
        
        // Calculate favorite location
        let locationCounts = posts.compactMap { $0.locationName }.reduce(into: [String: Int]()) { counts, location in
            counts[location, default: 0] += 1
        }
        favoriteLocation = locationCounts.max(by: { $0.value < $1.value })?.key ?? "אין נתונים"
    }
    
    private func calculateMonthlyStats(from posts: [FeedPost]) {
        let calendar = Calendar.current
        let dateFormatter = DateFormatter()
        dateFormatter.locale = Locale(identifier: "he")
        dateFormatter.dateFormat = "MMMM yyyy"
        
        // Group posts by month
        let groupedPosts = Dictionary(grouping: posts) { post in
            let date = Date(timeIntervalSince1970: post.timestamp)
            return dateFormatter.string(from: date)
        }
        
        monthlyStats = groupedPosts.map { month, posts in
            let totalDistance = Double(posts.count) * 2.5 // Mock calculation
            let uniqueDays = Set(posts.map { post in
                let date = Date(timeIntervalSince1970: post.timestamp)
                return calendar.component(.day, from: date)
            }).count
            
            return MonthlyStats(
                month: month,
                postsCount: posts.count,
                likesReceived: posts.reduce(0) { $0 + $1.likes },
                commentsCount: 0, // Would need to load from comments collection
                totalDistance: totalDistance,
                activeDays: uniqueDays
            )
        }.sorted {
            let date1 = dateFormatter.date(from: $0.month) ?? Date()
            let date2 = dateFormatter.date(from: $1.month) ?? Date()
            return date1 > date2
        }
    }
    
    func getAchievements() -> [Achievement] {
        var achievements: [Achievement] = []
        
        // First Post Achievement
        achievements.append(Achievement(
            title: "פוסט ראשון",
            description: "פרסמת את הפוסט הראשון שלך!",
            icon: "🌟",
            color: .bronze,
            isUnlocked: totalPosts >= 1
        ))
        
        // Active User Achievement
        achievements.append(Achievement(
            title: "משתמש פעיל",
            description: "פרסמת 10 פוסטים",
            icon: "🔥",
            color: .silver,
            isUnlocked: totalPosts >= 10
        ))
        
        // Popular Posts Achievement
        achievements.append(Achievement(
            title: "פוסטים פופולריים",
            description: "קיבלת 50 לייקים",
            icon: "❤️",
            color: .gold,
            isUnlocked: totalLikes >= 50
        ))
        
        // Distance Achievement
        achievements.append(Achievement(
            title: "מטייל מקצועי",
            description: "הלכת יותר מ-50 ק\"מ",
            icon: "🏃‍♂️",
            color: .platinum,
            isUnlocked: totalDistance >= 50
        ))
        
        return achievements
    }
    
    func refreshStats() async {
        await MainActor.run {
            loadStatistics()
        }
    }
}



// MARK: - Utility Extensions
extension Date {
    var timeAgo: String {
        let formatter = RelativeDateTimeFormatter()
        formatter.locale = Locale(identifier: "he")
        formatter.unitsStyle = .abbreviated
        return formatter.localizedString(for: self, relativeTo: Date())
    }
}

func timeAgo(_ timestamp: Double) -> String {
    let date = Date(timeIntervalSince1970: timestamp)
    return date.timeAgo
}

// MARK: - Custom Text Field Style
struct PetPalsTextFieldStyle: TextFieldStyle {
    func _body(configuration: TextField<Self._Label>) -> some View {
        configuration
            .padding()
            .background(Color.white)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color.logoBrown, lineWidth: 1)
            )
            .cornerRadius(12)
    }
}
