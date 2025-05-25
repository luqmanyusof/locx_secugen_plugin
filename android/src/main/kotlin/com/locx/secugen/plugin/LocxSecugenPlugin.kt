package com.locx.secugen.plugin

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.annotation.NonNull
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.ArrayList

import SecuGen.FDxSDKPro.JSGFPLib
import SecuGen.FDxSDKPro.SGAutoOnEventNotifier
import SecuGen.FDxSDKPro.SGFDxConstant
import SecuGen.FDxSDKPro.SGFDxDeviceName
import SecuGen.FDxSDKPro.SGFDxErrorCode
import SecuGen.FDxSDKPro.SGFDxSecurityLevel
import SecuGen.FDxSDKPro.SGFDxTemplateFormat
import SecuGen.FDxSDKPro.SGFingerInfo
import SecuGen.FDxSDKPro.SGFingerPresentEvent
import SecuGen.FDxSDKPro.SGImpressionType
import SecuGen.FDxSDKPro.SGDeviceInfoParam

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
// Remove dependency on specific Flutter lifecycle implementation
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** LocxSecugenPlugin */
class LocxSecugenPlugin: FlutterPlugin, MethodCallHandler, ActivityAware, SGFingerPresentEvent, DefaultLifecycleObserver {
  private val TAG = "LocxSecugenPlugin"

  // Method names for channel calls
  private val METHOD_INIT = "initializeDevice"
  private val METHOD_TOGGLE_LED = "toggleLed"
  private val METHOD_TOGGLE_SMART_CAPTURE = "toggleSmartCapture"
  private val METHOD_SET_BRIGHTNESS = "setBrightness"
  private val METHOD_CAPTURE_FINGERPRINT = "captureFingerprint"
  private val METHOD_CAPTURE_FINGERPRINT_WITH_QUALITY = "captureFingerprintWithQuality"
  private val METHOD_VERIFY_FINGERPRINT = "verifyFingerprint"
  private val METHOD_GET_MATCHING_SCORE = "getMatchingScore"

  // Error codes
  private val ERROR_NOT_SUPPORTED = "NOT_SUPPORTED"
  private val ERROR_INITIALIZATION_FAILED = "INITIALIZATION_FAILED"
  private val ERROR_SENSOR_NOT_FOUND = "SENSOR_NOT_FOUND"
  private val ERROR_SMART_CAPTURE_ENABLED = "SMART_CAPTURE_ENABLED"
  private val ERROR_OUT_OF_RANGE = "OUT_OF_RANGE"
  private val ERROR_NO_FINGERPRINT = "NO_FINGERPRINT"
  private val ERROR_TEMPLATE_INITIALIZE_FAILED = "TEMPLATE_INITIALIZE_FAILED"
  private val ERROR_TEMPLATE_MATCHING_FAILED = "TEMPLATE_MATCHING_FAILED"

  private var channel : MethodChannel? = null
  private var activity: Activity? = null
  private var context: Context? = null
  private var lifecycle: Lifecycle? = null

  // Secugen SDK variables
  private var sgfplib: JSGFPLib? = null
  private var isInitialized = false
  private var smartCaptureEnabled = false
  private var autoOnEventNotifier: SGAutoOnEventNotifier? = null
  private var deviceInfo: SGDeviceInfoParam? = null

  // Device properties
  private var deviceID = 0
  private var imgWidth = 0
  private var imgHeight = 0

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "com.locx.secugen.plugin/fingerprintReader")
    channel?.setMethodCallHandler(this)
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      METHOD_INIT -> {
        initializeDevice(result)
      }
      METHOD_TOGGLE_LED -> {
        val enable = call.arguments as Boolean
        enableLed(enable, result)
      }
      METHOD_TOGGLE_SMART_CAPTURE -> {
        val enable = call.arguments as Boolean
        enableSmartCapture(enable, result)
      }
      METHOD_SET_BRIGHTNESS -> {
        val brightness = call.arguments as Int
        setBrightness(brightness, result)
      }
      METHOD_CAPTURE_FINGERPRINT -> {
        val auto = call.arguments as Boolean
        captureFingerprint(auto, result)
      }
      METHOD_CAPTURE_FINGERPRINT_WITH_QUALITY -> {
        val args = call.arguments as List<*>
        val timeout = args[0] as Int
        val quality = args[1] as Int
        val auto = args[2] as Boolean
        captureFingerprintWithQuality(timeout, quality, auto, result)
      }
      METHOD_VERIFY_FINGERPRINT -> {
        val args = call.arguments as List<*>
        val template1 = args[0] as ByteArray
        val template2 = args[1] as ByteArray
        verifyFingerprint(template1, template2, result)
      }
      METHOD_GET_MATCHING_SCORE -> {
        val args = call.arguments as List<*>
        val template1 = args[0] as ByteArray
        val template2 = args[1] as ByteArray
        getMatchingScore(template1, template2, result)
      }
      else -> {
        result.notImplemented()
      }
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel?.setMethodCallHandler(null)
    channel = null
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
    context = binding.activity.applicationContext
    // Get activity lifecycle directly using ProcessLifecycleOwner
    lifecycle = androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle
    lifecycle?.addObserver(this)
  }

  override fun onDetachedFromActivityForConfigChanges() {
    onDetachedFromActivity()
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    onAttachedToActivity(binding)
  }

  override fun onDetachedFromActivity() {
    activity = null
    context = null
    lifecycle?.removeObserver(this)
    lifecycle = null
  }

  override fun onDestroy(owner: LifecycleOwner) {
    Log.d(TAG, "onDestroy")
    if (sgfplib != null) {
      sgfplib?.CloseDevice()
      isInitialized = false
    }
  }

  override fun SGFingerPresentCallback() {
    // Handle finger present event (used with auto-on feature)
    Log.d(TAG, "Finger present event")
  }

  private fun initializeDevice(result: Result) {
    try {
      // Make sure we have a valid context
      val contextToUse = context ?: activity?.applicationContext
      if (contextToUse == null) {
        result.error(ERROR_INITIALIZATION_FAILED, "Context not available", null)
        return
      }
      
      val usbManager = contextToUse.getSystemService(Context.USB_SERVICE) as UsbManager?
      if (usbManager == null) {
        result.error(ERROR_NOT_SUPPORTED, "USB Manager not available", null)
        return
      }
      
      // Initialize the Secugen fingerprint library
      sgfplib = JSGFPLib(contextToUse, usbManager)
      autoOnEventNotifier = SGAutoOnEventNotifier(sgfplib, this)
      
      // Set up permission intent
      val ACTION_USB_PERMISSION = "com.locx.secugen.plugin.USB_PERMISSION"
      val mPermissionIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        PendingIntent.getBroadcast(
          contextToUse, 
          0, 
          Intent(ACTION_USB_PERMISSION), 
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
      } else {
        PendingIntent.getBroadcast(contextToUse, 0, Intent(ACTION_USB_PERMISSION), 0)
      }
      
      // Reset state variables
      isInitialized = false
      smartCaptureEnabled = false
      
      // Initialize the device
      val error = sgfplib?.Init(SGFDxDeviceName.SG_DEV_AUTO)
      
      if (error != SGFDxErrorCode.SGFDX_ERROR_NONE) {
        val errorCode: String
        val errorMsg: String
        
        if (error == SGFDxErrorCode.SGFDX_ERROR_DEVICE_NOT_FOUND) {
          errorCode = ERROR_NOT_SUPPORTED
          errorMsg = "The attached fingerprint device is not supported!"
        } else {
          errorCode = ERROR_INITIALIZATION_FAILED
          errorMsg = "Fingerprint device initialization failed!"
        }
        
        Log.e(TAG, errorMsg)
        result.error(errorCode, errorMsg, null)
        return
      }
      
      // Check if device is available
      val usbDevice = sgfplib?.GetUsbDevice()
      
      if (usbDevice == null) {
        val errorMsg = "SecuGen fingerprint sensor not found!"
        Log.e(TAG, errorMsg)
        result.error(ERROR_SENSOR_NOT_FOUND, errorMsg, null)
        return
      }
      
      // Check for USB permission
      val hasPermission = sgfplib?.GetUsbManager()?.hasPermission(usbDevice) ?: false
      
      if (!hasPermission) {
        Log.e(TAG, "Requesting USB Permission")
        sgfplib?.GetUsbManager()?.requestPermission(usbDevice, mPermissionIntent)
        result.error(ERROR_SENSOR_NOT_FOUND, "USB permission required", null)
        return
      }
      
      Log.e(TAG, "Opening SecuGen Device")
      
      // Open the device
      val openResult = sgfplib?.OpenDevice(0)
      
      if (openResult == SGFDxErrorCode.SGFDX_ERROR_NONE) {
        isInitialized = true
        
        // Get device info
        deviceInfo = SGDeviceInfoParam()
        sgfplib?.GetDeviceInfo(deviceInfo)
        
        // Get dimensions
        imgWidth = deviceInfo?.imageWidth?.toInt() ?: 0
        imgHeight = deviceInfo?.imageHeight?.toInt() ?: 0
        
        // Set template format
        sgfplib?.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_ISO19794)
        
        // Enable smart capture
        sgfplib?.WriteData(SGFDxConstant.WRITEDATA_COMMAND_ENABLE_SMART_CAPTURE, 1.toByte())
        smartCaptureEnabled = true
        
        Log.e(TAG, "SecuGen Device Ready")
        result.success(true)
        return
      }
      
      Log.e(TAG, "Waiting for USB Permission")
      result.error(ERROR_SENSOR_NOT_FOUND, "Failed to open device", null)
    } catch (e: Exception) {
      Log.e(TAG, "Exception during initialization: ${e.message}")
      result.error(ERROR_INITIALIZATION_FAILED, "Exception: ${e.message}", null)
    }
  }

  private fun enableLed(enable: Boolean, result: Result) {
    if (!isInitialized) {
      result.error(ERROR_INITIALIZATION_FAILED, "Device not initialized", null)
      return
    }

    sgfplib?.SetLedOn(enable)
    result.success(null)
  }

  private fun enableSmartCapture(enable: Boolean, result: Result) {
    if (!isInitialized) {
      result.error(ERROR_INITIALIZATION_FAILED, "Device not initialized", null)
      return
    }

    smartCaptureEnabled = enable
    result.success(null)
  }

  private fun setBrightness(brightness: Int, result: Result) {
    if (!isInitialized) {
      result.error(ERROR_INITIALIZATION_FAILED, "Device not initialized", null)
      return
    }

    sgfplib?.SetBrightness(brightness)
    result.success(null)
  }

  private fun captureFingerprint(auto: Boolean, result: Result) {
    if (!isInitialized) {
      result.error(ERROR_INITIALIZATION_FAILED, "Device not initialized", null)
      return
    }

    if (auto && !smartCaptureEnabled) {
      result.error(ERROR_SMART_CAPTURE_ENABLED, "Smart capture is not enabled", null)
      return
    }

    // Capture the fingerprint
    val imageBuffer = ByteArray(imgWidth * imgHeight)
    val ret = sgfplib?.GetImageEx(imageBuffer, 10000, 0)

    if (ret != SGFDxErrorCode.SGFDX_ERROR_NONE) {
      result.error(ERROR_NO_FINGERPRINT, "Failed to capture fingerprint", null)
      return
    }

    // Convert to bitmap and extract raw template using the approach from SecugenFlutter
    val bits = ByteArray(imageBuffer.size * 4)
    for (i in 0 until imageBuffer.size) {
      // Set RGB values (3 bytes)
      bits[i * 4] = imageBuffer[i]      // R
      bits[i * 4 + 1] = imageBuffer[i]  // G
      bits[i * 4 + 2] = imageBuffer[i]  // B
      // Set alpha (4th byte) to 0xFF (fully opaque)
      bits[i * 4 + 3] = 0xFF.toByte()
    }
    
    val bitmap = Bitmap.createBitmap(imgWidth, imgHeight, Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(bits))

    // Get quality
    val quality = IntArray(1)
    sgfplib?.GetImageQuality(imgWidth.toLong(), imgHeight.toLong(), imageBuffer, quality)

    // Convert bitmap to byte array
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    val bitmapBytes = stream.toByteArray()

    // Create template
    val maxSize = IntArray(1)
    sgfplib?.GetMaxTemplateSize(maxSize)
    val templateBuffer = ByteArray(maxSize[0])
    
    val fingerInfo = SGFingerInfo()
    fingerInfo.FingerNumber = 0 // Using 0 instead of SG_FINGPOS_NULL
    fingerInfo.ImageQuality = quality[0]
    fingerInfo.ImpressionType = SGImpressionType.SG_IMPTYPE_LP
    fingerInfo.ViewNumber = 1

    val templateRet = sgfplib?.CreateTemplate(fingerInfo, imageBuffer, templateBuffer)
    if (templateRet != SGFDxErrorCode.SGFDX_ERROR_NONE) {
      result.error(ERROR_TEMPLATE_INITIALIZE_FAILED, "Failed to create template", null)
      return
    }

    // Return results
    val resultList = ArrayList<ByteArray>()
    resultList.add(templateBuffer)
    resultList.add(bitmapBytes)
    resultList.add(ByteBuffer.allocate(4).putInt(quality[0]).array())

    result.success(resultList)
  }

  private fun captureFingerprintWithQuality(timeout: Int, minQuality: Int, autoCapture: Boolean, result: Result) {
    if (!isInitialized) {
      result.error(ERROR_INITIALIZATION_FAILED, "Device not initialized", null)
      return
    }

    if (autoCapture && !smartCaptureEnabled) {
      result.error(ERROR_SMART_CAPTURE_ENABLED, "Smart capture is not enabled", null)
      return
    }

    val imageBuffer = ByteArray(imgWidth * imgHeight)
    val ret = sgfplib?.GetImageEx(imageBuffer, timeout.toLong(), 0)

    if (ret != SGFDxErrorCode.SGFDX_ERROR_NONE) {
      result.error(ERROR_NO_FINGERPRINT, "Failed to capture fingerprint", null)
      return
    }

    // Get quality
    val quality = IntArray(1)
    sgfplib?.GetImageQuality(imgWidth.toLong(), imgHeight.toLong(), imageBuffer, quality)

    if (quality[0] < minQuality) {
      result.error(ERROR_NO_FINGERPRINT, "Fingerprint quality too low", null)
      return
    }

    // Convert to bitmap
    val rgbData = IntArray(imgWidth * imgHeight)
    for (i in 0 until imgWidth * imgHeight) {
      rgbData[i] = (imageBuffer[i].toInt() and 0xFF) * 0x00010101
    }

    val bitmap = Bitmap.createBitmap(rgbData, imgWidth, imgHeight, Bitmap.Config.ARGB_8888)

    // Convert bitmap to byte array
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    val bitmapBytes = stream.toByteArray()

    // Create template
    val maxSize = IntArray(1)
    sgfplib?.GetMaxTemplateSize(maxSize)
    val templateBuffer = ByteArray(maxSize[0])
    
    val fingerInfo = SGFingerInfo()
    fingerInfo.FingerNumber = 0 // Using 0 instead of SG_FINGPOS_NULL
    fingerInfo.ImageQuality = quality[0]
    fingerInfo.ImpressionType = SGImpressionType.SG_IMPTYPE_LP
    fingerInfo.ViewNumber = 1

    val templateRet = sgfplib?.CreateTemplate(fingerInfo, imageBuffer, templateBuffer)
    if (templateRet != SGFDxErrorCode.SGFDX_ERROR_NONE) {
      result.error(ERROR_TEMPLATE_INITIALIZE_FAILED, "Failed to create template", null)
      return
    }

    // Return results
    val resultList = ArrayList<ByteArray>()
    resultList.add(templateBuffer)
    resultList.add(bitmapBytes)
    resultList.add(ByteBuffer.allocate(4).putInt(quality[0]).array())

    result.success(resultList)
  }

  private fun verifyFingerprint(template1: ByteArray, template2: ByteArray, result: Result) {
    if (!isInitialized) {
      result.error(ERROR_INITIALIZATION_FAILED, "Device not initialized", null)
      return
    }

    val matched = BooleanArray(1)
    val ret = sgfplib?.MatchTemplate(template1, template2, SGFDxSecurityLevel.SL_NORMAL, matched)

    if (ret != SGFDxErrorCode.SGFDX_ERROR_NONE) {
      result.error(ERROR_TEMPLATE_MATCHING_FAILED, "Failed to match templates", null)
      return
    }

    result.success(matched[0])
  }

  private fun getMatchingScore(template1: ByteArray, template2: ByteArray, result: Result) {
    if (!isInitialized) {
      result.error(ERROR_INITIALIZATION_FAILED, "Device not initialized", null)
      return
    }

    val score = IntArray(1)
    val ret = sgfplib?.GetMatchingScore(template1, template2, score)

    if (ret != SGFDxErrorCode.SGFDX_ERROR_NONE) {
      result.error(ERROR_TEMPLATE_MATCHING_FAILED, "Failed to get matching score", null)
      return
    }

    result.success(score[0])
  }
}
