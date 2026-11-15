package com.airservice.pro.util

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.charset.Charset

/**
 * Generador de PDF autocontenido (sin librerias externas).
 *
 * Implementa la estructura PDF 1.4 de forma estricta:
 *  - Los offsets de la tabla xref se calculan sobre el TAMAÑO EN BYTES real.
 *  - El /Length de cada stream es la cantidad de BYTES, no de caracteres.
 *  - El texto se codifica en WinAnsi (CP1252) con la fuente Helvetica estandar,
 *    de modo que los acentos y la letra n tilde se rendericen correctamente.
 */
object PdfGenerator {

    private val WINANSI: Charset = Charset.forName("Cp1252")
    private val LATIN1: Charset = Charset.forName("ISO-8859-1")

    private const val ANCHO = 595f
    private const val ALTO = 842f
    private const val MARGEN = 48f
    private const val TAMANIO_TITULO = 22f
    private const val TAMANIO_SUBTITULO = 12f
    private const val TAMANIO_CUERPO = 10f
    private const val TAMANIO_PIE = 8f
    private const val INTERLINEADO = 15f

    /** Azul cian de marca (#00D4FF). */
    private const val COLOR_MARCA = "0.000 0.831 1.000"
    /** Azul marino profundo (#0B132B). */
    private const val COLOR_FONDO = "0.043 0.075 0.169"
    private const val COLOR_GRIS = "0.420 0.470 0.560"

    data class Documento(
        val titulo: String,
        val numero: String,
        val cliente: String,
        val equipo: String,
        val tipo: String,
        val tecnico: String,
        val fecha: String,
        val detalle: String,
        val observacion: String,
        val monto: String
    )

    // ------------------------------------------------------------------
    // API publica
    // ------------------------------------------------------------------

    @Throws(IOException::class)
    fun generarComprobante(doc: Documento, destino: File) = escribir(doc, destino, esComprobante = true)

    @Throws(IOException::class)
    fun generarPresupuesto(doc: Documento, destino: File) = escribir(doc, destino, esComprobante = false)

    // ------------------------------------------------------------------
    // Construccion
    // ------------------------------------------------------------------

    private fun escribir(doc: Documento, destino: File, esComprobante: Boolean) {
        val paginas = construirPaginas(doc, esComprobante)
        val bytes = ensamblar(paginas)
        destino.parentFile?.mkdirs()
        destino.writeBytes(bytes)
    }

    /** Cada pagina es una lista de operadores PDF ya listos para concatenar. */
    private fun construirPaginas(doc: Documento, esComprobante: Boolean): List<List<String>> {
        val paginas = mutableListOf<MutableList<String>>()
        var actual = mutableListOf<String>()

        // --- Banda superior de marca ---
        actual += "q $COLOR_MARCA rg 0 ${ALTO - 96} $ANCHO 96 re f Q"
        actual += texto(
            MARGEN, ALTO - 46, TAMANIO_TITULO, "F2",
            "TecniClima", COLOR_FONDO
        )
        actual += texto(
            MARGEN, ALTO - 68, TAMANIO_SUBTITULO, "F1",
            doc.titulo, COLOR_FONDO
        )
        actual += texto(
            ANCHO - MARGEN - 170, ALTO - 46, TAMANIO_SUBTITULO, "F2",
            doc.numero, COLOR_FONDO
        )
        actual += texto(
            ANCHO - MARGEN - 170, ALTO - 68, TAMANIO_PIE + 1, "F1",
            "Fecha: ${doc.fecha}", COLOR_FONDO
        )

        var y = ALTO - 126f

        // --- Bloque de datos ---
        val etiquetaX = MARGEN
        val valorX = MARGEN + 92f
        actual += lineaSeparadora(y + 8, COLOR_MARCA, 1.4f)
        y -= 12f

        val pares = buildList {
            add("Cliente" to doc.cliente)
            add("Equipo" to doc.equipo)
            add("Tipo" to doc.tipo)
            if (doc.tecnico.isNotBlank()) add("Tecnico" to doc.tecnico)
        }
        for ((etiqueta, valor) in pares) {
            if (y < 120f) {
                paginas += actual
                actual = mutableListOf()
                y = ALTO - 90f
            }
            actual += texto(etiquetaX, y, TAMANIO_CUERPO, "F2", etiqueta, COLOR_GRIS)
            actual += texto(valorX, y, TAMANIO_CUERPO, "F1", valor.ifBlank { "-" }, COLOR_FONDO)
            y -= INTERLINEADO + 2f
        }

        y -= 8f

        // --- Detalle ---
        if (doc.detalle.isNotBlank()) {
            actual += texto(MARGEN, y, TAMANIO_CUERPO, "F2", "Detalle", COLOR_GRIS)
            y -= INTERLINEADO + 2f
            for (linea in envolver(doc.detalle, 88)) {
                if (y < 120f) {
                    paginas += actual
                    actual = mutableListOf()
                    y = ALTO - 90f
                }
                actual += texto(MARGEN, y, TAMANIO_CUERPO, "F1", linea, COLOR_FONDO)
                y -= INTERLINEADO
            }
            y -= 8f
        }

        // --- Observacion ---
        if (doc.observacion.isNotBlank()) {
            actual += texto(MARGEN, y, TAMANIO_CUERPO, "F2", "Observacion", COLOR_GRIS)
            y -= INTERLINEADO + 2f
            for (linea in envolver(doc.observacion, 88)) {
                if (y < 120f) {
                    paginas += actual
                    actual = mutableListOf()
                    y = ALTO - 90f
                }
                actual += texto(MARGEN, y, TAMANIO_CUERPO, "F1", linea, COLOR_FONDO)
                y -= INTERLINEADO
            }
            y -= 8f
        }

        // --- Total ---
        val totalTexto = NumeroUtil.monedaDecimal(doc.monto)
        val anchoTotal = 220f
        if (y < 110f) {
            paginas += actual
            actual = mutableListOf()
            y = ALTO - 90f
        }
        actual += "q $COLOR_MARCA rg ${ANCHO - MARGEN - anchoTotal} ${y - 10} $anchoTotal 34 re f Q"
        actual += texto(ANCHO - MARGEN - anchoTotal + 16, y + 3, TAMANIO_CUERPO + 1, "F2", "TOTAL", COLOR_FONDO)
        actual += texto(
            ANCHO - MARGEN - 16 - 130, y + 3, TAMANIO_CUERPO + 3, "F2",
            totalTexto, COLOR_FONDO
        )
        y -= 34f

        // --- Pie ---
        actual += lineaSeparadora(74f, COLOR_GRIS, 0.6f)
        actual += texto(
            MARGEN, 58f, TAMANIO_PIE, "F1",
            if (esComprobante) "Comprobante de servicio" else "Presupuesto de servicios",
            COLOR_GRIS
        )
        actual += texto(
            ANCHO - MARGEN - 150f, 58f, TAMANIO_PIE, "F1",
            "TecniClima - Gestion Tecnica", COLOR_GRIS
        )

        paginas += actual
        return paginas
    }

    private fun envolver(texto: String, maxCaracteres: Int): List<String> {
        val lineas = mutableListOf<String>()
        texto.replace("\r", "").split("\n").forEach { parrafo ->
            if (parrafo.isBlank()) {
                lineas += ""
                return@forEach
            }
            var actual = StringBuilder()
            for (palabra in parrafo.split(" ")) {
                if (actual.isEmpty()) {
                    actual.append(palabra)
                } else if (actual.length + 1 + palabra.length <= maxCaracteres) {
                    actual.append(' ').append(palabra)
                } else {
                    lineas += actual.toString()
                    actual = StringBuilder(palabra)
                }
            }
            if (actual.isNotEmpty()) lineas += actual.toString()
        }
        return lineas
    }

    private fun texto(x: Float, y: Float, size: Float, fuente: String, valor: String, color: String): String {
        val seguro = valor.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
        return "BT $color rg /$fuente $size Tf $x $y Td ($seguro) Tj ET"
    }

    private fun lineaSeparadora(y: Float, color: String, grosor: Float): String =
        "q $color RG 0.6 w $MARGEN $y ${ANCHO - MARGEN * 2} m $MARGEN ${y + grosor} l S Q"

    // ------------------------------------------------------------------
    // Ensamblado del archivo
    // ------------------------------------------------------------------

    private fun ensamblar(paginas: List<List<String>>): ByteArray {
        val out = ByteArrayOutputStream()
        val offsets = ArrayList<Int>()

        fun escribir(texto: String) = out.write(texto.toByteArray(LATIN1))
        fun escribirBytes(bytes: ByteArray) = out.write(bytes)

        // Cabecera con marcador binario
        escribir("%PDF-1.4\n")
        escribirBytes(byteArrayOf(0x25, 0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte(), 0x0A))

        val numPaginas = paginas.size
        val cantidadObjetos = 4 + numPaginas * 2
        val primeraPagina = 5
        val primerContenido = primeraPagina + numPaginas

        // 1 Catalog
        offsets += out.size()
        escribir("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

        // 2 Pages
        offsets += out.size()
        val kids = (0 until numPaginas).joinToString(" ") { "${primeraPagina + it} 0 R" }
        escribir("2 0 obj\n<< /Type /Pages /Kids [$kids] /Count $numPaginas >>\nendobj\n")

        // 3 Font F1
        offsets += out.size()
        escribir("3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n")

        // 4 Font F2
        offsets += out.size()
        escribir("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n")

        // Paginas
        paginas.forEachIndexed { indice, operadores ->
            offsets += out.size()
            val numeroPagina = primeraPagina + indice
            val numeroContenido = primerContenido + indice
            escribir(
                "$numeroPagina 0 obj\n" +
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $ANCHO $ALTO] " +
                    "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> " +
                    "/Contents $numeroContenido 0 R >>\nendobj\n"
            )

            val contenido = operadores.joinToString("\n")
            val bytesContenido = contenido.toByteArray(WINANSI)
            offsets += out.size()
            escribir("$numeroContenido 0 obj\n<< /Length ${bytesContenido.size} >>\nstream\n")
            escribirBytes(bytesContenido)
            escribir("\nendstream\nendobj\n")
        }

        // Tabla xref
        val inicioXref = out.size()
        escribir("xref\n")
        escribir("0 ${cantidadObjetos + 1}\n")
        escribir("0000000000 65535 f\r\n")
        offsets.forEach { escribir(String.format("%010d 00000 n\r\n", it)) }
        escribir("trailer\n<< /Size ${cantidadObjetos + 1} /Root 1 0 R >>\n")
        escribir("startxref\n$inicioXref\n%%EOF\n")

        return out.toByteArray()
    }
}
