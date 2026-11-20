package com.airservice.pro.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresupuestoCalculadorTest {

    private fun item(cantidad: Double, precio: Double) =
        PresupuestoCalculador.Item("Concepto", cantidad, precio)

    @Test
    fun `el subtotal suma cantidad por precio`() {
        val items = listOf(item(2.0, 1000.0), item(1.0, 2500.0))
        assertEquals(4500.0, PresupuestoCalculador.subtotal(items), 0.001)
    }

    @Test
    fun `el subtotal de una lista vacia es cero`() {
        assertEquals(0.0, PresupuestoCalculador.subtotal(emptyList()), 0.001)
    }

    @Test
    fun `un recargo por porcentaje se calcula sobre el subtotal`() {
        val importe = PresupuestoCalculador.importeRecargo(
            subtotal = 10000.0,
            modo = PresupuestoCalculador.MODO_PORCENTAJE,
            valor = 15.0
        )
        assertEquals(1500.0, importe, 0.001)
    }

    @Test
    fun `un recargo por monto no depende del subtotal`() {
        val importe = PresupuestoCalculador.importeRecargo(
            subtotal = 10000.0,
            modo = PresupuestoCalculador.MODO_MONTO,
            valor = 5000.0
        )
        assertEquals(5000.0, importe, 0.001)
    }

    @Test
    fun `un recargo de temporada no se aplica en abril`() {
        val importe = PresupuestoCalculador.importeRecargo(
            subtotal = 10000.0,
            modo = PresupuestoCalculador.MODO_PORCENTAJE,
            valor = 20.0,
            esTemporada = true,
            mes = 4
        )
        assertEquals(0.0, importe, 0.001)
    }

    @Test
    fun `un recargo de temporada se aplica en enero y en octubre`() {
        assertEquals(
            2000.0,
            PresupuestoCalculador.importeRecargo(
                10000.0, PresupuestoCalculador.MODO_PORCENTAJE, 20.0, esTemporada = true, mes = 1
            ),
            0.001
        )
        assertEquals(
            2000.0,
            PresupuestoCalculador.importeRecargo(
                10000.0, PresupuestoCalculador.MODO_PORCENTAJE, 20.0, esTemporada = true, mes = 10
            ),
            0.001
        )
    }

    @Test
    fun `la temporada alta va de octubre a marzo`() {
        listOf(10, 11, 12, 1, 2, 3).forEach {
            assertTrue("mes $it deberia ser temporada alta", PresupuestoCalculador.esTemporadaAlta(it))
        }
        listOf(4, 5, 6, 7, 8, 9).forEach {
            assertFalse("mes $it no deberia ser temporada alta", PresupuestoCalculador.esTemporadaAlta(it))
        }
    }

    @Test
    fun `el calculo suma el subtotal con los recargos`() {
        val recargo = PresupuestoCalculador.Charge(
            nombre = "Urgencia",
            modo = PresupuestoCalculador.MODO_PORCENTAJE,
            valor = 10.0,
            importe = 450.0
        )
        val resultado = PresupuestoCalculador.calcular(listOf(item(1.0, 4500.0)), listOf(recargo))

        assertEquals(4500.0, resultado.subtotal, 0.001)
        assertEquals(450.0, resultado.totalRecargos, 0.001)
        assertEquals(4950.0, resultado.total, 0.001)
        assertEquals(1, resultado.recargos.size)
    }

    @Test
    fun `el calculo sin recargos deja el total igual al subtotal`() {
        val resultado = PresupuestoCalculador.calcular(listOf(item(3.0, 1000.0)), emptyList())

        assertEquals(3000.0, resultado.subtotal, 0.001)
        assertEquals(0.0, resultado.totalRecargos, 0.001)
        assertEquals(3000.0, resultado.total, 0.001)
    }

    @Test
    fun `precio convierte el texto a numero`() {
        assertEquals(35000.0, PresupuestoCalculador.precio("$ 35.000"), 0.001)
        assertEquals(0.0, PresupuestoCalculador.precio("abc"), 0.001)
    }

    @Test
    fun `redondear deja dos decimales`() {
        assertEquals(10.13, PresupuestoCalculador.redondear(10.126), 0.0001)
        assertEquals(10.0, PresupuestoCalculador.redondear(10.0), 0.0001)
    }
}
