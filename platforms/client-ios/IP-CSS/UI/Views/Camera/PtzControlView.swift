import SwiftUI

/// PTZ контроллер для управления камерой (iOS).
/// Поддерживает панорамирование, наклон и зум.
struct PtzControlView: View {
    let cameraId: String
    @State private var isConnected = false
    @State private var zoomLevel: Float = 1.0
    @State private var isMoving = false
    @State private var errorMessage: String?
    @Environment(\.dismiss) private var dismiss

    private let apiBaseURL = ProcessInfo.processInfo.environment["API_BASE_URL"] ?? "http://localhost:8080"

    var body: some View {
        VStack(spacing: 20) {
            // Заголовок
            HStack {
                Text("PTZ Controls")
                    .font(.title2)
                    .fontWeight(.bold)
                Spacer()
                Button("Done") { dismiss() }
            }
            .padding(.horizontal)

            // Индикатор подключения
            HStack {
                Circle()
                    .fill(isConnected ? Color.green : Color.red)
                    .frame(width: 10, height: 10)
                Text(isConnected ? "Connected" : "Disconnected")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            // D-Pad (панорамирование и наклон)
            VStack(spacing: 0) {
                // Верхний ряд
                HStack(spacing: 40) {
                    moveButton(direction: "UP_LEFT", icon: "arrow.up.left")
                    moveButton(direction: "UP", icon: "arrow.up")
                    moveButton(direction: "UP_RIGHT", icon: "arrow.up.right")
                }
                // Средний ряд
                HStack(spacing: 40) {
                    moveButton(direction: "LEFT", icon: "arrow.left")
                    Circle()
                        .fill(Color.gray.opacity(0.2))
                        .frame(width: 60, height: 60)
                        .overlay(Text("PTZ").font(.caption2))
                    moveButton(direction: "RIGHT", icon: "arrow.right")
                }
                // Нижний ряд
                HStack(spacing: 40) {
                    moveButton(direction: "DOWN_LEFT", icon: "arrow.down.left")
                    moveButton(direction: "DOWN", icon: "arrow.down")
                    moveButton(direction: "DOWN_RIGHT", icon: "arrow.down.right")
                }
            }
            .padding()
            .background(Color.gray.opacity(0.1))
            .cornerRadius(16)

            // Zoom контрол
            VStack(spacing: 12) {
                Text("Zoom")
                    .font(.headline)

                HStack(spacing: 30) {
                    Button(action: { zoomOut() }) {
                        Image(systemName: "minus.magnifyingglass")
                            .font(.title)
                            .foregroundColor(.blue)
                    }
                    .disabled(isMoving)

                    Slider(value: $zoomLevel, in: 0.1...10.0, step: 0.1)
                        .tint(.blue)

                    Button(action: { zoomIn() }) {
                        Image(systemName: "plus.magnifyingglass")
                            .font(.title)
                            .foregroundColor(.blue)
                    }
                    .disabled(isMoving)
                }

                Text(String(format: "%.1fx", zoomLevel))
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding()
            .background(Color.gray.opacity(0.1))
            .cornerRadius(16)

            // Сообщение об ошибке
            if let error = errorMessage {
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
                    .padding()
            }

            Spacer()
        }
        .padding()
        .onAppear { connect() }
    }

    // MARK: - Кнопка направления
    private func moveButton(direction: String, icon: String) -> some View {
        Button(action: { sendPtzCommand(direction: direction) }) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(.blue)
                .frame(width: 50, height: 50)
                .background(Color.blue.opacity(0.1))
                .cornerRadius(12)
        }
        .disabled(isMoving || !isConnected)
    }

    // MARK: - API вызовы
    private func connect() {
        guard let url = URL(string: "\(apiBaseURL)/api/v1/health") else { return }
        URLSession.shared.dataTask(with: url) { data, response, error in
            DispatchQueue.main.async {
                if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 {
                    isConnected = true
                    errorMessage = nil
                } else {
                    isConnected = false
                    errorMessage = "Cannot connect to server"
                }
            }
        }.resume()
    }

    private func sendPtzCommand(direction: String) {
        isMoving = true
        errorMessage = nil

        guard let url = URL(string: "\(apiBaseURL)/api/v1/cameras/\(cameraId)/ptz") else {
            errorMessage = "Invalid URL"
            isMoving = false
            return
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body: [String: Any] = ["direction": direction, "speed": 0.5]
        request.httpBody = try? JSONSerialization.data(withJSONObject: body)

        URLSession.shared.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                isMoving = false
                if let error = error {
                    errorMessage = "Error: \(error.localizedDescription)"
                } else if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode != 200 {
                    errorMessage = "Server error: \(httpResponse.statusCode)"
                }
            }
        }.resume()
    }

    private func zoomIn() {
        zoomLevel = min(zoomLevel + 0.5, 10.0)
        sendPtzCommand(direction: "ZOOM_IN")
    }

    private func zoomOut() {
        zoomLevel = max(zoomLevel - 0.5, 0.1)
        sendPtzCommand(direction: "ZOOM_OUT")
    }
}

#Preview {
    PtzControlView(cameraId: "preview-camera-1")
}
