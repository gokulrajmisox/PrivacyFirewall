# PrivacyFirewall — Hackathon Submission Guide

## One-line pitch

**PrivacyFirewall is an offline Android privacy layer that detects sensitive information before it reaches AI, messaging, or browser applications and lets the user redact it with one tap.**

## What judges should evaluate

PrivacyFirewall is submitted as an **Open Innovation / Developer Tools** project. Its core value is prevention at the keyboard layer rather than post-hoc scanning after data has already been sent.

The system combines deterministic detection for structured secrets and personal data with an on-device BERT NER model for names, organizations, and locations. The user can review the warning and replace detected values with explicit redaction markers before sending.

## Fastest demo path

1. Install the demo APK on the iQOO phone.
2. Open **Privacy Firewall** and confirm that the dashboard shows **BERT-NER Neural Engine — Status: Ready**.
3. Open the **Live Detection Sandbox**.
4. Tap **Fill Sample PII/Key**.
5. Show the detected API key, SSN, and named entity.
6. Tap **REDACT** and show the sanitized text.
7. Disable Wi-Fi and mobile data, repeat the test, and show that local detection still works.
8. Enable the Privacy Keyboard, open a text field in a target app, and repeat the same detection/redaction flow.

Use only fake credentials and synthetic personal information in the demonstration.

## Judge-facing proof points

| Claim | How to verify it |
|---|---|
| Local structured detection | Enter an email, phone number, SSN, card-like number, JWT, or API-key-shaped value in airplane mode. |
| Local semantic detection | Enter a synthetic sentence containing a person, organization, or location name and inspect the warning. |
| Redaction | Tap **REDACT** and confirm that the original sensitive values are absent from the resulting text. |
| Keyboard interception | Enable Privacy Keyboard in Android settings and type into a separate app. |
| No cloud inference | Run the demo with Wi-Fi and mobile data disabled. |
| Persistent metrics | Redact a sample and confirm that the dashboard threat counter updates. |

## Repository setup

The neural model is stored through **Git LFS**. A normal Git checkout may initially show a small pointer file. Judges should install Git LFS and pull the binary before building:

```bash
git lfs install
git lfs pull
ls -lh app/src/main/assets/model_quantized.onnx
```

The model should be approximately 106 MB and reported as binary data, not ASCII text. The vocabulary file is stored directly in the repository.

Build requirements:

- Android Studio with Android SDK 36
- JDK 17
- A physical Android device or emulator
- Android 10 or newer recommended for the full keyboard demonstration

Build command:

```bash
./gradlew test assembleDebug
```

## Three-minute pitch structure

**0:00–0:30 — Problem.** Users paste production-like secrets, PII, and private names into AI tools and messaging applications without realizing that the data may leave the device.

**0:30–1:15 — Product.** PrivacyFirewall operates as an Android InputMethodService. It checks text locally with a fast rule engine and an on-device NER model, then warns the user before transmission.

**1:15–2:15 — Live proof.** Enter synthetic sensitive data, show the warning, tap **REDACT**, and show the sanitized output. Repeat in airplane mode.

**2:15–2:45 — Technical depth.** Explain the Kotlin engine, ONNX Runtime Mobile, WordPiece tokenizer, real character offsets, debounce handling, and DataStore metrics.

**2:45–3:00 — Impact.** PrivacyFirewall adds a last-mile privacy control to any Android application that accepts text, without requiring the target application to integrate a new SDK.

## Submission checklist

- [ ] Git LFS model is present and approximately 106 MB.
- [ ] APK installs on the target iQOO phone.
- [ ] Dashboard reports the AI model as ready.
- [ ] Regex detection works offline.
- [ ] NER detection works offline.
- [ ] Redaction removes the original values.
- [ ] Privacy Keyboard works in a second app.
- [ ] Demo uses only synthetic secrets and personal information.
- [ ] Screen recording is under three minutes.
- [ ] Team can explain the phone-first and Office Kit workflow truthfully.

## Important submission note

Do not claim Snapdragon NPU acceleration, Office Kit integration, or measured latency unless those capabilities have been demonstrated on the event device. The strongest defensible claims are **on-device inference**, **offline operation**, **keyboard-layer interception**, and **one-tap redaction**.

## Repository

[GitHub repository](https://github.com/gokulrajmisox/PrivacyFirewall)

[Demo APK link](https://drive.google.com/file/d/1Amfnf3JzGKEElA3oOVwUAUe-9Yv4t4Fn/view?usp=sharing)

## License status

The repository advertises an MIT badge. Add or verify a `LICENSE` file before submission, and confirm that the third-party model terms permit redistribution with the APK.

## Final pre-submission action

Install the APK on the event phone, run the checklist above, and record the complete flow in one take.
