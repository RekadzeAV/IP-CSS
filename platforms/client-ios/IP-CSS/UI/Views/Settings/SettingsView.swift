import SwiftUI

/// Settings view для iOS приложения IP-CSS.
struct SettingsView: View {
    @AppStorage("server_url") private var serverUrl = ""
    @AppStorage("notifications_enabled") private var notificationsEnabled = true
    @AppStorage("dark_mode") private var darkMode = true
    @AppStorage("motion_detection") private var motionDetection = true
    @AppStorage("recording_quality") private var recordingQuality = "High"
    @AppStorage("retention_days") private var retentionDays = 30
    @AppStorage("auto_record") private var autoRecord = true
    @AppStorage("cloud_sync") private var cloudSync = false

    @State private var showResetAlert = false
    @State private var showTestResult = false
    @State private var testResultMessage = ""
    @State private var isTesting = false

    private let apiBaseURL: String = {
        ProcessInfo.processInfo.environment["API_BASE_URL"] ?? "http://localhost:8080"
    }()

    var body: some View {
        NavigationStack {
            Form {
                // MARK: - Connection
                Section {
                    HStack {
                        Label("Server", systemImage: "server.rack")
                        Spacer()
                        TextField("http://192.168.1.100:8080", text: $serverUrl)
                            .textContentType(.URL)
                            .autocapitalization(.none)
                            .disableAutocorrection(true)
                            .multilineTextAlignment(.trailing)
                            .foregroundColor(.secondary)
                    }

                    Button(action: testConnection) {
                        HStack {
                            if isTesting {
                                ProgressView()
                                    .scaleEffect(0.8)
                            } else {
                                Image(systemName: "antenna.radiowaves.left.and.right")
                            }
                            Text("Test Connection")
                        }
                    }
                    .disabled(isTesting || serverUrl.isEmpty)
                } header: {
                    Text("Connection")
                } footer: {
                    Text("Enter the IP-CSS server URL")
                }

                // MARK: - Recording
                Section {
                    Toggle(isOn: $autoRecord) {
                        Label("Auto Record", systemImage: "record.circle")
                    }

                    Toggle(isOn: $motionDetection) {
                        Label("Motion Detection", systemImage: "waveform.path.ecg")
                    }

                    Picker(selection: $recordingQuality) {
                        Text("Low").tag("Low")
                        Text("Medium").tag("Medium")
                        Text("High").tag("High")
                    } label: {
                        Label("Quality", systemImage: "video")
                    }

                    Stepper(value: $retentionDays, in: 1...365) {
                        Label("Retention: \(retentionDays) days", systemImage: "calendar")
                    }
                } header: {
                    Text("Recording")
                }

                // MARK: - Notifications
                Section {
                    Toggle(isOn: $notificationsEnabled) {
                        Label("Push Notifications", systemImage: "bell.badge")
                    }

                    Toggle(isOn: $cloudSync) {
                        Label("Cloud Sync", systemImage: "icloud")
                    }
                } header: {
                    Text("Notifications & Sync")
                }

                // MARK: - Appearance
                Section {
                    Toggle(isOn: $darkMode) {
                        Label("Dark Mode", systemImage: darkMode ? "moon.fill" : "sun.max")
                    }
                } header: {
                    Text("Appearance")
                }

                // MARK: - About
                Section {
                    LabeledContent("Version", value: "0.3.0-beta")
                    LabeledContent("Build", value: Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1")
                    LabeledContent("Server", value: serverUrl.isEmpty ? "Not connected" : serverUrl)

                    Link(destination: URL(string: "https://github.com/RekadzeAV/IP-CSS")!) {
                        HStack {
                            Label("GitHub", systemImage: "link")
                            Spacer()
                            Image(systemName: "arrow.up.right")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }

                    Link(destination: URL(string: "https://github.com/RekadzeAV/IP-CSS/issues")!) {
                        HStack {
                            Label("Report Issue", systemImage: "exclamationmark.bubble")
                            Spacer()
                            Image(systemName: "arrow.up.right")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                } header: {
                    Text("About")
                }

                // MARK: - Reset
                Section {
                    Button(role: .destructive, action: { showResetAlert = true }) {
                        HStack {
                            Spacer()
                            Image(systemName: "arrow.counterclockwise")
                            Text("Reset All Settings")
                            Spacer()
                        }
                    }
                }
            }
            .navigationTitle("Settings")
            .alert("Test Connection", isPresented: $showTestResult) {
                Button("OK") { }
            } message: {
                Text(testResultMessage)
            }
            .alert("Reset Settings", isPresented: $showResetAlert) {
                Button("Cancel", role: .cancel) { }
                Button("Reset", role: .destructive) { resetSettings() }
            } message: {
                Text("This will reset all settings to defaults. This action cannot be undone.")
            }
        }
    }

    private func testConnection() {
        guard let url = URL(string: serverUrl.isEmpty ? apiBaseURL : serverUrl) else {
            testResultMessage = "Invalid URL"
            showTestResult = true
            return
        }

        isTesting = true
        let healthUrl = url.appendingPathComponent("/api/v1/health")

        URLSession.shared.dataTask(with: healthUrl) { data, response, error in
            DispatchQueue.main.async {
                isTesting = false
                if let error = error {
                    testResultMessage = "Connection failed: \(error.localizedDescription)"
                } else if let httpResponse = response as? HTTPURLResponse {
                    if httpResponse.statusCode == 200 {
                        testResultMessage = "✅ Connected successfully!"
                    } else {
                        testResultMessage = "⚠️ Server responded with status \(httpResponse.statusCode)"
                    }
                }
                showTestResult = true
            }
        }.resume()
    }

    private func resetSettings() {
        serverUrl = ""
        notificationsEnabled = true
        darkMode = true
        motionDetection = true
        recordingQuality = "High"
        retentionDays = 30
        autoRecord = true
        cloudSync = false
    }
}

#Preview {
    SettingsView()
}
