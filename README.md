# VelioraFTB

Plugin Paper 1.21.8 untuk tiga skill berbatas waktu:

1. **Vein Miner** — menambang kumpulan ore sejenis yang saling terhubung.
2. **Tree Feller** — menebang satu struktur pohon, bukan satu kali pakai; daun alaminya ikut rontok.
3. **Farmer** — memanen tanaman matang dan langsung menanamnya kembali.

## Requirements
- **Server:** Paper 1.21.8 or higher
- **Java:** Java 21
- **Economy:** Vault & an active economy plugin (e.g., EssentialsX)

## Commands
- `/velioraftb` — membuka GUI pembelian skill.
- `/velioraftb status` — melihat sisa waktu semua skill.
- `/velioraftb reload` — memuat ulang seluruh `config.yml`.

## Features
- Harga default setiap skill: `2.500` melalui Vault.
- Durasi default setiap pembelian: `5 jam`.
- Skill tidak bisa dibeli ulang sebelum waktunya berakhir.
- Pengingat sisa waktu default setiap `30 menit`.
- Semua harga, durasi, batas blok, tool, ore, crop, dunia, GUI, dan pesan dapat diubah dari `config.yml`.
- Tree Feller memiliki suara tebang dan animasi cincin partikel yang mengelilingi tinggi pohon.
- Sound, jenis partikel, radius, kepadatan, dan durasi efek Tree Feller dapat diatur dari `config.yml`.
- Data lama dari folder `plugins/VelioraVein` dapat dimigrasikan otomatis.
