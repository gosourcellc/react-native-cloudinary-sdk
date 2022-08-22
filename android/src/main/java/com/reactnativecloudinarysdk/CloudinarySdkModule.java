package com.reactnativecloudinarysdk;

import androidx.annotation.NonNull;

import com.cloudinary.android.MediaManager;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.module.annotations.ReactModule;

@ReactModule(name = CloudinarySdkModule.NAME)
public class CloudinarySdkModule extends ReactContextBaseJavaModule {
    public static final String NAME = "CloudinarySdk";

    public CloudinarySdkModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    @NonNull
    public String getName() {
        return NAME;
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
}
