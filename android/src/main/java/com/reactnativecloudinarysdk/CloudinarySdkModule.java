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

@ReactModule(name = CloudinarySdkModule.NAME)
public class CloudinarySdkModule extends ReactContextBaseJavaModule {
    public static final String NAME = "CloudinarySdk";

    public final int MAX_IMAGE_DIMENSION = 1500;

    private ReadableMap setupParams;
    private ReadableMap uploadParams;

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
            setupParams = params;
            try {
                MediaManager.init(this.getReactApplicationContext(), new SignatureProvider() {
                    @Override
                    public Signature provideSignature(Map options) {
                        return new Signature(
                                uploadParams.getString("signature"),
                                setupParams.getString("api_key"),
                                uploadParams.getInt("timestamp")
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
        uploadParams = params;
        boolean signed = params.getString("signature") != null;
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
            int angle = Utils.getExifAngle(filePath);
            uploadRequest.preprocess(
                    ImagePreprocessChain.limitDimensionsChain(MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
                            .addStep(new DimensionsValidator(10, 10, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION))
                            .addStep(new Rotate(angle))
                            .saveWith(new BitmapEncoder(BitmapEncoder.Format.JPEG, 80))
            );
        }

        Map<String, Object> options = new HashMap<>();
        options.put("resource_type", getResourceType(params.getString("type")));
        if (signed) {
            options.put("public_id", params.getString("publicId"));
            options.put("folder", params.getString("folder"));
            options.put("context", params.getString("context"));
            options.put("upload_preset", params.getString("presetName"));
            uploadRequest.options(options);
        } else {
            uploadRequest.options(options);
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
                        promise.resolve(Utils.mapToWritableMap(resultData));
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
