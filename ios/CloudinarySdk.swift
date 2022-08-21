import Cloudinary

@objc(CloudinarySdk)
class CloudinarySdk: RCTEventEmitter {
    open var cloudinary: CLDCloudinary!
    var hasListeners = false
    
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
    
    @objc(setup:withResolver:withRejecter:)
    func setup(options: NSDictionary, resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
//        NSLog("CloudinarySdk Options %@", options)
        if options["cloud_name"] != nil {
            cloudinary = CLDCloudinary(configuration: CLDConfiguration(options: options as! [String : AnyObject])!)
            resolve({})
        } else {
            reject("cloud_name_missed", "cloudName isn't set", nil)
        }
    }
    
    @objc(upload:withResolver:withRejecter:)
    func upload(params: Dictionary<String, String>, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) -> Void {
        let resourceType = CLDUrlResourceType.image
        let url = params["url"] ?? ""
        let presetName = params["presetName"] ?? ""
        let uid = params["uid"] ?? ""
        if let url = URL(string: url) {
            CloudinaryHelper.upload(cloudinary: cloudinary, url: url, presetName: presetName, resourceType: resourceType)
                .progress({ progress in
                    if self.hasListeners {
                        self.sendEvent(withName: "progressChanged", body: ["uid": uid, "progress": Float(progress.fractionCompleted)])
                    }
                }).response({ response, error in
                    if (response != nil) {
                        resolve(response?.secureUrl)
//                        PersistenceHelper.resourceUploaded(localPath: name!, publicId: (response?.publicId)!)
                        // cleanup - once a file is uploaded we don't use the local copy
//                        try? FileManager.default.removeItem(at: url)
                    } else if (error != nil) {
                        reject(String(error!.code), error?.description, error)
                        //                        PersistenceHelper.resourceError(localPath: name!, code: (error?.code) != nil ? (error?.code)! : -1, description: (error?.userInfo["message"] as? String))
                    }
                })
        }
        
    }
}

