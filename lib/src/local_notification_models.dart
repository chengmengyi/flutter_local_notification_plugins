enum LocalNotificationPayload {
  local('local'),
  lock('lock'),
  fcm('fcm'),
  media('media');

  const LocalNotificationPayload(this.value);

  final String value;

  static LocalNotificationPayload? fromValue(String? value) {
    for (final payload in values) {
      if (payload.value == value) {
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

class LocalNotificationEvent {
  const LocalNotificationEvent({
    required this.id,
    this.title,
    this.body,
    this.payload,
  });

  final int id;
  final String? title;
  final String? body;
  final String? payload;

  /// 从原生层返回的数据生成点击事件对象。
  factory LocalNotificationEvent.fromMap(Map<dynamic, dynamic> map) {
    return LocalNotificationEvent(
      id: (map['id'] as num?)?.toInt() ?? 0,
      title: map['title']?.toString(),
      body: map['body']?.toString(),
      payload: map['payload']?.toString(),
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
