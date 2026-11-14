package com.example.tuner.system;

// Berjalan di proses Shizuku (uid shell). Mengembalikan "<exitCode>\n<output>".
interface IShellService {
    void destroy() = 16777114; // kode transaksi wajib untuk Shizuku UserService
    String exec(in String[] argv) = 1;
}
