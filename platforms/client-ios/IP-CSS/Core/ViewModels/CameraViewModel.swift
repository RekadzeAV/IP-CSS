//
//  CameraViewModel.swift
//  IP-CSS iOS
//
//  Created by NLP-Core-Team
//  Copyright © 2026 NLP-Core-Team. All rights reserved.
//

import Foundation
import Combine
import AVFoundation

class CameraViewModel: ObservableObject {
    @Published var streamUrl: String = ""
    @Published var isOnline: Bool = false
    @Published var isRecording: Bool = false
    @Published var isPTZMoving: Bool = false
    @Published var currentPreset: Int?
    @Published var errorMessage: String?
    
    private var camera: Camera?
    private var player: AVPlayer?
    private var playerItem: AVPlayerItem?
    private var reconnectTimer: Timer?
    private var apiClient: APIClient
    private var cancellables = Set<AnyCancellable>()
    
    init() {
        self.apiClient = APIClient.shared
    }
    
    // MARK: - Connection
    
    func connect(camera: Camera) {
        self.camera = camera
        self.streamUrl = camera.rtspUrl
        self.isOnline = camera.status == .online
        
        setupPlayer()
        startReconnectTimer()
    }
    
    func disconnect() {
        player?.pause()
        player = nil
        playerItem = nil
        reconnectTimer?.invalidate()
        reconnectTimer = nil
    }
    
    private func setupPlayer() {
        guard let camera = camera,
              let url = URL(string: camera.rtspUrl) else {
            errorMessage = "Invalid stream URL"
            return
        }
        
        playerItem = AVPlayerItem(url: url)
        player = AVPlayer(playerItem: playerItem)
        
        // Observe player status
        playerItem?.publisher(for: \.status)
            .sink { [weak self] status in
                switch status {
                case .readyToPlay:
                    self?.isOnline = true
                    self?.player?.play()
                case .failed:
                    self?.isOnline = false
                    self?.handleStreamFailure()
                default:
                    break
                }
            }
            .store(in: &cancellables)
    }
    
    private func startReconnectTimer() {
        reconnectTimer = Timer.scheduledTimer(withTimeInterval: 5.0, repeats: true) { [weak self] _ in
            self?.checkConnection()
        }
    }
    
    private func checkConnection() {
        guard let camera = camera else { return }
        
        apiClient.getCameraStatus(cameraId: camera.id)
            .sink(
                receiveCompletion: { _ in },
                receiveValue: { [weak self] status in
                    self?.isOnline = status.isOnline
                }
            )
            .store(in: &cancellables)
    }
    
    private func handleStreamFailure() {
        errorMessage = "Stream connection lost. Reconnecting..."
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) { [weak self] in
            self?.setupPlayer()
        }
    }
    
    // MARK: - Recording
    
    func startRecording() {
        guard let camera = camera else { return }
        
        apiClient.startRecording(cameraId: camera.id)
            .sink(
                receiveCompletion: { completion in
                    if case .failure(let error) = completion {
                        self.errorMessage = "Failed to start recording: \(error.localizedDescription)"
                    }
                },
                receiveValue: { [weak self] in
                    self?.isRecording = true
                }
            )
            .store(in: &cancellables)
    }
    
    func stopRecording() {
        guard let camera = camera else { return }
        
        apiClient.stopRecording(cameraId: camera.id)
            .sink(
                receiveCompletion: { completion in
                    if case .failure(let error) = completion {
                        self.errorMessage = "Failed to stop recording: \(error.localizedDescription)"
                    }
                },
                receiveValue: { [weak self] in
                    self?.isRecording = false
                }
            )
            .store(in: &cancellables)
    }
    
    // MARK: - Screenshot
    
    func takeScreenshot() {
        guard let camera = camera else { return }
        
        apiClient.takeScreenshot(cameraId: camera.id)
            .sink(
                receiveCompletion: { completion in
                    if case .failure(let error) = completion {
                        self.errorMessage = "Failed to take screenshot: \(error.localizedDescription)"
                    }
                },
                receiveValue: { imageData in
                    self.saveScreenshot(imageData)
                }
            )
            .store(in: &cancellables)
    }
    
    private func saveScreenshot(_ imageData: Data) {
        let filename = "screenshot_\(Date().timeIntervalSince1970).jpg"
        let path = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent(filename)
        
        try? imageData.write(to: path)
    }
    
    // MARK: - PTZ Controls
    
    func movePTZ(direction: PTZDirection) {
        guard let camera = camera else { return }
        
        withAnimation {
            isPTZMoving = true
        }
        
        apiClient.movePTZ(cameraId: camera.id, direction: direction)
            .sink(
                receiveCompletion: { [weak self] _ in
                    withAnimation {
                        self?.isPTZMoving = false
                    }
                },
                receiveValue: { _ in }
            )
            .store(in: &cancellables)
    }
    
    func zoomPTZ(zoom: PTZZoom) {
        guard let camera = camera else { return }
        
        apiClient.zoomPTZ(cameraId: camera.id, zoom: zoom)
            .sink(
                receiveCompletion: { _ in },
                receiveValue: { _ in }
            )
            .store(in: &cancellables)
    }
    
    func loadPreset(_ presetId: Int) {
        guard let camera = camera else { return }
        
        currentPreset = presetId
        
        apiClient.loadPreset(cameraId: camera.id, presetId: presetId)
            .sink(
                receiveCompletion: { _ in },
                receiveValue: { _ in }
            )
            .store(in: &cancellables)
    }
}

// MARK: - Models

enum PTZDirection: String {
    case up, down, left, right
}

enum PTZZoom {
    case zoomIn, zoomOut
}

struct PTZPreset: Identifiable {
    let id: Int
    let name: String
}

struct Camera {
    let id: String
    let name: String
    let rtspUrl: String
    let status: CameraStatus
    let isRecording: Bool
    let ipAddress: String?
    let ptzPresets: [PTZPreset] = []
}

enum CameraStatus: String {
    case online, offline
}

// MARK: - API Client

class APIClient {
    static let shared = APIClient()
    
    private let baseURL = "http://localhost:8080/api"
    
    func getCameraStatus(cameraId: String) -> AnyPublisher<CameraStatusResult, Error> {
        // Implementation
        Future { promise in
            promise(.success(CameraStatusResult(isOnline: true)))
        }
        .eraseToAnyPublisher()
    }
    
    func startRecording(cameraId: String) -> AnyPublisher<Void, Error> {
        Future { promise in
            promise(.success(()))
        }
        .eraseToAnyPublisher()
    }
    
    func stopRecording(cameraId: String) -> AnyPublisher<Void, Error> {
        Future { promise in
            promise(.success(()))
        }
        .eraseToAnyPublisher()
    }
    
    func takeScreenshot(cameraId: String) -> AnyPublisher<Data, Error> {
        Future { promise in
            promise(.success(Data()))
        }
        .eraseToAnyPublisher()
    }
    
    func movePTZ(cameraId: String, direction: PTZDirection) -> AnyPublisher<Void, Error> {
        Future { promise in
            promise(.success(()))
        }
        .eraseToAnyPublisher()
    }
    
    func zoomPTZ(cameraId: String, zoom: PTZZoom) -> AnyPublisher<Void, Error> {
        Future { promise in
            promise(.success(()))
        }
        .eraseToAnyPublisher()
    }
    
    func loadPreset(cameraId: String, presetId: Int) -> AnyPublisher<Void, Error> {
        Future { promise in
            promise(.success(()))
        }
        .eraseToAnyPublisher()
    }
}

struct CameraStatusResult {
    let isOnline: Bool
}
