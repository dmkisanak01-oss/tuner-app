# Tuner (clone clean-room, M1)

Status: **ditulis, belum pernah dikompilasi atau dijalankan.** Dibuat di lingkungan tanpa Android SDK dan tanpa akses Maven.

## Cara build pertama kali

1. Buka folder ini di Android Studio (Ladybug atau lebih baru). Android Studio akan membuat `local.properties` dan mengunduh dependensi.
2. Versi di `gradle/libs.versions.toml` ditulis dari memori. Kalau sync gagal, naikkan/turunkan versi AGP, Kotlin, KSP, dan Compose BOM agar kompatibel.
3. Ganti `com.example.tuner` (namespace, applicationId, folder `java/`) dengan package milik lu.
4. Pasang di HP Android 12+ dengan Shizuku berjalan (atau root), lalu ikuti Definition of Done M1 di `replica/build-log.md`.

## Isi M1 (vertical slice)

S02 akses (Shizuku/Root), S03 daftar game, S04 Game Mode + reset. Perintah: `cmd game mode`, `cmd game list-configs`, `cmd game reset`.
Semua perintah ditandai VERIFY di `replica/architecture.md` dan harus diuji di perangkat nyata.
