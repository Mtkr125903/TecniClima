package com.airservice.pro.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fechas en ISO (yyyy-MM-dd HH:mm) para ordenamiento lexicografico correcto en SQLite.
 * Se muestran en formato local (dd/MM/yyyy).
 */
object FechaUtil {

    private const val PATRON_ISO = "yyyy-MM-dd HH:mm"
    private const val PATRON_ISO_DIA = "yyyy-MM-dd"
    private const val PATRON_MOSTRAR = "dd/MM/yyyy"
    private const val PATRON_MOSTRAR_HORA = "dd/MM/yyyy HH:mm"

    private fun formato(patron: String) = SimpleDateFormat(patron, Locale.getDefault())

    fun ahoraIso(): String = formato(PATRON_ISO).format(Date())

    fun hoyIso(): String = formato(PATRON_ISO_DIA).format(Date())

    fun ahoraMostrar(): String = formato(PATRON_MOSTRAR_HORA).format(Date())

    fun mostrarDia(): String = formato(PATRON_MOSTRAR).format(Date())

    fun iso(fecha: Date): String = formato(PATRON_ISO).format(fecha)

    fun isoDia(fecha: Date): String = formato(PATRON_ISO_DIA).format(fecha)

    /** Convierte un valor ISO a texto legible. Si no es ISO, lo devuelve sin tocar. */
    fun mostrar(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        val date = parseIso(iso) ?: return iso
        val patron = if (iso.length <= 10) PATRON_MOSTRAR else PATRON_MOSTRAR_HORA
        return formato(patron).format(date)
    }

    /** Dia (yyyy-MM-dd) a texto corto (dd/MM). */
    fun mostrarDiaCorto(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        val date = parseIso(iso) ?: return iso
        return SimpleDateFormat("dd/MM", Locale.getDefault()).format(date)
    }

    fun parseIso(iso: String): Date? = try {
        if (iso.length <= 10) {
            formato(PATRON_ISO_DIA).parse(iso)
        } else {
            formato(PATRON_ISO).parse(iso)
        }
    } catch (_: Exception) {
        null
    }

    /** Devuelve true si el ISO corresponde al dia de hoy. */
    fun esHoy(iso: String?): Boolean =
        !iso.isNullOrBlank() && iso.substring(0, minOf(10, iso.length)) == hoyIso()

    /** Suma (o resta) dias a un dia ISO (yyyy-MM-dd) y devuelve el nuevo dia ISO. */
    fun sumarDias(isoDia: String, dias: Int): String {
        val base = parseIso(isoDia) ?: Date()
        val calendar = java.util.Calendar.getInstance()
        calendar.time = base
        calendar.add(java.util.Calendar.DAY_OF_YEAR, dias)
        return isoDia(calendar.time)
    }

    /** Dia de la semana en texto corto para la cabecera de agenda. */
    fun nombreDia(isoDia: String): String {
        val date = parseIso(isoDia) ?: return ""
        return SimpleDateFormat("EEEE", Locale.getDefault())
            .format(date)
            .replaceFirstChar { it.uppercase(Locale.getDefault()) }
    }
}
