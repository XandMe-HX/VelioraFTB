# 2.0.1

- GUI membedakan mode aktivasi berikutnya (gratis/berbayar) dan sumber sesi aktif.
- Mode gratis tidak lagi menampilkan harga uang sebagai biaya yang akan ditagih.
- Asal sesi baru disimpan di players.yml activation-mode per skill, bertahan setelah restart.
- Sesi sebelum update ditandai legacy/tidak tercatat: tidak menebak apakah sesi itu dahulu dibayar.
- Mengganti mode global tidak menghapus waktu sesi yang sudah aktif. Harga/mode baru berlaku pada aktivasi berikutnya.
- Klik sesi aktif masih mematikannya; GUI memperingatkan sisa waktunya akan hilang.
- Tidak mengubah saldo atau menciptakan pembayaran ulang untuk sesi yang masih aktif.

Backup dan restart server setelah mengganti JAR. Build berhasil; transaksi ekonomi langsung belum diuji dengan server live.
