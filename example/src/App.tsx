import * as React from 'react';
import * as ImagePicker from 'react-native-image-picker';

import { StyleSheet, View, Text, Button } from 'react-native';
import * as Cloudinary from 'react-native-cloudinary-sdk';

import Config from '../config';

export default function App() {
  const [response, setResponse] =
    React.useState<ImagePicker.ImagePickerResponse | null>(null);
  const [progress, setProgress] = React.useState<Record<string, number>>({});
  const onButtonPress = React.useCallback((type, options) => {
    if (type === 'capture') {
      ImagePicker.launchCamera(options, setResponse);
    } else {
      ImagePicker.launchImageLibrary(options, setResponse);
    }
  }, []);

  React.useEffect(() => {
    const setup = async () => {
      await Cloudinary.setup(Config);
    };
    setup();
  }, []);

  const uploadPress = async (
    url: string | undefined,
    _type: string | undefined
  ) => {
    let type = Cloudinary.URL_TYPES.auto;
    if (_type?.includes('image')) {
      type = Cloudinary.URL_TYPES.image;
    } else if (_type?.includes('video')) {
      type = Cloudinary.URL_TYPES.video;
    }
    if (url) {
      try {
        const uploaded_url = await Cloudinary.upload(
          {
            url,
            presetName: Config.presetName,
            type,
          },
          (data) => {
            console.warn('onProgress - ', data.progress);
            setProgress({ ...progress, [url]: data.progress });
          }
        );
        console.warn(uploaded_url);
      } catch (error) {
        console.warn(error);
      }
    }
  };

  return (
    <View style={styles.container}>
      {response && (
        <View style={{ width: '100%', alignItems: 'center' }}>
          {response?.assets?.map((asset) => (
            <View key={asset.uri} style={styles.mediaContainer}>
              <Text style={{ paddingBottom: 10 }}>Asset: {asset?.uri}</Text>
              <Text style={{ paddingBottom: 10 }}>
                Progress: {progress[asset?.uri ?? '']}
              </Text>
              <Button
                title={'Upload'}
                onPress={() => uploadPress(asset?.uri, asset.type)}
              />
            </View>
          ))}
          <View style={styles.separator} />
        </View>
      )}
      <Button
        title={'Take Photo'}
        onPress={() =>
          onButtonPress('capture', {
            saveToPhotos: true,
            mediaType: 'mixed',
            includeBase64: false,
          })
        }
      />
      <Button
        title={'Pick an image'}
        onPress={() =>
          onButtonPress('library', {
            selectionLimit: 0,
            mediaType: 'mixed',
            includeBase64: false,
          })
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 20,
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
  separator: {
    height: 1,
    width: 100,
    backgroundColor: 'black',
    margin: 20,
  },
  mediaContainer: {
    width: '100%',
    alignItems: 'center',
    borderColor: 'black',
    borderWidth: 1,
  },
});
