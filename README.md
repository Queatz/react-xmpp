# react-xmpp
XMPP library for React Native

## Setup

For testing, a better Android emulator is available here:
https://www.genymotion.com

Install the `react-native` commandline tool:

```bash
npm install -g react-native-cli
```

## Running & Testing

To test, install the React tools and run the following command from the root directory.

```bash
react-native run-android
```

## Releasing

From the `android/` folder, the following command will produce a distributable .apk file.

```bash
./gradlew assembleRelease
```

The apk can then be found at: `android/app/build/outputs/apk/app-release.apk`

To install the production apk on your device, use:

```bash
./gradlew installRelease
```
