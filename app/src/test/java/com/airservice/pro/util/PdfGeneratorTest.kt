package com.airservice.pro.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PdfGeneratorTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun documento(detalle: String = "Carga de gas\nCambio de presostato") = PdfGenerator.Documento(
        titulo = "Comprobante de servicio",
        numero = "REC-0001",
        cliente = "María Fernández",
        equipo = "Samsung Wind-Free 12.000",
        tipo = "Reparación",
        tecnico = "Juan Pérez",
        fecha = "06/11/2026",
        detalle = detalle,
        observacion = "Pago en efectivo",
        monto = "87000"
    )

    private fun documentoConRenglones() = documento().copy(
        titulo = "Presupuesto",
        numero = "PRE-0001",
        monto = "115000",
        renglones = listOf(
            PdfGenerator.Renglon(
                concepto = "Carga de gas R410A",
                cantidad = 1.0,
                precio = 45000.0,
                subtotal = 45000.0
            ),
            PdfGenerator.Renglon(
                concepto = "Limpieza de filtros y cambio de presostato",
                cantidad = 2.0,
                precio = 25000.0,
                subtotal = 50000.0
            )
        ),
        recargos = listOf(
            PdfGenerator.Recargo(nombre = "Urgencia", importe = 20000.0)
        )
    )

    @Test
    fun `genera un pdf con cabecera y cierre correctos`() {
        val destino = File(temp.newFolder(), "comprobante.pdf")
        PdfGenerator.generarComprobante(documento(), destino)

        assertTrue(destino.exists())
        val contenido = destino.readBytes()
        val texto = String(contenido, Charsets.ISO_8859_1)

        assertTrue(texto.startsWith("%PDF-1.4"))
        assertTrue(texto.trimEnd().endsWith("%%EOF"))
        assertTrue(texto.contains("/Type /Catalog"))
        assertTrue(texto.contains("/Type /Pages"))
    }

    @Test
    fun `los offsets de la tabla xref apuntan a los objetos`() {
        val destino = File(temp.newFolder(), "presupuesto.pdf")
        PdfGenerator.generarPresupuesto(documento(), destino)

        val contenido = destino.readBytes()
        val texto = String(contenido, Charsets.ISO_8859_1)

        val inicioXref = texto.substringAfter("startxref").trim().substringBefore("\n").toInt()
        assertTrue("startxref debe apuntar a la tabla xref", texto.startsWith("xref", inicioXref))

        val entradas = texto.substring(inicioXref)
            .lines()
            .filter { it.matches(Regex("\\d{10} \\d{5} n")) }

        assertTrue("La tabla xref debe tener entradas", entradas.isNotEmpty())

        entradas.forEach { linea ->
            val offset = linea.substring(0, 10).trim().toInt()
            val fragmento = String(contenido, offset, contenido.size - offset, Charsets.ISO_8859_1)
            assertTrue(
                "El offset $offset debe apuntar a un objeto PDF",
                Regex("^\\d+ 0 obj").containsMatchIn(fragmento)
            )
        }
    }

    @Test
    fun `el length del stream coincide con los bytes reales`() {
        val destino = File(temp.newFolder(), "largo.pdf")
        PdfGenerator.generarComprobante(documento(detalle = "x".repeat(4000)), destino)

        val contenido = destino.readBytes()
        val iso = String(contenido, Charsets.ISO_8859_1)

        val match = Regex("<< /Length (\\d+) >>\\s*stream\\n").find(iso)
        assertTrue("Debe existir un stream con /Length", match != null)

        val declared = match!!.groupValues[1].toInt()
        val inicioStream = match.range.last + 1
        val finStream = iso.indexOf("\nendstream", inicioStream)
        val real = finStream - inicioStream

        assertEquals("El /Length debe coincidir con el tamaño real del stream", real, declared)
    }

    @Test
    fun `el presupuesto y el comprobante se distinguen por su pie`() {
        val carpeta = temp.newFolder()
        val presupuesto = File(carpeta, "pre.pdf")
        val comprobante = File(carpeta, "rec.pdf")

        PdfGenerator.generarPresupuesto(documento(), presupuesto)
        PdfGenerator.generarComprobante(documento(), comprobante)

        val textoPre = String(presupuesto.readBytes(), Charsets.ISO_8859_1)
        val textoRec = String(comprobante.readBytes(), Charsets.ISO_8859_1)

        assertTrue(textoPre.contains("Presupuesto de servicios"))
        assertTrue(textoRec.contains("Comprobante de servicio"))
        assertTrue(textoPre.contains("TecniClima"))
    }

    @Test
    fun `el PDF muestra los renglones, recargos y totales del desglose`() {
        val destino = File(temp.newFolder(), "desglose.pdf")
        PdfGenerator.generarPresupuesto(documentoConRenglones(), destino)

        val contenido = destino.readBytes()
        val texto = String(contenido, Charsets.ISO_8859_1)

        // Conceptos y cantidades
        assertTrue(texto.contains("Carga de gas R410A"))
        assertTrue(texto.contains("Limpieza de filtros y cambio de presostato"))
        assertTrue(texto.contains("Urgencia"))
        // Encabezados de tabla y totales
        assertTrue(texto.contains("Concepto"))
        assertTrue(texto.contains("Subtotal"))
        assertTrue(texto.contains("Recargos"))
        assertTrue(texto.contains("TOTAL"))
        // El monto total del presupuesto
        assertTrue(texto.contains("$ 115.000"))
    }

    @Test
    fun `un presupuesto sin renglones ni recargos no incluye la tabla ni los totales`() {
        val destino = File(temp.newFolder(), "simple.pdf")
        PdfGenerator.generarPresupuesto(documento(), destino)

        val texto = String(destino.readBytes(), Charsets.ISO_8859_1)

        assertTrue(texto.contains("TOTAL"))
        assertTrue("no debe mostrar el encabezado de la tabla de renglones",
            !texto.contains("Concepto"))
        assertTrue("no debe mostrar la linea de subtotal del desglose",
            !texto.contains("Subtotal"))
    }
}
