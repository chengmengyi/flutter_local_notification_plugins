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
      didNotificationLaunchApp: map['didNotificationLaunchApp'] == true ||
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
  });

  final String title;
  final String subtitle;
  final String button;

  Map<String, Object?> toMap() {
    return <String, Object?>{
      'title': title,
      'subtitle': subtitle,
      'button': button,
    };
  }
}

class TimerOverlayClickEvent {
  const TimerOverlayClickEvent({
    required this.timestamp,
    this.layoutName,
    this.title,
    this.subtitle,
    this.button,
    this.appState,
  });

  final int timestamp;
  final String? layoutName;
  final String? title;
  final String? subtitle;
  final String? button;
  final String? appState;

  factory TimerOverlayClickEvent.fromMap(Map<dynamic, dynamic> map) {
    return TimerOverlayClickEvent(
      timestamp: (map['timestamp'] as num?)?.toInt() ?? 0,
      layoutName: map['layoutName']?.toString(),
      title: map['title']?.toString(),
      subtitle: map['subtitle']?.toString(),
      button: map['button']?.toString(),
      appState: map['appState']?.toString(),
    );
  }
}
