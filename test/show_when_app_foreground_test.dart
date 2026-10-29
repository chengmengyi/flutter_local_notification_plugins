import 'package:flutter/services.dart';
import 'package:flutter_local_notification_plugins/flutter_local_notification_plugins.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel('flutter_local_notification_plugins');
  final calls = <MethodCall>[];

  setUp(() {
    calls.clear();
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
          calls.add(call);
          return null;
        });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  test('show defaults showWhenAppForeground to false', () async {
    await FlutterLocalNotificationPlugins.instance.show(id: 1);

    expect(calls.single.method, 'show');
    expect(
      (calls.single.arguments
          as Map<Object?, Object?>)['showWhenAppForeground'],
      isFalse,
    );
  });

  test('show forwards showWhenAppForeground true', () async {
    await FlutterLocalNotificationPlugins.instance.show(
      id: 2,
      showWhenAppForeground: true,
    );

    expect(calls.single.method, 'show');
    expect(
      (calls.single.arguments
          as Map<Object?, Object?>)['showWhenAppForeground'],
      isTrue,
    );
  });
}
