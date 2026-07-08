# flutter_local_notification_plugins

`flutter_local_notification_plugins` 是一个本地通知插件，当前提供这些能力：

- 初始化通知通道
- 发送单次本地通知
- 按固定间隔循环发送通知
- 监听通知点击事件
- 获取“通过通知冷启动 App”的启动参数
- Android 悬浮处理进度层
- Android 常驻快捷入口通知
- Android 解锁触发通知
- Android FCM Topic 订阅和通知样式配置

## 平台支持

| 功能 | Android | iOS |
| --- | --- | --- |
| `initNotification` | 支持 | 支持 |
| `show` | 支持 | 支持 |
| `periodicallyShowLocalWithDuration` | 支持 | 当前未实现 |
| `periodicallyShowMediaWithDuration` | 支持 | 当前未实现 |
| 通知点击回调 | 支持 | 支持 |
| 冷启动通知参数 | 支持 | 支持 |
| 悬浮进度层相关 API | 支持 | 不支持 |
| 常驻快捷入口通知 | 支持 | 不支持 |
| 解锁触发通知 | 支持 | 不支持 |
| `subscribeToTopic` | 支持 | 当前未实现 |
| `moveAppToBack` | 支持 | 当前未实现 |

## 安装

在宿主工程的 `pubspec.yaml` 中添加依赖：

```yaml
dependencies:
  flutter_local_notification_plugins:
    path: /your/path/flutter_local_notification_plugins
```

然后执行：

```bash
flutter pub get
```

## 开始使用

### 1. 导入插件

```dart
import 'package:flutter_local_notification_plugins/flutter_local_notification_plugins.dart';
```

### 2. 获取单例

```dart
final notificationPlugin = FlutterLocalNotificationPlugins.instance;
```

### 3. 注册监听

建议在应用启动时尽早注册监听，这样 Android 通知展示、前台点击通知和点击悬浮层都能收到回调。

```dart
final notificationPlugin = FlutterLocalNotificationPlugins.instance;

notificationPlugin.setListeners(
  onNotificationDisplayed: (event) {
    debugPrint('通知已展示: id=${event.id}, payload=${event.payload}');
  },
  onNotificationClicked: (event) {
    debugPrint('通知被点击: id=${event.id}, payload=${event.payload}');
  },
  onProcessingOverlayClicked: (taskId) {
    debugPrint('悬浮层被点击: taskId=$taskId');
  },
);
```

### 4. 初始化通知

通常在 `main()` 或首页初始化时调用一次。

```dart
Future<void> initLocalNotification() async {
  final notificationPlugin = FlutterLocalNotificationPlugins.instance;

  final success = await notificationPlugin.initNotification(
    channelId: 'default_notification_channel',
    channelName: 'Notifications',
    channelDescription: 'App notifications',
  );

  debugPrint('初始化结果: $success');
}
```

如果你需要 Android 自定义通知布局，可以传 `customLayout`：

```dart
await notificationPlugin.initNotification(
  channelId: 'default_notification_channel',
  channelName: 'Notifications',
  channelDescription: 'App notifications',
  customLayout: const AndroidCustomNotificationLayout(
    smallLayoutName: 'fln_beauty_notify_content',
    bigLayoutName: 'fln_beauty_notify_big_content',
    actionText: '立即查看',
  ),
);
```

### 5. 处理通过通知启动 App 的场景

如果 App 是被通知点击后拉起的，可以在启动时读取启动参数：

```dart
Future<void> handleNotificationLaunch() async {
  final notificationPlugin = FlutterLocalNotificationPlugins.instance;
  final launchDetails =
      await notificationPlugin.getNotificationAppLaunchDetails();

  if (launchDetails.didNotificationLaunchApp) {
    debugPrint(
      '通过通知启动: payload=${launchDetails.notificationResponse?.payload}',
    );
  }
}
```

建议启动流程如下：

```dart
Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final notificationPlugin = FlutterLocalNotificationPlugins.instance;

  notificationPlugin.setListeners(
    onNotificationDisplayed: (event) {
      debugPrint('Android 通知已展示: ${event.payload}');
    },
    onNotificationClicked: (event) {
      debugPrint('前台点击通知: ${event.payload}');
    },
  );

  await notificationPlugin.initNotification(
    channelId: 'default_notification_channel',
    channelName: 'Notifications',
    channelDescription: 'App notifications',
  );

  final launchDetails =
      await notificationPlugin.getNotificationAppLaunchDetails();
  if (launchDetails.didNotificationLaunchApp) {
    debugPrint('冷启动通知参数: ${launchDetails.notificationResponse?.payload}');
  }

  runApp(const MyApp());
}
```

## 基础功能

### 发送一条普通通知

```dart
await notificationPlugin.show(
  id: 1,
  title: '新消息',
  body: '你有一条新的提醒',
  payload: LocalNotificationPayload.local,
  clickPayload: '{"page":"message","id":1}',
);
```

参数说明：

- `id`：通知业务 ID
- `title`：通知标题
- `body`：通知内容
- `payload`：通知附带信息
- `clickPayload`：点击通知时回传给 Flutter 的数据，不传时默认使用 `payload`

### 获取已展示通知数量

这个方法会按 `payload` 返回数量，并在读取后清空该 `payload` 的计数。

```dart
final count = await notificationPlugin.consumeDisplayedNotificationCount(
  payload: LocalNotificationPayload.local,
);
debugPrint('已展示通知数量: $count');
```

### 获取平台版本

```dart
final version = await notificationPlugin.getPlatformVersion();
debugPrint('平台版本: $version');
```

## 周期通知

### 固定间隔循环提醒

```dart
await notificationPlugin.periodicallyShowLocalWithDuration(
  id: 100,
  title: '喝水提醒',
  body: '记得补充水分',
  repeatDurationInterval: const Duration(minutes: 30),
);
```

### 使用候选通知内容随机展示

```dart
await notificationPlugin.periodicallyShowLocalWithDuration(
  id: 101,
  repeatDurationInterval: const Duration(minutes: 30),
  notificationList: const [
    LocalNotificationContent(
      title: '休息一下',
      body: '起来活动活动',
    ),
    LocalNotificationContent(
      title: '喝口水',
      body: '别忘了补水',
    ),
  ],
);
```

### Android 自定义通知参数

如果你要使用 Android 的优先级、重要级别或样式，可以传入 `AndroidNotificationDetails`：

```dart
const secret = 'debug_secret';
final mediaSessionClass =
    await notificationPlugin.encryptReflectionString(
  secret: secret,
  value: 'android.support.v4.media.session.MediaSessionCompat',
);
final mediaSessionTokenClass =
    await notificationPlugin.encryptReflectionString(
  secret: secret,
  value: 'android.support.v4.media.session.MediaSessionCompat\$Token',
);
final mediaSessionTag = await notificationPlugin.encryptReflectionString(
  secret: secret,
  value: 'FLNMediaSession',
);
final playbackStateClass =
    await notificationPlugin.encryptReflectionString(
  secret: secret,
  value: 'android.support.v4.media.session.PlaybackStateCompat',
);
final playbackStateBuilderClass =
    await notificationPlugin.encryptReflectionString(
  secret: secret,
  value: 'android.support.v4.media.session.PlaybackStateCompat\$Builder',
);
final mediaStyleClass =
    await notificationPlugin.encryptReflectionString(
  secret: secret,
  value: 'androidx.media.app.NotificationCompat\$MediaStyle',
);
final setFlagsMethod = await notificationPlugin.encryptReflectionString(
  secret: secret,
  value: 'setFlags',
);
final setActiveMethod = await notificationPlugin.encryptReflectionString(
  secret: secret,
  value: 'setActive',
);
final setPlaybackStateMethod =
    await notificationPlugin.encryptReflectionString(
  secret: secret,
  value: 'setPlaybackState',
);
final getSessionTokenMethod =
    await notificationPlugin.encryptReflectionString(
  secret: secret,
  value: 'getSessionToken',
);
final setStateMethod = await notificationPlugin.encryptReflectionString(
  secret: secret,
  value: 'setState',
);
final buildMethod = await notificationPlugin.encryptReflectionString(
  secret: secret,
  value: 'build',
);
final setMediaSessionMethod =
    await notificationPlugin.encryptReflectionString(
  secret: secret,
  value: 'setMediaSession',
);

await notificationPlugin.periodicallyShowMediaWithDuration(
  id: 102,
  repeatDurationInterval: const Duration(minutes: 15),
  notificationDetails: AndroidNotificationDetails(
    'media_channel',
    'Media Notifications',
    channelDescription: 'media notifications',
    styleInformation: const MediaStyleInformation(image: 'home'),
    priority: Priority.high,
    importance: Importance.high,
    replaceExisting: true,
  ),
  notificationList: const [
    LocalNotificationContent(
      title: '媒体通知',
      body: '这是一条媒体样式通知',
    ),
  ],
  reflectionConfig: MediaReflectionConfig(
    secret: secret,
    mediaSessionClass: mediaSessionClass,
    mediaSessionTokenClass: mediaSessionTokenClass,
    mediaSessionTag: mediaSessionTag,
    playbackStateClass: playbackStateClass,
    playbackStateBuilderClass: playbackStateBuilderClass,
    mediaStyleClass: mediaStyleClass,
    setFlagsMethod: setFlagsMethod,
    setActiveMethod: setActiveMethod,
    setPlaybackStateMethod: setPlaybackStateMethod,
    getSessionTokenMethod: getSessionTokenMethod,
    setStateMethod: setStateMethod,
    buildMethod: buildMethod,
    setMediaSessionMethod: setMediaSessionMethod,
  ),
);
```

## Android 扩展功能

### 1. 悬浮处理进度层

先检查并申请悬浮窗权限：

```dart
final hasPermission = await notificationPlugin.checkOverlayPermission();
if (!hasPermission) {
  await notificationPlugin.requestOverlayPermission();
}
```

显示悬浮层：

```dart
await notificationPlugin.showProcessingOverlay(
  taskId: 'task_001',
  title: '正在处理文件',
  progress: 0.2,
);
```

更新悬浮层：

```dart
await notificationPlugin.updateProcessingOverlay(
  taskId: 'task_001',
  title: '正在处理文件',
  progress: 0.8,
);
```

关闭悬浮层：

```dart
await notificationPlugin.closeProcessingOverlay();
```

查询当前是否仍然显示：

```dart
final isActive = await notificationPlugin.isProcessingOverlayActive();
debugPrint('悬浮层是否显示中: $isActive');
```

如果 App 是通过点击悬浮层拉起的，可以读取任务 ID：

```dart
final taskId =
    await notificationPlugin.consumeProcessingOverlayLaunchTaskId();
debugPrint('悬浮层启动任务 ID: $taskId');
```

### 2. 常驻快捷入口通知

```dart
await notificationPlugin.showPersistentShortcutNotification(
  homeText: '首页',
  mergeText: '合并',
  importText: '导入',
  convertText: '转换',
);
```

也可以自定义布局：

```dart
await notificationPlugin.showPersistentShortcutNotification(
  homeText: '首页',
  mergeText: '合并',
  importText: '导入',
  convertText: '转换',
  customLayout: const AndroidPersistentShortcutLayout(
    smallLayoutName: 'fln_shortcut_notification_small',
    bigLayoutName: 'fln_shortcut_notification_big',
  ),
);
```

### 3. 解锁触发通知

设备解锁后，按你设置的最小间隔随机展示通知。

```dart
await notificationPlugin.startUnlockTriggeredNotifications(
  interval: const Duration(minutes: 30),
  notificationList: const [
    LocalNotificationContent(
      title: '欢迎回来',
      body: '继续处理刚刚的任务吧',
    ),
    LocalNotificationContent(
      title: '新的提醒',
      body: '别忘了查看待办事项',
    ),
  ],
);
```

### 4. FCM Topic 订阅

这个方法主要用于 Android 端订阅 FCM Topic，并保存通知展示样式配置。

```dart
final success = await notificationPlugin.subscribeToTopic(
  'marketing_topic',
  channelId: 'focus_channel_fcm',
  channelName: 'focus_channel_name_fcm',
  channelDescription: 'FCM channel',
  priority: Priority.high,
  importance: Importance.high,
  style: 'beauty',
  beautyTitle: '活动通知',
  beautyBody: '点击查看最新活动',
  beautyImage: 'home',
  beautyButton: '立即查看',
  beautyAppIcon: 'ic_launcher',
);

debugPrint('订阅结果: $success');
```

如果你的宿主工程使用了 FCM，请确认已经完成 Firebase 初始化和 Android 端配置。

### 5. 将应用切到后台

```dart
final moved = await notificationPlugin.moveAppToBack();
debugPrint('是否成功切到后台: $moved');
```

## 完整示例

```dart
import 'package:flutter/material.dart';
import 'package:flutter_local_notification_plugins/flutter_local_notification_plugins.dart';

final notificationPlugin = FlutterLocalNotificationPlugins.instance;

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  notificationPlugin.setListeners(
    onNotificationDisplayed: (event) {
      debugPrint('展示通知 payload=${event.payload}');
    },
    onNotificationClicked: (event) {
      debugPrint('点击通知 payload=${event.payload}');
    },
    onProcessingOverlayClicked: (taskId) {
      debugPrint('点击悬浮层 taskId=$taskId');
    },
  );

  await notificationPlugin.initNotification(
    channelId: 'default_notification_channel',
    channelName: 'Notifications',
    channelDescription: 'App notifications',
  );

  final launchDetails =
      await notificationPlugin.getNotificationAppLaunchDetails();
  if (launchDetails.didNotificationLaunchApp) {
    debugPrint('通知启动参数: ${launchDetails.notificationResponse?.payload}');
  }

  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('Notification Demo')),
        body: Center(
          child: ElevatedButton(
            onPressed: () async {
              await notificationPlugin.show(
                id: 1,
                title: '测试通知',
                body: '这是一条本地通知',
                payload: LocalNotificationPayload.local,
              );
            },
            child: const Text('发送通知'),
          ),
        ),
      ),
    );
  }
}
```

## 注意事项

- 建议在应用启动早期调用 `setListeners()` 和 `initNotification()`
- Android 通知展示时，如果 Flutter 还活着会触发 `onNotificationDisplayed`；否则会累计到本地，可通过 `consumeDisplayedNotificationCount(payload: ...)` 读取
- `payload` 使用 `LocalNotificationPayload` 枚举，避免手写字符串导致展示计数读取不到
- `consumeDisplayedNotificationCount(payload: ...)` 和 `consumeProcessingOverlayLaunchTaskId()` 都是“读取后清空”
- Android 悬浮层功能需要系统悬浮窗权限
- iOS 当前没有暴露单独的通知权限申请方法，宿主工程需要自行确保通知权限已授权
- `subscribeToTopic()` 依赖 Firebase Messaging，使用前请先完成 Firebase 配置
