package com.airservice.pro.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.airservice.pro.R

object WhatsAppHelper {

    /**
     * Prefijos de area Argentina que son siempre moviles. Solo se agregan
     * automaticamente; el resto debe ingresarse con el prefijo internacional.
     */
    private val AREAS_MOVILES = setOf("11", "15", "16", "17", "18", "19")

    /**
     * Normaliza un telefono a formato internacional sin signos, aceptando
     * formatos locales argentinos (+54, 0XX, 011 15 XXXXXXXX) y numeros con guiones.
     */
    fun normalizarTelefono(telefono: String?): String {
        if (telefono.isNullOrBlank()) return ""
        var digitos = telefono.filter { it.isDigit() }
        if (digitos.isEmpty()) return ""

        // Prefijo local (011 / 0XX) -> se quita el 0 inicial
        while (digitos.startsWith("0") && digitos.length > 10) {
            digitos = digitos.substring(1)
        }
        // Moviles locales con "15" (11 15 XXXXXXXX) -> se quita el 15
        val conQuince = Regex("^(\\d{2})15(\\d{8})$").find(digitos)
        if (conQuince != null) {
            digitos = conQuince.groupValues[1] + conQuince.groupValues[2]
        }
        // Numero de 10 digitos (sin pais) -> Argentina
        if (digitos.length == 10) {
            val esMovil = digitos.take(2) in AREAS_MOVILES
            digitos = "54" + (if (esMovil) "9" else "") + digitos
        }
        return digitos
    }

    /**
     * Envia un mensaje por WhatsApp al numero indicado.
     * @return true si se pudo abrir WhatsApp, false si no hay cliente instalado.
     */
    fun enviarWhatsApp(context: Context, telefono: String?, mensaje: String): Boolean {
        val numero = normalizarTelefono(telefono)
        if (numero.isEmpty()) {
            Toast.makeText(context, R.string.msg_no_phone, Toast.LENGTH_SHORT).show()
            return false
        }

        val url = Uri.parse("https://wa.me/$numero?text=${Uri.encode(mensaje)}")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, url))
            return true
        } catch (_: ActivityNotFoundException) {
            // No hay navegador ni WhatsApp: probar con el selector de apps
        }

        return try {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, mensaje)
            intent.setPackage("com.whatsapp")
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.msg_whatsapp_error, Toast.LENGTH_SHORT).show()
            false
        }
    }

    /** Abre el dialer con el numero cargado. */
    fun abrirLlamada(context: Context, telefono: String?): Boolean {
        val numero = telefono?.filter { it.isDigit() || it == '+' }.orEmpty()
        if (numero.isEmpty()) {
            Toast.makeText(context, R.string.msg_no_phone, Toast.LENGTH_SHORT).show()
            return false
        }
        return try {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$numero")))
            true
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.msg_error, Toast.LENGTH_SHORT).show()
            false
        }
    }
}
