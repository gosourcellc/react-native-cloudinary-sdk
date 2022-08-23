import {
  EmitterSubscription,
  NativeEventEmitter,
  NativeModules,
  Platform,
} from 'react-native';

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

const emitter = new NativeEventEmitter(CloudinarySdk);

export enum CloudinaryEvent {
  UPLOAD_PROGRESS_EVENT = 'progressChanged',
}

const { ImageUrlType, VideoUrlType, RawUrlType, AutoUrlType } =
  CloudinarySdk.getConstants();

export const URL_TYPES = {
  image: ImageUrlType,
  video: VideoUrlType,
  raw: RawUrlType,
  auto: AutoUrlType,
};

type CloudinaryListener = (data: { uid: string; progress: number }) => void;

function addEventListener(
  event: CloudinaryEvent,
  listener: CloudinaryListener
) {
  return emitter.addListener(event, listener);
}

function removeSubscription(subscription: EmitterSubscription) {
  return emitter.removeSubscription(subscription);
}

const listeners: Record<string, EmitterSubscription | undefined> = {};

const addEventListenerForUID = (
  uid: string,
  event: CloudinaryEvent,
  listener: CloudinaryListener
) => {
  removeSubscriptionForUID(uid);

  listeners[uid] = addEventListener(event, listener);
};

const removeSubscriptionForUID = (key: string) => {
  const previousListener = listeners[key];
  if (previousListener) {
    removeSubscription(previousListener);
    listeners[key] = undefined;
  }
};

export function setup(options: Record<string, any>): Promise<void> {
  return CloudinarySdk.setup(options);
}

export type UploadParams = {
  url: string;
  presetName: string;
  type?: typeof URL_TYPES[keyof typeof URL_TYPES];
};

export const upload = async (
  params: UploadParams,
  onProgress?: CloudinaryListener
): Promise<void> => {
  const uid =
    Math.random().toString(36).substring(2, 15) +
    Math.random().toString(36).substring(2, 15);

  if (onProgress) {
    addEventListenerForUID(
      uid,
      CloudinaryEvent.UPLOAD_PROGRESS_EVENT,
      (data) => {
        if (uid === data.uid) {
          onProgress(data);
        }
      }
    );
  }

  const result = await CloudinarySdk.upload({ uid, ...params });
  if (onProgress) {
    removeSubscriptionForUID(uid);
  }
  return result;
};
