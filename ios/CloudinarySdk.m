#import "React/RCTEventEmitter.h"
#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(CloudinarySdk, RCTEventEmitter)

RCT_EXTERN_METHOD(setup:(NSDictionary *)options
                 withResolver:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

RCT_EXTERN_METHOD(upload:(NSDictionary *)params
                 withResolver:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

@end
