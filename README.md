# Yourtube

Aplikasi pemutar YouTube untuk Android, ditulis dari nol dengan Kotlin + Jetpack Compose.
Semua data dan stream diekstrak langsung di perangkat memakai NewPipeExtractor — tanpa server perantara, tanpa login.

## Fitur
- **Beranda** — trending + chip topik (Musik, Game, Film, Berita, Olahraga, Komedi, Kuliner)
- **Cari** — saran pencarian langsung + hasil dengan muat lanjutan
- **Putar** — pemain Media3 dengan:
  - kualitas otomatis (HLS/DASH adaptif) atau manual hingga 1080p+ (video + audio digabung)
  - cadangan otomatis ke sumber lain bila satu sumber gagal, di posisi yang sama
  - layar penuh (lanskap, bilah sistem disembunyikan), ketuk dua kali = maju/mundur 10 detik
  - dijeda otomatis saat aplikasi ke latar; deskripsi, video terkait, komentar
- **Channel** — profil channel + daftar videonya
- **Koleksi** — riwayat tontonan (otomatis) + Tandai (bookmark), tersimpan lokal

## Struktur proyek
```
app/src/main/java/com/app/yourtube/
├── MainActivity.kt           # titik masuk
├── YourtubeApplication.kt    # pemanasan mesin ekstraksi
├── data/                     # model, repositori (NewPipeExtractor), unduhan, penyimpanan lokal
├── player/                   # PlayerController: sumber media, kualitas, fallback
├── theme/                    # warna & tema Material 3
└── ui/                       # ViewModel, navigasi, dan layar (Home, Search, Watch, Channel, Library)
```

## Build
GitHub Actions → **Build Debug APK** → unduh artefak `yourtube-debug`.
Uji emulator: workflow **Verify in Emulator**. Rilis: workflow **Release APK** (GitHub Releases).

- Application ID: `com.app.yourtube`
- Android 7.0+ (minSdk 24)
