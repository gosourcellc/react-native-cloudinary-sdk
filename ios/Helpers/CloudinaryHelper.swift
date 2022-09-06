//
//  CloudinaryHelper.swift
//  SampleApp
//
//  Created by Nitzan Jaitman on 05/09/2017.
//  Copyright © 2017 Cloudinary. All rights reserved.
//

import UIKit
import Foundation
import Cloudinary

class CloudinaryHelper {
    static let defaultImageFormat = "png"
    
    static func upload(cloudinary: CLDCloudinary, url: URL, presetName: String, params: CLDUploadRequestParams, resourceType: CLDUrlResourceType, signed: Bool) -> CLDUploadRequest {
        if (resourceType == CLDUrlResourceType.image) {
            let chain = CLDImagePreprocessChain().addStep(CLDPreprocessHelpers.limit(width: 1500, height: 1500))
                .setEncoder(CLDPreprocessHelpers.customImageEncoder(format: EncodingFormat.JPEG, quality: 80))
            if (signed) {
                return cloudinary.createUploader().signedUploadLarge(url: url, params: params, preprocessChain: chain, chunkSize: 5 * 1024 * 1024)
                //                signedUploadLarge(url: url, uploadPreset: presetName, params: params, preprocessChain: chain, chunkSize: 5 * 1024 * 1024)
            }
            return cloudinary.createUploader().uploadLarge(url: url, uploadPreset: presetName, params: params, preprocessChain: chain, chunkSize: 5 * 1024 * 1024)
        } else {
            return cloudinary.createUploader().uploadLarge(url: url, uploadPreset: presetName, params: params, chunkSize: 5 * 1024 * 1024)
        }
    }
}

class EffectMetadata {
    let transformation: CLDTransformation!
    let name: String!
    let description: String!

    init(transformation: CLDTransformation!, name: String!, description: String!) {
        self.transformation = transformation
        self.name = name
        self.description = description
    }
}

// NOTE: The following extension is to demonstrate Cloudinary internals and is NOT provided for production use:
extension CLDTransformation: NSCopying {
    public func copy(with zone: NSZone? = nil) -> Any {
        let copy = CLDTransformation()
        let mirror = Mirror(reflecting: self)
        var lastParams: [String: String]?
        for child in mirror.children {
            if child.label == "currentTransformationParams" {
                lastParams = child.value as? [String: String]
            } else if child.label == "transformations" {
                if let transformations = child.value as? [[String: String]] {
                    for params in transformations {
                        copyParams(params: params, copy: copy)
                        copy.chain()
                    }
                }
            }
        }

        if let params = lastParams {
            copyParams(params: params, copy: copy)
        }

        return copy
    }

    fileprivate func copyParams(params: [String: String], copy: CLDTransformation) {
        for param in params {
            copy.setParam(param.key, value: param.value)
        }
    }
}
