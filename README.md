# Location Demo Android App

Project Android native ini dibuat untuk testing dan demo internal di VS Code.

## Fitur

- Kotlin native Android dengan OSMDroid map (OpenStreetMap)
- Tap map untuk memilih titik demo
- GPS real-time: akses lokasi perangkat dengan indikator marker
- Tombol kompak gaya Google Maps:
  - ◉ Kembali ke lokasi sekarang
  - \+ Zoom in
  - − Zoom out
- Tombol "Start" kecil di tengah bawah untuk menerapkan titik demo
- Mode debug untuk testing internal tanpa mengubah GPS perangkat
- Warning reminder untuk aktifkan "Izinkan lokasi palsu" di Developer Options

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

## Catatan penting: Developer Options

Aplikasi ini memerlukan pengaturan di device Android:

1. Buka **Settings** → **About phone**
2. Tap **Build number** 7 kali untuk unlock Developer Options
3. Buka **Settings** → **Developer Options**
4. Aktifkan **Select mock location app** dan pilih **Location Demo** sebagai app untuk mock location
5. Atau cari opsi **"Izinkan lokasi palsu"** dan aktifkan untuk app ini

Tanpa setting ini, fitur GPS mock tidak akan bekerja.

## Instalasi APK ke device

```bash
adb install path/to/app-debug.apk
```

Pastikan device sudah terhubung USB dan USB Debugging aktif.

## Permission

- `INTERNET`: Untuk load map tiles
- `ACCESS_FINE_LOCATION`: Untuk GPS presisi tinggi
- `ACCESS_COARSE_LOCATION`: Alternatif GPS dengan presisi rendah
