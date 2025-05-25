# Locx SecuGen Plugin

A Flutter plugin for SecuGen fingerprint scanner integration. This plugin provides a native interface between Flutter applications and SecuGen USB fingerprint scanners on Android devices.

## Features

### Fingerprint Scanner Functionality
- Device initialization and connection management
- Fingerprint capture with quality assessment
- LED control for user feedback
- Smart capture mode for improved usability
- Template and image extraction for fingerprint data
- Quality control and adjustments

### API Features
- Clean Dart interface with typed results
- Comprehensive error handling with custom exceptions
- Asynchronous operations for non-blocking UI
- Configurable capture parameters
- Support for fingerprint matching and verification

### Technical Implementation
- Native Java/Kotlin code for Android platform
- SecuGen SDK integration for reliable device communication
- Flutter method channel for cross-platform bridge
- Structured result handling with binary data support
- Proper resource management

## Dependencies and Requirements

### Core Platform Requirements
- **Flutter SDK**: ^3.7.2
- **Dart SDK**: ^3.7.2
- **Java**: OpenJDK 21 (build 21.0.5+-12932927-b750.29) from Android Studio
- **Gradle**: 8.10.2 (from gradle-wrapper.properties)
- **Android compileSdk**: 34
- **Android minSdk**: 21 (Android 5.0+)
- **NDK Version**: 27.0.12077973 (required for SecuGen native libraries)

### Flutter Dependencies
- **flutter**: sdk
- **plugin_platform_interface**: ^2.0.2

### Native Dependencies
- **SecuGen SDK**: Integrated with native JNI libraries for fingerprint processing
- **USB Host API**: For device communication

### Build Tool Configurations
- **Kotlin Version**: 1.9.0
- **Gradle Plugin Version**: 8.3.0

## Installation

Add this dependency to your application's `pubspec.yaml` file:

```yaml
dependencies:
  locx_secugen_plugin:
    path: ../locx_secugen_plugin  # Local path for development
```

## Setup and Usage

### Prerequisites
- Flutter SDK (version 3.7.2 or higher)
- Android Studio with the bundled JDK
- SecuGen fingerprint scanner device
- Android device with USB host capability (Android 5.0+)

### USB Permissions

Ensure your Android app has the necessary USB permissions by adding the following to your `AndroidManifest.xml`:

```xml
<uses-feature android:name="android.hardware.usb.host" />
<uses-permission android:name="android.permission.USB_PERMISSION" />
```

### Basic Usage

```dart
import 'package:locx_secugen_plugin/locx_secugen_plugin.dart';

// Get the plugin instance
final fingerprintPlugin = LocxSecugenPlugin.instance;

// Initialize the device
final bool initialized = await fingerprintPlugin.initializeDevice();
if (initialized) {
  // Device is ready to use
}

// Capture a fingerprint
try {
  // Turn on LED to signal user
  await fingerprintPlugin.toggleLed(true);
  
  // Capture the fingerprint
  final FingerprintResult result = await fingerprintPlugin.captureFingerprint(false);
  
  // Process the result
  final Uint8List template = result.template;
  final Uint8List image = result.image;
  final int quality = result.quality;
  
  // Turn off LED
  await fingerprintPlugin.toggleLed(false);
} catch (e) {
  // Handle errors
  print('Fingerprint capture error: $e');
}
```

## API Reference

### Main Methods

- **`initializeDevice()`**: Initializes the fingerprint scanner
- **`toggleLed(bool enable)`**: Controls the scanner LED
- **`toggleSmartCapture(bool enable)`**: Enables/disables smart capture mode
- **`setBrightness(int brightness)`**: Sets scanner brightness (0-100)
- **`captureFingerprint(bool auto)`**: Captures a fingerprint
- **`captureFingerprintWithQuality(int timeout, int minQuality, bool auto)`**: Captures with quality control
- **`matchFingerprints(Uint8List template1, Uint8List template2)`**: Compares two fingerprint templates

### Response Objects

- **`FingerprintResult`**: Contains template, image, and quality data
- **`FingerprintException`**: Custom exception for error handling

## Troubleshooting

### Common Issues
- **Device Not Detected**: Ensure proper USB permissions and connections
- **Capture Failures**: Check lighting conditions and finger placement
- **Performance Issues**: Adjust brightness and smart capture settings
- **Integration Problems**: Verify proper project dependencies and platform configurations

## Device Compatibility

### Android Requirements
- Devices with USB host capability (Android 5.0+)
- OTG (On-The-Go) support
- Required permissions: USB host, storage

### Supported SecuGen Fingerprint Scanners

#### U20 Series
- U20
- U20-A
- U20-AP

#### U10 Series
- U10
- U10-A
- U10-AP

#### Hamster Series
- Hamster Pro
- Hamster Pro 20
- Hamster Pro Duo
- Hamster Pro Duo/CL

#### Other Compatible Models
- U30
- U30-A
- U30-AP
- Hamster IV
- Hamster Pro+

### Notes on Device Compatibility
- Devices must be connected via USB OTG cable or adapter
- Some features may vary by scanner model
- Driver optimization specific to U20 and U10 series

## License

This project is proprietary and confidential. All rights reserved.
