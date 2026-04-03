import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'flutter_local_notification_plugins_method_channel.dart';

abstract class FlutterLocalNotificationPluginsPlatform
    extends PlatformInterface {
  /// Constructs a FlutterLocalNotificationPluginsPlatform.
  FlutterLocalNotificationPluginsPlatform() : super(token: _token);

  static final Object _token = Object();

  static FlutterLocalNotificationPluginsPlatform _instance =
      MethodChannelFlutterLocalNotificationPlugins();

  /// The default instance of [FlutterLocalNotificationPluginsPlatform] to use.
  ///
  /// Defaults to [MethodChannelFlutterLocalNotificationPlugins].
  static FlutterLocalNotificationPluginsPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FlutterLocalNotificationPluginsPlatform] when
  /// they register themselves.
  static set instance(FlutterLocalNotificationPluginsPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  /// 获取当前平台版本信息。
  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  /// 取出并清空已展示通知数量。
  Future<int> consumeDisplayedNotificationCount() {
    throw UnimplementedError(
      'consumeDisplayedNotificationCount() has not been implemented.',
    );
  }

  /// 检查悬浮层权限是否已开启。
  Future<bool> checkOverlayPermission() {
    throw UnimplementedError(
      'checkOverlayPermission() has not been implemented.',
    );
  }

  /// 请求系统悬浮层权限。
  Future<bool> requestOverlayPermission() {
    throw UnimplementedError(
      'requestOverlayPermission() has not been implemented.',
    );
  }

  /// 显示处理中的悬浮进度层。
  Future<void> showProcessingOverlay({
    required String taskId,
    required String title,
    required double progress,
  }) {
    throw UnimplementedError(
      'showProcessingOverlay() has not been implemented.',
    );
  }

  /// 更新处理中的悬浮进度层。
  Future<void> updateProcessingOverlay({
    required String taskId,
    required String title,
    required double progress,
  }) {
    throw UnimplementedError(
      'updateProcessingOverlay() has not been implemented.',
    );
  }

  /// 关闭处理中的悬浮进度层。
  Future<void> closeProcessingOverlay() {
    throw UnimplementedError(
      'closeProcessingOverlay() has not been implemented.',
    );
  }

  /// 判断悬浮进度层是否仍在显示。
  Future<bool> isProcessingOverlayActive() {
    throw UnimplementedError(
      'isProcessingOverlayActive() has not been implemented.',
    );
  }

  /// 取出并清空通过悬浮层拉起应用时的任务 ID。
  Future<String?> consumeProcessingOverlayLaunchTaskId() {
    throw UnimplementedError(
      'consumeProcessingOverlayLaunchTaskId() has not been implemented.',
    );
  }

  /// 将应用切到后台。
  Future<bool> moveAppToBack() {
    throw UnimplementedError('moveAppToBack() has not been implemented.');
  }

  /// 配置需要屏蔽通知的手机品牌列表。
  Future<void> configureBlockedManufacturers({
    required List<String> manufacturers,
  }) {
    throw UnimplementedError(
      'configureBlockedManufacturers() has not been implemented.',
    );
  }

  /// 判断当前手机是否为三星。
  Future<bool> isSamsungDevice() {
    throw UnimplementedError('isSamsungDevice() has not been implemented.');
  }

  /// 配置 Android 的 WorkManager 循环间隔。
  Future<void> configureAndroidWorkManager({
    Duration interval = const Duration(minutes: 60),
  }) {
    throw UnimplementedError(
      'configureAndroidWorkManager() has not been implemented.',
    );
  }

  /// 获取通知点击拉起应用的启动信息。
  Future<Map<String, dynamic>> getNotificationAppLaunchDetails() {
    throw UnimplementedError(
      'getNotificationAppLaunchDetails() has not been implemented.',
    );
  }

  /// 订阅 FCM 主题并保存通知样式配置。
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
  }) {
    throw UnimplementedError('subscribeToTopic() has not been implemented.');
  }

  /// 初始化通知通道和自定义布局配置。
  Future<bool> initNotification({
    String channelId = 'default_notification_channel',
    String channelName = 'Notifications',
    String? channelDescription,
    Map<String, Object?>? customLayout,
  }) {
    throw UnimplementedError('initNotification() has not been implemented.');
  }

  /// 显示常驻快捷入口通知。
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
    throw UnimplementedError(
      'showPersistentShortcutNotification() has not been implemented.',
    );
  }

  /// 立即显示一条本地通知。
  Future<void> show({
    required int id,
    String? title,
    String? body,
    String? payload,
    String? clickPayload,
    String? mediaBackgroundImageName,
  }) {
    throw UnimplementedError('show() has not been implemented.');
  }

  /// 按固定时间间隔循环展示通知。
  Future<void> periodicallyShowWithDuration({
    required int id,
    String? title,
    String? body,
    Duration repeatDurationInterval = const Duration(minutes: 30),
    String? payload,
    String? mediaBackgroundImageName,
    Map<String, Object?>? notificationDetails,
    List<Map<String, Object?>>? notificationList,
  }) {
    throw UnimplementedError(
      'periodicallyShowWithDuration() has not been implemented.',
    );
  }

  /// 开启解锁触发的通知提醒。
  Future<void> startUnlockTriggeredNotifications({
    Duration interval = const Duration(minutes: 30),
    List<Map<String, Object?>>? notificationList,
  }) {
    throw UnimplementedError(
      'startUnlockTriggeredNotifications() has not been implemented.',
    );
  }
}
