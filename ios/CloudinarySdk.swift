import Cloudinary

@objc(CloudinarySdk)
class CloudinarySdk: NSObject {
    open var cloudinary: CLDCloudinary!
    
  @objc(setup:withResolver:withRejecter:)
  func setup(options: NSDictionary, resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
    NSLog("CloudinarySdk Options %@", options)
      if options["cloud_name"] != nil {
        cloudinary = CLDCloudinary(configuration: CLDConfiguration(options: options as! [String : AnyObject])!)
          resolve({})
      } else {
          reject("cloud_name_missed", "cloudName isn't set", nil)
      }
  }
}

