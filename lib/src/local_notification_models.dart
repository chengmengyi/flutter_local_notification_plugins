enum LocalNotificationPayload {
  local('local'),
  lock('lock'),
  fcm('fcm'),
  media('media'),
  userPresent('USER_PRESENT'),
  actionPowerConnected('ACTION_POWER_CONNECTED'),
  actionPowerDisconnected('ACTION_POWER_DISCONNECTED'),
  batteryChanged('BATTERY_CHANGED'),
  screenOn('SCREEN_ON'),
  screenOff('SCREEN_OFF'),
  packageAdded('PACKAGE_ADDED'),
  packageRemoved('PACKAGE_REMOVED'),
  packageReplaced('PACKAGE_REPLACED'),
  closeSystemDialogs('CLOSE_SYSTEM_DIALOGS'),
  configurationChanged('CONFIGURATION_CHANGED');

  const LocalNotificationPayload(this.value);

  final String value;

  static LocalNotificationPayload? fromValue(String? value) {
    final normalizedValue = value?.trim();
    for (final payload in values) {
      if (payload.value == normalizedValue || payload.name == normalizedValue) {
        return payload;
      }
    }
    return null;
  }
}

class LocalNotificationContent {
  const LocalNotificationContent({this.title, this.body, this.payload});

  final String? title;
  final String? body;
  final LocalNotificationPayload? payload;

  /// 转成原生层可识别的通知内容。
  Map<String, Object?> toMap() => <String, Object?>{
    'title': title,
    'body': body,
    'payload': payload?.value ?? '',
  };

  /// 从原生层返回的数据生成通知内容对象。
  factory LocalNotificationContent.fromMap(Map<dynamic, dynamic> map) {
    return LocalNotificationContent(
      title: map['title']?.toString(),
      body: map['body']?.toString(),
      payload: LocalNotificationPayload.fromValue(map['payload']?.toString()),
    );
  }
}

class MediaReflectionConfig {
  const MediaReflectionConfig({
    required this.secret,
    required this.mediaSessionClass,
    required this.mediaSessionTokenClass,
    required this.mediaSessionTag,
    required this.playbackStateClass,
    required this.playbackStateBuilderClass,
    required this.mediaStyleClass,
    required this.setFlagsMethod,
    required this.setActiveMethod,
    required this.setPlaybackStateMethod,
    required this.getSessionTokenMethod,
    required this.setStateMethod,
    required this.buildMethod,
    required this.setMediaSessionMethod,
  });

  final String secret;
  final String mediaSessionClass;
  final String mediaSessionTokenClass;
  final String mediaSessionTag;
  final String playbackStateClass;
  final String playbackStateBuilderClass;
  final String mediaStyleClass;
  final String setFlagsMethod;
  final String setActiveMethod;
  final String setPlaybackStateMethod;
  final String getSessionTokenMethod;
  final String setStateMethod;
  final String buildMethod;
  final String setMediaSessionMethod;

  Map<String, Object?> toMap() => <String, Object?>{
    'secret': secret,
    'mediaSessionClass': mediaSessionClass,
    'mediaSessionTokenClass': mediaSessionTokenClass,
    'mediaSessionTag': mediaSessionTag,
    'playbackStateClass': playbackStateClass,
    'playbackStateBuilderClass': playbackStateBuilderClass,
    'mediaStyleClass': mediaStyleClass,
    'setFlagsMethod': setFlagsMethod,
    'setActiveMethod': setActiveMethod,
    'setPlaybackStateMethod': setPlaybackStateMethod,
    'getSessionTokenMethod': getSessionTokenMethod,
    'setStateMethod': setStateMethod,
    'buildMethod': buildMethod,
    'setMediaSessionMethod': setMediaSessionMethod,
  };
}

class TimerOverlayReflectionConfig {
  const TimerOverlayReflectionConfig({
    required this.secret,
    required this.settingsClass,
    required this.canDrawOverlaysMethod,
    required this.contextGetSystemServiceMethod,
    required this.windowServiceName,
    required this.windowManagerLayoutParamsClass,
    required this.viewGroupLayoutParamsClass,
    required this.windowManagerClass,
    required this.addViewMethod,
    required this.removeViewMethod,
    required this.gravityField,
    required this.xField,
    required this.yField,
  });

  final String secret;
  final String settingsClass;
  final String canDrawOverlaysMethod;
  final String contextGetSystemServiceMethod;
  final String windowServiceName;
  final String windowManagerLayoutParamsClass;
  final String viewGroupLayoutParamsClass;
  final String windowManagerClass;
  final String addViewMethod;
  final String removeViewMethod;
  final String gravityField;
  final String xField;
  final String yField;

  Map<String, Object?> toMap() => <String, Object?>{
    'secret': secret,
    'settingsClass': settingsClass,
    'canDrawOverlaysMethod': canDrawOverlaysMethod,
    'contextGetSystemServiceMethod': contextGetSystemServiceMethod,
    'windowServiceName': windowServiceName,
    'windowManagerLayoutParamsClass': windowManagerLayoutParamsClass,
    'viewGroupLayoutParamsClass': viewGroupLayoutParamsClass,
    'windowManagerClass': windowManagerClass,
    'addViewMethod': addViewMethod,
    'removeViewMethod': removeViewMethod,
    'gravityField': gravityField,
    'xField': xField,
    'yField': yField,
  };
}

class ProcessingOverlayReflectionConfig {
  const ProcessingOverlayReflectionConfig({
    required this.secret,
    required this.settingsClass,
    required this.canDrawOverlaysMethod,
    required this.contextGetSystemServiceMethod,
    required this.windowServiceName,
    required this.windowManagerLayoutParamsClass,
    required this.viewGroupLayoutParamsClass,
    required this.windowManagerClass,
    required this.addViewMethod,
    required this.removeViewMethod,
    required this.updateViewLayoutMethod,
    required this.gravityField,
    required this.xField,
    required this.yField,
  });

  final String secret;
  final String settingsClass;
  final String canDrawOverlaysMethod;
  final String contextGetSystemServiceMethod;
  final String windowServiceName;
  final String windowManagerLayoutParamsClass;
  final String viewGroupLayoutParamsClass;
  final String windowManagerClass;
  final String addViewMethod;
  final String removeViewMethod;
  final String updateViewLayoutMethod;
  final String gravityField;
  final String xField;
  final String yField;

  Map<String, Object?> toMap() => <String, Object?>{
    'secret': secret,
    'settingsClass': settingsClass,
    'canDrawOverlaysMethod': canDrawOverlaysMethod,
    'contextGetSystemServiceMethod': contextGetSystemServiceMethod,
    'windowServiceName': windowServiceName,
    'windowManagerLayoutParamsClass': windowManagerLayoutParamsClass,
    'viewGroupLayoutParamsClass': viewGroupLayoutParamsClass,
    'windowManagerClass': windowManagerClass,
    'addViewMethod': addViewMethod,
    'removeViewMethod': removeViewMethod,
    'updateViewLayoutMethod': updateViewLayoutMethod,
    'gravityField': gravityField,
    'xField': xField,
    'yField': yField,
  };
}

class BroadcastNotificationConfig {
  const BroadcastNotificationConfig({
    required this.payload,
    required this.interval,
  });

  final LocalNotificationPayload payload;
  final Duration interval;

  Map<String, Object?> toMap() => <String, Object?>{
    'payload': payload.value,
    'intervalMilliseconds': interval.inMilliseconds,
  };
}

class LocalNotificationEvent {
  const LocalNotificationEvent({
    required this.id,
    this.title,
    this.body,
    this.payload,
    this.payloadType,
  });

  final int id;
  final String? title;
  final String? body;
  final String? payload;
  final LocalNotificationPayload? payloadType;

  /// 从原生层返回的数据生成点击事件对象。
  factory LocalNotificationEvent.fromMap(Map<dynamic, dynamic> map) {
    final payload = map['payload']?.toString();
    return LocalNotificationEvent(
      id: (map['id'] as num?)?.toInt() ?? 0,
      title: map['title']?.toString(),
      body: map['body']?.toString(),
      payload: payload,
      payloadType: LocalNotificationPayload.fromValue(
        map['payloadType']?.toString() ?? payload,
      ),
    );
  }
}

class LocalNotificationAppLaunchDetails {
  const LocalNotificationAppLaunchDetails({
    required this.didNotificationLaunchApp,
    this.notificationResponse,
  });

  final bool didNotificationLaunchApp;
  final LocalNotificationEvent? notificationResponse;

  /// 从原生层返回的数据生成启动详情对象。
  factory LocalNotificationAppLaunchDetails.fromMap(Map<dynamic, dynamic> map) {
    return LocalNotificationAppLaunchDetails(
      didNotificationLaunchApp:
          map['didNotificationLaunchApp'] == true ||
          map['didNotificationLaunchApp']?.toString() == 'true',
      notificationResponse: map['notificationResponse'] is Map
          ? LocalNotificationEvent.fromMap(
              map['notificationResponse'] as Map<dynamic, dynamic>,
            )
          : null,
    );
  }
}

class TimerOverlayContent {
  const TimerOverlayContent({
    required this.title,
    required this.subtitle,
    required this.button,
    this.button2,
  });

  final String title;
  final String subtitle;
  final String button;
  final String? button2;

  Map<String, Object?> toMap() {
    return <String, Object?>{
      'title': title,
      'subtitle': subtitle,
      'button': button,
      'button2': button2,
    };
  }
}

class TimerOverlayClickEvent {
  const TimerOverlayClickEvent({
    required this.timestamp,
    this.clickType,
    this.layoutName,
    this.title,
    this.subtitle,
    this.button,
    this.button2,
    this.appState,
  });

  final int timestamp;
  final String? clickType;
  final String? layoutName;
  final String? title;
  final String? subtitle;
  final String? button;
  final String? button2;
  final String? appState;

  factory TimerOverlayClickEvent.fromMap(Map<dynamic, dynamic> map) {
    return TimerOverlayClickEvent(
      timestamp: (map['timestamp'] as num?)?.toInt() ?? 0,
      clickType: map['clickType']?.toString(),
      layoutName: map['layoutName']?.toString(),
      title: map['title']?.toString(),
      subtitle: map['subtitle']?.toString(),
      button: map['button']?.toString(),
      button2: map['button2']?.toString(),
      appState: map['appState']?.toString(),
    );
  }
}
