package com.example.aaaaketahuan.util

import com.example.aaaaketahuan.data.model.Transaksi
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

data class ImportResult(
    val transaksiList: List<Transaksi>,
    val successCount: Int,
    val failCount: Int
)

object CsvImporter {

    fun import(inputStream: InputStream): ImportResult {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val lines = reader.readLines()
        if (lines.isEmpty()) return ImportResult(emptyList(), 0, 0)

        // Skip header (first line)
        val dataLines = lines.drop(1)
        val transaksiList = mutableListOf<Transaksi>()
        var failCount = 0

        for (line in dataLines) {
            try {
                val parts = parseCsvLine(line)
                if (parts.size < 9) {
                    failCount++
                    continue
                }
                val transaksi = Transaksi(
                    id = parts[0].ifBlank { java.util.UUID.randomUUID().toString() },
                    tanggal = parts[1],
                    jenis = parts[2],
                    jumlah = parts[3].toDouble(),
                    namaBarang = parts[4],
                    keterangan = parts[5],
                    kategori = parts[6],
                    bulan = parts[7].toInt(),
                    tahun = parts[8].toInt()
                )
                transaksiList.add(transaksi)
            } catch (e: Exception) {
                failCount++
            }
        }

        return ImportResult(transaksiList, transaksiList.size, failCount)
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString().trim())
        return result
    }
}
