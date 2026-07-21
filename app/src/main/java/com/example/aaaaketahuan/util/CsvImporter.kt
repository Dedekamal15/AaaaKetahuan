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
                    tahun = parts[8].toInt(),
                    metodeBayar = if (parts.size > 9) parts[9] else "",
                    sumber = if (parts.size > 10) parts[10] else "",
                    isSynced = if (parts.size > 11) parts[11].toBoolean() else false
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
        var i = 0

        while (i < line.length) {
            val char = line[i]
            when {
                char == '"' && !inQuotes -> {
                    // Opening quote
                    inQuotes = true
                }
                char == '"' && inQuotes -> {
                    // Could be closing quote or escaped quote
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        // Escaped quote "" inside quoted field → literal "
                        current.append('"')
                        i++ // Skip next quote
                    } else {
                        // Closing quote
                        inQuotes = false
                    }
                }
                char == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }
}
