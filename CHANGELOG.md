# AEmulator Kazutchi 0.0.0.9

**🇬🇧 English** · [🇯🇵 日本語](docs/i18n/CHANGELOG.ja.md)

## 0.0.0.9 — ANGLE graphics integration

### Graphics and performance
- Added an Android 17+ system ANGLE preference for supported GLES rendering paths.
- Routed the modern KK guest (Android 4.4 / API 19 and newer) through the in-process GLES host on supported Android 17+ devices, allowing the platform ANGLE driver preference to apply.
- Kept the isolated renderer as a compatibility fallback when the direct renderer cannot start.
- Added renderer diagnostics so logs distinguish confirmed ANGLE renderer strings from an unconfirmed ANGLE preference.
- Built and packaged the 32-bit guest GLES bridge required by the KK engine.

### Compatibility and reliability
- Preserved the isolated `glserverd` route for legacy GB and older KK guest configurations.
- Kept the existing application ID, update signing identity, and user data in place.
- Updated the release pipeline to build, verify, sign, and publish versioned APK releases with release notes.

### Important limitations
- Android's system ANGLE preference is not a guarantee that ANGLE will be selected; the active renderer must be checked in device logs.
- Vulkan backend use and performance improvements have not yet been confirmed on physical hardware.
- Legacy GB and KK guests below API 19 are not migrated to the ANGLE-preferred route.
- The release has passed CI build and packaging checks; physical-device stability and performance testing remain outstanding.
