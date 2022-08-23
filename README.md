# react-native-cloudinary-sdk
react native wrapper for iOS, Android Cloudinary SDKs
## Installation
It's not published to npm yet
```sh
npm install react-native-cloudinary-sdk
```

## Usage

```js
import * as Cloudinary from 'react-native-cloudinary-sdk';

// ...
const Config = {
  cloud_name: 'your-cloud-name',
  secure: true,
  presetName: 'your-preset-name',
}
Cloudinary.setup(Config);
// ...

const uploaded_url = await Cloudinary.upload(
  {
    url,
    presetName: Config.presetName,
    type,
  },
  (data) => {
    console.warn('onProgress - ', data.progress);
  }
);
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
