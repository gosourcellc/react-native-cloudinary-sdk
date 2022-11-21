package com.reactnativecloudinarysdk;

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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@ReactModule(name = CloudinarySdkModule.NAME)
public class CloudinarySdkModule extends ReactContextBaseJavaModule {
    final Debouncer debouncer = new Debouncer();

    public static final String NAME = "CloudinarySdk";

    public final int MAX_IMAGE_DIMENSION = 1500;
    public final int DEBOUNCE_TIME = 500;

    private ReadableMap setupParams;

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
                        String signature = (String) options.get("signature");
                        int timestamp = (int) options.get("timestamp");
                        return new Signature(
                                signature,
                                setupParams.getString("api_key"),
                                timestamp
                        );
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
        ReadableMap uploadParams = params.getMap("params");
        boolean signed = uploadParams.getString("signature") != null;
        ReactContext reactContext = getReactApplicationContext();
        String filePath = params.getString("url").replaceFirst("^file://", "");
        Uri fileUri = null;
        if (filePath.startsWith("content://")) {
            fileUri = Uri.parse(filePath);
        }

        String presetName = uploadParams.getString("preset_name");
        MediaManager mediaManager = MediaManager.get();
        UploadRequest uploadRequest;
        if (fileUri != null) {
            uploadRequest = mediaManager.upload(fileUri);
        } else {
            uploadRequest = mediaManager.upload(filePath);
        }

        Map<String, Object> options = new HashMap(uploadParams.toHashMap());
        options.put("resource_type", getResourceType(params.getString("type")));
        if (uploadParams.hasKey("timestamp")) {
            options.put("timestamp", uploadParams.getInt("timestamp"));
        }
        options.put("chunk_size", 100 * 1024 * 1024);
        uploadRequest.options(options);
        if (!signed) {
            uploadRequest.unsigned(presetName);
        }

        String requestId = uploadRequest
                .maxFileSize(100 * 1024 * 1024) // max 100mb
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        // your code here
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                      debouncer.debounce(requestId, new Runnable() {
                        @Override public void run() {
                          // ...
                          Double progress = (double) bytes / totalBytes;

                          WritableMap eventBody = Arguments.createMap();
                          eventBody.putDouble("progress", progress);
                          eventBody.putString("uid", params.getString("uid"));

//            Log.d("CloudinarySdk", "progress: " + progress + " uid: " + params.getString("uid"));
                          sendEvent(reactContext, "progressChanged", eventBody);
                        }
                      }, DEBOUNCE_TIME, TimeUnit.MILLISECONDS);
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                       requests.remove(requestId);
                        // get secure url from result data
//          Log.d(NAME, "onSuccess: " + resultData.toString());
                        promise.resolve(Utils.mapToWritableMap(resultData));
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        requests.remove(requestId);
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
        requests.put(params.getString("uid"), requestId);
    }
}

