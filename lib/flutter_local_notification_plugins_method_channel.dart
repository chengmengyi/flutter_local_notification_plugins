import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'flutter_local_notification_plugins_platform_interface.dart';
import 'src/local_notification_models.dart';

/// An implementation of [FlutterLocalNotificationPluginsPlatform] that uses method channels.
class MethodChannelFlutterLocalNotificationPlugins
    extends FlutterLocalNotificationPluginsPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel(
    'flutter_local_notification_plugins',
  );

  ValueChanged<LocalNotificationEvent>? onNotificationClicked;
  ValueChanged<String>? onProcessingOverlayClicked;

  MethodChannelFlutterLocalNotificationPlugins() {
    methodChannel.setMethodCallHandler(_handleMethodCall);
  }

  /// 通过原生通道获取平台版本。
  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>(
      'getPlatformVersion',
    );
    return version;
  }

  /// 通过原生通道取出并清空已展示通知数量。
  @override
  Future<int> consumeDisplayedNotificationCount() async {
    final result = await methodChannel.invokeMethod<int>(
      'consumeDisplayedNotificationCount',
    );
    return result ?? 0;
  }

  /// 通过原生通道检查悬浮层权限。
  @override
  Future<bool> checkOverlayPermission() async {
    final result = await methodChannel.invokeMethod<bool>(
      'checkOverlayPermission',
    );
    return result ?? false;
  }

  /// 通过原生通道请求悬浮层权限。
  @override
  Future<bool> requestOverlayPermission() async {
    final result = await methodChannel.invokeMethod<bool>(
      'requestOverlayPermission',
    );
    return result ?? false;
  }

  /// 通过原生通道显示悬浮进度层。
  @override
  Future<void> showProcessingOverlay({
    required String taskId,
    required String title,
    required double progress,
  }) {
    return methodChannel.invokeMethod<void>('showProcessingOverlay', {
      'taskId': taskId,
      'title': title,
      'progress': progress,
    });
  }

  /// 通过原生通道更新悬浮进度层。
  @override
  Future<void> updateProcessingOverlay({
    required String taskId,
    required String title,
    required double progress,
  }) {
    return methodChannel.invokeMethod<void>('updateProcessingOverlay', {
      'taskId': taskId,
      'title': title,
      'progress': progress,
    });
  }

  /// 通过原生通道关闭悬浮进度层。
  @override
  Future<void> closeProcessingOverlay() {
    return methodChannel.invokeMethod<void>('closeProcessingOverlay');
  }

  /// 通过原生通道查询悬浮进度层状态。
  @override
  Future<bool> isProcessingOverlayActive() async {
    final result = await methodChannel.invokeMethod<bool>(
      'isProcessingOverlayActive',
    );
    return result ?? false;
  }

  /// 通过原生通道取出悬浮层启动任务 ID。
  @override
  Future<String?> consumeProcessingOverlayLaunchTaskId() {
    return methodChannel.invokeMethod<String>(
      'consumeProcessingOverlayLaunchTaskId',
    );
  }

  /// 通过原生通道将应用切到后台。
  @override
  Future<bool> moveAppToBack() async {
    final result = await methodChannel.invokeMethod<bool>('moveAppToBack');
    return result ?? false;
  }

  /// 通过原生通道配置 Android 的 WorkManager 循环间隔。
  @override
  Future<void> configureAndroidWorkManager({
    Duration interval = const Duration(minutes: 60),
  }) {
    return methodChannel.invokeMethod<void>('configureAndroidWorkManager', {
      'intervalMilliseconds': interval.inMilliseconds,
    });
  }

  /// 通过原生通道获取通知启动信息。
  @override
  Future<Map<String, dynamic>> getNotificationAppLaunchDetails() async {
    final result = await methodChannel.invokeMapMethod<String, dynamic>(
      'getNotificationAppLaunchDetails',
    );
    return result ?? <String, dynamic>{'didNotificationLaunchApp': false};
  }

  /// 通过原生通道订阅 FCM 主题并保存配置。
  @override
  Future<bool> subscribeToTopic({
    required String topic,
    required String channelId,
    required String channelName,
    String? channelDescription,
    required int priority,
    required int importance,
    String? style,
    String? beautyTitle,
    String? beautyBody,
    String? beautyImage,
    String? beautyButton,
    String? beautyAppIcon,
  }) async {
    final result = await methodChannel.invokeMethod<bool>('subscribeToTopic', {
      'topic': topic,
      'channelId': channelId,
      'channelName': channelName,
      'channelDescription': channelDescription,
      'priority': priority,
      'importance': importance,
      'style': style,
      'beautyTitle': beautyTitle,
      'beautyBody': beautyBody,
      'beautyImage': beautyImage,
      'beautyButton': beautyButton,
      'beautyAppIcon': beautyAppIcon,
    });
    return result ?? false;
  }

  /// 通过原生通道初始化通知配置。
  @override
  Future<bool> initNotification({
    String channelId = 'default_notification_channel',
    String channelName = 'Notifications',
    String? channelDescription,
    Map<String, Object?>? customLayout,
  }) async {
    final result = await methodChannel.invokeMethod<bool>('initNotification', {
      'channelId': channelId,
      'channelName': channelName,
      'channelDescription': channelDescription,
      'customLayout': customLayout,
    });
    return result ?? false;
  }

  /// 通过原生通道显示常驻快捷通知。
  @override
  Future<void> showPersistentShortcutNotification({
    required String homeText,
    required String mergeText,
    required String importText,
    required String convertText,
    required String homeIcon,
    required String mergeIcon,
    required String importIcon,
    required String convertIcon,
    Map<String, Object?>? customLayout,
  }) {
    return methodChannel
        .invokeMethod<void>('showPersistentShortcutNotification', {
          'homeText': homeText,
          'mergeText': mergeText,
          'importText': importText,
          'convertText': convertText,
          'homeIcon': homeIcon,
          'mergeIcon': mergeIcon,
          'importIcon': importIcon,
          'convertIcon': convertIcon,
          'customLayout': customLayout,
        });
  }

  /// 通过原生通道立即显示一条通知。
  @override
  Future<void> show({
    required int id,
    String? title,
    String? body,
    String? payload,
    String? clickPayload,
  }) {
    return methodChannel.invokeMethod<void>('show', {
      'id': id,
      'title': title,
      'body': body,
      'payload': payload ?? '',
      'clickPayload': clickPayload,
    });
  }

  /// 通过原生通道按时间间隔循环展示通知。
  @override
  Future<void> periodicallyShowWithDuration({
    required int id,
    String? title,
    String? body,
    Duration repeatDurationInterval = const Duration(minutes: 30),
    String? payload,
    Map<String, Object?>? notificationDetails,
    List<Map<String, Object?>>? notificationList,
  }) {
    return methodChannel.invokeMethod<void>('periodicallyShowWithDuration', {
      'id': id,
      'title': title,
      'body': body,
      'payload': payload ?? '',
      'repeatIntervalMilliseconds': repeatDurationInterval.inMilliseconds,
      'notificationDetails': notificationDetails,
      'notificationList': notificationList,
    });
  }

  /// 通过原生通道开启解锁触发通知。
  @override
  Future<void> startUnlockTriggeredNotifications({
    Duration interval = const Duration(minutes: 30),
    List<Map<String, Object?>>? notificationList,
  }) {
    return methodChannel
        .invokeMethod<void>('startUnlockTriggeredNotifications', {
          'intervalMilliseconds': interval.inMilliseconds,
          'notificationList': notificationList,
        });
  }

  /// 处理原生层主动回传的方法调用。
  Future<void> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onNotificationClicked':
        final args = (call.arguments as Map?) ?? <dynamic, dynamic>{};
        onNotificationClicked?.call(LocalNotificationEvent.fromMap(args));
        break;
      case 'onProcessingOverlayClicked':
        final args = (call.arguments as Map?) ?? <dynamic, dynamic>{};
        final taskId = args['taskId']?.toString();
        if (taskId != null && taskId.isNotEmpty) {
          onProcessingOverlayClicked?.call(taskId);
        }
        break;
      default:
        break;
    }
  }
}
