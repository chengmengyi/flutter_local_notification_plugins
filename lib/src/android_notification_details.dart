abstract class NotificationStyleInformation {
  const NotificationStyleInformation();

  /// 转成原生层可识别的样式配置。
  Map<String, Object?> toMap();
}

class AndroidCustomNotificationLayout {
  const AndroidCustomNotificationLayout({
    required this.smallLayoutName,
    required this.bigLayoutName,
    this.actionText,
  });

  final String smallLayoutName;
  final String bigLayoutName;
  final String? actionText;

  /// 转成通知自定义布局配置。
  Map<String, Object?> toMap() => <String, Object?>{
    'smallLayoutName': smallLayoutName,
    'bigLayoutName': bigLayoutName,
    'actionText': actionText,
  };
}

class AndroidPersistentShortcutLayout {
  const AndroidPersistentShortcutLayout({
    this.smallLayoutName,
    this.bigLayoutName,
  });

  final String? smallLayoutName;
  final String? bigLayoutName;

  /// 转成常驻快捷通知布局配置。
  Map<String, Object?> toMap() => <String, Object?>{
    'smallLayoutName': smallLayoutName,
    'bigLayoutName': bigLayoutName,
  };
}

class BeautyStyleInformation extends NotificationStyleInformation {
  const BeautyStyleInformation({
    required this.title,
    required this.body,
    required this.image,
    required this.button,
    required this.appIcon,
  });

  final String title;
  final String body;
  final String image;
  final String button;
  final String appIcon;

  /// 转成美化通知样式配置。
  @override
  Map<String, Object?> toMap() => <String, Object?>{
    'style': 'beauty',
    'title': title,
    'body': body,
    'image': image,
    'button': button,
    'appIcon': appIcon,
  };
}

class MediaStyleInformation extends NotificationStyleInformation {
  const MediaStyleInformation({required this.image});

  final String image;

  /// 转成媒体通知样式配置。
  @override
  Map<String, Object?> toMap() => <String, Object?>{
    'style': 'media',
    'image': image,
  };
}

enum Priority { min, low, defaultPriority, high, max }

enum Importance { unspecified, none, min, low, defaultImportance, high, max }

class AndroidNotificationDetails {
  const AndroidNotificationDetails(
    this.channelId,
    this.channelName, {
    this.channelDescription,
    this.styleInformation,
    this.priority = Priority.high,
    this.importance = Importance.high,
    this.replaceExisting = false,
  });

  final String channelId;
  final String channelName;
  final String? channelDescription;
  final NotificationStyleInformation? styleInformation;
  final Priority priority;
  final Importance importance;
  final bool replaceExisting;

  /// 转成 Android 通知参数。
  Map<String, Object?> toMap() => <String, Object?>{
    'channelId': channelId,
    'channelName': channelName,
    'channelDescription': channelDescription,
    'priority': priority.index,
    'importance': importance.index,
    'replaceExisting': replaceExisting,
    'styleInformation': styleInformation?.toMap(),
  };
}
