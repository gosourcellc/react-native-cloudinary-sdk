package com.reactnativecloudinarysdk;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cloudinary.Cloudinary;
import com.cloudinary.ProgressCallback;
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
import com.cloudinary.utils.ObjectUtils;
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

import org.cloudinary.json.JSONObject;

import java.io.File;
import java.io.IOException;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@ReactModule(name = CloudinarySdkModule.NAME)
public class CloudinarySdkModule extends ReactContextBaseJavaModule {
    final Debouncer debouncer = new Debouncer();

    public static final String NAME = "CloudinarySdk";

    public final int MAX_IMAGE_DIMENSION = 1500;
    public final int DEBOUNCE_TIME = 500;
    public final int BUFFER_SIZE = 5 * 1024 * 1024;

    private ReadableMap setupParams;

    private Map<String, RequestControlData> requests = new HashMap<>();

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
        RequestControlData requestControlData = requests.get(uid);
        if (requestControlData != null) {
            MediaManager.get().cancelRequest(requestControlData.requestId);
            requestControlData.promise.reject("cancel", "Upload request canceled");
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
                                timestamp);
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
        ReactContext reactContext = getReactApplicationContext();
        String filePath = params.getString("url").replaceFirst("^file://", "");

        Map<String, Object> options = new HashMap(uploadParams.toHashMap());
        options.put("resource_type", getResourceType(params.getString("type")));
        if (uploadParams.hasKey("timestamp")) {
            options.put("timestamp", uploadParams.getInt("timestamp"));
        }
        options.put("chunk_size", BUFFER_SIZE);

        File file;
        try {
            file = new File(filePath);
        } catch (Exception e) {
            promise.reject("CloudinarySdk", e.getMessage());
            return;
        }

        Cloudinary cloudinary = MediaManager.get().getCloudinary();
        String uniqueUploadId = cloudinary.randomPublicId();
        try {
            RequestControlData rData = new RequestControlData(uniqueUploadId, promise);
            requests.put(params.getString("uid"), rData);

            Map resultData = cloudinary.uploader()
                    .uploadLarge(file, options, BUFFER_SIZE, 0, uniqueUploadId, new ProgressCallback() {
                        @Override
                        public void onProgress(long bytes, long totalBytes) {
                            debouncer.debounce(params.getString("uid"), new Runnable() {
                                @Override
                                public void run() {
                                    Double progress = (double) bytes / totalBytes;

                                    WritableMap eventBody = Arguments.createMap();
                                    eventBody.putDouble("progress", progress);
                                    eventBody.putString("uid", params.getString("uid"));

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
