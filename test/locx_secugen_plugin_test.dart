import 'package:flutter_test/flutter_test.dart';
import 'package:locx_secugen_plugin/locx_secugen_plugin.dart';
import 'package:locx_secugen_plugin/locx_secugen_plugin_platform_interface.dart';
import 'package:locx_secugen_plugin/locx_secugen_plugin_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockLocxSecugenPluginPlatform
    with MockPlatformInterfaceMixin
    implements LocxSecugenPluginPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final LocxSecugenPluginPlatform initialPlatform = LocxSecugenPluginPlatform.instance;

  test('$MethodChannelLocxSecugenPlugin is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelLocxSecugenPlugin>());
  });

  test('getPlatformVersion', () async {
    LocxSecugenPlugin locxSecugenPlugin = LocxSecugenPlugin();
    MockLocxSecugenPluginPlatform fakePlatform = MockLocxSecugenPluginPlatform();
    LocxSecugenPluginPlatform.instance = fakePlatform;

    expect(await locxSecugenPlugin.getPlatformVersion(), '42');
  });
}
