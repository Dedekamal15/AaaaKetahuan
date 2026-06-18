package com.example.aaaaketahuan.util

import com.example.aaaaketahuan.data.model.Transaksi
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object JsonHelper {

    private val gson = Gson()

    fun bacaTransaksi(file: File): List<Transaksi> {
        return try {
            if (!file.exists()) return emptyList()
            val json = file.readText()
            if (json.isBlank()) return emptyList()
            val type = object : TypeToken<Array<Transaksi>>() {}.type
            gson.fromJson<Array<Transaksi>>(json, type).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun simpanTransaksi(file: File, list: List<Transaksi>) {
        try {
            val tempFile = File(file.parentFile, "transaksi_temp.json")
            tempFile.writeText(gson.toJson(list))
            tempFile.renameTo(file)
        } catch (e: Exception) {
            throw e
        }
    }

    fun bacaFrekuensi(file: File): MutableMap<String, Int> {
        return try {
            if (!file.exists()) return mutableMapOf()
            val json = file.readText()
            if (json.isBlank()) return mutableMapOf()
            val type = object : TypeToken<Map<String, Int>>() {}.type
            gson.fromJson<Map<String, Int>>(json, type).toMutableMap()
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    fun simpanFrekuensi(file: File, map: Map<String, Int>) {
        try {
            val tempFile = File(file.parentFile, "nama_barang_freq_temp.json")
            tempFile.writeText(gson.toJson(map))
            tempFile.renameTo(file)
        } catch (e: Exception) {
            throw e
        }
    }
}
