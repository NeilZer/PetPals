
import SwiftUI
import FirebaseAuth
import FirebaseFirestore

// MARK: - Auth Manager
final class AuthManager: ObservableObject {
    @Published var isLoggedIn = false
    @Published var currentUser: User?
    @Published var isEmailVerified = false
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private var authStateListenerHandle: AuthStateDidChangeListenerHandle?
    
    init() {
        setupAuthListener()
    }
    
    deinit {
        if let handle = authStateListenerHandle {
            Auth.auth().removeStateDidChangeListener(handle)
        }
    }
    
    private func setupAuthListener() {
        authStateListenerHandle = Auth.auth().addStateDidChangeListener { [weak self] auth, user in
            DispatchQueue.main.async {
                self?.currentUser = user
                self?.isLoggedIn = user != nil
                self?.isEmailVerified = user?.isEmailVerified ?? false
                
                if let user = user, !user.isEmailVerified {
                    print("משתמש מחובר אך לא אימת את האימייל")
                }
            }
        }
    }
    
    func signIn(email: String, password: String) async {
        isLoading = true
        errorMessage = nil
        
        do {
            try await Auth.auth().signIn(withEmail: email, password: password)
        } catch {
            errorMessage = handleAuthError(error)
        }
        
        isLoading = false
    }
    
    func createUser(email: String, password: String, petName: String) async -> Bool {
        isLoading = true
        errorMessage = nil
        
        do {
            let result = try await Auth.auth().createUser(withEmail: email, password: password)
            
            // Create user profile in Firestore
            let userProfile = UserProfile(petName: petName)
            try await Firestore.firestore().collection("users")
                .document(result.user.uid)
                .setData(userProfile.asDict)
            
            // Send email verification
            try await result.user.sendEmailVerification()
            errorMessage = "נשלח מייל אימות ל-\(email), בדקו את תיבת הדואר שלכם"
            isLoading = false
            return true
        } catch {
            errorMessage = handleAuthError(error)
            isLoading = false
            return false
        }
    }
    
    func signOut() {
        do {
            try Auth.auth().signOut()
        } catch {
            print("Error signing out: \(error.localizedDescription)")
        }
    }
    
    func deleteAccount() async throws {
        guard let user = Auth.auth().currentUser else {
            throw AuthError.noCurrentUser
        }
        
        try await user.delete()
    }
    
    func sendEmailVerification() async throws {
        guard let user = Auth.auth().currentUser else {
            throw AuthError.noCurrentUser
        }
        
        try await user.sendEmailVerification()
    }
    
    func refreshUser() async {
        guard let user = Auth.auth().currentUser else { return }
        
        do {
            try await user.reload()
            await MainActor.run {
                self.currentUser = Auth.auth().currentUser
                self.isEmailVerified = Auth.auth().currentUser?.isEmailVerified ?? false
            }
        } catch {
            print("Error refreshing user: \(error)")
        }
    }
    
    private func handleAuthError(_ error: Error) -> String {
        if let authError = error as NSError? {
            switch authError.localizedDescription {
            case let msg where msg.contains("password"):
                return "סיסמה שגויה"
            case let msg where msg.contains("email"):
                return "כתובת אימייל לא קיימת"
            case let msg where msg.contains("already in use"):
                return "כתובת האימייל כבר קיימת במערכת"
            case let msg where msg.contains("weak"):
                return "הסיסמה חלשה מדי - לפחות 6 תווים"
            case let msg where msg.contains("invalid"):
                return "כתובת אימייל לא תקינה"
            default:
                return error.localizedDescription
            }
        }
        return "שגיאה לא ידועה"
    }
}

enum AuthError: LocalizedError {
    case noCurrentUser
    case invalidData
    case networkError
    
    var errorDescription: String? {
        switch self {
        case .noCurrentUser:
            return "אין משתמש מחובר"
        case .invalidData:
            return "נתונים לא תקינים"
        case .networkError:
            return "בעיה בחיבור לאינטרנט"
        }
    }
}


// שימוש חוזר בלוגו
struct LogoView: View {
    var size: CGFloat = 160
    var body: some View {
        Image("app_logo")
            .resizable()
            .renderingMode(.original)
            .scaledToFit()
            .frame(width: size, height: size)
            .accessibilityHidden(true)
    }
}
// MARK: - Login View
struct LoginView: View {
    @EnvironmentObject var authManager: AuthManager
    @Environment(\.verticalSizeClass) private var vClass
    @Environment(\.dynamicTypeSize)   private var typeSize

    @State private var email = ""
    @State private var password = ""

    enum AuthSheet: Identifiable { case signUp, forgot
        var id: Int { hashValue }
    }
    @State private var activeSheet: AuthSheet?

    // התאמות רספונסיביות קטנות
    private var headerGap: CGFloat { vClass == .compact ? 8 : 12 }
    private var formSpacing: CGFloat { vClass == .compact ? 8 : 10 }
    private var logoSize: CGFloat {
        var base: CGFloat = (vClass == .compact ? 220 : 280)
        if typeSize.isAccessibilitySize { base -= 40 }
        return max(180, base)
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 12) {
                Spacer(minLength: 6)

                // Header + Logo
                VStack(spacing: 8) {
                    LogoView(size: logoSize)
                    Text("שתפו את המסע עם חיות המחמד שלכם")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding(.bottom, headerGap)

                // Form
                VStack(spacing: formSpacing) {
                    TextField("אימייל", text: $email)
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .textFieldStyle(PetPalsTextFieldStyle())

                    SecureField("סיסמה", text: $password)
                        .textFieldStyle(PetPalsTextFieldStyle())

                    if let errorMessage = authManager.errorMessage {
                        Text(errorMessage)
                            .foregroundColor(.red)
                            .font(.caption)
                            .multilineTextAlignment(.center)
                            .fixedSize(horizontal: false, vertical: true)
                    }

                    Button("התחבר") {
                        Task { await authManager.signIn(email: email, password: password) }
                    }
                    .buttonStyle(PrimaryButtonStyle())
                    .disabled(authManager.isLoading || email.isEmpty || password.isEmpty)

                    HStack {
                        Button("שכחתי סיסמה") { activeSheet = .forgot }
                            .font(.caption)
                            .foregroundColor(.logoBrown)

                        Spacer()

                        Button("הרשמה") { activeSheet = .signUp }
                            .font(.caption)
                            .foregroundColor(.logoBrown)
                    }
                }
                .padding(.horizontal, 24)

                Spacer(minLength: 6)
            }
            .background(Color.logoBackground)
            .sheet(item: $activeSheet) { item in
                switch item {
                case .signUp:  SignUpView().environmentObject(authManager)
                case .forgot:  ForgotPasswordView()
                }
            }
        }
    }
}

// MARK: - Sign Up View
struct SignUpView: View {
    @EnvironmentObject var authManager: AuthManager
    @Environment(\.dismiss) private var dismiss
    @Environment(\.verticalSizeClass) private var vClass
    @Environment(\.dynamicTypeSize)   private var typeSize

    @State private var email = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var petName = ""
    @State private var agreeToTerms = false

    private var headerGap: CGFloat { vClass == .compact ? 10 : 12 }
    private var formSpacing: CGFloat { vClass == .compact ? 10 : 12 }
    private var logoSize: CGFloat {
        var base: CGFloat = (vClass == .compact ? 160 : 220)
        if typeSize.isAccessibilitySize { base -= 30 }
        return max(150, base)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    // Header + Logo
                    VStack(spacing: 8) {
                        LogoView(size: logoSize)
                        Text("הצטרפו ל-PetPals")
                            .font(.title2).fontWeight(.bold)
                            .foregroundColor(.logoBrown)
                    }
                    .padding(.top, 12)
                    .padding(.bottom, headerGap)

                    // Form
                    VStack(spacing: formSpacing) {
                        TextField("שם חיית המחמד", text: $petName)
                            .textFieldStyle(PetPalsTextFieldStyle())

                        TextField("אימייל", text: $email)
                            .keyboardType(.emailAddress)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .textFieldStyle(PetPalsTextFieldStyle())

                        SecureField("סיסמה", text: $password)
                            .textFieldStyle(PetPalsTextFieldStyle())

                        SecureField("אימות סיסמה", text: $confirmPassword)
                            .textFieldStyle(PetPalsTextFieldStyle())

                        if let errorMessage = authManager.errorMessage {
                            Text(errorMessage)
                                .foregroundColor(errorMessage.contains("נשלח מייל") ? .green : .red)
                                .font(.caption)
                                .multilineTextAlignment(.center)
                                .fixedSize(horizontal: false, vertical: true)
                        }

                        Toggle("אני מסכים לתנאי השימוש", isOn: $agreeToTerms)
                            .font(.caption)

                        Button("הירשם") {
                            Task {
                                let ok = await authManager.createUser(email: email, password: password, petName: petName)
                                if ok { DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) { dismiss() } }
                            }
                        }
                        .buttonStyle(PrimaryButtonStyle())
                        .disabled(authManager.isLoading || !isFormValid)
                    }
                    .padding(.horizontal, 24)
                }
            }
            .navigationTitle("הרשמה")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("ביטול") { dismiss() }
                }
            }
        }
    }

    private var isFormValid: Bool {
        !email.isEmpty &&
        !password.isEmpty &&
        !confirmPassword.isEmpty &&
        !petName.isEmpty &&
        password == confirmPassword &&
        password.count >= 6 &&
        agreeToTerms
    }
}

// MARK: - Forgot Password View
struct ForgotPasswordView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var email = ""
    @State private var isLoading = false
    @State private var message = ""
    @State private var isSuccess = false
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 30) {
                Text("🔐")
                    .font(.system(size: 60))
                
                VStack(spacing: 16) {
                    Text("שחזור סיסמה")
                        .font(.title2)
                        .fontWeight(.bold)
                    
                    Text("הכניסו את כתובת האימייל שלכם ונשלח לכם קישור לשחזור הסיסמה")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                }
                
                VStack(spacing: 16) {
                    TextField("אימייל", text: $email)
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                        .textFieldStyle(PetPalsTextFieldStyle())
                    
                    if !message.isEmpty {
                        Text(message)
                            .foregroundColor(isSuccess ? .green : .red)
                            .font(.caption)
                            .multilineTextAlignment(.center)
                    }
                    
                    Button("שלח קישור") {
                        resetPassword()
                    }
                    .buttonStyle(PrimaryButtonStyle())
                    .disabled(isLoading || email.isEmpty)
                }
                .padding(.horizontal, 32)
                
                Spacer()
            }
            .navigationTitle("שחזור סיסמה")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("ביטול") { dismiss() }
                }
            }
        }
    }
    
    private func resetPassword() {
        isLoading = true
        message = ""
        
        Auth.auth().sendPasswordReset(withEmail: email) { error in
            DispatchQueue.main.async {
                isLoading = false
                
                if let error = error {
                    message = "שגיאה בשליחת הודעת השחזור: \(error.localizedDescription)"
                    isSuccess = false
                } else {
                    message = "נשלח קישור שחזור ל-\(email), בדקו את תיבת הדואר שלכם"
                    isSuccess = true
                }
            }
        }
    }
}

// MARK: - Button Styles
struct PrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .frame(maxWidth: .infinity)
            .frame(height: 50)
            .background(configuration.isPressed ? Color.logoBrown.opacity(0.8) : Color.logoBrown)
            .foregroundColor(.white)
            .cornerRadius(12)
            .scaleEffect(configuration.isPressed ? 0.98 : 1.0)
            .animation(.easeInOut(duration: 0.1), value: configuration.isPressed)
    }
}
