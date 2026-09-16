//
//  IP_CSSApp.swift
//  IP-CSS iOS
//
//  Created by NLP-Core-Team
//  Copyright © 2026 NLP-Core-Team. All rights reserved.
//

import SwiftUI

@main
struct IP_CSSApp: App {
    @StateObject private var appState = AppState()
    
    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(appState)
                .onAppear {
                    setupAppearance()
                }
        }
    }
    
    private func setupAppearance() {
        // Global appearance settings
        UINavigationBarAppearance().configureWithOpaqueBackground()
        UITabBarAppearance().configureWithOpaqueBackground()
    }
}

// MARK: - App State

class AppState: ObservableObject {
    @Published var isAuthenticated = false
    @Published var currentUser: User?
    @Published var theme: AppTheme = .dark
    
    enum AppTheme {
        case light, dark, system
    }
}

// MARK: - Root View

struct RootView: View {
    @EnvironmentObject var appState: AppState
    
    var body: some View {
        Group {
            if appState.isAuthenticated {
                MainTabView()
            } else {
                LoginView()
            }
        }
    }
}

// MARK: - Main Tab View

struct MainTabView: View {
    var body: some View {
        TabView {
            HomeView()
                .tabItem {
                    Label("Cameras", systemImage: "video.fill")
                }
            
            EventsView()
                .tabItem {
                    Label("Events", systemImage: "bell.fill")
                }
            
            TimelineView()
                .tabItem {
                    Label("Timeline", systemImage: "clock.fill")
                }
            
            SettingsView()
                .tabItem {
                    Label("Settings", systemImage: "gear")
                }
        }
    }
}

// MARK: - Preview

struct IP_CSSApp_Previews: PreviewProvider {
    static var previews: some View {
        IP_CSSApp()
    }
}
