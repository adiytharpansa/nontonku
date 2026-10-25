# NontonKu

Aplikasi nonton YouTube untuk Android, ditulis dari nol dengan Kotlin + Jetpack Compose.
Semua data dan stream diekstrak langsung di perangkat memakai NewPipeExtractor — tanpa server perantara, tanpa login.

## Fitur
- **Beranda** — trending + chip topik (Musik, Game, Film, Berita, Olahraga, Komedi, Kuliner)
- **Cari** — saran pencarian langsung + hasil dengan muat lanjutan
- **Putar** — pemain Media3 (DASH adaptif / HLS / progresif), deskripsi, video terkait, komentar
- **Channel** — profil channel + daftar videonya
- **Koleksi** — riwayat tontonan (otomatis) + Tandai (bookmark), tersimpan lokal

## Build
GitHub Actions → **Build Debug APK** → unduh artefak `nontonku-debug`.
Uji emulator: workflow **Verify in Emulator**. Rilis: workflow **Release APK** (GitHub Releases).
