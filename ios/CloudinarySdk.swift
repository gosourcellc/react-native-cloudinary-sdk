import Cloudinary

enum CloudinaryErrorCodes: Int {
    case CancelUpload = 11001
}

@objc(CloudinarySdk)
class CloudinarySdk: RCTEventEmitter {
    open var cloudinary: CLDCloudinary!
    var hasListeners = false
    var resourceTypes = [
        "ImageUrlType": CLDUrlResourceType.image,
        "VideoUrlType": CLDUrlResourceType.video,
        "RawUrlType": CLDUrlResourceType.raw,
        "AutoUrlType": CLDUrlResourceType.auto
    ]
    var requests: [String: RequestControlData] = [:];

    @objc override static func requiresMainQueueSetup() -> Bool {
        return true
    }

    @objc override func supportedEvents() -> [String]? {
        return [
            "progressChanged"
        ]
    }

    @objc override func startObserving() {
        hasListeners = true
    }

    @objc override func stopObserving() {
        hasListeners = false
    }

    @objc override func constantsToExport() -> [AnyHashable: Any]! {
        return [
            "ImageUrlType": "ImageUrlType",
            "VideoUrlType": "VideoUrlType",
            "RawUrlType": "RawUrlType",
            "AutoUrlType": "AutoUrlType"
        ]
    }

    @objc(setup:withResolver:withRejecter:)
    func setup(options: NSDictionary, resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
        NSLog(options.description)
        if options["cloud_name"] != nil {
            cloudinary = CLDCloudinary(configuration: CLDConfiguration(options: options as! [String : AnyObject])!)
            resolve({})
        } else {
            reject("cloud_name_missed", "cloudName isn't set", nil)
        }
    }

    @objc(upload:withResolver:withRejecter:)
    func upload(dictWithParams: Dictionary<String, Any>, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) -> Void {
        let resourceType: CLDUrlResourceType
        let params = dictWithParams["params"] as! Dictionary<String, AnyObject>
        if let type = params["type"] {
            resourceType = resourceTypes[type as! String] ?? CLDUrlResourceType.auto
        } else {
            resourceType = CLDUrlResourceType.auto
        }
        let url = dictWithParams["url"] ?? ""
        let presetName = (params["preset_name"] as? String) ?? ""
        let uid = dictWithParams["uid"] as? String ?? ""

        NSLog(url as! String)
        if let url = URL(string: url as! String) {

            let uploadParams = CLDUploadRequestParams(params: params)
            uploadParams.setResourceType(resourceType)

            var signed: Bool = false
            if let signature = params["signature"] {
                let timestamp = params["timestamp"] as! NSNumber
                uploadParams.setSignature(CLDSignature(signature: signature as! String, timestamp: timestamp))
                signed = true
            }

            let request: CLDUploadRequest = CloudinaryHelper.upload(cloudinary: cloudinary, url: url, presetName: presetName as! String, params: uploadParams, resourceType: resourceType, signed: signed)
                .progress({ progress in
                    if self.hasListeners {
                        self.sendEvent(withName: "progressChanged", body: ["uid": uid, "progress": Float(progress.fractionCompleted)])
                    }
                }).response({ response, error in
                    self.requests[uid] = nil;
                    if (response != nil) {
                        resolve(response?.resultJson)
                    } else if (error != nil) {
                        reject(String(error!.code), error?.description, error)
                    }
                })
            let requestControlData = RequestControlData(request: request, resolve: resolve, reject: reject)
            requests[uid] = requestControlData;
        }

    }

    @objc(cancel:)
    func cancel(uid: String) -> Void {
        let requestControlData = self.requests[uid]
        requestControlData?.request.cancel()
        
        let error = NSError(domain: "cancel", code: CloudinaryErrorCodes.CancelUpload.rawValue)
        requestControlData?.reject("cancel", "Upload request canceled", error)
    }
}


