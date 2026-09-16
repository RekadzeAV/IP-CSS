import SwiftUI

/// Timeline view для просмотра записей и событий по временной шкале.
struct TimelineView: View {
    @State private var selectedDate = Date()
    @State private var events: [TimelineEvent] = []
    @State private var isLoading = false
    @State private var selectedEvent: TimelineEvent?

    private let calendar = Calendar.current
    private let apiBaseURL = ProcessInfo.processInfo.environment["API_BASE_URL"] ?? "http://localhost:8080"

    var body: some View {
        VStack(spacing: 0) {
            // Date picker
            DatePicker(
                "Select Date",
                selection: $selectedDate,
                displayedComponents: [.date]
            )
            .datePickerStyle(.graphical)
            .padding()
            .onChange(of: selectedDate) { _, _ in fetchEvents() }

            // Timeline header
            HStack {
                Text(formatDate(selectedDate))
                    .font(.headline)
                Spacer()
                Text("\(events.count) events")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding(.horizontal)

            // Timeline content
            if isLoading {
                Spacer()
                ProgressView("Loading...")
                Spacer()
            } else if events.isEmpty {
                Spacer()
                VStack(spacing: 12) {
                    Image(systemName: "clock.arrow.circlepath")
                        .font(.system(size: 48))
                        .foregroundColor(.secondary)
                    Text("No events for this date")
                        .font(.headline)
                    Text("Select another date or check camera connection")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Spacer()
            } else {
                List(events) { event in
                    TimelineEventRow(event: event)
                        .onTapGesture { selectedEvent = event }
                }
                .listStyle(.plain)
            }
        }
        .navigationTitle("Timeline")
        .onAppear(perform: fetchEvents)
        .sheet(item: $selectedEvent) { event in
            TimelineEventDetailView(event: event)
        }
    }

    private func fetchEvents() {
        isLoading = true
        let formatter = ISO8601DateFormatter()
        let dateStr = formatter.string(from: selectedDate)

        guard let url = URL(string: "\(apiBaseURL)/api/v1/events?from=\(dateStr)&limit=50") else {
            isLoading = false
            return
        }

        URLSession.shared.dataTask(with: url) { data, response, error in
            DispatchQueue.main.async {
                isLoading = false
                if let data = data {
                    // Parse JSON response
                    if let json = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] {
                        events = json.compactMap { dict in
                            guard let id = dict["id"] as? String,
                                  let type = dict["type"] as? String,
                                  let message = dict["message"] as? String,
                                  let timestamp = dict["timestamp"] as? String else {
                                return nil
                            }
                            return TimelineEvent(
                                id: id,
                                type: type,
                                message: message,
                                timestamp: timestamp,
                                cameraId: dict["cameraId"] as? String,
                                severity: dict["severity"] as? String ?? "INFO"
                            )
                        }
                    }
                }
            }
        }.resume()
    }

    private func formatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .long
        formatter.timeStyle = .none
        return formatter.string(from: date)
    }
}

// MARK: - Models

struct TimelineEvent: Identifiable, Codable {
    let id: String
    let type: String
    let message: String
    let timestamp: String
    let cameraId: String?
    let severity: String
}

// MARK: - Subviews

struct TimelineEventRow: View {
    let event: TimelineEvent

    var body: some View {
        HStack(spacing: 12) {
            // Time indicator
            VStack(spacing: 4) {
                Circle()
                    .fill(severityColor)
                    .frame(width: 10, height: 10)
                Rectangle()
                    .fill(Color.gray.opacity(0.3))
                    .frame(width: 2)
            }

            // Event content
            VStack(alignment: .leading, spacing: 4) {
                Text(event.message)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .lineLimit(2)

                HStack {
                    Text(formatTimestamp(event.timestamp))
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    if let cameraId = event.cameraId {
                        Text("· Camera: \(cameraId.prefix(8))...")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }

                Text(event.type)
                    .font(.caption2)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(severityColor.opacity(0.2))
                    .cornerRadius(4)
            }
        }
        .padding(.vertical, 4)
    }

    private var severityColor: Color {
        switch event.severity.uppercased() {
        case "CRITICAL": return .red
        case "WARNING": return .orange
        default: return .blue
        }
    }

    private func formatTimestamp(_ iso: String) -> String {
        let formatter = ISO8601DateFormatter()
        guard let date = formatter.date(from: iso) else { return iso }
        let display = DateFormatter()
        display.timeStyle = .short
        return display.string(from: date)
    }
}

struct TimelineEventDetailView: View {
    let event: TimelineEvent
    @Environment(\.dismiss) var dismiss

    var body: some View {
        NavigationStack {
            Form {
                Section("Event Details") {
                    LabeledContent("Type", value: event.type)
                    LabeledContent("Severity", value: event.severity)
                    LabeledContent("Time", value: event.timestamp)
                    if let cameraId = event.cameraId {
                        LabeledContent("Camera", value: cameraId)
                    }
                }

                Section("Message") {
                    Text(event.message)
                        .font(.body)
                }

                Section {
                    Button("Acknowledge") {
                        acknowledgeEvent()
                    }
                    .buttonStyle(.borderedProminent)

                    Button("View Recording", systemImage: "video") {
                        // Navigate to recording
                    }
                }
            }
            .navigationTitle("Event")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }

    private func acknowledgeEvent() {
        let url = URL(string: "\(ProcessInfo.processInfo.environment["API_BASE_URL"] ?? "http://localhost:8080")/api/v1/events/\(event.id)/acknowledge")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        URLSession.shared.dataTask(with: request).resume()
        dismiss()
    }
}

#Preview {
    NavigationStack {
        TimelineView()
    }
}
