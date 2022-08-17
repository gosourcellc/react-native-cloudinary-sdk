import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-cloudinary-sdk' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';

const CloudinarySdk = NativeModules.CloudinarySdk
  ? NativeModules.CloudinarySdk
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export function setup(options: Record<string, any>): Promise<void> {
  return CloudinarySdk.setup(options);
}
