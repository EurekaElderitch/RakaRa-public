# Audit Report — RakaRa
Tanggal: 2026-10-28  
Commit: `85da6e8`

## 1. Executive Summary
Audit statis menunjukkan pemisahan modul yang cukup jelas antara aplikasi Compose, `engine-manager`, `source-api`, dan engine yang dimuat secara dinamis. Pada jalur yang ditinjau tidak terlihat circular dependency yang terbukti; akses repository secara umum disalurkan melalui `RakaraApp` dan `EngineRepository`. Namun, semua temuan di bawah adalah observasi dari source/config yang ada, bukan hasil eksekusi build atau aplikasi.

Terdapat 3 temuan P1, 5 temuan P2, dan 3 temuan P3. Area paling berisiko adalah keamanan signing dan distribusi kode engine: kredensial release tertulis langsung di Gradle, arsip engine diekstrak tanpa validasi path, dan jalur distribusi engine mengizinkan HTTP serta memakai checksum yang berada di dalam arsip yang sama.

Di sisi kestabilan, mutasi state repository berbasis file dapat berjalan bersamaan tanpa sinkronisasi; kesalahan parsing data lokal disamarkan sebagai daftar kosong. Alur update APK juga belum memiliki konfigurasi `FileProvider` yang sesuai di resource/manifest yang diperiksa, dan aksi Cancel tidak membatalkan coroutine unduhan.

Pada UI, beberapa Flow DataStore dikoleksi tanpa lifecycle-aware collector, dan sejumlah lazy list dinamis tidak memasang key stabil. Progress baca belum disimpan selain selalu `lastPage = 0`. Tidak ditemukan source test (`src/test`/`src/androidTest`) dalam hasil inventaris, jadi perilaku installer, persistence, updater, dan ViewModel tidak memiliki bukti test otomatis dalam workspace yang diaudit.

Audit ini read-only dan bersifat statis. Worktree telah memiliki perubahan lokal pada `app/src/main/kotlin/dev/rakara/app/ui/SplashScreen.kt`; perubahan itu tidak diubah dan tidak dibandingkan/ditinjau terhadap commit. `secrets.json` tidak dibuka. Tidak dilakukan build atau eksekusi aplikasi.

## 2. Ringkasan Temuan
| ID | Prioritas | Kategori | File:Baris | Ringkasan |
|---|---|---|---|---|
| AUD-01 | P1 | Security / Signing | `app/build.gradle.kts:23-25` | Password keystore release tersimpan hardcoded. |
| AUD-02 | P1 | Security / Engine | `engine-manager/src/main/kotlin/dev/rakara/engine/EngineInstaller.kt:162-167` | Ekstraksi ZIP tidak memvalidasi bahwa path entry tetap di dalam staging directory. |
| AUD-03 | P1 | Security / Network | `engine-manager/src/main/kotlin/dev/rakara/engine/EngineUpdater.kt:59`; `app/src/main/AndroidManifest.xml:13` | HTTP diizinkan untuk URL engine, dan aplikasi mengaktifkan cleartext traffic global. |
| AUD-04 | P2 | APK updater | `app/src/main/res/xml/file_paths.xml:2`; `app/src/main/kotlin/dev/rakara/app/update/ApkUpdater.kt:163` | FileProvider digunakan, tetapi paths kosong dan provider tidak didaftarkan di manifest aplikasi yang diperiksa. |
| AUD-05 | P2 | State / Concurrency | `app/src/main/kotlin/dev/rakara/app/data/LibraryRepository.kt:34-38` | Mutasi StateFlow dan persist file tidak diserialisasi; operasi paralel dapat kehilangan update. |
| AUD-06 | P2 | Error handling / Data loss | `app/src/main/kotlin/dev/rakara/app/data/LibraryRepository.kt:47-50` | Kegagalan parsing data dibuang dan diperlakukan sebagai data kosong. |
| AUD-07 | P2 | State / Reader | `app/src/main/kotlin/dev/rakara/app/vm/ReaderViewModel.kt:83-84`; `:104` | Progress selalu disimpan sebagai halaman 0; callback perubahan halaman kosong. |
| AUD-08 | P2 | Coroutine / APK updater | `app/src/main/kotlin/dev/rakara/app/MainActivity.kt:271,280-283` | Tombol Cancel hanya mengubah UI; job download tetap berjalan. |
| AUD-09 | P3 | Compose / Lifecycle | `app/src/main/kotlin/dev/rakara/app/ui/ProfileScreen.kt:33-35` | Flow DataStore dikoleksi dengan `collectAsState`, bukan collector lifecycle-aware. |
| AUD-10 | P3 | Compose / Lazy list | `app/src/main/kotlin/dev/rakara/app/ui/SearchScreen.kt:156`; `app/src/main/kotlin/dev/rakara/app/ui/UpdatesScreen.kt:93` | Lazy list hasil dinamis tidak memberikan key stabil. |
| AUD-11 | P3 | Dependencies | `app/build.gradle.kts:75-88` | Ada deklarasi dependency duplikat dan dependency networking yang tidak direferensikan oleh source app. |

## 3. Detail per Prioritas

### P1

### [AUD-01] Password signing release hardcoded
- **Lokasi**: `app/build.gradle.kts:23-25`
- **Bukti**:
  ```kotlin
            storePassword = "rakara123"
            keyAlias = "rakara"
            keyPassword = "rakara123"
  ```
- **Dampak**: Siapa pun yang dapat membaca source dapat mengetahui password yang dipakai konfigurasi signing release. Jika keystore terkait dipakai untuk publikasi, kredensial dan keystore perlu dianggap terekspos sampai diverifikasi.
- **Prioritas**: P1

### [AUD-02] Path entry ZIP tidak dibatasi saat ekstraksi engine
- **Lokasi**: `engine-manager/src/main/kotlin/dev/rakara/engine/EngineInstaller.kt:162-167`
- **Bukti**:
  ```kotlin
    private fun extract(zip: ZipFile, to: File) {
        for (e in zip.entries()) {
            val t = File(to, e.name)
            if (e.isDirectory) { t.mkdirs(); continue }
  ```
- **Dampak**: Nama entry seperti `../...` dapat mengarahkan output di luar staging directory dan menimpa file lain yang dapat ditulis oleh proses aplikasi. SHA-256 `classes.dex` tidak membatasi nama path entry arsip lainnya.
- **Prioritas**: P1

### [AUD-03] Jalur distribusi engine menerima HTTP dan checksum tidak mengautentikasi sumber
- **Lokasi**: `engine-manager/src/main/kotlin/dev/rakara/engine/EngineUpdater.kt:59`; `engine-manager/src/main/kotlin/dev/rakara/engine/EngineInstaller.kt:152-155`; `app/src/main/AndroidManifest.xml:13`
- **Bukti**:
  ```kotlin
        if (entry.url.startsWith("http://") || entry.url.startsWith("https://")) {
            return entry.url
        }
  ```
  ```kotlin
            val bytes = zip.getInputStream(dexEntry).readBytes()
            val actual = sha256(bytes)
            require(actual == manifest.sha256) {
  ```
- **Dampak**: URL `http://` diterima untuk paket kode dinamis. Digest yang diperiksa berasal dari `manifest.json` dalam paket yang sama, sehingga tidak membuktikan keaslian paket jika transport/sumber paket dapat diganti. Manifest aplikasi juga mengizinkan cleartext traffic untuk seluruh aplikasi.
- **Prioritas**: P1

### P2

### [AUD-04] Konfigurasi FileProvider untuk pemasangan APK tidak tersedia
- **Lokasi**: `app/src/main/res/xml/file_paths.xml:2`; `app/src/main/kotlin/dev/rakara/app/update/ApkUpdater.kt:163`; `app/src/main/AndroidManifest.xml:7-18`
- **Bukti**:
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <paths/>
  ```
- **Dampak**: `installApk()` meminta URI dari authority `${packageName}.fileprovider`, tetapi XML path yang dipakai kosong dan manifest aplikasi yang diperiksa tidak mendeklarasikan provider tersebut. Alur pemasangan berpotensi gagal saat meminta URI. **PERLU VERIFIKASI MANUAL** terhadap manifest hasil merge/dependency dan konfigurasi runtime yang terpasang.
- **Prioritas**: P2

### [AUD-05] Mutasi repository file dapat kehilangan update bersamaan
- **Lokasi**: `app/src/main/kotlin/dev/rakara/app/data/LibraryRepository.kt:34-38`; pola serupa ada di `NotificationRepository` dan `ReadMarkRepository`.
- **Bukti**:
  ```kotlin
      suspend fun add(entry: LibraryEntry) = withContext(Dispatchers.IO) {
          if (contains(entry.sourceId, entry.mangaUrl)) return@withContext
          _state.value = _state.value + entry
          persist()
      }
  ```
- **Dampak**: Operasi dapat dipanggil dari beberapa coroutine; tidak ada mutex/serialisasi untuk rangkaian read-modify-write dan `file.writeText`. Dua operasi paralel dapat menghitung state dari nilai lama lalu salah satu perubahan tertimpa, serta dapat menulis file secara bersamaan.
- **Prioritas**: P2

### [AUD-06] Korupsi/ketidakcocokan JSON diam-diam dianggap sebagai data kosong
- **Lokasi**: `app/src/main/kotlin/dev/rakara/app/data/LibraryRepository.kt:47-50`; pola serupa ada di `NotificationRepository` dan `ReadMarkRepository`.
- **Bukti**:
  ```kotlin
      private fun load(): List<LibraryEntry> = runCatching {
          if (!file.exists()) emptyList()
          else json.decodeFromString<List<LibraryEntry>>(file.readText())
      }.getOrDefault(emptyList())
  ```
- **Dampak**: File yang korup atau tidak kompatibel tampak seperti library kosong; alasan kegagalan tidak dilaporkan. Mutasi berikutnya dapat menulis state kosong ke file, menghilangkan data yang masih mungkin dipulihkan.
- **Prioritas**: P2

### [AUD-07] Progress membaca tidak pernah tersimpan
- **Lokasi**: `app/src/main/kotlin/dev/rakara/app/vm/ReaderViewModel.kt:83-84,104`
- **Bukti**:
  ```kotlin
                      chapterName = safeChapterName,
                      lastPage = 0
  ```
  ```kotlin
      fun onPageChanged(page: Int) {}
  ```
- **Dampak**: Setiap pembukaan chapter menyimpan halaman nol, dan perubahan halaman tidak memperbarui read mark. Fitur lanjut membaca tidak dapat memulihkan posisi halaman.
- **Prioritas**: P2

### [AUD-08] Cancel unduhan hanya mengubah flag UI
- **Lokasi**: `app/src/main/kotlin/dev/rakara/app/MainActivity.kt:271,280-283`
- **Bukti**:
  ```kotlin
                      Button(onClick = { downloading = false }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
  ```
  ```kotlin
                          scope.launch {
                              downloading = true
                              val file = updater.downloadApk(info) { dl, total -> if (total > 0) progress = dl.toFloat() / total }
  ```
- **Dampak**: Tombol tidak membatalkan `Job` atau request OkHttp. Transfer dan penulisan file dapat berlanjut setelah UI kembali ke state non-downloading; callback masih dapat memperbarui state dialog.
- **Prioritas**: P2

### P3

### [AUD-09] Collector DataStore tidak mengikuti lifecycle UI
- **Lokasi**: `app/src/main/kotlin/dev/rakara/app/ui/ProfileScreen.kt:33-35`; pola serupa ada di `HomeScreen.kt` dan `MainActivity.kt`.
- **Bukti**:
  ```kotlin
      val name by prefsRepo.profileName.collectAsState(initial = "User Raka")
      val photoUri by prefsRepo.profilePhotoUri.collectAsState(initial = null)
      val isDarkMode by prefsRepo.isDarkMode.collectAsState(initial = false)
  ```
- **Dampak**: Flow dikoleksi selama composable masih berada di Composition, termasuk saat lifecycle UI tidak aktif. Ini dapat mempertahankan pekerjaan/rekomposisi yang tidak diperlukan di background. StateFlow ViewModel pada layar lain sudah memakai `collectAsStateWithLifecycle`.
- **Prioritas**: P3

### [AUD-10] Lazy list dinamis tidak menggunakan key stabil
- **Lokasi**: `app/src/main/kotlin/dev/rakara/app/ui/SearchScreen.kt:156`; `app/src/main/kotlin/dev/rakara/app/ui/UpdatesScreen.kt:93`; juga terlihat pada beberapa lazy list lain.
- **Bukti**:
  ```kotlin
                    items(results) { m ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { nav.navigate("manga/eng01/${encode(m.url)}") }.padding(vertical = 8.dp),
  ```
- **Dampak**: Saat konten berubah/bertambah, Compose menggunakan posisi item untuk mengidentifikasi elemen. Ini dapat memicu pekerjaan ulang dan mempertahankan state item pada identitas yang keliru bila konten bergeser.
- **Prioritas**: P3

### [AUD-11] Dependency berulang dan dependency networking tidak terpakai
- **Lokasi**: `app/build.gradle.kts:66-88`
- **Bukti**:
  ```kotlin
        implementation("com.squareup.retrofit2:retrofit:2.11.0")
        implementation("com.squareup.retrofit2:converter-gson:2.11.0")
        implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
  ```
- **Dampak**: Pencarian source `app/src/main` tidak menemukan pemakaian Retrofit, Gson converter, atau logging interceptor. Deklarasi `navigation-compose`, `lifecycle-viewmodel-compose`, dan `coil-compose` juga berulang di blok dependencies. Ini menambah dependency/transitive surface dan menyulitkan perawatan versi.
- **Prioritas**: P3

## Catatan Cakupan
- Tidak ditemukan folder source test `src/test` atau `src/androidTest` pada inventaris file workspace.
- Permission manifest yang terlihat hanya `INTERNET` dan `ACCESS_NETWORK_STATE`; tidak ditemukan deep link pada activity. `android:exported="true"` berada pada activity launcher dengan MAIN/LAUNCHER intent-filter.
- Engine menyediakan `close()` dan `EngineManager.unload()` memanggilnya; implementasi engine `eng01` menutup executor dan connection pool OkHttp. `DexClassLoader` sendiri tidak memiliki pemanggilan close pada jalur yang ditinjau.
- Keberadaan/kepekaan nilai di `secrets.json`, dampak kredensial signing terhadap keystore aktif, manifest hasil merge, dan konfigurasi runtime di luar source yang diperiksa memerlukan verifikasi pemilik proyek. Nilai `secrets.json` tidak dibaca.
