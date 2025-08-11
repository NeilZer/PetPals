import SwiftUI
import PhotosUI
import CoreLocation
import UIKit
import FirebaseAuth
import FirebaseFirestore
import FirebaseStorage

// MARK: - New Post View
struct NewPostView: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var locationManager = NewPostLocationManager()

    @State private var text = ""
    @State private var selectedItem: PhotosPickerItem?
    @State private var selectedImage: UIImage?
    @State private var isUploading = false
    @State private var errorMessage: String?
    @State private var includeLocation = true

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    // Title
                    VStack {
                        Text("🐾").font(.system(size: 40))
                        Text("פוסט חדש").font(.title2).bold()
                    }
                    .frame(maxWidth: .infinity)

                    // Text
                    VStack(alignment: .leading, spacing: 8) {
                        Text("מה חיית המחמד שלך עושה היום?").font(.headline)
                        TextEditor(text: $text)
                            .frame(minHeight: 120)
                            .padding(8)
                            .background(Color.gray.opacity(0.1))
                            .cornerRadius(8)
                    }

                    // Image
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text("תמונה").font(.headline)
                            Spacer()
                            PhotosPicker(selection: $selectedItem, matching: .images, photoLibrary: .shared()) {
                                HStack { Image(systemName: "photo.badge.plus"); Text("בחר תמונה") }
                                    .foregroundColor(.logoBrown)
                            }
                        }

                        if let selectedImage {
                            ZStack(alignment: .topTrailing) {
                                Image(uiImage: selectedImage)
                                    .resizable().scaledToFill()
                                    .frame(height: 200).clipped()
                                    .cornerRadius(8)
                                Button {
                                    self.selectedImage = nil
                                    self.selectedItem = nil
                                } label: {
                                    Image(systemName: "xmark.circle.fill")
                                        .font(.title2)
                                        .foregroundColor(.red)
                                        .background(.white)
                                        .clipShape(Circle())
                                }
                                .padding(8)
                            }
                        }
                    }
                    .onChange(of: selectedItem) { newItem in
                        Task {
                            guard let newItem else { return }
                            do {
                                if let data = try await newItem.loadTransferable(type: Data.self),
                                   let uiImage = UIImage(data: data) {
                                    await MainActor.run { selectedImage = uiImage; errorMessage = nil }
                                } else {
                                    await MainActor.run { errorMessage = "לא ניתן לטעון את התמונה" }
                                }
                            } catch {
                                await MainActor.run { errorMessage = "כשל בטעינת תמונה: \(error.localizedDescription)" }
                            }
                        }
                    }

                    // Location
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Text("מיקום").font(.headline)
                            Spacer()
                            Toggle("כלול מיקום", isOn: $includeLocation)
                        }

                        if includeLocation {
                            HStack {
                                Image(systemName: "location.fill").foregroundColor(locationStatusColor)
                                Text(locationStatusText).foregroundColor(locationStatusColor)
                                Spacer()
                                if locationManager.authorizationStatus == .denied {
                                    Button("הפעל מיקום") {
                                        if let url = URL(string: UIApplication.openSettingsURLString) {
                                            UIApplication.shared.open(url)
                                        }
                                    }
                                    .font(.caption)
                                    .foregroundColor(.logoBrown)
                                }
                            }
                            .padding()
                            .background(Color.gray.opacity(0.1))
                            .cornerRadius(8)
                        }
                    }

                    // Publish
                    Button(action: publishPost) {
                        HStack {
                            if isUploading { ProgressView().scaleEffect(0.9) }
                            Text(isUploading ? "מפרסם..." : "פרסם").fontWeight(.medium)
                        }
                        .frame(maxWidth: .infinity, minHeight: 50)
                        .background(canPublish ? Color.logoBrown : Color.gray)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                    }
                    .disabled(!canPublish || isUploading)

                    // Error
                    if let errorMessage {
                        Text(errorMessage)
                            .foregroundColor(.red)
                            .font(.caption)
                            .padding()
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color.red.opacity(0.1))
                            .cornerRadius(8)
                    }
                }
                .padding()
            }
            .navigationTitle("פוסט חדש")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .topBarLeading) { Button("בטל") { dismiss() } } }
        }
        .onAppear { if includeLocation { locationManager.requestLocationPermission() } }
        .onChange(of: includeLocation) { newValue in
            if newValue { locationManager.requestLocationPermission() }
        }
    }

    // MARK: - Helpers
    private var locationStatusText: String {
        switch locationManager.authorizationStatus {
        case .denied, .restricted: return "אין הרשאת מיקום"
        case .authorizedWhenInUse, .authorizedAlways:
            return locationManager.location != nil ? "מיקום נוסף לפוסט ✓" : "מחפש מיקום..."
        default: return "מחפש מיקום..."
        }
    }
    private var locationStatusColor: Color {
        switch locationManager.authorizationStatus {
        case .denied, .restricted: return .red
        case .authorizedWhenInUse, .authorizedAlways:
            return locationManager.location != nil ? .green : .orange
        default: return .orange
        }
    }
    private var canPublish: Bool {
        !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && selectedImage != nil
    }

    private func publishPost() {
        guard let userId = Auth.auth().currentUser?.uid,
              let image = selectedImage,
              canPublish else { return }

        isUploading = true
        errorMessage = nil

        Task {
            do {
                let loc = includeLocation ? locationManager.location : nil
                try await uploadPost(userId: userId, text: text, image: image, location: loc)
                await MainActor.run { isUploading = false; dismiss() }
            } catch {
                await MainActor.run {
                    errorMessage = "שגיאה בפרסום הפוסט: \(error.localizedDescription)"
                    isUploading = false
                }
            }
        }
    }

    private func uploadPost(userId: String, text: String, image: UIImage, location: CLLocation?) async throws {
        let storage = Storage.storage()
        let db = Firestore.firestore()

        guard let imageData = image.jpegData(compressionQuality: 0.85) else {
            throw NSError(domain: "ImageError", code: 0,
                          userInfo: [NSLocalizedDescriptionKey: "כשל בדחיסת תמונה"])
        }

        let filename = "posts/\(userId)_\(Int(Date().timeIntervalSince1970)).jpg"
        let imageRef = storage.reference().child(filename)
        let meta = StorageMetadata(); meta.contentType = "image/jpeg"

        _ = try await imageRef.putDataAsync(imageData, metadata: meta)
        let downloadURL = try await imageRef.downloadURL()

        var postData: [String: Any] = [
            "userId": userId,
            "text": text.trimmingCharacters(in: .whitespacesAndNewlines),
            "imageUrl": downloadURL.absoluteString,
            "timestamp": Date().timeIntervalSince1970,
            "likes": 0,
            "likedBy": []
        ]

        if let location {
            postData["location"] = GeoPoint(latitude: location.coordinate.latitude,
                                            longitude: location.coordinate.longitude)
            try? await addLocationAddress(to: &postData, location: location)
        }

        _ = try await db.collection("posts").addDocument(data: postData)

        if let location {
            try await db.collection("users").document(userId).updateData([
                "location": GeoPoint(latitude: location.coordinate.latitude,
                                     longitude: location.coordinate.longitude),
                "lastLocationUpdate": Date().timeIntervalSince1970
            ])
        }
    }

    private func addLocationAddress(to postData: inout [String: Any], location: CLLocation) async throws {
        let geocoder = CLGeocoder()
        let placemarks = try await geocoder.reverseGeocodeLocation(location)
        if let p = placemarks.first {
            var parts: [String] = []
            if let name = p.name { parts.append(name) }
            if let loc = p.locality { parts.append(loc) }
            if let country = p.country { parts.append(country) }
            if !parts.isEmpty { postData["locationName"] = parts.joined(separator: ", ") }
        }
    }
}

// MARK: - Location Manager for New Post
final class NewPostLocationManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    @Published var location: CLLocation?
    @Published var authorizationStatus: CLAuthorizationStatus = .notDetermined
    private let manager = CLLocationManager()

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.distanceFilter = 5
        authorizationStatus = manager.authorizationStatus
    }

    func requestLocationPermission() {
        switch manager.authorizationStatus {
        case .notDetermined: manager.requestWhenInUseAuthorization()
        case .authorizedWhenInUse, .authorizedAlways: manager.startUpdatingLocation()
        default: break
        }
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        DispatchQueue.main.async {
            self.authorizationStatus = manager.authorizationStatus
            switch self.authorizationStatus {
            case .authorizedWhenInUse, .authorizedAlways: manager.startUpdatingLocation()
            case .denied, .restricted: self.location = nil
            default: break
            }
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        DispatchQueue.main.async { self.location = locations.last }
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("Location error:", error.localizedDescription)
        DispatchQueue.main.async { self.location = nil }
    }
}
import CoreLocation

// אפשר לשים מחוץ ל-struct NewPostView, באותו קובץ:
private func addLocationAddress(to postData: inout [String: Any], location: CLLocation) async throws {
    let geocoder = CLGeocoder()
    // שימי לב: ה-API האסינכרוני דורש iOS 15+
    let placemarks = try await geocoder.reverseGeocodeLocation(location)
    if let p = placemarks.first {
        var parts: [String] = []
        if let name = p.name { parts.append(name) }
        if let loc = p.locality { parts.append(loc) }
        if let country = p.country { parts.append(country) }
        if !parts.isEmpty {
            postData["locationName"] = parts.joined(separator: ", ")
        }
    }
}

private func uploadPost(userId: String, text: String, image: UIImage, location: CLLocation?) async throws {
    let storage = Storage.storage()
    let db = Firestore.firestore()

    guard let imageData = image.jpegData(compressionQuality: 0.85) else {
        throw NSError(domain: "ImageError", code: 0,
                      userInfo: [NSLocalizedDescriptionKey: "כשל בדחיסת תמונה"])
    }

    let filename = "posts/\(userId)_\(Int(Date().timeIntervalSince1970)).jpg"
    let imageRef = storage.reference().child(filename)
    let meta = StorageMetadata(); meta.contentType = "image/jpeg"

    _ = try await imageRef.putDataAsync(imageData, metadata: meta)
    let downloadURL = try await imageRef.downloadURL()

    var postData: [String: Any] = [
        "userId": userId,
        "text": text.trimmingCharacters(in: .whitespacesAndNewlines),
        "imageUrl": downloadURL.absoluteString,
        "timestamp": Date().timeIntervalSince1970,
        "likes": 0,
        "likedBy": []
    ]

    if let location {
        postData["location"] = GeoPoint(latitude: location.coordinate.latitude,
                                        longitude: location.coordinate.longitude)
        postData["hasLocation"] = true   // ✅ חדש: דגל לזיהוי מהיר של פוסטים עם מיקום
        try? await addLocationAddress(to: &postData, location: location)
    } else {
        postData["hasLocation"] = false
    }

    _ = try await db.collection("posts").addDocument(data: postData)

    if let location {
        try await db.collection("users").document(userId).updateData([
            "location": GeoPoint(latitude: location.coordinate.latitude,
                                 longitude: location.coordinate.longitude),
            "lastLocationUpdate": Date().timeIntervalSince1970
        ])
    }
}
