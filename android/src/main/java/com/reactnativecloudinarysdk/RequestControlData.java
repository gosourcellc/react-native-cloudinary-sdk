package com.reactnativecloudinarysdk;

import com.facebook.react.bridge.Promise;

public class RequestControlData {
   String requestId;
   Promise promise;

   public RequestControlData(String requestId, Promise promise) {
       this.promise = promise;
       this.requestId = requestId;
   }
}
