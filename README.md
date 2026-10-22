# NusaStream

NusaStream is being developed as a separate Android app based on the public community Stremio Android source, rather than by reverse-engineering and repackaging the supplied APK.

## Current build path

The workflow **Build NusaStream from Stremio Android** checks out the upstream source and its submodules, applies NusaStream branding (application ID com.nusastream.app / display name NusaStream), adds the Android TV Leanback launcher category, then compiles debug APKs using JDK 21 and Android SDK 37.

### Build an APK

1. Open the Actions tab in this repository.
2. Select **Build NusaStream from Stremio Android**.
3. Choose **Run workflow**.
4. When the run completes successfully, download **NusaStream-Stremio-based-debug-APKs** from the run's Artifacts section.

The artifact includes APK outputs and SHA-256 checksums. These are debug builds, not release-signed APKs.

## Important status and limitations

- This workflow builds from the community source repository and submodules; it does not copy proprietary signing keys from the supplied APK.
- The app ID is changed to com.nusastream.app so NusaStream can be installed separately from Stremio.
- Android TV launcher visibility is added, but a complete TV-first remote/D-pad UI still needs testing and potentially further changes.
- This first build is a branded baseline. We must test sign-in, add-ons, playback, subtitles, and Android TV navigation before claiming full compatibility.
- Upstream source changes may require updating the branding patch in the workflow.
- Respect the upstream repository and submodule licences when redistributing builds.

## Source

- Community Android client: https://github.com/stremio-native/stremio-android
- Stremio core Kotlin: https://github.com/perpetus/stremio-core-kotlin
- Streaming server: https://github.com/perpetus/stream-server
