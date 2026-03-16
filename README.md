# AASRA Android App 📱🆘

![AASRA Android App Cover](placeholder_image_url_here)
*Caption: AASRA Android Mobile Application screens.*

The **AASRA Android App** is the primary user-facing component of the disaster management ecosystem. Built natively with **Kotlin** and **Jetpack Compose**, it provides an intuitive and fast interface for users to report emergencies, view safe zones, and receive critical broadcasts during crisis situations.

It leverages Firebase for real-time state synchronization, OSMDroid for mapping, and integrates with the dedicated AASRA AI Engine via Retrofit for intelligent report verification.

---

## ✨ Key Features

*   **Modern Declarative UI**: Entirely built using **Jetpack Compose** and Material 3 design guidelines for smooth animations and an exceptional user experience.
*   **Emergency Reporting**: Users can capture photos, provide descriptions, and submit geo-tagged disaster reports directly from the app.
*   **Real-time Map Integration**: Utilizes **OSMDroid** to display interactive maps, pinpointing incidents and highlighting safe zones without relying on proprietary maps.
*   **Secure Authentication**: Implements **Firebase Authentication** and Google ID credentials for quick and secure user onboarding.
*   **AI Verification Link**: Communicates directly with the AASRA AI Engine via **Retrofit** to process and verify uploaded incident images.
*   **Offline Resilience**: Designed with Coroutines and local state management to handle spotty network conditions typical in disaster scenarios.

---

## 🛠️ Tech Stack & Dependencies

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Android Studio](https://img.shields.io/badge/Android%20Studio-3DDC84?style=for-the-badge&logo=android-studio&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)

*   **Language**: Kotlin (JVM Target 11)
*   **UI Framework**: Jetpack Compose (Material 3)
*   **Architecture & Asynchrony**: Kotlin Coroutines, ViewModel
*   **Networking**: Retrofit 2, Gson
*   **Database & Auth**: Firebase Firestore, Firebase Auth, Google ID
*   **Mapping**: OSMDroid (OpenStreetMap)
*   **Minimum SDK**: 24 (Android 7.0)
*   **Target SDK**: 36 (Android 16)

---

## 🚀 Local Setup Instructions

Follow these instructions to build and run the Android application via Android Studio.

### Prerequisites
*   **Android Studio** (Latest Stable release recommended).
*   Android SDK targeting API Level 36.

### Step 1: Open the Project
1.  Launch Android Studio.
2.  Select **Open** and navigate to the `AASRA-android-app` directory.
3.  Allow Gradle to complete its initial sync (this might take a few minutes as it downloads the Compose BOM and other libraries).

### Step 2: Configure Firebase
1.  Go to the Firebase Console and register your Android app with the package name `com.roshnab.aasra`.
2.  Download the `google-services.json` file.
3.  Place the `google-services.json` file inside the `app/` directory of the project.

### Step 3: Configure Mapping (OSMDroid)
OSMDroid requires an internet connection and proper permissions (configured in the `AndroidManifest.xml`). No API keys are required for the base OpenStreetMap tiles.

### Step 4: Build and Run
1.  Connect a physical Android device (with USB Debugging enabled) or start an Android Emulator.
2.  Click the **Run 'app'** button (`Shift + F10`) in Android Studio.
3.  The app will compile and install on your target device.

---

## 📸 Application Images
*(Images to be added here)*

![App Home Screen](placeholder_image_url_here)
*Caption: User reporting interface and emergency quick actions.*

![Map View](placeholder_image_url_here)
*Caption: OSMDroid map displaying real-time incident markers.*

---
*Created for the AASRA Final Year Project.*
