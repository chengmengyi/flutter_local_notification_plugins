import 'package:flutter_local_notification_plugins/flutter_local_notification_plugins.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('parses server JSON and maps seconds to native milliseconds', () {
    final config = NotificationConfigBean.fromJson(<String, dynamic>{
      'all_push_cd': 60,
      'local_interval': 3600,
      'action_interval': 600,
      'battery_changed': 1200,
      'push_max_time': 30,
      'push_max_broadcast': 20,
      'push_refresh_1': 2.5,
      'push_refresh_2': 30,
      'close_system_dialogs_delay_time': 3,
    });

    expect(config.toMap(), <String, Object?>{
      'allPushCdMilliseconds': 60000,
      'localIntervalMilliseconds': 3600000,
      'actionIntervalMilliseconds': 600000,
      'batteryChangedIntervalMilliseconds': 1200000,
      'pushMaxTime': 30,
      'pushMaxBroadcast': 20,
      'pushRefreshIntervalMilliseconds': 2500,
      'pushNotifyTotalCount': 30,
      'closeSystemDialogsDelayTimeMilliseconds': 3000,
    });
  });

  test('requires closeSystemDialogsDelayTime', () {
    expect(
      () => NotificationConfigBean.fromJson(<String, dynamic>{
        'all_push_cd': 60,
        'local_interval': 3600,
        'action_interval': 600,
        'battery_changed': 1200,
        'push_max_time': 30,
        'push_max_broadcast': 20,
        'push_refresh_1': 2,
        'push_refresh_2': 30,
      }),
      throwsFormatException,
    );
  });
}
