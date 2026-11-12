# Reels Blocker

Android app that leaves Instagram Reels the moment they open. It is an
accessibility service limited to `com.instagram.android`, so it only runs
while Instagram is on screen. No internet permission, no data collected.

## What it does
- Reels tab tapped: jumps back to the Home tab.
- A reel opened from the feed, Explore or a DM: presses Back.
- Everything else in Instagram (feed, stories, DMs, search) is untouched.

## Build the APK

### Option A: GitHub (works from a phone)
1. Create a new repository on github.com and upload every file of this project,
   keeping the folders (including `.github/workflows/build.yml`).
2. Open the repository's **Actions** tab and wait for "Build APK" to finish.
3. Open the finished run and download the **ReelsBlocker-apk** artifact; the zip
   contains `app-debug.apk`.

### Option B: Android Studio
Open this folder in Android Studio, let it sync, then
**Build > Build App Bundle(s) / APK(s) > Build APK(s)**.
The APK lands in `app/build/outputs/apk/debug/`.

### Option C: command line
With JDK 17, Gradle 8.7 and the Android SDK (platform 34) installed:
`gradle assembleDebug`

## Install and switch on
1. Install the APK (allow "install unknown apps" when asked).
2. Open Reels Blocker and tap **Open Accessibility settings**.
3. Choose Reels Blocker under installed/downloaded apps and turn it on.
4. Android 13 or newer, switch greyed out: Settings > Apps > Reels Blocker >
   three-dot menu > **Allow restricted settings**, then repeat step 3.

## If Instagram changes
Detection relies on Instagram's internal view ids (`clips_tab`,
`clips_viewer_view_pager`, `feed_tab`). If an Instagram update renames them,
edit the arrays at the top of `ReelsBlockerService.java`.
