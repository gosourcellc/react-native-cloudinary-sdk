//
//  RequestControlData.swift
//  react-native-cloudinary-sdk
//
//  Created by Dmitriy Portenko on 01.12.2022.
//

import Cloudinary

struct RequestControlData {
    var request: CLDUploadRequest;
    var resolve: RCTPromiseResolveBlock;
    var reject: RCTPromiseRejectBlock;
    
    init(request: CLDUploadRequest, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        self.request = request
        self.resolve = resolve
        self.reject = reject
    }
}
