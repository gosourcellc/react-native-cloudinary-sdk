package com.reactnativecloudinarysdk;

import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.UploadRequest;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.cloudinary.android.preprocess.BitmapEncoder;
import com.cloudinary.android.preprocess.DimensionsValidator;
import com.cloudinary.android.preprocess.ImagePreprocessChain;
import com.cloudinary.android.preprocess.Rotate;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ReactModule(name = CloudinarySdkModule.NAME)
public class CloudinarySdkModule extends ReactContextBaseJavaModule {
  public static final String NAME = "CloudinarySdk";

  public final int MAX_IMAGE_DIMENSION = 1500;

  public CloudinarySdkModule(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @Override
  public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();
    constants.put("ImageUrlType", "ImageUrlType");
    constants.put("VideoUrlType", "VideoUrlType");
    constants.put("RawUrlType", "RawUrlType");
    constants.put("AutoUrlType", "AutoUrlType");
    return constants;
  }

  public String getResourceType(String type) {
    if (type.equals("ImageUrlType")) {
      return "image";
    } else if (type.equals("VideoUrlType")) {
      return "video";
    } else if (type.equals("RawUrlType")) {
      return "raw";
    } else if (type.equals("AutoUrlType")) {
      return "auto";
    } else {
      return "auto";
    }
  }

  private void sendEvent(ReactContext reactContext,
                         String eventName,
                         @Nullable WritableMap params) {
    reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
      .emit(eventName, params);
  }

  @ReactMethod
  public void setup(ReadableMap params, Promise promise) {
    if (params.getString("cloud_name") != null) {
      try {
        MediaManager.init(this.getReactApplicationContext(), params.toHashMap());
        promise.resolve(true);
      } catch (Exception e) {
        promise.reject(e);
      }
    } else {
      promise.reject("CloudinarySdk", "Cloud name is required");
    }
  }

  public int getExifAngle(String filePath) {
    int angle = 0;
    try {
      ExifInterface exif = new ExifInterface(filePath);
      int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
//      Log.d("EXIF", "Exif: " + orientation);
      switch (orientation) {
        case ExifInterface.ORIENTATION_ROTATE_90:
          angle = 90;
          break;
        case ExifInterface.ORIENTATION_ROTATE_180:
          angle = 180;
          break;
        case ExifInterface.ORIENTATION_ROTATE_270:
          angle = 270;
          break;
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

    return angle;
  }

  @ReactMethod
  public void upload(ReadableMap params, Promise promise) {
    ReactContext reactContext = getReactApplicationContext();
    String filePath = params.getString("url").replaceFirst("^file://", "");
    Uri fileUri = null;
    if (filePath.startsWith("content://")) {
      fileUri = Uri.parse(filePath);
    }

    String presetName = params.getString("presetName");
    MediaManager mediaManager = MediaManager.get();
    UploadRequest uploadRequest;
    if (fileUri != null) {
      uploadRequest = mediaManager.upload(fileUri);
    } else {
      uploadRequest = mediaManager.upload(filePath);
    }

    if (params.getString("type").contentEquals("ImageUrlType")) {
      int angle = getExifAngle(filePath);
      uploadRequest.preprocess(
        ImagePreprocessChain.limitDimensionsChain(MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
          .addStep(new DimensionsValidator(10, 10, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION))
          .addStep(new Rotate(angle))
          .saveWith(new BitmapEncoder(BitmapEncoder.Format.JPEG, 80))
      );
    }

    String requestId = uploadRequest.unsigned(presetName)
      .maxFileSize(100 * 1024 * 1024) // max 100mb
      .option("resource_type", getResourceType(params.getString("type")))
      .callback(new UploadCallback() {
        @Override
        public void onStart(String requestId) {
          // your code here
        }

        @Override
        public void onProgress(String requestId, long bytes, long totalBytes) {
          Double progress = (double) bytes / totalBytes;

          WritableMap eventBody = Arguments.createMap();
          eventBody.putDouble("progress", progress);
          eventBody.putString("uid", params.getString("uid"));

//          Log.d("CloudinarySdk", "progress: " + progress + " uid: " + params.getString("uid"));
          sendEvent(reactContext, "progressChanged", eventBody);
        }

        @Override
        public void onSuccess(String requestId, Map resultData) {
          // get secure url from result data
//          Log.d(NAME, "onSuccess: " + resultData.toString());
          String secureUrl = resultData.get("secure_url").toString();
          promise.resolve(secureUrl);
        }

        @Override
        public void onError(String requestId, ErrorInfo error) {
          // your code here
//          Log.d(NAME, String.format("Code - %d", error.getCode()) + " " + error.getDescription());
          promise.reject(String.format("Code - %d", error.getCode()), error.getDescription());
        }

        @Override
        public void onReschedule(String requestId, ErrorInfo error) {
          // your code here
        }
      })
      .dispatch(reactContext);
  }
}
