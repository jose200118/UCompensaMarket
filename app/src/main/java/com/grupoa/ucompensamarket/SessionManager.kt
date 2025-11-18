package com.grupoa.ucompensamarket

import android.content.Context

object SessionManager {

    fun getRol(context: Context): String {
        val prefs = context.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        return prefs.getString("ROL_USUARIO", "") ?: ""
    }

    fun isAdmin(context: Context): Boolean {
        return getRol(context) == "Admin"
    }

    fun isVendedor(context: Context): Boolean {
        return getRol(context) == "Vendedor"
    }

    fun isCliente(context: Context): Boolean {
        return getRol(context) == "Cliente"
    }
}
