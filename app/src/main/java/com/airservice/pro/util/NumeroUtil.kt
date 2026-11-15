package com.airservice.pro.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object NumeroUtil {

    private fun separador(miles: Boolean) =
        if (miles) '.' else ','

    private fun simbolos(miles: Boolean): DecimalFormatSymbols {
        val symbols = DecimalFormatSymbols(Locale.getDefault())
        symbols.groupingSeparator = separador(miles)
        symbols.decimalSeparator = '.'
        return symbols
    }

    /** "$ 35.000" - separador de miles con punto (formato local). */
    fun moneda(monto: String?): String {
        val valor = aNumero(monto)
        val df = DecimalFormat("#,##0", simbolos(true))
        return "$ ${df.format(valor)}"
    }

    /** "$ 35.000,50" */
    fun monedaDecimal(monto: String?): String {
        val valor = aNumero(monto)
        val df = DecimalFormat("#,##0.00", simbolos(true))
        return "$ ${df.format(valor)}"
    }

    /** Convierte el texto de un monto a numero. Acepta "$ 35.000", "35000,50", "35000.50". */
    fun aNumero(monto: String?): Double {
        if (monto.isNullOrBlank()) return 0.0
        var limpio = monto.replace(Regex("[^0-9,.-]"), "")
        if (limpio.isEmpty()) return 0.0

        val negativo = limpio.startsWith("-")
        limpio = limpio.removePrefix("-")
        if (limpio.isEmpty()) return 0.0

        val tieneComa = limpio.contains(',')
        val tienePunto = limpio.contains('.')

        val normalizado = when {
            tieneComa && tienePunto -> {
                // El separador decimal es el separador que aparece mas a la derecha
                if (limpio.lastIndexOf(',') > limpio.lastIndexOf('.')) {
                    limpio.replace(".", "").replace(',', '.')
                } else {
                    limpio.replace(",", "")
                }
            }
            tieneComa -> if (esSeparadorDeMiles(limpio, ',')) {
                limpio.replace(",", "")
            } else {
                limpio.replace(',', '.')
            }
            tienePunto -> if (esSeparadorDeMiles(limpio, '.')) {
                limpio.replace(".", "")
            } else {
                limpio
            }
            else -> limpio
        }

        val valor = normalizado.toDoubleOrNull() ?: 0.0
        return if (negativo) -valor else valor
    }

    /**
     * Un separador de miles es el unico de su tipo y le siguen exactamente
     * 3 digitos (35.000). Si le siguen 1 o 2 digitos es un decimal (35.5).
     */
    private fun esSeparadorDeMiles(limpio: String, separador: Char): Boolean {
        if (limpio.count { it == separador } != 1) return true
        return limpio.substringAfterLast(separador).length == 3
    }

    /** Normaliza un texto de monto a "35000.50" para persistir. */
    fun normalizar(monto: String?): String {
        val valor = aNumero(monto)
        return if (valor == valor.toLong().toDouble()) {
            valor.toLong().toString()
        } else {
            valor.toString()
        }
    }
}
