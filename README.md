# Location Demo Android App

Project Android native ini dibuat untuk testing dan demo internal di VS Code.

## Isi project

- Kotlin native Android
- Peta berbasis osmdroid
- Tap pada map untuk memilih titik demo
- Tombol untuk menerapkan titik demo di dalam aplikasi, tanpa mengubah GPS perangkat
- Build dilakukan di GitHub Actions

## Cara kerja

1. Edit source di VS Code.
2. Push ke GitHub.
3. Workflow GitHub Actions membangun APK debug otomatis.
4. APK bisa diambil dari artifacts untuk diuji di device Android.

## Workflow Release APK

Ada dua workflow tersedia:

- **android-build.yml**: Trigger otomatis pada setiap push. Build APK debug dan upload ke artifact.
- **release.yml**: Trigger manual atau pada push tag (misal `git tag v1.0.0`). Build APK debug & release, dan jika push tag, membuat GitHub Release dengan kedua APK attached untuk didownload.

Untuk membuat release:
```bash
git tag v1.0.0
git push origin v1.0.0
```

Atau trigger manual di tab "Actions" > "Release APK" > "Run workflow".

## Catatan testing

Project ini tidak memasang Android Studio, Android SDK, atau emulator di laptop lokal. Jika kamu ingin sideload langsung ke device via USB, kamu bisa memakai platform-tools standalone di luar project ini.
