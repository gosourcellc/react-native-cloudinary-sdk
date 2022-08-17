import * as React from 'react';
import * as ImagePicker from 'react-native-image-picker';

import { StyleSheet, View, Text, Button } from 'react-native';
import * as Cloudinary from 'react-native-cloudinary-sdk';

import Config from '../config';

export default function App() {
  const [response, setResponse] =
    React.useState<ImagePicker.ImagePickerResponse | null>(null);

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

  return (
    <View style={styles.container}>
      {response && (
        <View style={{ width: '100%', alignItems: 'center' }}>
          <Text style={{ paddingBottom: 30 }}>
            Result: {response?.assets?.[0]?.uri}
          </Text>
          <Button
            title={'Upload'}
            onPress={() => {
              // Cloudinary.upload(response?.assets?.[0]?.uri, {
              //   folder: 'test',
              //   public_id: 'test',
              // }).then(setResult);
            }}
          />
          <View style={styles.separator} />
        </View>
      )}
      <Button
        title={'Take Photo'}
        onPress={() =>
          onButtonPress('capture', {
            saveToPhotos: true,
            mediaType: 'photo',
            includeBase64: false,
          })
        }
      />
      <Button
        title={'Pick an image'}
        onPress={() =>
          onButtonPress('library', {
            selectionLimit: 0,
            mediaType: 'photo',
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
});
