# Food Tracker (Android)

A starter Android project for food tracking using:
- CameraX for taking a food photo
- OpenAI Chat Completions endpoint for image analysis

## What it does
1. Opens the back camera preview.
2. Captures a photo.
3. Sends the image to `v1/chat/completions` as a base64 data URL.
4. Displays identified foods and estimated calories.

## Setup
1. Open in Android Studio (Hedgehog or newer recommended).
2. Sync Gradle.
3. Run on a device/emulator with camera support.
4. Enter your OpenAI API key in the app UI.

## Notes
- API key is kept only in memory in this starter implementation.
- For production, move secrets handling to a secure backend and add stronger error handling.
