//
//  EventsView.swift
//  IP-CSS iOS
//
//  Created by NLP-Core-Team
//  Copyright © 2026 NLP-Core-Team. All rights reserved.
//

import SwiftUI

struct EventsView: View {
    @StateObject private var viewModel = EventsViewModel()
    @State private var searchText = ""
    @State private var showingFilters = false
    @State private var selectedEvent: EventItem?
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Event Stats
                EventStatsRow(
                    total: viewModel.totalCount,
                    motion: viewModel.motionCount,
                    ai: viewModel.aiCount,
                    alerts: viewModel.alertCount
                )
                .padding(.vertical, 12)
                
                // Filter Bar
                FilterBar(
                    selectedType: $viewModel.selectedType,
                    selectedSeverity: $viewModel.selectedSeverity,
                    onFilters: { showingFilters = true }
                )
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                
                // Events List
                if viewModel.filteredEvents.isEmpty {
                    EmptyStateView(
                        icon: "bell.slash",
                        title: "No Events",
                        subtitle: "Events will appear here when motion or AI detection occurs",
                        actionLabel: nil,
                        action: nil
                    )
                } else {
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            ForEach(viewModel.filteredEvents) { event in
                                EventCardView(event: event)
                                    .onTapGesture {
                                        selectedEvent = event
                                    }
                            }
                        }
                        .padding(16)
                    }
                }
            }
            .navigationTitle("Events")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { viewModel.refresh() }) {
                        Image(systemName: "arrow.clockwise")
                            .foregroundColor(.primary)
                            .rotationEffect(.degrees(viewModel.isLoading ? 360 : 0))
                    }
                    .disabled(viewModel.isLoading)
                }
            }
            .sheet(isPresented: $showingFilters) {
                EventFiltersView(
                    selectedTypes: $viewModel.selectedTypes,
                    selectedSeverities: $viewModel.selectedSeverities,
                    dateRange: $viewModel.dateRange
                )
            }
            .navigationDestination(item: $selectedEvent) { event in
                EventDetailView(event: event)
            }
        }
        .onAppear {
            viewModel.loadEvents()
        }
    }
}

// MARK: - Event Stats Row

struct EventStatsRow: View {
    let total: Int
    let motion: Int
    let ai: Int
    let alerts: Int
    
    var body: some View {
        HStack(spacing: 12) {
            StatCard(
                label: "Total",
                value: "\(total)",
                icon: "bell.fill",
                color: .blue
            )
            
            StatCard(
                label: "Motion",
                value: "\(motion)",
                icon: "waveform.path.ecg",
                color: .orange
            )
            
            StatCard(
                label: "AI",
                value: "\(ai)",
                icon: "brain.head.profile",
                color: .purple
            )
            
            StatCard(
                label: "Alerts",
                value: "\(alerts)",
                icon: "exclamationmark.triangle.fill",
                color: .red
            )
        }
        .padding(.horizontal, 16)
    }
}

// MARK: - Filter Bar

struct FilterBar: View {
    @Binding var selectedType: EventType?
    @Binding var selectedSeverity: EventSeverity?
    let onFilters: () -> Void
    
    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                FilterChip(
                    label: "All",
                    isSelected: selectedType == nil,
                    action: { selectedType = nil }
                )
                
                FilterChip(
                    label: "Motion",
                    icon: "waveform.path.ecg",
                    isSelected: selectedType == .motion,
                    action: { selectedType = .motion }
                )
                
                FilterChip(
                    label: "Face",
                    icon: "faceid",
                    isSelected: selectedType == .faceRecognition,
                    action: { selectedType = .faceRecognition }
                )
                
                FilterChip(
                    label: "Plate",
                    icon: "car.fill",
                    isSelected: selectedType == .plateRecognition,
                    action: { selectedType = .plateRecognition }
                )
                
                Spacer()
                
                Button(action: onFilters) {
                    Image(systemName: "line.3.horizontal.decrease.circle")
                        .font(.system(size: 20))
                        .foregroundColor(.accentColor)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Color.accentColor.opacity(0.1))
                        .cornerRadius(20)
                }
            }
        }
    }
}

// MARK: - Filter Chip

struct FilterChip: View {
    var label: String?
    var icon: String?
    var isSelected: Bool
    let action: () -> Void
    
    init(label: String, isSelected: Bool, action: @escaping () -> Void) {
        self.label = label
        self.icon = nil
        self.isSelected = isSelected
        self.action = action
    }
    
    init(icon: String, isSelected: Bool, action: @escaping () -> Void) {
        self.label = nil
        self.icon = icon
        self.isSelected = isSelected
        self.action = action
    }
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 4) {
                if let icon = icon {
                    Image(systemName: icon)
                }
                if let label = label {
                    Text(label)
                }
            }
            .font(.system(size: 14, weight: .medium))
            .foregroundColor(isSelected ? .white : .primary)
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .background(isSelected ? Color.accentColor : Color(.systemGray5))
            .cornerRadius(20)
        }
    }
}

// MARK: - Event Card View

struct EventCardView: View {
    let event: EventItem
    
    private var typeColor: Color {
        switch event.type {
        case .motion: return .orange
        case .faceRecognition: return .purple
        case .plateRecognition: return .blue
        case .alert: return .red
        }
    }
    
    private var typeIcon: String {
        switch event.type {
        case .motion: return "waveform.path.ecg"
        case .faceRecognition: return "faceid"
        case .plateRecognition: return "car.fill"
        case .alert: return "exclamationmark.triangle.fill"
        }
    }
    
    private var severityColor: Color {
        switch event.severity {
        case .info: return .blue
        case .warning: return .orange
        case .critical: return .red
        }
    }
    
    private var timeString: String {
        let date = Date(timeIntervalSince1970: event.timestamp)
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        return formatter.string(from: date)
    }
    
    private var dateString: String {
        let date = Date(timeIntervalSince1970: event.timestamp)
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM d"
        return formatter.string(from: date)
    }
    
    var body: some View {
        HStack(spacing: 12) {
            // Thumbnail
            ZStack(alignment: .topLeading) {
                Rectangle()
                    .fill(Color(.systemGray5))
                    .frame(width: 100, height: 75)
                    .cornerRadius(8)
                    .overlay(
                        Image(systemName: typeIcon)
                            .font(.system(size: 24))
                            .foregroundColor(typeColor)
                    )
                
                if let snapshotPath = event.snapshotPath {
                    Image(uiImage: UIImage(contentsOfFile: snapshotPath) ?? UIImage())
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(width: 100, height: 75)
                        .cornerRadius(8)
                        .clipped()
                }
                
                // Severity indicator
                Circle()
                    .fill(severityColor)
                    .frame(width: 10, height: 10)
                    .padding(6)
            }
            
            // Event Info
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text(event.cameraName)
                        .font(.system(size: 15, weight: .semibold))
                    
                    Spacer()
                    
                    Text(timeString)
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                }
                
                Text(event.type.description)
                    .font(.system(size: 14))
                    .foregroundColor(typeColor)
                
                if let description = event.description {
                    Text(description)
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                }
                
                HStack {
                    Label(dateString, systemImage: "calendar")
                        .font(.system(size: 11))
                        .foregroundColor(.secondary)
                    
                    if event.isViewed {
                        Label("Viewed", systemImage: "eye")
                            .font(.system(size: 11))
                            .foregroundColor(.secondary)
                    }
                }
            }
            
            Spacer()
            
            // Confidence
            if let confidence = event.confidence {
                VStack {
                    Text("\(Int(confidence * 100))%")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(.primary)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(Color(.systemGray5))
                .cornerRadius(8)
            }
        }
        .padding(12)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
    }
}

// MARK: - Event Filters View

struct EventFiltersView: View {
    @Binding var selectedTypes: Set<EventType>
    @Binding var selectedSeverities: Set<EventSeverity>
    @Binding var dateRange: DateRange
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationView {
            Form {
                Section("Event Types") {
                    ForEach(EventType.allCases, id: \.self) { type in
                        Toggle(isOn: Binding(
                            get: { selectedTypes.contains(type) },
                            set: { isSelected in
                                if isSelected {
                                    selectedTypes.insert(type)
                                } else {
                                    selectedTypes.remove(type)
                                }
                            }
                        )) {
                            Label(type.description, systemImage: type.icon)
                        }
                    }
                }
                
                Section("Severity") {
                    ForEach(EventSeverity.allCases, id: \.self) { severity in
                        Toggle(isOn: Binding(
                            get: { selectedSeverities.contains(severity) },
                            set: { isSelected in
                                if isSelected {
                                    selectedSeverities.insert(severity)
                                } else {
                                    selectedSeverities.remove(severity)
                                }
                            }
                        )) {
                            Label(severity.description, systemImage: severity.icon)
                        }
                    }
                }
                
                Section("Date Range") {
                    DatePicker("From", selection: Binding(
                        get: { Date(timeIntervalSince1970: dateRange.startTime) },
                        set: { dateRange.startTime = $0.timeIntervalSince1970 }
                    ), displayedComponents: .date)
                    
                    DatePicker("To", selection: Binding(
                        get: { Date(timeIntervalSince1970: dateRange.endTime) },
                        set: { dateRange.endTime = $0.timeIntervalSince1970 }
                    ), displayedComponents: .date)
                }
            }
            .navigationTitle("Filters")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Reset") {
                        selectedTypes = Set(EventType.allCases)
                        selectedSeverities = Set(EventSeverity.allCases)
                    }
                }
            }
        }
    }
}

// MARK: - Event Detail View

struct EventDetailView: View {
    let event: EventItem
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Snapshot
                if let snapshotPath = event.snapshotPath {
                    Image(uiImage: UIImage(contentsOfFile: snapshotPath) ?? UIImage())
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .cornerRadius(12)
                        .shadow(color: .black.opacity(0.1), radius: 8)
                }
                
                // Event Info Card
                VStack(alignment: .leading, spacing: 16) {
                    HStack {
                        Label(event.type.description, systemImage: event.type.icon)
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(event.type.color)
                        
                        Spacer()
                        
                        Text(event.severity.description)
                            .font(.system(size: 14, weight: .medium))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(event.severity.color.opacity(0.1))
                            .foregroundColor(event.severity.color)
                            .cornerRadius(8)
                    }
                    
                    Divider()
                    
                    InfoRow(label: "Camera", value: event.cameraName)
                    InfoRow(label: "Time", value: formattedDate(event.timestamp))
                    if let confidence = event.confidence {
                        InfoRow(label: "Confidence", value: "\(Int(confidence * 100))%")
                    }
                    if let description = event.description {
                        InfoRow(label: "Description", value: description)
                    }
                    if let zone = event.zone {
                        InfoRow(label: "Zone", value: zone)
                    }
                }
                .padding()
                .background(Color(.systemBackground))
                .cornerRadius(12)
                
                // Actions
                HStack(spacing: 16) {
                    ActionButton(
                        icon: "bookmark",
                        label: "Save",
                        color: .blue
                    ) {
                        // Save event
                    }
                    
                    ActionButton(
                        icon: "square.and.arrow.up",
                        label: "Share",
                        color: .green
                    ) {
                        // Share event
                    }
                    
                    ActionButton(
                        icon: "trash",
                        label: "Delete",
                        color: .red
                    ) {
                        // Delete event
                    }
                }
            }
            .padding()
        }
        .navigationTitle("Event Details")
        .navigationBarTitleDisplayMode(.inline)
    }
    
    private func formattedDate(_ timestamp: TimeInterval) -> String {
        let date = Date(timeIntervalSince1970: timestamp)
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM d, yyyy HH:mm:ss"
        return formatter.string(from: date)
    }
}

struct InfoRow: View {
    let label: String
    let value: String
    
    var body: some View {
        HStack {
            Text(label)
                .font(.system(size: 14))
                .foregroundColor(.secondary)
            
            Spacer()
            
            Text(value)
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(.primary)
        }
    }
}

struct ActionButton: View {
    let icon: String
    let label: String
    let color: Color
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.system(size: 20))
                    .foregroundColor(color)
                
                Text(label)
                    .font(.system(size: 12))
                    .foregroundColor(color)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(color.opacity(0.1))
            .cornerRadius(10)
        }
    }
}

// MARK: - Preview

struct EventsView_Previews: PreviewProvider {
    static var previews: some View {
        EventsView()
    }
}
