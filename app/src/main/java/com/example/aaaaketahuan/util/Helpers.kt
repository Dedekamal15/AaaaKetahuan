package com.example.aaaaketahuan.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun Double.toRupiah(): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(this).replace("Rp", "Rp ")
}

fun String.formatNominal(): String {
    if (this.isEmpty() || this == "0") return this
    val digitsOnly = this.filter { it.isDigit() }
    if (digitsOnly.isEmpty()) return ""
    val number = digitsOnly.toLongOrNull() ?: return this
    return NumberFormat.getNumberInstance(Locale("id", "ID")).format(number)
}

fun String.stripFormatNominal(): String {
    return this.filter { it.isDigit() }
}

fun String.toDisplayDate(): String {
    return try {
        val date = LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE)
        date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID")))
    } catch (e: Exception) {
        this
    }
}
