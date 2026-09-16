//
//  PushNotificationManager.swift
//  IP-CSS iOS
//
//  Created by NLP-Core-Team
//  Copyright © 2026 NLP-Core-Team. All rights reserved.
//

import Foundation
import UserNotifications

// MARK: - Push Notification Manager

class PushNotificationManager: NSObject, ObservableObject {
    static let shared = PushNotificationManager()
    
    @Published var isAuthorized = false
    @Published var deviceToken: String?
    
    private var notificationHandler: ((UNNotification) -> Void)?
    
    override init() {
        super.init()
        setupNotificationCategories()
    }
    
    // MARK: - Authorization
    
    func requestAuthorization() async -> Bool {
        let authOptions: UNAuthorizationOptions = [.alert, .badge, .sound, .criticalAlert]
        
        do {
            let granted = try await UNUserNotificationCenter.current()
                .requestAuthorization(options: authOptions)
            
            await MainActor.run {
                self.isAuthorized = granted
            }
            
            if granted {
                registerForRemoteNotifications()
            }
            
            return granted
        } catch {
            print("Push notification authorization error: \(error)")
            return false
        }
    }
    
    func registerForRemoteNotifications() {
        DispatchQueue.main.async {
            UIApplication.shared.registerForRemoteNotifications()
        }
    }
    
    func unregisterFromRemoteNotifications() {
        UIApplication.shared.unregisterForRemoteNotifications()
        deviceToken = nil
    }
    
    // MARK: - Device Token
    
    func setDeviceToken(_ token: Data) {
        let tokenParts = token.map { data in String(format: "%02.2hhx", data) }
        deviceToken = tokenParts.joined()
        
        // Send token to server
        Task {
            await sendTokenToServer(deviceToken!)
        }
    }
    
    private func sendTokenToServer(_ token: String) async {
        // Implement API call to send token to backend
        print("Device token sent to server: \(token)")
    }
    
    // MARK: - Notification Categories
    
    private func setupNotificationCategories() {
        // Motion Detection Category
        let motionAction = UNNotificationAction(
            identifier: "VIEW_MOTION",
            title: "View Live",
            options: .foreground
        )
        
        let motionCategory = UNNotificationCategory(
            identifier: "MOTION_DETECTED",
            actions: [motionAction],
            intentIdentifiers: [],
            options: .customDismissAction
        )
        
        // Face Recognition Category
        let viewFaceAction = UNNotificationAction(
            identifier: "VIEW_FACE",
            title: "View Photo",
            options: .foreground
        )
        
        let dismissAction = UNNotificationAction(
            identifier: "DISMISS",
            title: "Dismiss",
            options: .destructive
        )
        
        let faceCategory = UNNotificationCategory(
            identifier: "FACE_RECOGNIZED",
            actions: [viewFaceAction, dismissAction],
            intentIdentifiers: [],
            options: []
        )
        
        // Plate Recognition Category
        let viewPlateAction = UNNotificationAction(
            identifier: "VIEW_PLATE",
            title: "View Details",
            options: .foreground
        )
        
        let plateCategory = UNNotificationCategory(
            identifier: "PLATE_DETECTED",
            actions: [viewPlateAction, dismissAction],
            intentIdentifiers: [],
            options: []
        )
        
        // Set categories
        UNUserNotificationCenter.current().setNotificationCategories([
            motionCategory,
            faceCategory,
            plateCategory
        ])
    }
    
    // MARK: - Local Notifications
    
    func scheduleLocalNotification(
        identifier: String,
        title: String,
        body: String,
        categoryIdentifier: String? = nil,
        userInfo: [String: Any]? = nil,
        delay: TimeInterval = 0
    ) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        content.badge = 1
        
        if let category = categoryIdentifier {
            content.categoryIdentifier = category
        }
        
        if let info = userInfo {
            content.userInfo = info
        }
        
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: delay, repeats: false)
        
        let request = UNNotificationRequest(
            identifier: identifier,
            content: content,
            trigger: trigger
        )
        
        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                print("Error scheduling notification: \(error)")
            }
        }
    }
    
    func cancelNotification(identifier: String) {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [identifier])
        UNUserNotificationCenter.current().removeDeliveredNotifications(withIdentifiers: [identifier])
    }
    
    func cancelAllNotifications() {
        UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
        UNUserNotificationCenter.current().removeAllDeliveredNotifications()
    }
    
    // MARK: - Handle Notifications
    
    func handleNotification(_ notification: UNNotification) {
        notificationHandler?(notification)
    }
    
    func setNotificationHandler(_ handler: @escaping (UNNotification) -> Void) {
        notificationHandler = handler
    }
}

// MARK: - UNUserNotificationCenterDelegate

extension PushNotificationManager: UNUserNotificationCenterDelegate {
    
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        // Show notification even when app is in foreground
        completionHandler([.banner, .sound, .badge])
    }
    
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let actionIdentifier = response.actionIdentifier
        let userInfo = response.notification.request.content.userInfo
        
        switch actionIdentifier {
        case "VIEW_MOTION":
            // Navigate to live view
            navigateToCamera(cameraId: userInfo["cameraId"] as? String)
            
        case "VIEW_FACE":
            // Navigate to face recognition details
            navigateToFaceDetails(eventId: userInfo["eventId"] as? String)
            
        case "VIEW_PLATE":
            // Navigate to plate recognition details
            navigateToPlateDetails(eventId: userInfo["eventId"] as? String)
            
        case "DISMISS":
            // Just dismiss
            break
            
        default:
            // Default action - open app
            break
        }
        
        completionHandler()
    }
    
    // Navigation helpers
    
    private func navigateToCamera(cameraId: String?) {
        // Implement navigation to camera view
        NotificationCenter.default.post(
            name: .navigateToCamera,
            object: nil,
            userInfo: ["cameraId": cameraId]
        )
    }
    
    private func navigateToFaceDetails(eventId: String?) {
        NotificationCenter.default.post(
            name: .navigateToEvent,
            object: nil,
            userInfo: ["eventId": eventId]
        )
    }
    
    private func navigateToPlateDetails(eventId: String?) {
        NotificationCenter.default.post(
            name: .navigateToEvent,
            object: nil,
            userInfo: ["eventId": eventId]
        )
    }
}

// MARK: - Notification Extensions

extension Notification.Name {
    static let navigateToCamera = Notification.Name("navigateToCamera")
    static let navigateToEvent = Notification.Name("navigateToEvent")
}

// MARK: - Push Notification Payload Models

struct PushNotificationPayload: Decodable {
    let type: NotificationType
    let title: String
    let body: String
    let cameraId: String?
    let cameraName: String?
    let eventId: String?
    let snapshotUrl: String?
    let timestamp: TimeInterval
    let priority: Priority
    
    enum NotificationType: String, Decodable {
        case motion
        case faceRecognition
        case plateRecognition
        case alert
        case system
    }
    
    enum Priority: String, Decodable {
        case low
        case normal
        case high
        case critical
    }
}

// MARK: - Remote Notification Handler

class RemoteNotificationHandler {
    
    static func handleRemoteNotification(_ userInfo: [AnyHashable: Any]) {
        guard let payload = parsePayload(userInfo) else {
            print("Failed to parse notification payload")
            return
        }
        
        switch payload.type {
        case .motion:
            handleMotionNotification(payload)
        case .faceRecognition:
            handleFaceRecognitionNotification(payload)
        case .plateRecognition:
            handlePlateRecognitionNotification(payload)
        case .alert:
            handleAlertNotification(payload)
        case .system:
            handleSystemNotification(payload)
        }
    }
    
    private static func parsePayload(_ userInfo: [AnyHashable: Any]) -> PushNotificationPayload? {
        guard
            let typeString = userInfo["type"] as? String,
            let type = PushNotificationPayload.NotificationType(rawValue: typeString),
            let title = userInfo["title"] as? String,
            let body = userInfo["body"] as? String,
            let timestamp = userInfo["timestamp"] as? TimeInterval
        else {
            return nil
        }
        
        return PushNotificationPayload(
            type: type,
            title: title,
            body: body,
            cameraId: userInfo["cameraId"] as? String,
            cameraName: userInfo["cameraName"] as? String,
            eventId: userInfo["eventId"] as? String,
            snapshotUrl: userInfo["snapshotUrl"] as? String,
            timestamp: timestamp,
            priority: PushNotificationPayload.Priority(rawValue: userInfo["priority"] as? String ?? "normal") ?? .normal
        )
    }
    
    private static func handleMotionNotification(_ payload: PushNotificationPayload) {
        print("Motion detected: \(payload.body)")
        // Handle motion notification
    }
    
    private static func handleFaceRecognitionNotification(_ payload: PushNotificationPayload) {
        print("Face recognized: \(payload.body)")
        // Handle face recognition notification
    }
    
    private static func handlePlateRecognitionNotification(_ payload: PushNotificationPayload) {
        print("Plate detected: \(payload.body)")
        // Handle plate recognition notification
    }
    
    private static func handleAlertNotification(_ payload: PushNotificationPayload) {
        print("Alert: \(payload.body)")
        // Handle alert notification
    }
    
    private static func handleSystemNotification(_ payload: PushNotificationPayload) {
        print("System: \(payload.body)")
        // Handle system notification
    }
}
