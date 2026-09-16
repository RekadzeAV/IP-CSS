//
//  HomeView.swift
//  IP-CSS iOS
//
//  Created by NLP-Core-Team
//  Copyright © 2026 NLP-Core-Team. All rights reserved.
//

import SwiftUI

struct HomeView: View {
    @StateObject private var viewModel = HomeViewModel()
    @State private var searchText = ""
    @State private var showingAddCamera = false
    @State private var selectedCamera: Camera?
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Stats Row
                if !viewModel.cameras.isEmpty {
                    StatsRow(
                        online: viewModel.onlineCount,
                        offline: viewModel.offlineCount,
                        recording: viewModel.recordingCount
                    )
                    .padding(.vertical, 12)
                }
                
                // Search Bar
                SearchBar(text: $searchText, placeholder: "Search cameras...")
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                
                // Camera Grid
                if viewModel.filteredCameras.isEmpty {
                    EmptyStateView(
                        icon: "video.slash",
                        title: "No Cameras",
                        subtitle: "Add your first camera to get started",
                        actionLabel: "Add Camera",
                        action: { showingAddCamera = true }
                    )
                } else {
                    ScrollView {
                        LazyVGrid(
                            columns: [
                                GridItem(.flexible(), spacing: 16),
                                GridItem(.flexible(), spacing: 16)
                            ],
                            spacing: 16
                        ) {
                            ForEach(viewModel.filteredCameras) { camera in
                                CameraCardView(camera: camera)
                                    .onTapGesture {
                                        selectedCamera = camera
                                    }
                            }
                        }
                        .padding(16)
                    }
                }
            }
            .navigationTitle("IP-CSS")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { /* Open Settings */ }) {
                        Image(systemName: "gear")
                            .foregroundColor(.primary)
                    }
                }
                
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { showingAddCamera = true }) {
                        Image(systemName: "plus")
                            .foregroundColor(.accentColor)
                    }
                }
            }
            .sheet(isPresented: $showingAddCamera) {
                AddCameraView()
            }
            .navigationDestination(item: $selectedCamera) { camera in
                CameraViewScreen(camera: camera)
            }
        }
        .onAppear {
            viewModel.loadCameras()
        }
    }
}

// MARK: - Stats Row

struct StatsRow: View {
    let online: Int
    let offline: Int
    let recording: Int
    
    var body: some View {
        HStack(spacing: 12) {
            StatCard(
                label: "Online",
                value: "\(online)",
                icon: "checkmark.circle.fill",
                color: .green
            )
            
            StatCard(
                label: "Offline",
                value: "\(offline)",
                icon: "xmark.circle.fill",
                color: .red
            )
            
            StatCard(
                label: "Recording",
                value: "\(recording)",
                icon: "record.circle.fill",
                color: .blue
            )
        }
        .padding(.horizontal, 16)
    }
}

// MARK: - Stat Card

struct StatCard: View {
    let label: String
    let value: String
    let icon: String
    let color: Color
    
    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(color)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(value)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.primary)
                
                Text(label)
                    .font(.system(size: 12))
                    .foregroundColor(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(color.opacity(0.1))
        .cornerRadius(12)
    }
}

// MARK: - Search Bar

struct SearchBar: View {
    @Binding var text: String
    var placeholder: String = "Search"
    
    var body: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.secondary)
            
            TextField(placeholder, text: $text)
                .autocapitalization(.none)
            
            if !text.isEmpty {
                Button(action: { text = "" }) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding(10)
        .background(Color(.systemGray6))
        .cornerRadius(10)
    }
}

// MARK: - Camera Card View

struct CameraCardView: View {
    let camera: Camera
    
    private var statusColor: Color {
        camera.status == .online ? .green : .red
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Thumbnail
            ZStack(alignment: .topLeading) {
                Rectangle()
                    .fill(Color(.systemGray5))
                    .aspectRatio(16/9, contentMode: .fit)
                    .overlay(
                        Image(systemName: camera.status == .online ? "video.fill" : "video.slash.fill")
                            .font(.system(size: 40))
                            .foregroundColor(.secondary)
                    )
                
                // Status Indicator
                Circle()
                    .fill(statusColor)
                    .frame(width: 12, height: 12)
                    .padding(8)
                
                // Recording Indicator
                if camera.isRecording {
                    HStack(spacing: 4) {
                        Circle()
                            .fill(Color.red)
                            .frame(width: 8, height: 8)
                        Text("REC")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(.white)
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.red.opacity(0.8))
                    .cornerRadius(4)
                    .padding(8)
                }
            }
            
            // Info
            VStack(alignment: .leading, spacing: 6) {
                Text(camera.name)
                    .font(.system(size: 14, weight: .semibold))
                    .lineLimit(2)
                
                HStack(spacing: 4) {
                    Circle()
                        .fill(statusColor)
                        .frame(width: 8, height: 8)
                    
                    Text(camera.status == .online ? "Online" : "Offline")
                        .font(.system(size: 12))
                        .foregroundColor(statusColor)
                }
                
                if let ip = camera.ipAddress {
                    Text(ip)
                        .font(.system(size: 11))
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }
            }
            .padding(12)
        }
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
    }
}

// MARK: - Empty State View

struct EmptyStateView: View {
    let icon: String
    let title: String
    let subtitle: String
    let actionLabel: String
    let action: () -> Void
    
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: icon)
                .font(.system(size: 60))
                .foregroundColor(.secondary)
            
            Text(title)
                .font(.system(size: 20, weight: .bold))
            
            Text(subtitle)
                .font(.system(size: 14))
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            
            Button(action: action) {
                HStack {
                    Image(systemName: "plus")
                    Text(actionLabel)
                }
                .font(.system(size: 16, weight: .semibold))
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
                .background(Color.accentColor)
                .foregroundColor(.white)
                .cornerRadius(10)
            }
            .padding(.top, 8)
        }
        .padding(32)
    }
}

// MARK: - Preview

struct HomeView_Previews: PreviewProvider {
    static var previews: some View {
        HomeView()
    }
}
