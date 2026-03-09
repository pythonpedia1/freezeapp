# ❄️ FreezeApp — How to Build Your APK

## What This App Does
- Shows all apps installed on your phone
- Tick the apps you want to freeze (e.g. WhatsApp)
- Press **FREEZE** → those apps lose internet instantly
- You appear **OFFLINE** to them — messages tick once (sent, not delivered)
- Press **UNFREEZE** when you want to come back online
- A notification stays in your bar so you can unfreeze in one tap anytime

---

## Step 1 — Install Android Studio (Free, One Time)

1. Go to: **https://developer.android.com/studio**
2. Click **Download Android Studio**
3. Install it (just click Next/Next/Finish — default settings are fine)
4. When it opens, it will download some extra files automatically. Wait for it to finish.

---

## Step 2 — Open the FreezeApp Project

1. Extract the **FreezeApp.zip** file you downloaded
2. Open Android Studio
3. Click **"Open"** (or File → Open)
4. Navigate to the extracted **FreezeApp** folder
5. Click **OK**
6. Wait for it to finish loading (bottom bar will say "Gradle sync finished")

---

## Step 3 — Build the APK

1. Click the menu: **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. Wait about 1-2 minutes
3. A popup appears at the bottom right: **"APK(s) generated successfully"**
4. Click **"locate"** in that popup

This opens the folder where your APK is saved:
```
FreezeApp → app → build → outputs → apk → debug → app-debug.apk
```

---

## Step 4 — Install on Your Phone

**Option A — USB Cable:**
1. Connect your phone to your PC with a USB cable
2. On your phone, enable: **Settings → Developer Options → USB Debugging**
   *(To enable Developer Options: Settings → About Phone → tap "Build Number" 7 times)*
3. In Android Studio, click the ▶️ Play button at the top
4. Select your phone from the list
5. App installs automatically!

**Option B — Transfer the APK file:**
1. Copy the `app-debug.apk` file to your phone (via WhatsApp to yourself, Google Drive, USB, etc.)
2. On your phone, go to **Settings → Security → Allow Unknown Sources** (or "Install Unknown Apps")
3. Open the APK file on your phone
4. Tap **Install**

---

## Step 5 — First Time Using FreezeApp

1. Open **FreezeApp** on your phone
2. It will ask for **VPN permission** — tap **OK/Allow**
   *(This is NOT a real VPN — it never connects to any server. It stays 100% on your phone)*
3. Search for **WhatsApp** (or any app)
4. Tap it to tick it ✅
5. Press **❄️ FREEZE SELECTED APPS**
6. Done! WhatsApp now has zero internet — you appear offline.

---

## How to Unfreeze
- Open FreezeApp → tap **⚡ UNFREEZE ALL**
- OR pull down your notification bar → tap **Unfreeze** directly

---

## Common Questions

**"Will this drain my battery?"**
No. The VPN tunnel is local (stays on your phone). It uses almost no battery.

**"Can WhatsApp detect this?"**
No. To WhatsApp, it just looks like you have no internet connection.

**"Does it affect other apps?"**
Only the apps you tick get frozen. All other apps keep their normal internet.

**"Is my data sent anywhere?"**
No. This app has no internet of its own. Zero servers. Everything stays on your phone.

---

## Need Help?
If Android Studio shows any error, it's usually one of these:
- **"SDK not found"** → Android Studio → Tools → SDK Manager → install Android 14 (API 34)
- **"Gradle sync failed"** → Click the "Try Again" link that appears
- **"Emulator needed"** → You don't need an emulator, just build the APK and transfer it
