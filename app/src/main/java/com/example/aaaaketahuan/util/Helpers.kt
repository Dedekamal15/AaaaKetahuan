package com.example.aaaaketahuan.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun Double.toRupiah(): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(this).replace("Rp", "Rp ")
}

fun String.toDisplayDate(): String {
    return try {
        val date = LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE)
        date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID")))
    } catch (e: Exception) {
        this
    }
}
