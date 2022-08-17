#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(CloudinarySdk, NSObject)

RCT_EXTERN_METHOD(setup:(NSDictionary *)options
                 withResolver:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

@end
