import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/services.dart';

// Custom exception class to handle all fingerprint scanner errors
class FingerprintException implements Exception {
  final String code;
  final String message;
  
  FingerprintException(this.code, this.message);
  
  @override
  String toString() => 'FingerprintException: [$code] $message';
}

class FingerprintResult {
  final Uint8List template;
  final Uint8List image;
  final int quality;

  FingerprintResult(this.template, this.image, this.quality);
}

class LocxSecugenPlugin {
  static const MethodChannel _channel =
      MethodChannel('com.locx.secugen.plugin/fingerprintReader');

  // Plugin singleton instance
  static final LocxSecugenPlugin _instance = LocxSecugenPlugin._();
  static LocxSecugenPlugin get instance => _instance;

  // Private constructor
  LocxSecugenPlugin._();

  /// Initialize the fingerprint scanner device
  /// Returns true if initialization was successful
  Future<bool> initializeDevice() async {
    try {
      final bool result = await _channel.invokeMethod('initializeDevice');
      return result;
    } on PlatformException catch (e) {
      throw _mapException(e);
    }
  }

  /// Toggle the LED on the fingerprint scanner
  /// [enable] - true to turn on, false to turn off
  Future<void> toggleLed(bool enable) async {
    try {
      await _channel.invokeMethod('toggleLed', enable);
    } on PlatformException catch (e) {
      throw _mapException(e);
    }
  }

  /// Enable or disable smart capture mode
  /// [enable] - true to enable, false to disable
  Future<void> toggleSmartCapture(bool enable) async {
    try {
      await _channel.invokeMethod('toggleSmartCapture', enable);
    } on PlatformException catch (e) {
      throw _mapException(e);
    }
  }

  /// Set the brightness level of the fingerprint scanner
  /// [brightness] - brightness level (0-100)
  Future<void> setBrightness(int brightness) async {
    try {
      await _channel.invokeMethod('setBrightness', brightness);
    } on PlatformException catch (e) {
      throw _mapException(e);
    }
  }

  /// Capture a fingerprint
  /// [auto] - true to use auto-capture mode (requires smart capture to be enabled)
  /// Returns a FingerprintResult containing the template, image, and quality
  Future<FingerprintResult> captureFingerprint(bool auto) async {
    try {
      final List<dynamic> result = await _channel.invokeMethod('captureFingerprint', auto);
      
      // Parse result: [templateBytes, imageBytes, qualityBytes]
      final template = result[0] as Uint8List;
      final image = result[1] as Uint8List;
      final quality = _byteArrayToInt(result[2] as Uint8List);
      
      return FingerprintResult(template, image, quality);
    } on PlatformException catch (e) {
      throw _mapException(e);
    }
  }

  /// Capture a fingerprint with quality control
  /// [timeout] - timeout in milliseconds
  /// [minQuality] - minimum acceptable quality (0-100)
  /// [auto] - true to use auto-capture mode (requires smart capture to be enabled)
  Future<FingerprintResult> captureFingerprintWithQuality(
      int timeout, int minQuality, bool auto) async {
    try {
      final List<dynamic> result = await _channel.invokeMethod(
          'captureFingerprintWithQuality', [timeout, minQuality, auto]);
      
      // Parse result: [templateBytes, imageBytes, qualityBytes]
      final template = result[0] as Uint8List;
      final image = result[1] as Uint8List;
      final quality = _byteArrayToInt(result[2] as Uint8List);
      
      return FingerprintResult(template, image, quality);
    } on PlatformException catch (e) {
      throw _mapException(e);
    }
  }

  /// Verify two fingerprint templates
  /// Returns true if templates match
  Future<bool> verifyFingerprint(
      Uint8List template1, Uint8List template2) async {
    try {
      final bool result = await _channel.invokeMethod(
          'verifyFingerprint', [template1, template2]);
      return result;
    } on PlatformException catch (e) {
      throw _mapException(e);
    }
  }

  /// Get matching score between two templates
  /// Returns score (higher is better match)
  Future<int> getMatchingScore(
      Uint8List template1, Uint8List template2) async {
    try {
      final int result = await _channel.invokeMethod(
          'getMatchingScore', [template1, template2]);
      return result;
    } on PlatformException catch (e) {
      throw _mapException(e);
    }
  }

  // Helper method to convert byte array to int
  int _byteArrayToInt(Uint8List bytes) {
    ByteData byteData = ByteData.sublistView(bytes);
    return byteData.getInt32(0, Endian.big);
  }

  // Helper method to map platform exceptions to Dart exceptions
  Exception _mapException(PlatformException exception) {
    // Create a custom exception that extends Exception
    return FingerprintException(exception.code, exception.message ?? 'Unknown error');
  }
}
