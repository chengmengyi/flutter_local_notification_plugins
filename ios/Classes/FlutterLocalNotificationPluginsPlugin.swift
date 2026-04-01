import Flutter
import UIKit
import UserNotifications

public class FlutterLocalNotificationPluginsPlugin: NSObject, FlutterPlugin, UNUserNotificationCenterDelegate {
  private static let displayedNotificationCountKey = "displayed_notification_count"
  private static let launchDetailsKey = "launch_details"
  private let defaultChannelId = "default_notification_channel"
  private let defaultChannelName = "Notifications"
  private let defaultChannelDescription = "App notifications"
  private var channelId = "default_notification_channel"
  private var channelName = "Notifications"
  private var channelDescription = "App notifications"
  private var channel: FlutterMethodChannel?

  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "flutter_local_notification_plugins", binaryMessenger: registrar.messenger())
    let instance = FlutterLocalNotificationPluginsPlugin()
    instance.channel = channel
    UNUserNotificationCenter.current().delegate = instance
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {
    case "getPlatformVersion":
      result("iOS " + UIDevice.current.systemVersion)
    case "consumeDisplayedNotificationCount":
      result(consumeDisplayedNotificationCount())
    case "checkOverlayPermission":
      result(false)
    case "requestOverlayPermission":
      result(false)
    case "showProcessingOverlay":
      result(nil)
    case "updateProcessingOverlay":
      result(nil)
    case "closeProcessingOverlay":
      result(nil)
    case "isProcessingOverlayActive":
      result(false)
    case "consumeProcessingOverlayLaunchTaskId":
      result(nil)
    case "moveAppToBack":
      result(false)
    case "getNotificationAppLaunchDetails":
      result(getNotificationAppLaunchDetails())
    case "initNotification":
      initNotification(call, result: result)
    case "show":
      show(call, result: result)
    case "periodicallyShowWithDuration":
      periodicallyShowWithDuration(call, result: result)
    default:
      result(FlutterMethodNotImplemented)
    }
  }

  private func initNotification(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    let args = call.arguments as? [String: Any]
    channelId = args?["channelId"] as? String ?? defaultChannelId
    channelName = args?["channelName"] as? String ?? defaultChannelName
    channelDescription = args?["channelDescription"] as? String ?? defaultChannelDescription
    let center = UNUserNotificationCenter.current()
    center.delegate = self
    center.getNotificationSettings { settings in
      DispatchQueue.main.async {
        let status = settings.authorizationStatus
        let granted =
          status == .authorized ||
          status == .provisional ||
          status == .ephemeral
        result(granted)
      }
    }
  }

  private func show(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    guard let args = call.arguments as? [String: Any],
          let id = args["id"] as? Int else {
      result(FlutterError(code: "invalid_id", message: "Notification id is required", details: nil))
      return
    }
    let content = buildContent(args: args)
    let request = UNNotificationRequest(
      identifier: "\(id)_\(Int(Date().timeIntervalSince1970 * 1000))",
      content: content,
      trigger: nil
    )
    UNUserNotificationCenter.current().add(request) { error in
      DispatchQueue.main.async {
        if let error {
          result(FlutterError(code: "show_failed", message: error.localizedDescription, details: nil))
        } else {
          Self.increaseDisplayedNotificationCount()
          result(nil)
        }
      }
    }
  }

  private func periodicallyShowWithDuration(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    guard let args = call.arguments as? [String: Any],
          let id = args["id"] as? Int else {
      result(FlutterError(code: "invalid_id", message: "Notification id is required", details: nil))
      return
    }
    let intervalMilliseconds = (args["repeatIntervalMilliseconds"] as? NSNumber)?.doubleValue ?? 0
    let intervalSeconds = intervalMilliseconds / 1000.0
    if intervalSeconds < 60 {
      result(
        FlutterError(
          code: "invalid_repeat_interval",
          message: "iOS repeating notifications require at least 60 seconds",
          details: nil
        )
      )
      return
    }
    let center = UNUserNotificationCenter.current()
    center.getPendingNotificationRequests { requests in
      let prefix = "periodic_\(id)_"
      let identifiers = requests.map(\.identifier).filter { $0.hasPrefix(prefix) }
      if !identifiers.isEmpty {
        center.removePendingNotificationRequests(withIdentifiers: identifiers)
      }
      let messageList = args["notificationList"] as? [[String: Any]] ?? []
      let scheduleCount = 32
      let group = DispatchGroup()
      var scheduleError: Error?
      for index in 0..<scheduleCount {
        let content = self.buildContent(args: args, notificationList: messageList)
        let interval = intervalSeconds * Double(index + 1)
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: interval, repeats: false)
        let request = UNNotificationRequest(
          identifier: "\(prefix)\(index)",
          content: content,
          trigger: trigger
        )
        group.enter()
        center.add(request) { error in
          if scheduleError == nil {
            scheduleError = error
          }
          group.leave()
        }
      }
      group.notify(queue: .main) {
        if let scheduleError {
          result(FlutterError(code: "schedule_failed", message: scheduleError.localizedDescription, details: nil))
        } else {
          result(nil)
        }
      }
    }
  }

  private func buildContent(
    args: [String: Any],
    notificationList: [[String: Any]] = []
  ) -> UNMutableNotificationContent {
    let randomMessage = notificationList.isEmpty ? nil : notificationList.randomElement()
    let content = UNMutableNotificationContent()
    let title = (randomMessage?["title"] as? String) ?? (args["title"] as? String ?? "")
    let payload = (randomMessage?["payload"] as? String) ?? (args["payload"] as? String ?? "")
    let clickPayload = (args["clickPayload"] as? String).flatMap { value in
      let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
      return trimmed.isEmpty ? nil : trimmed
    } ?? payload
    let debugPayload = resolveDebugPayload(
      primaryPayload: args["payload"] as? String,
      fallbackPayload: payload
    )
    content.title = resolveDebugDisplayTitle(title: title, payload: debugPayload)
    content.body = (randomMessage?["body"] as? String) ?? (args["body"] as? String ?? "")
    content.sound = .default
    content.userInfo = [
      "payload": clickPayload,
      "channelId": channelId,
      "channelName": channelName,
      "channelDescription": channelDescription
    ]
    return content
  }

  private func resolveDebugPayload(primaryPayload: String?, fallbackPayload: String?) -> String? {
    let debugTypes: Set<String> = ["local", "lock", "fcm", "media"]
    if let primary = primaryPayload?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
       debugTypes.contains(primary) {
      return primary
    }
    if let fallback = fallbackPayload?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
       debugTypes.contains(fallback) {
      return fallback
    }
    return nil
  }

  private func resolveDebugDisplayTitle(title: String, payload: String?) -> String {
    #if DEBUG
    guard let payload, !payload.isEmpty else {
      return title
    }
    return title.isEmpty ? "[\(payload)]" : "\(title) [\(payload)]"
    #else
    return title
    #endif
  }

  public func userNotificationCenter(
    _ center: UNUserNotificationCenter,
    willPresent notification: UNNotification,
    withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
  ) {
    Self.increaseDisplayedNotificationCount()
    if #available(iOS 14.0, *) {
      completionHandler([.banner, .badge, .sound, .list])
    } else {
      completionHandler([.alert, .badge, .sound])
    }
  }

  public func userNotificationCenter(
    _ center: UNUserNotificationCenter,
    didReceive response: UNNotificationResponse,
    withCompletionHandler completionHandler: @escaping () -> Void
  ) {
    let arguments: [String: Any?] = [
      "id": Int(response.notification.request.identifier.split(separator: "_").dropFirst().first ?? "0") ?? 0,
      "title": response.notification.request.content.title,
      "body": response.notification.request.content.body,
      "payload": response.notification.request.content.userInfo["payload"] as? String
    ]
    if UIApplication.shared.applicationState == .active {
      channel?.invokeMethod("onNotificationClicked", arguments: arguments)
    } else {
      Self.cacheLaunchDetails(arguments)
    }
    completionHandler()
  }

  private static func increaseDisplayedNotificationCount() {
    let currentCount = UserDefaults.standard.integer(forKey: displayedNotificationCountKey)
    UserDefaults.standard.set(currentCount + 1, forKey: displayedNotificationCountKey)
  }

  private func consumeDisplayedNotificationCount() -> Int {
    let count = UserDefaults.standard.integer(forKey: Self.displayedNotificationCountKey)
    UserDefaults.standard.removeObject(forKey: Self.displayedNotificationCountKey)
    return count
  }

  private static func cacheLaunchDetails(_ arguments: [String: Any?]) {
    let payload: [String: Any] = [
      "id": arguments["id"] ?? 0,
      "title": arguments["title"] ?? "",
      "body": arguments["body"] ?? "",
      "payload": arguments["payload"] ?? ""
    ]
    UserDefaults.standard.set(payload, forKey: launchDetailsKey)
  }

  private func getNotificationAppLaunchDetails() -> [String: Any] {
    guard let payload = UserDefaults.standard.dictionary(forKey: Self.launchDetailsKey) else {
      return ["didNotificationLaunchApp": false]
    }
    UserDefaults.standard.removeObject(forKey: Self.launchDetailsKey)
    return [
      "didNotificationLaunchApp": true,
      "notificationResponse": payload
    ]
  }
}
