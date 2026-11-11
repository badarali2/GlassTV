# GlassTV Home

Apple TV style glass launcher for Android TV. No ads, no INTERNET permission, no tracking.

## Build the APK on GitHub (no Android Studio)
1. Push this folder to a GitHub repo (see upload commands below).
2. Open the repo > Actions > "Build APK" > latest run.
3. Download the `GlassTV-apk` artifact, unzip it, you get `app-debug.apk`.

## Upload with Git Bash (keeps folders intact)
    git init
    git add -A
    git commit -m "GlassTV launcher"
    git branch -M main
    git remote add origin https://github.com/YOUR_USERNAME/GlassTV.git
    git push -u origin main --force
