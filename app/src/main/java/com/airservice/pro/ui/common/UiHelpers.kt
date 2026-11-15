package com.airservice.pro.ui.common

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.airservice.pro.R

/** Utilidades compartidas por los fragmentos. */
object UiHelpers {

    /**
     * Conecta un AutoCompleteTextView como desplegable de seleccion.
     * @param items lista de elementos a mostrar
     * @param label funcion que produce el texto visible de cada elemento
     * @param onSelected callback con el elemento elegido (o null si se limpia)
     */
    fun <T> AutoCompleteTextView.bindDropdown(
        items: List<T>,
        label: (T) -> String,
        onSelected: (T?) -> Unit
    ) {
        val adapter = ArrayAdapter(context, R.layout.item_dropdown, items.map(label))
        setAdapter(adapter)
        setOnItemClickListener { _, _, position, _ ->
            if (position in items.indices) {
                onSelected(items[position])
            }
        }
        if (text.isNullOrBlank()) {
            setText("", false)
        }
    }

    /** Muestra u oculta el estado vacio segun corresponda. */
    fun emptyState(emptyView: View, isEmpty: Boolean) {
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    /** Aplica el color de estado a un badge (fondo translucido + texto). */
    fun applyBadge(badge: android.widget.TextView, colorRes: Int) {
        val color = ContextCompat.getColor(badge.context, colorRes)
        badge.backgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, 38))
        badge.setTextColor(color)
    }

    /** Resuelve el color de un estado de documento. */
    fun colorEstado(estado: String): Int = when (estado) {
        "Completado", "Aprobado", "Pagado" -> R.color.verde
        "Pendiente de aprobación", "Pendiente", "En espera", "Sin iniciar" -> R.color.naranja
        "Enviado", "En proceso", "Confirmado" -> R.color.celeste
        "Rechazado", "Cancelado" -> R.color.rojo
        else -> R.color.textSecondary
    }

    /** Color de avance: rojo -> naranja -> cian -> verde. */
    fun colorAvance(avance: Int): Int = when {
        avance >= 100 -> R.color.verde
        avance >= 60 -> R.color.celeste
        avance >= 30 -> R.color.naranja
        else -> R.color.rojo
    }
}
