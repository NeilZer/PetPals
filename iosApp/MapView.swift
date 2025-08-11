import SwiftUI
import MapKit
import CoreLocation
import FirebaseFirestore
import FirebaseAuth

struct MapView: View {
    @StateObject private var mapManager = MapManager()
    @StateObject private var locationManager = LocationManager()
    @State private var selectedPost: LocationPost?
    @State private var showPostDetail = false
    @State private var radiusKm: Double = 20

    // iOS 17+: משתמשים במצלמה
    @State private var cameraPosition: MapCameraPosition = .region(
        MKCoordinateRegion(center: CLLocationCoordinate2D(latitude: 32.0853, longitude: 34.7818),
                           span: MKCoordinateSpan(latitudeDelta: 0.1, longitudeDelta: 0.1))
    )

    // iOS 16 fallback
    @State private var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 32.0853, longitude: 34.7818),
        span: MKCoordinateSpan(latitudeDelta: 0.1, longitudeDelta: 0.1)
    )

    var body: some View {
        NavigationStack {
            ZStack {
                if #available(iOS 17.0, *) {
                    // ---- iOS 17+ ----
                    Map(position: $cameraPosition) {
                        // פוסטים
                        ForEach(mapManager.locationPosts) { post in
                            Annotation("", coordinate: post.coordinate) {
                                PostMapMarker(post: post) {
                                    selectedPost = post
                                    showPostDetail = true
                                }
                            }
                        }
                        // המיקום שלי
                        if let my = locationManager.location?.coordinate {
                            Annotation("me", coordinate: my) {
                                UserLocationMarker()
                            }
                        }
                    }
                    .ignoresSafeArea()
                } else {
                    // ---- iOS 16 fallback ----
                    Map(coordinateRegion: $region, annotationItems: mapManager.locationPosts) { post in
                        MapAnnotation(coordinate: post.coordinate) {
                            PostMapMarker(post: post) {
                                selectedPost = post
                                showPostDetail = true
                            }
                        }
                    }
                    .ignoresSafeArea()
                    // ב-iOS16 אין בעיה לשים MapAnnotation מחוץ ל-Map, אבל לא צריך:
                    // את סמן המשתמש נכניס כ-Overlay פשוט:
                    .overlay(alignment: .topLeading) {
                        EmptyView() // שמור על מבנה אחיד
                    }
                }

                // כפתורי רדיוס/קפיצה
                controlsOverlay
                // טעינה
                if mapManager.isLoading { loadingOverlay }
            }
            .navigationTitle("מפת פוסטים")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { reloadNearMe() } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                }
            }
        }
        .onAppear {
            locationManager.requestLocationPermission()
            reloadNearMe()
        }
        .onChange(of: locationManager.location) { newLoc in
            guard let loc = newLoc else { return }
            if #available(iOS 17.0, *) {
                withAnimation {
                    cameraPosition = .region(MKCoordinateRegion(
                        center: loc.coordinate,
                        span: MKCoordinateSpan(latitudeDelta: 0.05, longitudeDelta: 0.05)
                    ))
                }
            } else {
                withAnimation { region.center = loc.coordinate }
            }
            reloadNearMe()
        }
        .sheet(isPresented: $showPostDetail) {
            if let p = selectedPost { PostDetailView(post: p) }
        }
    }

    // MARK: - Views

    private var controlsOverlay: some View {
        VStack {
            Spacer()
            HStack {
                Menu {
                    ForEach([5, 10, 20, 30, 50], id: \.self) { km in
                        Button("\(km) ק״מ") {
                            radiusKm = Double(km)
                            reloadNearMe()
                        }
                    }
                } label: {
                    HStack(spacing: 8) {
                        Image(systemName: "scope")
                        Text("\(Int(radiusKm)) ק״מ")
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 12)
                    .frame(height: 40)
                    .background(Color.logoBrown)
                    .clipShape(Capsule())
                    .shadow(radius: 3)
                }
                .padding(.leading, 16)
                .padding(.bottom, 100)

                Spacer()

                Button {
                    moveToCurrentLocation()
                    reloadNearMe()
                } label: {
                    Image(systemName: "location.fill")
                        .foregroundColor(.white)
                        .font(.title2)
                        .frame(width: 50, height: 50)
                        .background(Color.logoBrown)
                        .clipShape(Circle())
                        .shadow(radius: 4)
                }
                .padding(.trailing, 16)
                .padding(.bottom, 100)
            }
        }
    }

    private var loadingOverlay: some View {
        Color.black.opacity(0.3)
            .ignoresSafeArea()
            .overlay(
                VStack {
                    ProgressView().progressViewStyle(CircularProgressViewStyle(tint: .white))
                    Text("טוען פוסטים...").foregroundColor(.white).padding(.top, 8)
                }
            )
    }

    // MARK: - Actions

    private func reloadNearMe() {
        mapManager.loadLocationPosts(near: locationManager.location?.coordinate, withinKm: radiusKm)
    }

    private func moveToCurrentLocation() {
        guard let loc = locationManager.location else {
            locationManager.requestLocationPermission()
            return
        }
        if #available(iOS 17.0, *) {
            withAnimation(.easeInOut(duration: 1.0)) {
                cameraPosition = .region(MKCoordinateRegion(
                    center: loc.coordinate,
                    span: MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01)
                ))
            }
        } else {
            withAnimation(.easeInOut(duration: 1.0)) {
                region.center = loc.coordinate
                region.span = MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01)
            }
        }
    }
}

// סמן מיקום המשתמש
private struct UserLocationMarker: View {
    var body: some View {
        ZStack {
            Circle().fill(Color.blue.opacity(0.2)).frame(width: 28, height: 28)
            Circle().fill(Color.blue).frame(width: 14, height: 14)
                .overlay(Circle().stroke(Color.white, lineWidth: 2))
                .shadow(radius: 2)
        }
        .accessibilityLabel("המיקום שלי")
    }
}

// MARK: - Post Map Marker
struct PostMapMarker: View {
    let post: LocationPost
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            ZStack {
                Circle()
                    .fill(Color.logoBrown)
                    .frame(width: 40, height: 40)
                    .overlay { Circle().stroke(Color.white, lineWidth: 2) }
                    .shadow(radius: 4)

                if !post.imageUrl.isEmpty, let url = URL(string: post.imageUrl) {
                    AsyncImage(url: url) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Image(systemName: "pawprint.fill")
                            .foregroundColor(.white)
                            .font(.system(size: 16))
                    }
                    .frame(width: 30, height: 30)
                    .clipShape(Circle())
                } else {
                    Image(systemName: "pawprint.fill")
                        .foregroundColor(.white)
                        .font(.system(size: 16))
                }
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(post.petName.isEmpty ? "פוסט" : post.petName))
    }
}
// MARK: - Post Detail View
struct PostDetailView: View {
    let post: LocationPost
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    HStack {
                        VStack(alignment: .leading) {
                            Text(post.petName.isEmpty ? "משתמש" : post.petName)
                                .font(.title2).fontWeight(.bold).foregroundColor(.logoBrown)
                            Text(timeAgo(post.timestamp))
                                .font(.caption).foregroundColor(.secondary)
                        }
                        Spacer()
                    }

                    if !post.imageUrl.isEmpty, let url = URL(string: post.imageUrl) {
                        AsyncImage(url: url) { image in
                            image.resizable().aspectRatio(contentMode: .fit)
                        } placeholder: {
                            Rectangle().fill(Color.gray.opacity(0.25))
                                .frame(height: 240)
                                .overlay { ProgressView() }
                        }
                        .cornerRadius(12)
                    }

                    if !post.text.isEmpty {
                        Text(post.text).font(.body)
                    }

                    HStack(spacing: 6) {
                        Image(systemName: "mappin.and.ellipse").foregroundColor(.logoBrown)
                        Text(post.locationName.isEmpty ? "מיקום הפוסט" : post.locationName)
                            .font(.caption).foregroundColor(.secondary)
                    }

                    Spacer(minLength: 12)
                }
                .padding()
            }
            .navigationTitle("פרטי פוסט")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("סגור") { dismiss() }
                }
            }
        }
    }
}
