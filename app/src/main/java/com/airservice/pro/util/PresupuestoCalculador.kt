package com.airservice.pro.util

/**
 * Calculo de un presupuesto: subtotal de renglones, recargos y total.
 *
 * Es logica pura (sin Android ni Room) para poder probarla en los tests JVM.
 */
object PresupuestoCalculador {

    const val MODO_PORCENTAJE = "Porcentaje"
    const val MODO_MONTO = "Monto"

    /** Un renglon del presupuesto. */
    data class Item(
        val concepto: String,
        val cantidad: Double,
        val precio: Double
    )

    /** Un recargo a aplicar sobre el subtotal. */
    data class Charge(
        val nombre: String,
        val modo: String,
        val valor: Double,
        val importe: Double
    )

    /** Resultado del calculo, listo para mostrar y para imprimir. */
    data class Resultado(
        val subtotal: Double,
        val recargos: List<Charge>,
        val totalRecargos: Double,
        val total: Double
    )

    /** Subtotal: suma de cantidad por precio unitario. */
    fun subtotal(items: List<Item>): Double =
        items.sumOf { it.cantidad * it.precio }

    /**
     * Importe de un recargo: porcentaje sobre el subtotal o monto fijo.
     * Un recargo de temporada solo se aplica entre octubre y marzo.
     */
    fun importeRecargo(
        subtotal: Double,
        modo: String,
        valor: Double,
        esTemporada: Boolean = false,
        mes: Int = mesActual()
    ): Double {
        if (esTemporada && !esTemporadaAlta(mes)) return 0.0
        return if (modo == MODO_MONTO) valor else subtotal * valor / 100.0
    }

    /** Calcula subtotal, recargos y total de una lista de renglones y recargos. */
    fun calcular(items: List<Item>, charges: List<Charge>): Resultado {
        val subtotal = subtotal(items)
        val totalRecargos = charges.sumOf { it.importe }
        return Resultado(
            subtotal = subtotal,
            recargos = charges,
            totalRecargos = totalRecargos,
            total = subtotal + totalRecargos
        )
    }

    /**
     * Temporada alta de climatizacion: de octubre a marzo (inclusive).
     * @param mes numero de mes (1 = enero).
     */
    fun esTemporadaAlta(mes: Int): Boolean = mes >= 10 || mes <= 3

    /** Mes en curso (1..12). */
    fun mesActual(): Int = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1

    /** Convierte el texto de un precio a numero. */
    fun precio(precio: String?): Double = NumeroUtil.aNumero(precio)

    /** Redondea a dos decimales para no arrastrar errores de coma flotante. */
    fun redondear(valor: Double): Double = Math.round(valor * 100.0) / 100.0
}