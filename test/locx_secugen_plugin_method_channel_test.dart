import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:locx_secugen_plugin/locx_secugen_plugin_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelLocxSecugenPlugin platform = MethodChannelLocxSecugenPlugin();
  const MethodChannel channel = MethodChannel('locx_secugen_plugin');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
        return '42';
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });
}
