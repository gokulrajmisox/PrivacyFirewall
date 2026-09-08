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

**[Read the judge-facing submission guide](SUBMISSION.md)**

> The full ONNX model is included through Git LFS. Run `git lfs pull` after cloning so the APK and local build include the approximately 106 MB model asset.

---

## 🏆 iQOO Hackathon 2026 — Judging Alignment

PrivacyFirewall is positioned for the **Open Innovation** and **Developer Tools** tracks. The project is designed around the event's phone-first workflow and should be demonstrated on the loaner iQOO phone. The official rubric combines **75% jury scoring** with **25% HackTracker device data**.

| Official criterion | Weight | What PrivacyFirewall demonstrates | Evidence to show judges |
|---|---:|---|---|
| **End product quality** | **30%** | A usable Android app, live detection, warning state, and one-tap redaction | Install the APK, run the sandbox, then use the Privacy Keyboard in a second app |
| **Novelty and impact** | **20%** | A last-mile privacy layer that protects text before it reaches AI and messaging apps | Explain the user problem, who benefits, and why keyboard-layer protection avoids app-by-app integrations |
| **Creative phone use** | **15%** | On-device AI inference and real-time mobile text scanning | Demonstrate the model loaded on the iQOO phone and repeat the test offline; do not claim NPU acceleration unless measured |
| **Technical depth** | **15%** | Kotlin architecture, ONNX Runtime Mobile, WordPiece tokenization, BIO NER parsing, character offsets, debounce handling, and DataStore metrics | Walk through the architecture and show the model status, responsive keyboard, redaction, and tests |
| **Office Kit usage** | **10%** | Phone-first development and phone/laptop bridge are supported as an event workflow | Use Office Kit during the event and show the phone remaining in the loop; this score is based on HackTracker data, not a README claim |
| **Demo and presentation** | **10%** | A focused problem-to-proof narrative | Deliver a compelling **3–5 minute** live pitch and demo on the iQOO phone |

### Event-format requirements to remember

- The build is **phone-first**: during Red Light, the iQOO phone is the primary build device through Office Kit; during Green Light, both phone and laptop may be used.
- The official schedule describes approximately **55% Red Light / phone-only** and **45% Green Light / both devices** during the build window.
- Office Kit connects the phone and laptop for screen mirroring, clipboard, files, and remote control. Its usage is scored from HackTracker telemetry.
- Two scored evaluation rounds feed the **Top 10** per bucket; the final live pitch determines winners. The city battle advances **Top 6** teams, with three student and three working-professional teams.
- The final demonstration should be on the **iQOO phone**, with synthetic secrets and personal data only.

### Claims we will and will not make

**Defensible claims:** on-device inference, offline detection after the model is loaded, keyboard-layer interception, local NER, and one-tap redaction.

**Do not claim without device evidence:** Snapdragon NPU acceleration, measured latency, Office Kit integration inside the product, or zero false positives.

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
  - 🪶 **Engineered for Mobile:** Quantized INT8 weights (approximately 106 MB in the repository) with debounced inference to keep typing responsive.

---

## 🛠️ Tech Stack & Dependencies

| Component | Technology | Rationale |
|---|---|---|
| **Language** | Kotlin 2.x | Modern, null-safe, coroutine-native language |
| **UI Layer** | Jetpack Compose + Material 3 | Declarative, dynamic reactive UI for settings & keyboard overlay |
| **Inference Runtime** | ONNX Runtime Mobile (`1.18.0`) | Cross-platform, hardware-accelerated on-device ML engine |
  | **Model Weights** | `bert-base-NER-uncased` (Quantized INT8) | Local BERT token-classification model for semantic entity detection |
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
   - `model_quantized.onnx` — the full binary model, downloaded through Git LFS
   - `vocab.txt`

   If Git LFS is configured for this clone, run:
   ```bash
   git lfs install
   git lfs pull
   ls -lh app/src/main/assets/model_quantized.onnx
   file app/src/main/assets/model_quantized.onnx
   ```

   The expected model size is approximately 106 MB and it should be reported as binary data, not ASCII text.

4. **Build & Run:**
   - Select your target device/emulator from the toolbar.
   - Click the green **▶ Run** button (`Shift + F10`) or run via terminal:
     ```bash
     ./gradlew assembleDebug
     ```

---

## 🎬 Demo

1. Open PrivacyFirewall and confirm that the local engines are ready.
2. Enter synthetic PII and a fake API key in the Live Detection Sandbox.
3. Review the real-time warning and tap **REDACT**.
4. Confirm that the original sensitive values have been replaced with redaction markers.
5. Repeat the test in airplane mode to demonstrate local processing.
6. Enable the Privacy Keyboard and use the same flow in another Android app.

Use only synthetic credentials and personal information when testing. See [`SUBMISSION.md`](SUBMISSION.md) for the detailed judging pitch and verification checklist.

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
