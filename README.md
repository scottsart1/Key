# HouseKey

An Android app that turns your phone into an emulated NFC credential using
**Host Card Emulation (HCE)**. Store one or more "keys" and present the active
one to a contactless reader by holding the phone to it.

> ⚠️ **Use only with credentials you own or are explicitly authorised to use.**
> This project is intended for learning about NFC/HCE and for consolidating your
> own tags. It does not, and on stock Android cannot, clone secured access cards
> (see [Limitations](#limitations)).

## What it does

- **Emulate an NDEF Type 4 tag** exposing plain text or a link/URL. Any NFC
  reader or another phone can read it — useful as a demonstrable, standards-based
  "tag" that lives on your phone.
- **Emulate a raw ISO-DEP (APDU) responder** for a custom Application ID (AID).
  You define the response to `SELECT` and a set of command → response pairs, for
  access systems that talk in APDUs.
- **Read your own tags** in reader mode to see their UID and technologies, and to
  import NDEF text/URI content into a new key.
- **Manage multiple keys** and switch which one is emulated. Only one key is
  active at a time.

## How it works

Android routes contactless APDUs for registered AIDs to a
[`HostApduService`](https://developer.android.com/reference/android/nfc/cardemulation/HostApduService).

- The standard **NDEF Tag Application** AID `D2760000850101` is declared
  statically in [`apduservice.xml`](app/src/main/res/xml/apduservice.xml). When a
  reader selects it, the app implements the Type 4 Tag command set:
  `SELECT` application → `SELECT` Capability Container / NDEF file → `READ BINARY`.
  The tag bytes (NDEF records, the NDEF file with its length prefix, and the
  Capability Container) are built by
  [`NdefFactory`](app/src/main/java/com/example/housekey/hce/NdefFactory.kt).
- For **raw keys**, a custom AID is registered at runtime with
  `CardEmulation.registerAidsForService(...)`, and the app replies to `SELECT`
  and to the command/response pairs you configured.

The active credential is persisted by
[`EmulationStore`](app/src/main/java/com/example/housekey/hce/EmulationStore.kt)
so the service — which the OS may start in a fresh process at tap time — can load
it synchronously. All APDU handling lives in the dependency-free
[`ApduProcessor`](app/src/main/java/com/example/housekey/hce/ApduProcessor.kt),
which is covered by JVM unit tests.

## Limitations

Host Card Emulation on a normal (non-rooted) phone **cannot**:

- **Clone a UID-only fob or card.** Many cheap building fobs and MIFARE Classic
  cards are identified only by their UID. Android's NFC controller assigns the
  emulated UID itself and does not let an app spoof another tag's UID.
- **Copy a card that keeps a secret key** (MIFARE DESFire, HID iCLASS, and most
  modern access control). The secret never leaves the original card, so it cannot
  be read or reproduced.
- **Emulate MIFARE Classic** at all — that protocol is not ISO-DEP/APDU based and
  is not exposed to HCE apps.

What *does* work is NDEF Type 4 tag emulation and custom APDU responders, which
is what this app implements.

## Building

Requirements: Android Studio (Ladybug or newer) or the Android command-line tools
with an SDK for API 35.

```bash
./gradlew assembleDebug        # build the debug APK
./gradlew test                 # run JVM unit tests (Hex, NDEF, APDU processor)
./gradlew installDebug         # install on a connected device
```

The app targets `minSdk 26` and `compileSdk 35`. A real NFC-capable device is
required to actually emulate or read tags; the emulator has no NFC.

## Usage

1. Launch the app and add a key, or tap the NFC icon to read one of your own tags
   and import its NDEF content.
2. Tap a key to make it the active (emulated) credential — the status card shows
   which key is live.
3. Hold the phone to a reader. Use the Stop action to disable emulation.

If another app is set as the default for tap responses, the app warns you; set
HouseKey as the default in the system NFC settings if needed.

## Project layout

```
app/src/main/java/com/example/housekey/
├── MainActivity.kt            # hosts Compose UI + NFC reader mode
├── HouseKeyApp.kt             # Application; wires the repository
├── hce/                       # Host Card Emulation
│   ├── KeyHostApduService.kt  # HostApduService entry point (delegates)
│   ├── ApduProcessor.kt       # pure APDU state machine (unit-tested)
│   ├── NdefFactory.kt         # builds Type 4 tag byte structures
│   ├── ActiveEmulation.kt     # the active credential model
│   ├── EmulationStore.kt      # persists the active credential
│   └── HceManager.kt          # capability checks + dynamic AID routing
├── data/                      # Room database + repository
├── nfc/NfcReader.kt           # parses discovered tags
├── ui/                        # Jetpack Compose screens + ViewModel
└── util/Hex.kt                # hex <-> bytes helpers
```

## Testing

Byte-level correctness of the NDEF structures and the APDU state machine is
covered by JVM unit tests under `app/src/test/`. Run them with `./gradlew test`.

## License

Released under the MIT License. See [LICENSE](LICENSE).
