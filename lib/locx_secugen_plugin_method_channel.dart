import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'locx_secugen_plugin_platform_interface.dart';

/// An implementation of [LocxSecugenPluginPlatform] that uses method channels.
class MethodChannelLocxSecugenPlugin extends LocxSecugenPluginPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('locx_secugen_plugin');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
