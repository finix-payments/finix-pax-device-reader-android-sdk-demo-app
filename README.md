# Finix PAX Device Reader — Sample Application

A sample Android app demonstrating how to integrate the Finix PAX Device Reader SDK
(`com.finix:pax-device-reader-sdk`) to run card-present transactions on an embedded PAX terminal.

It shows how to:

- Register the PAX vendor driver and create a `TerminalDevice`.
- Configure per-environment merchant credentials (PROD / SB).
- Start `SALE`, `AUTHORIZATION`, and `REFUND` transactions and observe live updates.
- Cancel an in-flight transaction.
- Attach transaction-level tags and split transfers.
- Capture a signature when the transaction result requires one.

## Architecture

The app follows a layered, unidirectional-data-flow architecture:

```
ui/            Jetpack Compose screens + components (stateless, driven by UiState)
  transactions/  TransactionsViewModel — the single source of UI state
  screen/        TransactionsScreen, ConfigurationSheet, OtherSheet
  signature/     Signature capture sheet + PNG/Base64 rendering
  state/         TransactionsUiState (immutable screen state)
  components/    Reusable form fields
domain/        Pure, testable logic: Money, TagParser, validators, TransactionLogger
data/          ConfigRepository backed by SharedPreferences + bundled asset defaults
device/        TerminalDeviceFactory — thin seam over FinixTerminalSDK
di/            Hilt modules and qualifiers
```

Key principles:

- **Single source of truth.** The ViewModel exposes one immutable `TransactionsUiState`
  via `StateFlow`; the UI is a pure function of that state plus callbacks.
- **The SDK is only touched at the edges.** `TerminalDeviceFactory` wraps
  `FinixTerminalSDK.createDevice(...)`, and the `TerminalDevice` is supplied to the
  ViewModel through Hilt assisted injection because it depends on the hosting `Activity`.
- **All I/O is off the main thread** and hidden behind `ConfigRepository`.
- **Business logic is framework-free**, so `domain/` can be unit-tested on the JVM.

## Configuration

Default credentials are read on first launch from `app/src/main/assets/merchant_config.json`,
keyed by environment name:

```json
{
  "PROD": { "applicationId": "AP...", "deviceId": "DV...", "merchantId": "MU...", "mid": "...", "userId": "...", "password": "..." },
  "SB":   { "applicationId": "AP...", "deviceId": "DV...", "merchantId": "MU...", "mid": "...", "userId": "...", "password": "..." }
}
```

Credentials can also be entered and saved at runtime via the **Configurations** menu; values
are validated (identifier prefixes, minimum password length) before being persisted per
environment.

## Requirements

- Android Studio (AGP 8.13+)
- JDK 17
- `minSdk` 26, `compileSdk` / `targetSdk` 36
- A PAX terminal for card-present transactions

The SDK is resolved from the Central Portal snapshots repository, configured in
`settings.gradle.kts`.

## Build conventions

- **Version catalog.** All dependency and plugin coordinates live in
  `gradle/libs.versions.toml` and are referenced as `libs.…` / `alias(libs.plugins.…)`
  from the build scripts, so versions are declared once.
- **KSP2 for annotation processing.** Hilt is processed with KSP rather than KAPT. KAPT is
  not compatible with Kotlin 2.3+, and KSP2 has been the default since 2025. KSP is versioned
  independently of Kotlin (this project uses KSP `2.3.10`).
- **Release shrinking.** The release build enables R8 code shrinking and resource shrinking;
  keep rules for Hilt, kotlinx.serialization, Compose, and the Finix SDK live in
  `app/proguard-rules.pro` (and `app/src/main/keepRules/`).
- **Gradle performance.** Parallel execution, the build cache, and the configuration cache are
  enabled in `gradle.properties`, along with non-transitive R classes.
