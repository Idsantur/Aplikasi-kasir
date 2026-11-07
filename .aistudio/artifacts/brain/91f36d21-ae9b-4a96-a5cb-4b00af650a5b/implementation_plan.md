# KasirKilat POS - Aplikasi Kasir & Manajemen Toko Serbaguna

Aplikasi Point of Sale (POS) modern, cepat, dan 100% offline-ready untuk Android yang memudahkan pencatatan transaksi kasir, manajemen stok inventaris, pembuatan struk digital siap kirim/cetak, serta analisis laba penjualan harian dan bulanan.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> Berdasarkan jawaban klarifikasi yang telah Anda pilih:
> - **Jenis Usaha**: Serbaguna / Fleksibel (Mendukung toko kelontong, retail, cafe/F&B, maupun penyedia jasa).
> - **Paket Fitur**: Paket Lengkap (Kasir Cepat, Manajemen Produk & Stok, Struk Digital Berbagi/Cetak, serta Laporan Penjualan & Laba).
> - **Penyimpanan**: 100% Penyimpanan Lokal di Perangkat (Room Database SQLite yang cepat, aman, dan tanpa ketergantungan koneksi internet).

---

## 1. Overview & Core Concept

- **Apa yang Dibangun**: Aplikasi Kasir lengkap (*Point of Sale*) dengan antarmuka Material 3 bernuansa emerald-fintech yang intuitif dan responsif.
- **Target Pengguna**: Pemilik UMKM, pedagang toko kelontong, kedai kopi/makanan, kios pulsa/jasa, dan kasir toko yang memerlukan pencatatan transaksi kilat tanpa ribet registrasi cloud.
- **Nilai Utama**:
  1. *Kecepatan Transaksi*: Tambah barang ke keranjang dengan 1 ketukan, pemilihan uang pecahan cepat (Uang Pas, Rp20.000, Rp50.000, Rp100.000), dan hitung kembalian otomatis.
  2. *Stok Terpantau*: Pengurangan stok otomatis saat transaksi berhasil, peringatan stok menipis (*low stock indicator*).
  3. *Struk Profesional*: Tampilan nota belanja autentik bergaya thermal receipt yang dapat dibagikan langsung ke WhatsApp pelanggan atau dicetak.
  4. *Analisis Keuangan Real-time*: Menampilkan total omset, keuntungan bersih (harga jual - harga modal), total transaksi, dan produk terlaris harian/bulanan.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Flow Kasir (Penjualan Cepat)**:
   - Layar utama menampilkan katalog produk dengan tab filter kategori (Semua, Makanan, Minuman, Sembako, Jasa, dll) dan bilah pencarian real-time.
   - Kasir mengetuk produk untuk memasukkan ke keranjang belanja (badge counter dinamis).
   - Mengetuk floating bar keranjang belanja untuk meninjau pesanan, mengubah jumlah item, atau menambahkan diskon dan pajak opsional.
   - Kasir menekan tombol "Bayar" -> Muncul dialog pembayaran dengan input uang tunai atau tombol cepat (Rp 10.000, Rp 50.000, Rp 100.000, dll) atau metode non-tunai (QRIS/Transfer).
   - Sistem memverifikasi pembayaran, menghitung kembalian, memotong stok barang, dan menampilkan lembar Struk Pembelian Sukses.
2. **Flow Struk & Riwayat**:
   - Menampilkan visual struk belanja lengkap dengan nama toko, alamat, daftar item, subtotal, diskon, pajak, total, uang diterima, dan kembalian.
   - Tombol "Bagikan Struk" mengirimkan teks nota rapi ke aplikasi pesan (WhatsApp, Telegram, SMS) atau siap disambungkan ke printer bluetooth.
3. **Flow Inventaris & Stok**:
   - Daftar barang dengan indikator stok (warna hijau jika aman, kuning/merah jika menipis).
   - Formulir tambah/edit barang: Nama barang, SKU/Barcode, Kategori, Harga Modal, Harga Jual, Stok Sekarang, dan Batas Minimum Stok.
4. **Flow Laporan & Keuangan**:
   - Kartu metrik: Total Omset, Total Laba Bersih, Rata-rata Nilai Transaksi, dan Jumlah Transaksi Hari Ini / Periode Dipilih.
   - Daftar produk paling laku (*Top Best Sellers*).
   - Log daftar invoice dengan rincian per transaksi.
5. **Flow Pengaturan Toko**:
   - Kustomisasi Nama Toko, Alamat, Nomor HP/WhatsApp, Catatan Kaki Struk (misal: "Barang yang dibeli tidak dapat ditukar").

### Visual Identity & Theme
- **Aesthetic Direction**: *Modern Emerald & Slate Fintech* — bersih, profesional, berkontras tinggi agar nyaman dipakai di bawah pencahayaan toko yang terang maupun redup.
- **Color Palette**:
  - Primary: Deep Emerald (`#00695C`) & Mint Accent (`#00BFA5`)
  - Surface: Crisp White (`#FFFFFF`) / Dark Slate (`#121816`) untuk dark mode
  - Warning/Stok Rendah: Warm Amber (`#FF8F00`)
  - Accent/Laba: Soft Teal (`#26A69A`)
- **Receipt Styling**: Kartu bergaya struk kasir (*thermal paper aesthetic*) dengan border zig-zag/perforated, font monospace untuk rincian harga, dan teks header/footer toko.

---

## 3. Key Product Decisions & Trade-Offs

- **Penyimpanan: Room SQLite Local Database**:
  - *Keputusan*: Menggunakan Room Database lokal Android dengan Flow terintegrasi.
  - *Alasan*: Menjawab kebutuhan 100% offline, instan tanpa latensi jaringan, tidak memerlukan login Google atau kuota data, serta data sepenuhnya tersimpan aman di perangkat pemilik toko.
- **Seeded Starter Data**:
  - *Keputusan*: Aplikasi menyertakan data awal contoh produk umum (makanan, minuman, sembako) saat pertama kali dibuka, sehingga pengguna bisa langsung mencoba transaksi tanpa harus menginput manual dari nol.
- **Manajemen Keranjang Reaktif**:
  - *Keputusan*: Keranjang belanja di-cache di dalam ViewModel state dengan update jumlah instan dan perhitungan subtotal otomatis tanpa query database berulang.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                       Jetpack Compose UI                    │
│ ┌───────────────┬──────────────┬─────────────┬────────────┐ │
│ │  Kasir Screen │ Produk/Stok  │ Riwayat/    │  Laporan   │ │
│ │  (Cart/Bayar) │ (CRUD/Alert) │ Struk View  │ (Omset/Top)│ │
│ └───────┬───────┴──────┬───────┴──────┬──────┴─────┬──────┘ │
│         │              │              │            │        │
│         ▼              ▼              ▼            ▼        │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │                   PosViewModel                          │ │
│ │ - cartState: Map<Product, Int>                          │ │
│ │ - activeCategory: String                                │ │
│ │ - dailyStats: StateFlow<SalesSummary>                   │ │
│ └─────────────────────────────┬───────────────────────────┘ │
└───────────────────────────────┼─────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                    PosRepository Layer                      │
│ ┌───────────────────────┬─────────────────────────────────┐ │
│ │ ProductDao            │ TransactionDao & ItemDao        │ │
│ │ - getAllProductsFlow  │ - insertTransactionWithItems()  │ │
│ │ - updateStock()       │ - getTransactionsWithItemsFlow()│ │
│ └───────────┬───────────┴─────────────────┬───────────────┘ │
└─────────────┼─────────────────────────────┼─────────────────┘
              │                             │
              ▼                             ▼
┌─────────────────────────────────────────────────────────────┐
│                 AppDatabase (Room Database)                 │
│  Tables: products, transactions, transaction_items, store  │
└─────────────────────────────────────────────────────────────┘
```

### Entity Schema & Models
1. **Product**: `id`, `name`, `barcode`, `category`, `costPrice`, `sellingPrice`, `stock`, `minStockAlert`, `createdAt`
2. **Transaction**: `id`, `invoiceNumber`, `timestamp`, `subtotal`, `discountAmount`, `taxAmount`, `grandTotal`, `totalCost`, `paymentMethod`, `amountPaid`, `changeAmount`, `customerNote`
3. **TransactionItem**: `id`, `transactionId`, `productId`, `productName`, `quantity`, `priceAtSale`, `costAtSale`
4. **StoreProfile**: `storeName`, `phone`, `address`, `receiptFooterNote`

### Interactive Component & State Mapping
- `PosCartBar`: Menampilkan badge total item dan total rupiah saat ada barang di keranjang; klik untuk membuka lembar pembayaran.
- `PaymentDialog`: Mendukung pilihan pembayaran Uang Pas atau pecahan cepat (Rp 10k, Rp 20k, Rp 50k, Rp 100k) serta menghitung kembalian seketika.
- `ShareReceiptHandler`: Menghasilkan representasi teks struk terformat rapi untuk Intent Android Share.
