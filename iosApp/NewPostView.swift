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

    // מצלמה
    @State private var showCamera = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    // Title
                    VStack {
                        Text("🐾").font(.system(size: 40))
                        Text("newpost.title").font(.title2).bold()
                    }
                    .frame(maxWidth: .infinity)

                    // Text (כיתוב לא חובה)
                    VStack(alignment: .leading, spacing: 8) {
                        Text("newpost.caption.label").font(.headline)
                        ZStack(alignment: .topLeading) {
                            if text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                                Text("newpost.caption.placeholder")
                                    .foregroundColor(.secondary)
                                    .padding(.top, 14)
                                    .padding(.leading, 12)
                            }
                            TextEditor(text: $text)
                                .frame(minHeight: 120)
                                .padding(8)
                                .background(Color.gray.opacity(0.1))
                                .cornerRadius(8)
                        }
                    }

                    // Image
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(spacing: 12) {
                            Text("newpost.media.title").font(.headline)
                            Spacer()

                            // גלריה
                            PhotosPicker(
                                selection: $selectedItem,
                                matching: .images,
                                photoLibrary: .shared()
                            ) {
                                HStack {
                                    Image(systemName: "photo.on.rectangle")
                                    Text("newpost.pick.gallery")
                                }
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(Color.gray.opacity(0.15))
                                .cornerRadius(8)
                            }

                            // מצלמה
                            Button {
                                if UIImagePickerController.isSourceTypeAvailable(.camera) {
                                    showCamera = true
                                } else {
                                    errorMessage = String(localized: "errors.camera.unavailable")
                                }
                            } label: {
                                HStack {
                                    Image(systemName: "camera")
                                    Text("newpost.pick.camera")
                                }
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(Color.gray.opacity(0.15))
                                .cornerRadius(8)
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
                                    await MainActor.run {
                                        selectedImage = uiImage
                                        errorMessage = nil
                                    }
                                } else {
                                    await MainActor.run { errorMessage = String(localized: "errors.image.load_failed") }
                                }
                            } catch {
                                await MainActor.run { errorMessage = String(localized: "errors.image.decode_failed") + ": \(error.localizedDescription)" }
                            }
                        }
                    }

                    // Location
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Text("newpost.location.title").font(.headline)
                            Spacer()
                            Toggle("newpost.include_location", isOn: $includeLocation)
                        }

                        if includeLocation {
                            HBoxLocationStatus
                        }
                    }

                    // Publish
                    Button(action: publishPost) {
                        HStack {
                            if isUploading { ProgressView().scaleEffect(0.9) }
                            Text(isUploading ? String(localized: "newpost.publishing") : String(localized: "common.post"))
                                .fontWeight(.medium)
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
            .navigationTitle(Text("newpost.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("common.cancel") { dismiss() }
                }
            }
            // מצלמה
            .sheet(isPresented: $showCamera) {
                CameraPicker(image: $selectedImage)
            }
        }
        .onAppear { if includeLocation { locationManager.requestLocationPermission() } }
        .onChange(of: includeLocation) { newValue in
            if newValue { locationManager.requestLocationPermission() }
        }
    }

    // MARK: - Location Helpers
    private var HBoxLocationStatus: some View {
        HStack {
            Image(systemName: "location.fill").foregroundColor(locationStatusColor)
            Text(locationStatusText).foregroundColor(locationStatusColor)
            Spacer()
            if locationManager.authorizationStatus == .denied {
                Button("newpost.location.enable") {
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

    // MARK: - Helpers
    private var locationStatusText: String {
        switch locationManager.authorizationStatus {
        case .denied, .restricted:
            return String(localized: "errors.location.denied")
        case .authorizedWhenInUse, .authorizedAlways:
            return locationManager.location != nil
            ? String(localized: "newpost.location.attached")
            : String(localized: "map.searching_location")
        default:
            return String(localized: "map.searching_location")
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
                    errorMessage = String(localized: "errors.post.publish_failed") + ": \(error.localizedDescription)"
                    isUploading = false
                }
            }
        }
    }

    private func uploadPost(userId: String, text: String, image: UIImage, location: CLLocation?) async throws {
        let storage = Storage.storage()
        let db = Firestore.firestore()

        guard let data = image.jpegData(compressionQuality: 0.85) else {
            throw NSError(domain: "ImageError", code: 0,
                          userInfo: [NSLocalizedDescriptionKey: String(localized: "errors.image.compress_failed")])
        }

        // מסמך מראש כדי להשתמש ב-id לשם הקובץ
        let postRef = db.collection("posts").document()
        let postId = postRef.documentID

        // נתיב: postImages/{uid}/{postId}.jpg
        let imageRef = storage.reference(withPath: "postImages/\(userId)/\(postId).jpg")

        let meta = StorageMetadata()
        meta.contentType = "image/jpeg"
        meta.customMetadata = ["userId": userId]

        _ = try await imageRef.putDataAsync(data, metadata: meta)
        let url = try await imageRef.downloadURL()

        var postData: [String: Any] = [
            "userId": userId,
            "text": text.trimmingCharacters(in: .whitespacesAndNewlines),
            "imageUrl": url.absoluteString,
            "timestamp": Date().timeIntervalSince1970,
            "likes": 0,
            "likedBy": []
        ]

        if let location {
            postData["location"] = GeoPoint(latitude: location.coordinate.latitude,
                                            longitude: location.coordinate.longitude)
            try? await addLocationAddress(to: &postData, location: location)
        }

        try await postRef.setData(postData)

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
            if let name = p.name      { parts.append(name) }
            if let city = p.locality  { parts.append(city) }
            if let country = p.country{ parts.append(country) }
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

// MARK: - Camera Picker (UIKit)
struct CameraPicker: UIViewControllerRepresentable {
    @Binding var image: UIImage?

    class Coordinator: NSObject, UINavigationControllerDelegate, UIImagePickerControllerDelegate {
        let parent: CameraPicker
        init(_ parent: CameraPicker) { self.parent = parent }

        func imagePickerController(_ picker: UIImagePickerController,
                                   didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
            let img = (info[.editedImage] ?? info[.originalImage]) as? UIImage
            parent.image = img
            picker.dismiss(animated: true)
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            picker.dismiss(animated: true)
        }
    }

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.allowsEditing = true
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}
}

