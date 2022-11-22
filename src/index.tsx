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
    previousListener.remove();
    listeners[key] = undefined;
  }
};

export function setup(options: Record<string, any>): Promise<void> {
  return CloudinarySdk.setup(options);
}

export type UploadParams = {
  url: string;
  type?: typeof URL_TYPES[keyof typeof URL_TYPES];
  params: { preset_name?: string } & Record<string, any>;
};

export type CloudinaryUploadResponse = {
  access_mode: string; // 'public';
  api_key: string; //'33938xxxxxx82';
  asset_id: string; //'48bf72eb1xxxxxx76db4ef0c6e';
  bytes: number; //12810;
  context: {
    custom: {
      resource_id: string; // '4xxxzr1';
      resource_type: string; // 'user_message_media'
    };
  };
  created_at: string; // '2022-09-08T12:42:25Z';
  etag: string; // 'cebab7f57c9905xxxxxa092ef6b5f';
  folder: string; // 'user_message_media/nxxxxx1';
  format: string; // 'jpg';
  height: number; // 225;
  original_filename: string; // '1';
  placeholder: false;
  public_id: string; // 'user_message_media/4nxxx1/2022xxx124223904267_4xxxg2g';
  resource_type: string; // 'image';
  secure_url: string; // 'https://res.cloudinary.com/go-source/image/upload/v166264sss5/user_message_media/4nxx1/2022090ssss3904267_42ug2g.jpg';
  signature: string; // '2f20fxxxxxxxx54c4c8efa05a374f342';
  tags: string[];
  type: string; //'upload';
  url: string; // 'https://res.cloudinary.com/go-source/image/upload/v166264sss5/user_message_media/4nxx1/2022090ssss3904267_42ug2g.jpg';
  width: number; // 225;
};

export const cancelUpload = (uid: string) => {
  CloudinarySdk.cancel(uid);
};

export const upload = async (
  params: UploadParams,
  onProgress?: CloudinaryListener,
  onUID?: (uid: string) => void
): Promise<CloudinaryUploadResponse | undefined> => {
  const uid =
    Math.random().toString(36).substring(2, 15) +
    Math.random().toString(36).substring(2, 15);
  onUID?.(uid);

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
