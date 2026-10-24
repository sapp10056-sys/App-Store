# NovaStore - Open Android Application Store & Distribution System

**NovaStore** is a complete, production-oriented Android application marketplace and distribution platform built with Kotlin, Jetpack Compose, Material 3, and Firebase Firestore. It features curated discovery catalogs, external HTTPS downloads via DevUploads, remote app update notifications, Telegram bot submission workflows, responsive web administration tools, and GitHub Actions CI/CD automation.

---

## Architecture Overview

```
                      ┌────────────────────────────────────────┐
                      │          NovaStore Android App         │
                      │  - First-Launch Legal Disclaimer       │
                      │  - Featured & Trending App Catalogs    │
                      │  - Live Search & Category Filtering    │
                      │  - 17-Point Submission Form + Previews │
                      │  - External Download Confirmation      │
                      │  - Remote Update System                │
                      └──────────────┬──────────────────┬──────┘
                                     │                  │
               Transparent Silent    │                  │  Fetches app-update.json
               Authentication        │                  │  over HTTPS
                                     ▼                  ▼
                        ┌──────────────────┐    ┌────────────────────┐
                        │Firebase Firestore│    │   GitHub / CDN     │
                        │ - applications   │    │  (app-update.json) │
                        │ - reports        │    └────────────────────┘
                        │ - configuration  │
                        └────────┬─────────┘
                                 │
                     ┌───────────┴────────────┐
                     │                        │
                     ▼                        ▼
        ┌───────────────────────┐  ┌───────────────────────┐
        │ Web Management Panel  │  │ Telegram Submission   │
        │ (web-admin/index.html)│  │ Channel (@YourBot)    │
        └───────────────────────┘  │      │                │
                                   │      ▼                │
                                   │ DevUploads Hosting    │
                                   └───────────────────────┘
```

---

## 20-Step Complete Setup & Deployment Guide

### STEP 1: Verify Codebase
Ensure all files listed in the project checklist are present and the build succeeds locally.

### STEP 2: Configure Application Metadata
Review `metadata.json` and ensure your store name and descriptor match `res/values/strings.xml`.

### STEP 3: Create GitHub Repository
1. Initialize a new GitHub repository: `git init`
2. Commit all files: `git add . && git commit -m "Initial commit of NovaStore"`
3. Push to GitHub: `git remote add origin https://github.com/YOUR_USERNAME/NovaStore.git && git push -u origin main`

### STEP 4: Enable GitHub Actions Workflow
The `.github/workflows/build-android.yml` workflow automatically triggers on every push to `main`, validating the build and executing all unit tests.

### STEP 5: Download & Test the Generated Debug APK
Navigate to the **Actions** tab on your GitHub repository, click on the latest workflow run, and download the `novastore-debug-apk` artifact.

### STEP 6: Firebase Project Setup
1. Open the [Firebase Console](https://console.firebase.google.com/).
2. Create or select project `app-store-a9559`.

### STEP 7: Register Android Application in Firebase
1. Click **Add App** -> **Android**.
2. Enter the Package Name: `com.aistudio.novastore.app`.
3. Provide your debug and release SHA-1 certificates.

### STEP 8: Install `google-services.json`
Place the downloaded `google-services.json` into `/app/google-services.json`. (A pre-configured file is already included).

### STEP 9: Enable Firebase Authentication
1. Go to **Authentication** in the Firebase Console.
2. Under the **Sign-in method** tab, enable **Anonymous** authentication so users can seamlessly submit listings without any login barrier.

### STEP 10: Create Cloud Firestore Database & Deploy Security Rules
1. Create a Cloud Firestore database in Production mode.
2. Deploy the provided `firestore.rules` file to protect the `applications` and `reports` collections.

### STEP 11: Configure Administrator Access
Use the in-app Admin Passcode (`admin1234` by default, customizable in `SettingsScreen.kt`) or configure custom claims using the Firebase Admin SDK.

### STEP 12: Configure Telegram Submission Bot
1. Open `@BotFather` on Telegram and create a bot (e.g., `@NovaStoreOfficialBot`).
2. Update the `TELEGRAM_BOT_URL` placeholder in `SettingsScreen.kt` or `PreferencesManager.kt`.

### STEP 13: Configure DevUploads and Test Download Links
1. Create an account on [DevUploads](https://devuploads.com/).
2. Upload a sample test APK to obtain a direct HTTPS download link.
3. Use this link in the submission form.

### STEP 14: Host `app-update.json` on GitHub Pages or Raw GitHub
Host the included `app-update.json` file on GitHub Pages or as a Raw URL (`https://raw.githubusercontent.com/...`).

### STEP 15: Test Listing Submission & Verification
1. Launch the app and accept the first-launch disclaimer.
2. Tap **Upload APK** -> **Submit APK**.
3. Fill in all 17 fields, including the Logo URL (preview updates in real time).
4. Tap **Publish Application** to confirm real-time Firestore persistence.

### STEP 16: Create a Signed Release APK / App Bundle
Generate a release keystore:
```bash
keytool -genkey -v -keystore release.jks -alias novastore -keyalg RSA -keysize 2048 -validity 10000
```

### STEP 17: Publish Release on GitHub Releases
Create a new GitHub Release (e.g. `v1.0.0`) and attach the signed `app-release.apk`.

### STEP 18: Update `app-update.json` for New Releases
Whenever releasing a new version, bump `latestVersionCode` and update `latestVersionName` and `downloadUrl` in `app-update.json`.

### STEP 19: Test Remote Update & Force Update Behavior
Change `latestVersionCode: 2` in `app-update.json`. The app will detect the update and prompt the user.

### STEP 20: Launch the Web Administration Console
Open `/web-admin/index.html` in any browser to moderate applications, approve pending submissions, and review user reports in real time!
