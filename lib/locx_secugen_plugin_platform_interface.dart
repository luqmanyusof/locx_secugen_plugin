import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'locx_secugen_plugin_method_channel.dart';

abstract class LocxSecugenPluginPlatform extends PlatformInterface {
  /// Constructs a LocxSecugenPluginPlatform.
  LocxSecugenPluginPlatform() : super(token: _token);

  static final Object _token = Object();

  static LocxSecugenPluginPlatform _instance = MethodChannelLocxSecugenPlugin();

  /// The default instance of [LocxSecugenPluginPlatform] to use.
  ///
  /// Defaults to [MethodChannelLocxSecugenPlugin].
  static LocxSecugenPluginPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [LocxSecugenPluginPlatform] when
  /// they register themselves.
  static set instance(LocxSecugenPluginPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
