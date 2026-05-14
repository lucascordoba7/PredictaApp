package com.lucas.predictaapp.ui.utils

import java.text.NumberFormat
import java.util.Locale

private val arsFormat = NumberFormat.getNumberInstance(Locale("es", "AR"))

fun Int.fmtArs(): String = arsFormat.format(this)

fun Int.fmtK(): String = when {
    this >= 1_000_000 -> "${this / 1_000_000},${(this % 1_000_000) / 100_000}M"
    this >= 1_000 -> "${this / 1_000}k"
    else -> fmtArs()
}
