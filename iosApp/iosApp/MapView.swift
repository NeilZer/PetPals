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

    // מצביע האם התחלנו להאזין כבר (נמנע התחלה כפולה)
    @State private var didStartListening = false

    // iOS 17+: מצלמה
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
                    Map(position: $cameraPosition) {
                        ForEach(mapManager.locationPosts) { post in
                            Annotation("", coordinate: post.coordinate) {
                                PostMapMarker(post: post) {
                                    selectedPost = post
                                    showPostDetail = true
                                }
                            }
                        }
                        if let my = locationManager.location?.coordinate {
                            Annotation("me", coordinate: my) { UserLocationMarker() }
                        }
                    }
                    .ignoresSafeArea()
                } else {
                    Map(coordinateRegion: $region, annotationItems: mapManager.locationPosts) { post in
                        MapAnnotation(coordinate: post.coordinate) {
                            PostMapMarker(post: post) {
                                selectedPost = post
                                showPostDetail = true
                            }
                        }
                    }
                    .ignoresSafeArea()
                }

                controlsOverlay

                if mapManager.isLoading { loadingOverlay }
            }
            .navigationTitle("מפת פוסטים")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { forceRefresh() } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                }
            }
        }
        .onAppear {
            locationManager.requestLocationPermission()
            // אם כבר יש מיקום מיידי (מהפעלה קודמת), נתחיל להאזין
            if let c = locationManager.location?.coordinate, !didStartListening {
                didStartListening = true
                mapManager.startListening(near: c, withinKm: radiusKm)
                moveCamera(to: c, span: 0.05)
            }
        }
        .onChange(of: locationManager.location) { newLoc in
            guard let loc = newLoc else { return }
            moveCamera(to: loc.coordinate, span: 0.05)

            // התחלה חד-פעמית של listener כשקיבלנו מיקום ראשון
            if !didStartListening {
                didStartListening = true
                mapManager.startListening(near: loc.coordinate, withinKm: radiusKm)
            } else {
                // אם כבר מאזינים – רק מעדכנים פילטר
                mapManager.updateFilter(center: loc.coordinate, radiusKm: radiusKm)
            }
        }
        .onDisappear {
            // כיבוי נקי של המאזין על המיין (במקום deinit ב-MapManager)
            mapManager.stopListening()
            didStartListening = false
        }
        .sheet(isPresented: $showPostDetail) {
            if let p = selectedPost { PostDetailView(post: p) }
        }
    }

    // MARK: - Overlays

    private var controlsOverlay: some View {
        VStack {
            Spacer()
            HStack {
                // צ’יפ רדיוס – צד ימין
                Menu {
                    ForEach([5, 10, 20, 30, 50], id: \.self) { km in
                        Button("\(km) ק״מ") {
                            radiusKm = Double(km)
                            if let c = locationManager.location?.coordinate {
                                mapManager.updateFilter(center: c, radiusKm: radiusKm)
                            } else {
                                mapManager.updateFilter(center: nil, radiusKm: radiusKm)
                            }
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

                Spacer()

                // כפתור "מרכז"
                Button {
                    moveToCurrentLocation()
                } label: {
                    Image(systemName: "location.fill")
                        .foregroundColor(.white)
                        .font(.title2)
                        .frame(width: 52, height: 52)
                        .background(Color.logoBrown)
                        .clipShape(Circle())
                        .shadow(radius: 4)
                }
                .padding(.trailing, 16)
            }
            .padding(.bottom, 16) // ↓ נמוך יותר
            .ignoresSafeArea(edges: .bottom)
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

    private func forceRefresh() {
        if let c = locationManager.location?.coordinate {
            didStartListening = true                 // חשוב כדי למנוע התחלה כפולה
            mapManager.startListening(near: c, withinKm: radiusKm) // מאפס מאזין + מאזין מחדש
            moveCamera(to: c, span: 0.05)
        } else {
            // אם אין מיקום – רק ליישם פילטר קיים על הדאטה הנוכחית
            mapManager.refreshIfNeeded()
        }
    }

    private func moveToCurrentLocation() {
        guard let loc = locationManager.location else {
            locationManager.requestLocationPermission()
            return
        }
        moveCamera(to: loc.coordinate, span: 0.01)
        mapManager.updateFilter(center: loc.coordinate, radiusKm: radiusKm)
    }

    private func moveCamera(to coordinate: CLLocationCoordinate2D, span: CLLocationDegrees) {
        if #available(iOS 17.0, *) {
            withAnimation(.easeInOut(duration: 1.0)) {
                cameraPosition = .region(MKCoordinateRegion(
                    center: coordinate,
                    span: MKCoordinateSpan(latitudeDelta: span, longitudeDelta: span)
                ))
            }
        } else {
            withAnimation(.easeInOut(duration: 1.0)) {
                region.center = coordinate
                region.span = MKCoordinateSpan(latitudeDelta: span, longitudeDelta: span)
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
            // ↓↓↓ מגדיל משמעותית את אזור הנגיעה
            .padding(12)
            .contentShape(Circle())    // מתאים לצורה עגולה
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(post.petName.isEmpty ? "פוסט" : post.petName))
        .allowsHitTesting(true)
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
