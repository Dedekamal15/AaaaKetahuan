package com.example.aaaaketahuan.util

import android.content.Context
import com.example.aaaaketahuan.data.model.Transaksi
import java.io.File
import java.io.FileWriter

object CsvExporter {

    fun export(list: List<Transaksi>, context: Context): File {
        val file = File(context.cacheDir, "export_transaksi.csv")
        FileWriter(file).use { writer ->
            // UTF-8 BOM for Windows Excel compatibility
            writer.write('\uFEFF')
            writer.write("id,tanggal,jenis,jumlah,namaBarang,keterangan,kategori,bulan,tahun,isSynced\n")
            list.forEach { t ->
                writer.write(
                    "${escapeCsv(t.id)},${escapeCsv(t.tanggal)},${escapeCsv(t.jenis)}," +
                    "${t.jumlah},${escapeCsv(t.namaBarang)},${escapeCsv(t.keterangan)}," +
                    "${escapeCsv(t.kategori)},${t.bulan},${t.tahun},${t.isSynced}\n"
                )
            }
        }
        return file
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
