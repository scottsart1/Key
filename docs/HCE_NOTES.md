# HCE and Type 4 tag emulation notes

Reference notes for how HouseKey emulates a card, and what a reader exchanges
with it.

## NDEF Type 4 tag exchange

A Type 4 Tag reader performs roughly this sequence; HouseKey answers each step in
[`ApduProcessor`](../app/src/main/java/com/example/housekey/hce/ApduProcessor.kt).

| Step | Command (hex) | Meaning | Response |
|------|---------------|---------|----------|
| 1 | `00 A4 04 00 07 D2760000850101 00` | SELECT NDEF Tag Application | `90 00` |
| 2 | `00 A4 00 0C 02 E103` | SELECT Capability Container file | `90 00` |
| 3 | `00 B0 0000 0F` | READ BINARY 15 bytes of CC | `<CC bytes> 90 00` |
| 4 | `00 A4 00 0C 02 E104` | SELECT NDEF file | `90 00` |
| 5 | `00 B0 0000 02` | READ BINARY the 2-byte NLEN | `<NLEN> 90 00` |
| 6 | `00 B0 0002 NN` | READ BINARY the NDEF message | `<message> 90 00` |

### Capability Container (15 bytes)

```
00 0F        CCLEN = 15
20           Mapping version 2.0
00 F6        MLe  (max R-APDU data)
00 FF        MLc  (max C-APDU data)
04 06        NDEF File Control TLV: T=04, L=06
E1 04        NDEF file id
04 00        Max NDEF file size (1024)
00           Read access granted
FF           Write access denied (read-only)
```

### NDEF file

`NLEN` (2 bytes, big-endian length of the message) followed by the NDEF message.
A single-record message sets both the Message Begin and Message End flags.

- **Text record**: `D1 01 <plen> 54 <status> <lang> <utf8 text>`, where the
  status byte's low bits hold the language-code length.
- **URI record**: `D1 01 <plen> 55 <prefix code> <rest>`, where the prefix code
  abbreviates a known scheme (e.g. `04` = `https://`).

## Status words used

| SW | Meaning |
|----|---------|
| `9000` | Success |
| `6A82` | File / application not found |
| `6B00` | Wrong P1/P2 (bad offset) |
| `6700` | Wrong length |
| `6D00` | Instruction not supported |
| `6E00` | Class not supported |
| `6986` | Command not allowed (no file selected) |

## Why UID-only systems can't be emulated

Android's NFC controller performs low-level anticollision and assigns the
emulated tag's UID; an HCE app never sees or controls it. Access systems that
authorise purely on a card's UID therefore cannot be reproduced by an app on a
non-rooted device. Systems that rely on secret keys stored inside the card
(DESFire, iCLASS) also cannot be copied, because the secret never leaves the
card.
