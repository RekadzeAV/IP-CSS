//
//  BiometricAuthenticator.swift
//  IP-CSS iOS
//
//  Created by NLP-Core-Team
//  Copyright © 2026 NLP-Core-Team. All rights reserved.
//

import Foundation
import LocalAuthentication
import Security

// MARK: - Biometric Authenticator

class BiometricAuthenticator: ObservableObject {
    @Published var isBiometricAvailable = false
    @Published var biometricType: LABiometryType = .none
    @Published var isBiometricEnabled = false
    
    private let context = LAContext()
    private let biometricsEnabledKey = "biometricsEnabled"
    
    init() {
        checkBiometricAvailability()
        loadBiometricSetting()
    }
    
    // MARK: - Check Availability
    
    func checkBiometricAvailability() {
        var error: NSError?
        
        if context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) {
            isBiometricAvailable = true
            biometricType = context.biometryType
            
            print("Biometric available: \(biometricType.description)")
        } else {
            isBiometricAvailable = false
            biometricType = .none
            
            if let error = error {
                print("Biometric not available: \(error.localizedDescription)")
            }
        }
    }
    
    // MARK: - Authentication
    
    func authenticate(reason: String = "Authenticate to access IP-CSS") async -> Result<Void, BiometricError> {
        // Check if biometrics are enabled in settings
        guard isBiometricEnabled else {
            return .failure(.notEnabled)
        }
        
        // Check availability
        guard isBiometricAvailable else {
            return .failure(.notAvailable)
        }
        
        return await withCheckedContinuation { continuation in
            context.evaluatePolicy(
                .deviceOwnerAuthenticationWithBiometrics,
                localizedReason: reason
            ) { success, error in
                DispatchQueue.main.async {
                    if success {
                        continuation.resume(returning: .success(()))
                    } else {
                        if let laError = error as? LAError {
                            continuation.resume(returning: .failure(BiometricError.from(laError: laError)))
                        } else {
                            continuation.resume(returning: .failure(.failed))
                        }
                    }
                }
            }
        }
    }
    
    // MARK: - Enable/Disable Biometrics
    
    func enableBiometrics() async -> Result<Void, BiometricError> {
        guard isBiometricAvailable else {
            return .failure(.notAvailable)
        }
        
        // Authenticate first time enabling
        let authResult = await authenticate(reason: "Enable biometric authentication")
        
        guard case .success = authResult else {
            return authResult
        }
        
        // Save biometric secret
        saveBiometricSecret()
        
        // Update setting
        isBiometricEnabled = true
        saveBiometricSetting()
        
        return .success(())
    }
    
    func disableBiometrics() {
        isBiometricEnabled = false
        deleteBiometricSecret()
        saveBiometricSetting()
    }
    
    // MARK: - Biometric Secret (Keychain)
    
    private func saveBiometricSecret() {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: "com.ipcss.biometric.secret",
            kSecValueData as String: Data("biometric_secret".utf8),
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        ]
        
        // Delete existing
        SecItemDelete(query as CFDictionary)
        
        // Add new
        SecItemAdd(query as CFDictionary, nil)
    }
    
    private func loadBiometricSecret() -> Bool {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: "com.ipcss.biometric.secret",
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        
        return status == errSecSuccess
    }
    
    private func deleteBiometricSecret() {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: "com.ipcss.biometric.secret"
        ]
        
        SecItemDelete(query as CFDictionary)
    }
    
    // MARK: - Settings Persistence
    
    private func saveBiometricSetting() {
        UserDefaults.standard.set(isBiometricEnabled, forKey: biometricsEnabledKey)
    }
    
    private func loadBiometricSetting() {
        isBiometricEnabled = UserDefaults.standard.bool(forKey: biometricsEnabledKey)
    }
    
    // MARK: - Verify Biometric Secret
    
    func verifyBiometricSecret() async -> Bool {
        let authResult = await authenticate(reason: "Verify biometric authentication")
        
        guard case .success = authResult else {
            return false
        }
        
        return loadBiometricSecret()
    }
}

// MARK: - LABiometryType Extension

extension LABiometryType {
    var description: String {
        switch self {
        case .none:
            return "None"
        case .touchID:
            return "Touch ID"
        case .faceID:
            return "Face ID"
        case .opticID:
            return "Optic ID"
        @unknown default:
            return "Unknown"
        }
    }
    
    var iconName: String {
        switch self {
        case .faceID, .opticID:
            return "faceid"
        case .touchID:
            return "touchid"
        case .none:
            return "lock.shield"
        @unknown default:
            return "lock.shield"
        }
    }
}

// MARK: - Biometric Error

enum BiometricError: LocalizedError {
    case notAvailable
    case notEnrolled
    case lockout
    case userCancel
    case userFallback
    case systemCancel
    case passcodeNotSet
    case notEnabled
    case failed
    
    var errorDescription: String? {
        switch self {
        case .notAvailable:
            return "Biometric authentication is not available on this device"
        case .notEnrolled:
            return "No biometric enrolled. Please set up biometric authentication in device settings"
        case .lockout:
            return "Too many failed attempts. Biometric authentication is temporarily locked"
        case .userCancel:
            return "Authentication cancelled by user"
        case .userFallback:
            return "User chose to use password instead"
        case .systemCancel:
            return "Authentication cancelled by system"
        case .passcodeNotSet:
            return "Device passcode is not set"
        case .notEnabled:
            return "Biometric authentication is not enabled in app settings"
        case .failed:
            return "Authentication failed"
        }
    }
    
    var recoverySuggestion: String? {
        switch self {
        case .notEnrolled:
            return "Please go to Settings > Face ID & Passcode (or Touch ID & Passcode) to set up biometric authentication"
        case .lockout:
            return "Please wait a moment and try again, or use your passcode"
        case .notEnabled:
            return "Please enable biometric authentication in the app settings"
        default:
            return nil
        }
    }
    
    static func from(laError: LAError) -> BiometricError {
        switch laError.code {
        case .authenticationFailed:
            return .failed
        case .userCancel, .userFallback:
            return .userCancel
        case .systemCancel:
            return .systemCancel
        case .passcodeNotSet:
            return .passcodeNotSet
        case .touchIDNotAvailable, .faceIDNotAvailable:
            return .notAvailable
        case .touchIDNotEnrolled, .faceIDNotEnrolled:
            return .notEnrolled
        case .lockout, .lockoutPermanent:
            return .lockout
        case .appCancel:
            return .userCancel
        default:
            return .failed
        }
    }
}

// MARK: - Biometric Settings View

struct BiometricSettingsView: View {
    @StateObject private var authenticator = BiometricAuthenticator()
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationView {
            Form {
                Section("Biometric Authentication") {
                    if authenticator.isBiometricAvailable {
                        HStack {
                            Image(systemName: authenticator.biometricType.iconName)
                                .font(.system(size: 24))
                                .foregroundColor(.accentColor)
                            
                            VStack(alignment: .leading, spacing: 4) {
                                Text("\(authenticator.biometricType.description) Authentication")
                                    .font(.system(size: 16))
                                Text("Use \(authenticator.biometricType.description) to quickly and securely access the app")
                                    .font(.system(size: 13))
                                    .foregroundColor(.secondary)
                            }
                            
                            Spacer()
                            
                            Toggle("", isOn: $authenticator.isBiometricEnabled)
                                .labelsHidden()
                        }
                        .padding(.vertical, 8)
                    } else {
                        HStack {
                            Image(systemName: "lock.shield")
                                .font(.system(size: 24))
                                .foregroundColor(.secondary)
                            
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Biometric Not Available")
                                    .font(.system(size: 16))
                                Text("Your device does not support biometric authentication")
                                    .font(.system(size: 13))
                                    .foregroundColor(.secondary)
                            }
                        }
                        .padding(.vertical, 8)
                    }
                }
                
                if authenticator.isBiometricAvailable && authenticator.isBiometricEnabled {
                    Section("Security") {
                        Button("Test Authentication") {
                            Task {
                                await testAuthentication()
                            }
                        }
                        
                        Button("Disable Biometric", role: .destructive) {
                            authenticator.disableBiometrics()
                        }
                    }
                }
                
                Section("About") {
                    Text("Biometric data is stored securely on your device and never leaves it. The app only receives a success/failure signal from the system.")
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                }
            }
            .navigationTitle("Biometric Auth")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
    
    private func testAuthentication() async {
        let result = await authenticator.authenticate(reason: "Test biometric authentication")
        
        switch result {
        case .success:
            print("Authentication successful!")
        case .failure(let error):
            print("Authentication failed: \(error.localizedDescription)")
        }
    }
}

// MARK: - App Launch Biometric Check

class AppLaunchBiometricHandler {
    static let shared = AppLaunchBiometricHandler()
    
    private let authenticator = BiometricAuthenticator()
    private var isAuthenticated = false
    
    func checkAuthentication() async -> Bool {
        // If biometrics not enabled, allow access
        guard authenticator.isBiometricEnabled else {
            return true
        }
        
        // If already authenticated in this session, allow access
        if isAuthenticated {
            return true
        }
        
        // Require biometric authentication
        let result = await authenticator.authenticate(reason: "Authenticate to access IP-CSS")
        
        if case .success = result {
            isAuthenticated = true
            return true
        }
        
        return false
    }
    
    func resetAuthentication() {
        isAuthenticated = false
    }
}
