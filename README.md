<p align="center">
  <img width="256px" src="https://raw.githubusercontent.com/la-espada-v1/wazzer-watch/main/app/src/main/icon-playstore.png" />
</p>

<h1 align="center">Wazzer Watch</h1>

<p align="center">
  A smart Wear OS application that monitors heart rate and detects sudden falls in real time.
</p>

---

## Overview
Wazzer Watch is a Wear OS application that is part of the larger Wazzer smart shower system.

It is built with Kotlin and enhances user safety by monitoring heart rate and detecting sudden falls. When a fall is detected, the app immediately triggers a loud alarm and vibration to alert designated emergency contacts.

At the same time, it sends an HTTP POST emergency request to a connected server, which can take further actions such as adjusting the water temperature.



---

## Tech Stack
- Platform: Wear OS (Android)
- Language: Kotlin
- IDE: Android Studio
- Communication: HTTP (REST API)
- Sensors: Heart Rate Sensor, Accelerometer

---

## Installation

### Requirements
- Android Studio installed
- Git installed
- Wear OS device or emulator

### Steps
```bash
# 1. Clone the repository
git clone https://github.com/la-espada-v1/wazzer-watch

# 2. Install Android Studio
https://developer.android.com/studio

# 3. Open the project in Android Studio

# 3. Let Gradle sync dependencies

# 4. Connect your Wear OS device

# 5. Run the app from Android Studio
