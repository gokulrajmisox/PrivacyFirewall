# 🛡️ PrivacyFirewall for Android

> **Zero-Leakage, On-Device AI Privacy Shield & Interception Keyboard.**  
> Intercepts sensitive data (PII, credentials, API keys, named entities) **before** it ever leaves your device to ChatGPT, Claude, Gemini, WhatsApp, or browsers.

---

[![Platform](https://img.shields.io/badge/Platform-Android%2010%2B-3DDC84?logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF?logo=kotlin&logoColor=white)](#)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](#)
[![ONNX Runtime](https://img.shields.io/badge/AI%20Engine-ONNX%20Runtime%20Mobile-005CED?logo=onnx&logoColor=white)](#)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20On--Device-success)](#)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

### 📥 Download Demo APK
**[Download the Hackathon Demo APK](https://drive.google.com/file/d/1Amfnf3JzGKEElA3oOVwUAUe-9Yv4t4Fn/view?usp=sharing)**

> Before judging, verify that this APK contains the real ONNX model and that the dashboard reports **AI model loaded**. A Git LFS pointer is not a model file.

---

## 📌 Executive Summary

Modern mobile workflows increasingly involve interacting with large language models (ChatGPT, Claude, Copilot, Gemini) and collaborative messaging apps. Users routinely copy-paste snippets containing proprietary tokens, client names, locations, API keys, or financial identifiers into prompts without realizing the data governance risk.

**PrivacyFirewall** is an autonomous, on-device data loss prevention (DLP) solution for Android. Operating primarily as an intelligent **Privacy Keyboard (InputMethodService)**, it scans outgoing keystrokes in real time using a dual-engine architecture:
1. **Deterministic Rule Engine:** Sub-millisecond Regex pattern matching for structured secrets (API keys, SSNs, credit cards, emails, phone numbers).
2. **Neural NER Engine:** Quantized BERT Named Entity Recognition model (`Xenova/bert-base-NER-uncased`) executed natively via **ONNX Runtime Mobile**, powered by a custom pure-Kotlin **WordPiece Tokenizer** (zero heavy C++ wrappers, crash-resilient).

**Result:** when the bundled model is loaded, detection and redaction run locally with zero cloud inference. Verify offline operation on the target phone before judging.

---

## 📐 Architecture & Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                       Target Apps                           │
│        ChatGPT • Claude • Gemini • WhatsApp • Chrome        │
└──────────────────────────────┬──────────────────────────────┘
                               │ User types text / pastes input
                               ▼
┌─────────────────────────────────────────────────────────────┐
│          PrivacyFirewall Keyboard (InputMethodService)      │
│  - Keystroke Interceptor                                    │
│  - Jetpack Compose Embedded Input View                      │
└──────────────────────────────┬──────────────────────────────┘
                               │ Async stream
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 PrivacyFirewallEngine                       │
│  ┌───────────────────────────┴───────────────────────────┐  │
│  ▼                                                       ▼  │
│  [ Deterministic Engine ]               [ Neural Engine ]   │
│  - RegexDetector (SSN, Cards,           - BertTokenizer.kt  │
│    API Keys, Emails, Phone)             - ONNX Runtime      │
│                                           (Quantized BERT)  │
│                                         - BIO Entity Parse  │
│  └───────────────────────────┬───────────────────────────┘  │
└──────────────────────────────┼──────────────────────────────┘
                               ▼
┌─────────────────────────────────────────────────────────────┐
│               Interception & Mitigation UI                  │
│  - Inline Real-time Warning Banner                          │
│  - One-tap Redaction / Warning Notification                 │
│  - User chooses to edit, redact, or override                │
└─────────────────────────────────────────────────────────────┘
```

---

## ✨ Key Features

- 🔒 **True Zero-Network Guarantee:** All models, vocabularies, and regex heuristics reside strictly within local assets. The firewall operates completely in Airplane Mode.
- ⚡ **Dual Detection Core:**
  - **Structured PII:** Credit Cards (Luhn algorithm friendly), US SSN, International Phone Numbers, Email Addresses.
  - **Developer Secrets:** AWS Keys, GitHub Personal Access Tokens, Generic High-Entropy API Keys (`Bearer`, `sk-...`, `api_key=`).
  - **Deep Semantic Entities:** Detects `PERSON`, `ORGANIZATION`, and `LOCATION` names via on-device BERT Token Classification.
- ⌨️ **Native Keyboard Integration:** Leverages Android's `InputMethodService` combined with modern `ComposeView` integration, enabling universal protection across all third-party apps without fragile accessibility hooks.
- ⚙️ **Customizable Sensitivity:** Configurable thresholds, toggleable scanning categories, and local preferences backed by Jetpack DataStore.
- 🪶 **Engineered for Mobile:** Quantized INT8 weights (~40MB) and lightweight memory footprint, optimized to avoid GC pauses during typing.

---

## 🛠️ Tech Stack & Dependencies

| Component | Technology | Rationale |
|---|---|---|
| **Language** | Kotlin 2.x | Modern, null-safe, coroutine-native language |
| **UI Layer** | Jetpack Compose + Material 3 | Declarative, dynamic reactive UI for settings & keyboard overlay |
| **Inference Runtime** | ONNX Runtime Mobile (`1.18.0`) | Cross-platform, hardware-accelerated on-device ML engine |
| **Model Weights** | `bert-base-NER-uncased` (Quantized INT8) | Standard 4-class NER model optimized for sub-100ms inference |
| **Tokenization** | Custom `BertTokenizer.kt` | Pure-Kotlin WordPiece implementation; zero fragile JNI dependencies |
| **Preferences** | AndroidX DataStore Preferences | Asynchronous, transactional configuration storage |
| **Build System** | Gradle 9.x + Android Gradle Plugin 9.x | Modern Android build tooling |

---

## 📁 Repository Structure

```text
PrivacyFirewall/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/
│   │   │   │   ├── model_quantized.onnx    # Quantized BERT NER ONNX weights
│   │   │   │   └── vocab.txt               # WordPiece vocabulary (30,522 tokens)
│   │   │   ├── java/com/example/privacyfirewall/
│   │   │   │   ├── data/
│   │   │   │   │   └── SettingsRepository.kt       # DataStore preference management
│   │   │   │   ├── domain/
│   │   │   │   │   ├── AiDetector.kt               # ONNX Runtime execution & BIO tag aggregation
│   │   │   │   │   ├── BertTokenizer.kt            # Zero-dependency Kotlin WordPiece tokenizer
│   │   │   │   │   ├── PrivacyFirewallEngine.kt    # Unified orchestrator (Regex + AI)
│   │   │   │   │   └── RegexDetector.kt            # High-performance regex heuristics
│   │   │   │   ├── service/
│   │   │   │   │   └── PrivacyKeyboardService.kt   # Android InputMethodService + Compose UI
│   │   │   │   ├── theme/                          # Material3 theme & palette
│   │   │   │   ├── ui/
│   │   │   │   │   ├── DashboardScreen.kt          # System status & quick actions
│   │   │   │   │   └── SettingsScreen.kt           # Protection toggles & AI thresholds
│   │   │   │   ├── MainActivity.kt                 # Primary application entry point
│   │   │   │   └── Navigation.kt                   # Compose Navigation graph
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   │       └── java/com/example/privacyfirewall/domain/
│   │           └── RegexDetectorTest.kt            # Unit test suite for pattern detectors
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml                          # Centralized version catalog
├── README.md
└── settings.gradle.kts
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio:** Hedgehog / Iguana / Jellyfish / Quail (recommended)
- **JDK:** Version 17
- **Android SDK:** Compile SDK `36`, Minimum SDK `24` (Android 7.0+)
- **Physical Device or Emulator:** Running Android 10+ with at least 2GB free RAM

### Setup & Installation

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/gokulrajmisox/PrivacyFirewall.git
   cd PrivacyFirewall
   ```

2. **Open in Android Studio:**
   - Launch Android Studio.
   - Select **File > Open...** and point to the cloned directory.
   - Allow Gradle to sync dependencies defined in `gradle/libs.versions.toml`.

3. **Verify Asset Files:**
   Ensure both neural assets exist in `app/src/main/assets/`:
   - `model_quantized.onnx` — the actual binary model, not a 134-byte Git LFS pointer
   - `vocab.txt`

   If Git LFS is configured for this clone, run:
   ```bash
   git lfs install
   git lfs pull
   ls -lh app/src/main/assets/model_quantized.onnx
   file app/src/main/assets/model_quantized.onnx
   ```

   The model must be substantially larger than a few bytes and must not be reported as ASCII text.

4. **Build & Run:**
   - Select your target device/emulator from the toolbar.
   - Click the green **▶ Run** button (`Shift + F10`) or run via terminal:
     ```bash
     ./gradlew assembleDebug
     ```

---

## 🎬 Three-Minute Judging Demo

Use this sequence rather than opening the app without context:

1. **State the problem:** “People paste API keys, PII, and private names into AI and messaging apps without noticing.”
2. **Show the app:** Open the dashboard and confirm that the local rule engine and AI model are loaded.
3. **Show a realistic prompt:** Type a fake API key, email address, phone number, and person name into the Privacy Keyboard.
4. **Show prevention:** Point out the real-time warning before pressing Send, then tap **Redact**.
5. **Show the result:** Confirm that the outgoing text contains redaction markers instead of the original sensitive values.
6. **Prove privacy:** Turn on airplane mode and repeat the test. Explain that the detector continues working locally.
7. **Close with impact:** “PrivacyFirewall adds a last-mile privacy layer to every app that accepts text.”

Record this flow and test the APK on the loaner iQOO phone before presenting.

## ✅ Pre-Judging Verification Checklist

- [ ] The APK installs on the target iQOO phone.
- [ ] The dashboard reports that the real AI model is loaded.
- [ ] Regex detection works with Wi-Fi and mobile data disabled.
- [ ] NER detection identifies at least one person, organization, or location.
- [ ] Redaction removes the original sensitive text from the outgoing content.
- [ ] The keyboard remains responsive while scanning longer text.
- [ ] The team can explain the phone-first and Office Kit workflow in one sentence.
- [ ] The demo video is less than three minutes and shows the complete flow.

## 🎮 How to Test

1. **Launch App:** Open **Privacy Firewall** from your app drawer. Confirm the dashboard indicates **Protection is ACTIVE**.
2. **Enable Keyboard:**
   - Tap **"Enable Privacy Keyboard in Settings"**.
   - Enable **Privacy Firewall** in your system's `Manage on-screen keyboards` settings.
3. **Trigger Real-Time Interception:**
   - Open any messaging or AI app (e.g., WhatsApp, ChatGPT, Notes).
   - Switch your active input method to **Privacy Firewall**.
   - Type a test prompt:
     ```text
     Hello! My name is Alice Smith, my key is sk-live-9876543210abcdef, and my SSN is 000-12-3456.
     ```
   - **Observe:** An inline warning banner instantly flags detected threats:
     - 🔴 `API_KEY`
     - 🔴 `SSN`
     - 🟡 `PERSON (Alice Smith)`
   - Tap **Redact** to replace detected values before sending.
   - The user is alerted before pressing send or transmitting data to cloud APIs.

---

## 🛡️ Threat Model & Security Posture

| Threat Vector | Mitigation |
|---|---|
| **Data Exfiltration by Host App** | Interception occurs at the keyboard layer before the host app even registers the submit event. |
| **Model Inversion / Leakage** | All weights are read-only assets executed within the application's isolated sandboxed memory space. |
| **Network Snooping / MITM** | No network permissions are required for core inference. Completely operational offline. |
| **Malicious OCR / Clipboard Sniffing** | Keeps credentials out of system clipboard buffers by processing in-memory. |

---

## 🛣️ Roadmap

- [x] High-performance regex engine for credentials and PII.
- [x] On-device BERT NER inference via ONNX Runtime Mobile.
- [x] Pure-Kotlin zero-dependency WordPiece Tokenizer.
- [x] Jetpack Compose Privacy Keyboard with inline alert strip.
- [x] **One-Tap Redaction:** Replace detected sensitive tokens with markers such as `[REDACTED: API_KEY]`.
- [ ] **Optional Accessibility Service:** Passive background scanning for users who prefer keeping Gboard or SwiftKey.
- [ ] **Custom Regex Manager:** Allow security teams to inject custom internal token regexes via JSON import.

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check out the [issues page](https://github.com/gokulrajmisox/PrivacyFirewall/issues).

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the **MIT License**. See `LICENSE` for more information.

---

<p align="center">
  Built with 🛡️ for local AI privacy & data governance.
</p>
