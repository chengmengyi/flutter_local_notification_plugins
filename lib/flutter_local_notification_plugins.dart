import 'flutter_local_notification_plugins_platform_interface.dart';
import 'flutter_local_notification_plugins_method_channel.dart';
import 'src/android_notification_details.dart';
import 'src/device_manufacturer.dart';
import 'src/local_notification_models.dart';

export 'src/android_notification_details.dart';
export 'src/device_manufacturer.dart';
export 'src/local_notification_models.dart';

class FlutterLocalNotificationPlugins {
  static const Duration _defaultLocalNotificationInterval = Duration(
    minutes: 30,
  );
  static const Duration _defaultWorkManagerInterval = Duration(minutes: 60);

  static final FlutterLocalNotificationPlugins instance =
      FlutterLocalNotificationPlugins();

  /// 注册通知展示、通知点击和悬浮层点击回调。
  void setListeners({
    void Function(LocalNotificationEvent event)? onNotificationDisplayed,
    void Function(LocalNotificationEvent event)? onNotificationClicked,
    void Function()? onProcessingOverlayClicked,
    void Function(TimerOverlayClickEvent event)? onTimerOverlayClicked,
  }) {
    final platform = FlutterLocalNotificationPluginsPlatform.instance;
    if (platform is MethodChannelFlutterLocalNotificationPlugins) {
      if (onNotificationDisplayed != null) {
        platform.onNotificationDisplayed = onNotificationDisplayed;
      }
      if (onNotificationClicked != null) {
        platform.onNotificationClicked = onNotificationClicked;
      }
      if (onProcessingOverlayClicked != null) {
        platform.onProcessingOverlayClicked = onProcessingOverlayClicked;
      }
      if (onTimerOverlayClicked != null) {
        platform.onTimerOverlayClicked = onTimerOverlayClicked;
      }
    }
  }

  /// 获取当前平台版本信息。
  Future<String?> getPlatformVersion() {
    return FlutterLocalNotificationPluginsPlatform.instance
        .getPlatformVersion();
  }

  /// 加密反射字符串。
  ///
  /// 传入 [secret] 和待加密的 [value]，返回可直接填入各类 ReflectionConfig
  /// 对应字段的密文。Android 原生层使用反射时会用同一个 [secret] 解密。
  Future<String> encryptReflectionString({
    required String secret,
    required String value,
  }) {
    return FlutterLocalNotificationPluginsPlatform.instance
        .encryptReflectionString(secret: secret, value: value);
  }

  /// 解密反射字符串。
  ///
  /// 传入加密时使用的 [secret] 和 [encryptReflectionString] 返回的 [value]，
  /// 返回原始明文。非加密格式的 [value] 会原样返回。
  Future<String> decryptReflectionString({
    required String secret,
    required String value,
  }) {
    return FlutterLocalNotificationPluginsPlatform.instance
        .decryptReflectionString(secret: secret, value: value);
  }

  /// 按 payload 取出并清空已展示通知数量。
  Future<int> consumeDisplayedNotificationCount({
    required LocalNotificationPayload payload,
  }) {
    return FlutterLocalNotificationPluginsPlatform.instance
        .consumeDisplayedNotificationCount(payload: payload.value);
  }

  /// 配置通知展示后的 Android 原生埋点上报。
  Future<void> configureNativePushReporting({
    required bool enabled,
    required String url,
    required Map<String, String> headers,
    required Map<String, Object?> payloadTemplate,
    required String distinctIdKey,
    required String logIdKey,
    required String clientTsKey,
    required String notificationSourceKey,
    required String packageKey,
  }) {
    return FlutterLocalNotificationPluginsPlatform.instance
        .configureNativePushReporting(
          enabled: enabled,
          url: url,
          headers: headers,
          payloadTemplate: payloadTemplate,
          distinctIdKey: distinctIdKey,
          logIdKey: logIdKey,
          clientTsKey: clientTsKey,
          notificationSourceKey: notificationSourceKey,
          packageKey: packageKey,
        );
  }

  /// 检查悬浮层权限是否已开启。
  Future<bool> checkOverlayPermission() {
    return FlutterLocalNotificationPluginsPlatform.instance
        .checkOverlayPermission();
  }

  /// 请求系统悬浮层权限。
  Future<bool> requestOverlayPermission({
    required String title,
    required String desc,
    required String overlayPermissionGuideLayout,
  }) {
    return FlutterLocalNotificationPluginsPlatform.instance
        .requestOverlayPermission(
          title: title,
          desc: desc,
          overlayPermissionGuideLayout: overlayPermissionGuideLayout,
        );
  }

  /// 显示处理中的悬浮进度层。
  ///
  /// [reflectionConfig] 中除 [ProcessingOverlayReflectionConfig.secret] 外，
  /// 其他字段都建议先通过 [encryptReflectionString] 加密后再传入。
  ///
  /// 参数和值的对应关系如下：
  ///
  /// - secret: 加密和 Android 原生层解密使用的密钥，原文传入，不需要加密。
  /// - settingsClass: android.provider.Settings
  /// - canDrawOverlaysMethod: canDrawOverlays
  /// - contextGetSystemServiceMethod: getSystemService
  /// - windowServiceName: window
  /// - windowManagerLayoutParamsClass: android.view.WindowManager$LayoutParams
  /// - viewGroupLayoutParamsClass: android.view.ViewGroup$LayoutParams
  /// - windowManagerClass: android.view.WindowManager
  /// - addViewMethod: addView
  /// - removeViewMethod: removeView
  /// - updateViewLayoutMethod: updateViewLayout
  /// - gravityField: gravity
  /// - xField: x
  /// - yField: y
  Future<void> showProcessingOverlay({
    required String taskId,
    required String title,
    required double progress,
    required ProcessingOverlayReflectionConfig reflectionConfig,
  }) {
    return FlutterLocalNotificationPluginsPlatform.instance
        .showProcessingOverlay(
          taskId: taskId,
          title: title,
          progress: progress,
          reflectionConfig: reflectionConfig,
        );
  }

  /// 更新处理中的悬浮进度层。
  Future<void> updateProcessingOverlay({
    required String taskId,
    required String title,
    required double progress,
  }) {
    return FlutterLocalNotificationPluginsPlatform.instance
        .updateProcessingOverlay(
          taskId: taskId,
          title: title,
          progress: progress,
        );
  }

  /// 关闭处理中的悬浮进度层。
  Future<void> closeProcessingOverlay() {
    return FlutterLocalNotificationPluginsPlatform.instance
        .closeProcessingOverlay();
  }

  /// 关闭当前显示的定时悬浮窗；保留配置和下一次定时任务。
  Future<void> closeTimerOverlay() {
    return FlutterLocalNotificationPluginsPlatform.instance.closeTimerOverlay();
  }

  /// 设置定时悬浮窗信息；配置后 Android 每 20 分钟在应用非前台时展示一次。
  ///
  /// [reflectionConfig] 里的字符串建议都通过 [encryptReflectionString] 生成密文后传入，
  /// 并且 [TimerOverlayReflectionConfig.secret] 要和加密时使用的 secret 一致。
  ///
  /// 需要加密的明文和参数对应关系：
  /// - settingsClass: android.provider.Settings
  /// - canDrawOverlaysMethod: canDrawOverlays
  /// - contextGetSystemServiceMethod: getSystemService
  /// - windowServiceName: window
  /// - windowManagerLayoutParamsClass: android.view.WindowManager$LayoutParams
  /// - viewGroupLayoutParamsClass: android.view.ViewGroup$LayoutParams
  /// - windowManagerClass: android.view.WindowManager
  /// - addViewMethod: addView
  /// - removeViewMethod: removeView
  /// - gravityField: gravity
  /// - xField: x
  /// - yField: y
  Future<void> setTimerOverlayInfo({
    required String layoutName,
    required List<TimerOverlayContent> contentList,
    String? layoutName2,
    List<TimerOverlayContent>? contentList2,
    required String continueReadingStr,
    required String lastPdfSubtitleTemplate,
    required String lastPdfButtonText,
    required TimerOverlayReflectionConfig reflectionConfig,
    Duration timerInterval = const Duration(minutes: 20),
  }) {
    return FlutterLocalNotificationPluginsPlatform.instance.setTimerOverlayInfo(
      layoutName: layoutName,
      contentList: contentList.map((value) => value.toMap()).toList(),
      layoutName2: layoutName2,
      contentList2: contentList2?.map((value) => value.toMap()).toList(),
      continueReadingStr: continueReadingStr,
      lastPdfSubtitleTemplate: lastPdfSubtitleTemplate,
      lastPdfButtonText: lastPdfButtonText,
      reflectionConfig: reflectionConfig.toMap(),
      timerInterval: timerInterval,
    );
  }

  /// 更新定时悬浮窗间隔和每天最大展示次数；oneDayMaxCount 为空时不限制。
  Future<void> updateTimerOverlayInfo({
    required Duration timerInterval,
    int? oneDayMaxCount,
    int? cdTime,
  }) {
    return FlutterLocalNotificationPluginsPlatform.instance
        .updateTimerOverlayInfo(
          timerInterval: timerInterval,
          oneDayMaxCount: oneDayMaxCount,
          cdTime: cdTime,
        );
  }

  /// 更新是否在通知触发前显示媒体通知。
  Future<void> updateShowMediaTag({required bool showMedia}) {
    return FlutterLocalNotificationPluginsPlatform.instance.updateShowMediaTag(
      showMedia: showMedia,
    );
  }

  /// 暂停定时悬浮窗；保留配置但取消下一次定时展示。
  Future<void> pauseTimerOverlay() {
    return FlutterLocalNotificationPluginsPlatform.instance.pauseTimerOverlay();
  }

  /// 恢复已配置的定时悬浮窗。
  Future<void> resumeTimerOverlay() {
    return FlutterLocalNotificationPluginsPlatform.instance
        .resumeTimerOverlay();
  }

  /// 记录定时悬浮窗可使用的最近 PDF 阅读位置。
  Future<void> setTimerOverlayLastPdfInfo({
    required String title,
    required int pageNumber,
  }) {
    return FlutterLocalNotificationPluginsPlatform.instance
        .setTimerOverlayLastPdfInfo(title: title, pageNumber: pageNumber);
  }

  /// 配置系统相册新增图片/截图时展示的本地通知文案。
  Future<void> setGalleryImageNotificationInfo({required String title}) {
    return FlutterLocalNotificationPluginsPlatform.instance
        .setGalleryImageNotificationInfo(title: title);
  }

  /// 取出并清空定时悬浮窗点击事件，用于冷启动/后台恢复后的打点兜底。
  Future<TimerOverlayClickEvent?> consumeTimerOverlayClickEvent() async {
    final result = await FlutterLocalNotificationPluginsPlatform.instance
        .consumeTimerOverlayClickEvent();
    if (result == null || result.isEmpty) {
      return null;
    }
    return TimerOverlayClickEvent.fromMap(result);
  }

  /// 判断悬浮进度层是否仍在显示。
  Future<bool> isProcessingOverlayActive() {
    return FlutterLocalNotificationPluginsPlatform.instance
        .isProcessingOverlayActive();
  }

  /// 取出并清空通过悬浮层拉起应用时的任务 ID。
  Future<String?> consumeProcessingOverlayLaunchTaskId() {
    return FlutterLocalNotificationPluginsPlatform.instance
        .consumeProcessingOverlayLaunchTaskId();
  }

  /// 将应用切到后台。
  Future<bool> moveAppToBack() {
    return FlutterLocalNotificationPluginsPlatform.instance.moveAppToBack();
  }

  /// 配置需要屏蔽通知的手机品牌列表。
  Future<void> configureBlockedManufacturers({
    required List<DeviceManufacturer> manufacturers,
  }) {
    return FlutterLocalNotificationPluginsPlatform.instance
        .configureBlockedManufacturers(
          manufacturers: manufacturers.map((value) => value.name).toList(),
        );
  }

  /// 判断当前手机是否为三星。
  Future<bool> isSamsungDevice() {
    return FlutterLocalNotificationPluginsPlatform.instance.isSamsungDevice();
  }

  /// 判断当前手机语言或地区是否为韩国。
  Future<bool> isKoreanLocale() {
    return FlutterLocalNotificationPluginsPlatform.instance.isKoreanLocale();
  }

  /// 配置 Android 的 WorkManager 循环间隔。
  Future<void> configureAndroidWorkManager({
    Duration interval = _defaultWorkManagerInterval,
  }) {
    return FlutterLocalNotificationPluginsPlatform.instance
        .configureAndroidWorkManager(interval: interval);
  }

  /// 获取通知点击拉起应用的启动信息。
  Future<LocalNotificationAppLaunchDetails>
  getNotificationAppLaunchDetails() async {
    final result = await FlutterLocalNotificationPluginsPlatform.instance
        .getNotificationAppLaunchDetails();
    return LocalNotificationAppLaunchDetails.fromMap(result);
  }

  /// 订阅 FCM 主题并保存通知样式配置。
  Future<bool> subscribeToTopic(
    String topic, {
    String channelId = 'focus_channel_fcm',
    String channelName = 'focus_channel_name_fcm',
    String? channelDescription,
    Priority priority = Priority.high,
    Importance importance = Importance.high,
    String? style,
    String? beautyTitle,
    String? beautyBody,
    String? beautyImage,
    String? beautyButton,
    String? beautyAppIcon,
  }) {
    return FlutterLocalNotificationPluginsPlatform.instance.subscribeToTopic(
      topic: topic,
      channelId: channelId,
      channelName: channelName,
      channelDescription: channelDescription,
      priority: priority.index,
      importance: importance.index,
      style: style,
      beautyTitle: beautyTitle,
      beautyBody: beautyBody,
      beautyImage: beautyImage,
      beautyButton: beautyButton,
      beautyAppIcon: beautyAppIcon,
    );
  }

  /// 初始化通知通道和自定义布局配置。
  Future<bool> initNotification({
    String channelId = 'default_notification_channel',
    String channelName = 'Notifications',
    String? channelDescription,
    String? icon,
    bool showMedia = true,
    AndroidCustomNotificationLayout? customLayout,
  }) {
    return FlutterLocalNotificationPluginsPlatform.instance.initNotification(
      channelId: channelId,
      channelName: channelName,
      channelDescription: channelDescription,
      icon: icon,
      showMedia: showMedia,
      customLayout: customLayout?.toMap(),
    );
  }

  /// 显示常驻快捷入口通知。
  Future<void> showPersistentShortcutNotification({
    required String homeText,
    required String mergeText,
    required String importText,
    required String convertText,
    String homeIcon = 'home',
    String mergeIcon = 'merge',
    String importIcon = 'shortcut_import',
    String convertIcon = 'convert',
    AndroidPersistentShortcutLayout? customLayout,
  }) {
    return FlutterLocalNotificationPluginsPlatform.instance
        .showPersistentShortcutNotification(
          homeText: homeText,
          mergeText: mergeText,
          importText: importText,
          convertText: convertText,
          homeIcon: homeIcon,
          mergeIcon: mergeIcon,
          importIcon: importIcon,
          convertIcon: convertIcon,
          customLayout: customLayout?.toMap(),
        );
  }

  /// 立即显示一条本地通知。
  Future<void> show({
    required int id,
    String? title,
    String? body,
    LocalNotificationPayload? payload,
    String? clickPayload,
    String? mediaBackgroundImageName,
    AndroidNotificationDetails? notificationDetails,
  }) {
    return FlutterLocalNotificationPluginsPlatform.instance.show(
      id: id,
      title: title,
      body: body,
      payload: payload?.value,
      clickPayload: clickPayload,
      mediaBackgroundImageName: mediaBackgroundImageName,
      notificationDetails: notificationDetails?.toMap(),
    );
  }

  /// 按固定时间间隔循环展示本地通知。
  Future<void> periodicallyShowLocalWithDuration({
    required int id,
    String? title,
    String? body,
    Duration repeatDurationInterval = _defaultLocalNotificationInterval,
    AndroidNotificationDetails? notificationDetails,
    List<LocalNotificationContent>? notificationList,
  }) {
    return FlutterLocalNotificationPluginsPlatform.instance
        .periodicallyShowLocalWithDuration(
          id: id,
          title: title,
          body: body,
          repeatDurationInterval: repeatDurationInterval,
          notificationDetails: notificationDetails?.toMap(),
          notificationList: notificationList
              ?.map((value) => value.toMap())
              .toList(growable: false),
        );
  }

  /// 按固定时间间隔循环展示媒体通知。
  ///
  /// [reflectionConfig] 里的字符串建议都通过 [encryptReflectionString] 生成密文后传入，
  /// 并且 [MediaReflectionConfig.secret] 要和加密时使用的 secret 一致。
  ///
  /// 需要加密的明文和参数对应关系：
  /// - mediaSessionClass: android.support.v4.media.session.MediaSessionCompat
  /// - mediaSessionTokenClass: android.support.v4.media.session.MediaSessionCompat$Token
  /// - mediaSessionTag: FLNMediaSession
  /// - playbackStateClass: android.support.v4.media.session.PlaybackStateCompat
  /// - playbackStateBuilderClass: android.support.v4.media.session.PlaybackStateCompat$Builder
  /// - mediaStyleClass: androidx.media.app.NotificationCompat$MediaStyle
  /// - setFlagsMethod: setFlags
  /// - setActiveMethod: setActive
  /// - setPlaybackStateMethod: setPlaybackState
  /// - getSessionTokenMethod: getSessionToken
  /// - setStateMethod: setState
  /// - buildMethod: build
  /// - setMediaSessionMethod: setMediaSession
  Future<void> periodicallyShowMediaWithDuration({
    required int id,
    String? title,
    String? body,
    Duration repeatDurationInterval = _defaultLocalNotificationInterval,
    String? mediaBackgroundImageName,
    AndroidNotificationDetails? notificationDetails,
    required List<LocalNotificationContent> notificationList,
    required MediaReflectionConfig reflectionConfig,
  }) {
    return FlutterLocalNotificationPluginsPlatform.instance
        .periodicallyShowMediaWithDuration(
          id: id,
          title: title,
          body: body,
          repeatDurationInterval: repeatDurationInterval,
          mediaBackgroundImageName: mediaBackgroundImageName,
          notificationDetails: notificationDetails?.toMap(),
          notificationList: notificationList
              .map((value) => value.toMap())
              .toList(growable: false),
          reflectionConfig: reflectionConfig.toMap(),
        );
  }

  /// 注册广播触发的通知提醒。
  Future<void> registerBroadcastNotifications({
    List<LocalNotificationContent>? notificationList,
    required List<BroadcastNotificationConfig> configList,
  }) {
    return FlutterLocalNotificationPluginsPlatform.instance
        .registerBroadcastNotifications(
          notificationList: notificationList
              ?.map((value) => value.toMap())
              .toList(growable: false),
          configList: configList
              .map((value) => value.toMap())
              .toList(growable: false),
        );
  }
}
