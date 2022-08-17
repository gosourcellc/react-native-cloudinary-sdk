import Cloudinary

@objc(CloudinarySdk)
class CloudinarySdk: NSObject {
    open var cloudinary: CLDCloudinary!
    
  @objc(multiply:withB:withResolver:withRejecter:)
  func multiply(a: Float, b: Float, resolve:RCTPromiseResolveBlock,reject:RCTPromiseRejectBlock) -> Void {
    resolve(a*b)
  }

  @objc(setup:withResolver:withRejecter:)
  func setup(options: NSDictionary, resolve:RCTPromiseResolveBlock, reject:RCTPromiseRejectBlock) -> Void {
    NSLog("CloudinarySdk Options %@", options)
      if let cloudName = options["cloudName"] {
        cloudinary = CLDCloudinary(configuration: CLDConfiguration(options: ["cloud_name": cloudName as AnyObject])!)
          resolve({})
      } else {
          reject("cloud_name_missed", "cloudName isn't set", nil)
      }
  }
}

