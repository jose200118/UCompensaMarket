package com.grupoa.ucompensamarket

import android.text.format.DateFormat
import java.util.Calendar
import java.util.Locale

object Constantes {
    fun obteneTiempoD() : Long {
        return System.currentTimeMillis()
    }

    fun formatoFecha(tiempo: Long) : String {
        val calendar = Calendar.getInstance(Locale.ENGLISH)
        calendar.timeInMillis = tiempo

        return DateFormat.format("dd MMM yyyy", calendar).toString()
    }
}