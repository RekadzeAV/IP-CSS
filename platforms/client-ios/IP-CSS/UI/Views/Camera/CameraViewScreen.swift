//
//  CameraViewScreen.swift
//  IP-CSS iOS
//
//  Created by NLP-Core-Team
//  Copyright © 2026 NLP-Core-Team. All rights reserved.
//

import SwiftUI
import AVFoundation

struct CameraViewScreen: View {
    let camera: Camera
    @StateObject private var viewModel = CameraViewModel()
    @State private var showPTZ = false
    @State private var showSettings = false
    @State private var isRecording = false
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        VStack(spacing: 0) {
            // Video Player
            VideoPlayerView(streamUrl: viewModel.streamUrl)
                .aspectRatio(16/9, contentMode: .fit)
                .overlay(
                    VideoOverlay(
                        cameraName: camera.name,
                        isRecording: isRecording,
                        isOnline: viewModel.isOnline
                    )
                )
            
            // Controls
            ControlBar(
                isRecording: isRecording,
                onRecord: {
                    withAnimation {
                        isRecording.toggle()
                        if isRecording {
                            viewModel.startRecording()
                        } else {
                            viewModel.stopRecording()
                        }
                    }
                },
                onScreenshot: viewModel.takeScreenshot,
                onPTZ: { showPTZ.toggle() },
                onFullscreen: { /* Toggle fullscreen */ },
                onSettings: { showSettings = true }
            )
            .padding()
            
            // PTZ Controls
            if showPTZ {
                PTZControlsView(
                    onDirection: viewModel.movePTZ,
                    onZoom: viewModel.zoomPTZ,
                    onPreset: viewModel.loadPreset,
                    presets: camera.ptzPresets
                )
                .transition(.move(edge: .bottom))
                .padding()
            }
        }
        .navigationTitle(camera.name)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: { dismiss() }) {
                    Image(systemName: "chevron.left")
                        .foregroundColor(.primary)
                }
            }
            
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button(action: { /* Share */ }) {
                        Label("Share", systemImage: "square.and.arrow.up")
                    }
                    Button(action: { /* Timeline */ }) {
                        Label("Timeline", systemImage: "clock")
                    }
                    Button(action: { showSettings = true }) {
                        Label("Settings", systemImage: "gear")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
            }
        }
        .sheet(isPresented: $showSettings) {
            CameraSettingsView(camera: camera)
        }
        .onAppear {
            viewModel.connect(camera: camera)
        }
        .onDisappear {
            viewModel.disconnect()
        }
    }
}

// MARK: - Video Overlay

struct VideoOverlay: View {
    let cameraName: String
    let isRecording: Bool
    let isOnline: Bool
    
    var body: some View {
        VStack {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(cameraName)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.white)
                    
                    HStack(spacing: 6) {
                        Circle()
                            .fill(isOnline ? .green : .red)
                            .frame(width: 8, height: 8)
                        
                        Text(isOnline ? "LIVE" : "OFFLINE")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(isOnline ? .green : .red)
                    }
                }
                .padding(12)
                .background(Color.black.opacity(0.6))
                .cornerRadius(8)
                .padding()
                
                Spacer()
                
                if isRecording {
                    HStack(spacing: 6) {
                        Circle()
                            .fill(Color.red)
                            .frame(width: 10, height: 10)
                            .shadow(color: .red, radius: 4)
                        
                        Text("REC")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(.red)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Color.black.opacity(0.6))
                    .cornerRadius(8)
                    .padding()
                }
            }
            
            Spacer()
        }
    }
}

// MARK: - Control Bar

struct ControlBar: View {
    let isRecording: Bool
    let onRecord: () -> Void
    let onScreenshot: () -> Void
    let onPTZ: () -> Void
    let onFullscreen: () -> Void
    let onSettings: () -> Void
    
    var body: some View {
        HStack(spacing: 20) {
            ControlButton(
                icon: isRecording ? "stop.fill" : "record.circle",
                color: isRecording ? .red : .primary,
                action: onRecord
            )
            
            ControlButton(
                icon: "camera.fill",
                color: .primary,
                action: onScreenshot
            )
            
            ControlButton(
                icon: "arrow.up.left.and.arrow.down.right",
                color: .primary,
                action: onPTZ
            )
            
            ControlButton(
                icon: "arrow.up.left.arrow.down.right",
                color: .primary,
                action: onFullscreen
            )
            
            ControlButton(
                icon: "gear",
                color: .primary,
                action: onSettings
            )
        }
    }
}

// MARK: - Control Button

struct ControlButton: View {
    let icon: String
    let color: Color
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: 22))
                .foregroundColor(color)
                .frame(width: 50, height: 50)
                .background(color.opacity(0.1))
                .clipShape(Circle())
        }
    }
}

// MARK: - PTZ Controls View

struct PTZControlsView: View {
    let onDirection: (PTZDirection) -> Void
    let onZoom: (PTZZoom) -> Void
    let onPreset: (Int) -> Void
    let presets: [PTZPreset]
    
    var body: some View {
        VStack(spacing: 16) {
            // Direction Pad
            DirectionPad(onDirection: onDirection)
            
            // Zoom Controls
            HStack(spacing: 24) {
                ZoomButton(action: { onZoom(.zoomIn) }) {
                    Image(systemName: "plus.magnifyingglass")
                }
                
                Text("ZOOM")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(.secondary)
                
                ZoomButton(action: { onZoom(.zoomOut) }) {
                    Image(systemName: "minus.magnifyingglass")
                }
            }
            
            // Presets
            if !presets.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(presets) { preset in
                            PresetButton(preset: preset) {
                                onPreset(preset.id)
                            }
                        }
                    }
                }
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(16)
    }
}

// MARK: - Direction Pad

struct DirectionPad: View {
    let onDirection: (PTZDirection) -> Void
    
    var body: some View {
        ZStack {
            Circle()
                .fill(Color(.systemGray5))
                .frame(width: 160, height: 160)
            
            VStack(spacing: 8) {
                DirectionButton(direction: .up) { onDirection(.up) }
                
                HStack(spacing: 40) {
                    DirectionButton(direction: .left) { onDirection(.left) }
                    DirectionButton(direction: .right) { onDirection(.right) }
                }
                
                DirectionButton(direction: .down) { onDirection(.down) }
            }
        }
    }
}

// MARK: - Direction Button

struct DirectionButton: View {
    let direction: PTZDirection
    let action: () -> Void
    
    var icon: String {
        switch direction {
        case .up: return "chevron.up"
        case .down: return "chevron.down"
        case .left: return "chevron.left"
        case .right: return "chevron.right"
        }
    }
    
    var body: some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(.primary)
                .frame(width: 44, height: 44)
                .background(Color(.systemBackground))
                .clipShape(Circle())
                .shadow(color: .black.opacity(0.1), radius: 2)
        }
    }
}

// MARK: - Zoom Button

struct ZoomButton<Content: View>: View {
    let action: () -> Void
    @ViewBuilder let content: Content
    
    var body: some View {
        Button(action: action) {
            content
                .font(.system(size: 24))
                .foregroundColor(.primary)
                .frame(width: 50, height: 50)
                .background(Color(.systemBackground))
                .clipShape(Circle())
                .shadow(color: .black.opacity(0.1), radius: 2)
        }
    }
}

// MARK: - Preset Button

struct PresetButton: View {
    let preset: PTZPreset
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            VStack(spacing: 4) {
                Image(systemName: "bookmark.fill")
                    .font(.system(size: 18))
                    .foregroundColor(.accentColor)
                
                Text(preset.name)
                    .font(.system(size: 11))
                    .foregroundColor(.primary)
            }
            .frame(width: 60, height: 60)
            .background(Color(.systemBackground))
            .cornerRadius(12)
            .shadow(color: .black.opacity(0.1), radius: 2)
        }
    }
}

// MARK: - Preview

struct CameraViewScreen_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            CameraViewScreen(
                camera: Camera(
                    id: "1",
                    name: "Front Door",
                    rtspUrl: "rtsp://192.168.1.100:554/stream",
                    status: .online,
                    isRecording: false,
                    ipAddress: "192.168.1.100"
                )
            )
        }
    }
}
