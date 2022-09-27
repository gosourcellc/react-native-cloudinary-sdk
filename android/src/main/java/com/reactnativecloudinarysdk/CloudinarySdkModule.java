package com.reactnativecloudinarysdk;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cloudinary.Cloudinary;
import com.cloudinary.ProgressCallback;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.signed.Signature;
import com.cloudinary.android.signed.SignatureProvider;
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

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@ReactModule(name = CloudinarySdkModule.NAME)
public class CloudinarySdkModule extends ReactContextBaseJavaModule {
  final Debouncer debouncer = new Debouncer();

  public static final String NAME = "CloudinarySdk";

  public final int BUFFER_SIZE = 5 * 1024 * 1024;
  public final int DEBOUNCE_TIME = 100;

  private ReadableMap setupParams;
  private ReadableMap uploadParams;

  private Map<String, String> requests = new HashMap<>();

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
  public void addListener(String eventName) {

  }

  @ReactMethod
  public void removeListeners(Integer count) {

  }

  @ReactMethod
  public void cancel(String uid) {
    String requestId = requests.get(uid);
    if (requestId != null) {
      MediaManager.get().cancelRequest(requestId);
    }
  }

  @ReactMethod
  public void setup(ReadableMap params, Promise promise) {
    if (params.getString("cloud_name") != null) {
      setupParams = params;
      try {
        MediaManager.init(this.getReactApplicationContext(), new SignatureProvider() {
          @Override
          public Signature provideSignature(Map options) {
            return new Signature(
                uploadParams.getString("signature"),
                setupParams.getString("api_key"),
                uploadParams.getInt("timestamp"));
          }

          @Override
          public String getName() {
            return null;
          }
        }, setupParams.toHashMap());
        promise.resolve(true);
      } catch (Exception e) {
        Log.e("CloudinarySdk", e.getMessage());
        promise.reject("CloudinarySdk", e.getMessage());
      }
    } else {
      promise.reject("CloudinarySdk", "Cloud name is required");
    }
  }

  @ReactMethod
  public void upload(ReadableMap params, Promise promise) {
    uploadParams = params;
    boolean signed = params.getString("signature") != null;
    ReactContext reactContext = getReactApplicationContext();
    String urlString = params.getString("url");
    try {
      urlString = URLDecoder.decode(urlString, "UTF-8");
    } catch (Exception exception) {
      promise.resolve(exception.getMessage());
      return;
    }
    Uri uri = Uri.parse(urlString);
    Log.e(NAME, urlString);

    String path;
    try {
      path = Utils.getPath(getReactApplicationContext(), uri);
    } catch (Exception exception) {
      promise.resolve(exception.getMessage());
      return;
    }

    File file = new File(path);

    Map<String, Object> options = new HashMap<>();
    options.put("resource_type", getResourceType(params.getString("type")));
    options.put("chunk_size", BUFFER_SIZE);
    options.put("upload_preset", params.getString("presetName"));
    if (signed) {
      options.put("public_id", params.getString("publicId"));
      options.put("folder", params.getString("folder"));
      options.put("context", params.getString("context"));
    } else {
      options.put("unsigned", true);
    }

    Cloudinary cloudinary = MediaManager.get().getCloudinary();
    String uniqueUploadId = cloudinary.randomPublicId();
    try {
      requests.put(params.getString("uid"), uniqueUploadId);
      Map resultData = cloudinary.uploader()
          .uploadLarge(file, options, BUFFER_SIZE, 0, uniqueUploadId, new ProgressCallback() {
            @Override
            public void onProgress(long bytesUploaded, long totalBytes) {
              debouncer.debounce(params.getString("uid"), new Runnable() {
                @Override
                public void run() {
                  // ...
                  Double progress = (double) bytesUploaded / totalBytes;

                  WritableMap eventBody = Arguments.createMap();
                  eventBody.putDouble("progress", progress);
                  eventBody.putString("uid", params.getString("uid"));

                  // Log.d("CloudinarySdk", "progress: " + progress + " uid: " +
                  // params.getString("uid"));
                  sendEvent(reactContext, "progressChanged", eventBody);
                }
              }, DEBOUNCE_TIME, TimeUnit.MILLISECONDS);
            }
          });
      requests.remove(uniqueUploadId);
      promise.resolve(Utils.mapToWritableMap(resultData));
    } catch (IOException exception) {
      requests.remove(uniqueUploadId);
      promise.reject(exception.getMessage());
    }

  }
}
