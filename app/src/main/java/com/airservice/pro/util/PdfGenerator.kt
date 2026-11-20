package com.airservice.pro.util

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.util.Locale

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

    /** Ancho de la tabla de renglones. */
    private const val COL_CONCEPTO = 48f
    private const val COL_CANTIDAD = 330f
    private const val COL_PRECIO = 425f
    private const val COL_SUBTOTAL = 547f

    /**
     * Anchos de los glifos de Helvetica en miles de em (valores de la
     * especificacion Adobe). Sirven para alinear los importes a la derecha.
     */
    private val ANCHO_HELVETICA = mapOf(
        ' ' to 278, '!' to 278, '"' to 355, '#' to 556, '$' to 556, '%' to 889,
        '&' to 667, '\'' to 191, '(' to 333, ')' to 333, '*' to 389, '+' to 584,
        ',' to 278, '-' to 333, '.' to 278, '/' to 278, ':' to 278, ';' to 278,
        '<' to 584, '=' to 584, '>' to 584, '?' to 556, '@' to 1015,
        '[' to 278, '\\' to 278, ']' to 278, '^' to 469, '_' to 556, '`' to 333,
        '{' to 334, '|' to 260, '}' to 334, '~' to 584,
        'A' to 667, 'B' to 667, 'C' to 722, 'D' to 722, 'E' to 667, 'F' to 611,
        'G' to 778, 'H' to 722, 'I' to 278, 'J' to 500, 'K' to 667, 'L' to 556,
        'M' to 833, 'N' to 722, 'O' to 778, 'P' to 667, 'Q' to 778, 'R' to 722,
        'S' to 667, 'T' to 611, 'U' to 722, 'V' to 667, 'W' to 944, 'X' to 667,
        'Y' to 667, 'Z' to 611,
        'a' to 556, 'b' to 556, 'c' to 500, 'd' to 556, 'e' to 556, 'f' to 278,
        'g' to 556, 'h' to 556, 'i' to 222, 'j' to 222, 'k' to 500, 'l' to 222,
        'm' to 833, 'n' to 556, 'o' to 556, 'p' to 556, 'q' to 556, 'r' to 333,
        's' to 500, 't' to 278, 'u' to 556, 'v' to 500, 'w' to 722, 'x' to 500,
        'y' to 500, 'z' to 500,
        '0' to 556, '1' to 556, '2' to 556, '3' to 556, '4' to 556,
        '5' to 556, '6' to 556, '7' to 556, '8' to 556, '9' to 556
    )

    /** Helvetica-Bold: solo cambian los glifos de las mayusculas y la barra. */
    private val ANCHO_HELVETICA_BOLD = ANCHO_HELVETICA + mapOf(
        'A' to 722, 'B' to 722, 'C' to 722, 'D' to 722, 'E' to 667, 'F' to 611,
        'G' to 778, 'H' to 722, 'I' to 278, 'J' to 556, 'K' to 722, 'L' to 611,
        'M' to 833, 'N' to 722, 'O' to 778, 'P' to 667, 'Q' to 778, 'R' to 722,
        'S' to 667, 'T' to 611, 'U' to 722, 'V' to 667, 'W' to 944, 'X' to 667,
        'Y' to 667, 'Z' to 611,
        'a' to 556, 'b' to 611, 'c' to 556, 'd' to 611, 'e' to 556, 'f' to 333,
        'g' to 611, 'h' to 611, 'i' to 278, 'j' to 278, 'k' to 556, 'l' to 278,
        'm' to 889, 'n' to 611, 'o' to 611, 'p' to 611, 'q' to 611, 'r' to 389,
        's' to 556, 't' to 333, 'u' to 611, 'v' to 556, 'w' to 778, 'x' to 556,
        'y' to 556, 'z' to 500,
        ':' to 333, ';' to 333, '"' to 474, '(' to 333, ')' to 333, '/' to 278,
        '\\' to 278, '[' to 333, ']' to 333, '@' to 975, '_' to 556, '?' to 611,
        '!' to 333, '#' to 556, '$' to 556, '%' to 889, '&' to 722, '\'' to 238,
        '*' to 389, '+' to 584, '<' to 584, '=' to 584, '>' to 584, '^' to 584,
        '`' to 333, '{' to 389, '|' to 280, '}' to 389, '~' to 584
    )

    /** Renglon del presupuesto mostrado en el PDF. */
    data class Renglon(
        val concepto: String,
        val cantidad: Double,
        val precio: Double,
        val subtotal: Double
    )

    /** Recargo del presupuesto mostrado en el PDF. */
    data class Recargo(
        val nombre: String,
        val importe: Double
    )

    /**
     * Encabezado con los datos de la empresa. Todos los campos son opcionales:
     * los vacios no se imprimen.
     */
    data class Empresa(
        val nombre: String = "",
        val matricula: String = "",
        val cuit: String = "",
        val telefono: String = "",
        val direccion: String = ""
    )

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
        val monto: String,
        val renglones: List<Renglon> = emptyList(),
        val recargos: List<Recargo> = emptyList(),
        val empresa: Empresa? = null
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

        // --- Datos de la empresa (encabezado) ---
        val empresa = doc.empresa
        if (empresa != null) {
            if (empresa.nombre.isNotBlank()) {
                actual += texto(MARGEN, y, TAMANIO_CUERPO, "F2", empresa.nombre, COLOR_FONDO)
                y -= INTERLINEADO + 2f
            }
            val contacto = listOfNotNull(
                empresa.matricula.takeIf { it.isNotBlank() }?.let { "Matricula: $it" },
                empresa.cuit.takeIf { it.isNotBlank() }?.let { "CUIT: $it" },
                empresa.telefono.takeIf { it.isNotBlank() }?.let { "Tel: $it" },
                empresa.direccion.takeIf { it.isNotBlank() }
            )
            if (contacto.isNotEmpty()) {
                for (linea in envolver(contacto.joinToString(" | "), 88)) {
                    actual += texto(MARGEN, y, TAMANIO_PIE + 1, "F1", linea, COLOR_GRIS)
                    y -= INTERLINEADO - 2f
                }
                y -= 4f
            }
        }

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

        // --- Renglones ---
        if (doc.renglones.isNotEmpty()) {
            if (y < 150f) {
                paginas += actual
                actual = mutableListOf()
                y = ALTO - 90f
            }
            y -= 4f
            actual += texto(MARGEN, y, TAMANIO_CUERPO, "F2", "Renglones", COLOR_GRIS)
            y -= INTERLINEADO + 2f

            val encabezadoRenglones = {
                actual += texto(COL_CONCEPTO, y, TAMANIO_PIE + 1, "F2", "Concepto", COLOR_GRIS)
                actual += textoDerecha(COL_CANTIDAD, y, TAMANIO_PIE + 1, "F2", "Cant.", COLOR_GRIS)
                actual += textoDerecha(COL_PRECIO, y, TAMANIO_PIE + 1, "F2", "P. unit.", COLOR_GRIS)
                actual += textoDerecha(COL_SUBTOTAL, y, TAMANIO_PIE + 1, "F2", "Subtotal", COLOR_GRIS)
                y -= 4f
                actual += lineaSeparadora(y + 5, COLOR_MARCA, 0.8f)
                y -= INTERLINEADO
            }
            encabezadoRenglones()

            for (renglon in doc.renglones) {
                val lineas = envolver(renglon.concepto, 46)
                val alto = maxOf(1, lineas.size) * INTERLINEADO
                if (y - alto < 120f) {
                    paginas += actual
                    actual = mutableListOf()
                    y = ALTO - 90f
                    encabezadoRenglones()
                }
                val yFila = y
                lineas.forEachIndexed { indice, linea ->
                    actual += texto(
                        COL_CONCEPTO,
                        yFila - indice * INTERLINEADO,
                        TAMANIO_CUERPO,
                        "F1",
                        linea,
                        COLOR_FONDO
                    )
                }
                actual += textoDerecha(
                    COL_CANTIDAD, yFila, TAMANIO_CUERPO, "F1",
                    numero(renglon.cantidad), COLOR_FONDO
                )
                actual += textoDerecha(
                    COL_PRECIO, yFila, TAMANIO_CUERPO, "F1",
                    NumeroUtil.monedaDecimal(renglon.precio.toString()), COLOR_FONDO
                )
                actual += textoDerecha(
                    COL_SUBTOTAL, yFila, TAMANIO_CUERPO, "F1",
                    NumeroUtil.monedaDecimal(renglon.subtotal.toString()), COLOR_FONDO
                )
                y -= alto
            }
            y -= 8f
        }

        // --- Recargos ---
        if (doc.recargos.isNotEmpty()) {
            if (y < 140f) {
                paginas += actual
                actual = mutableListOf()
                y = ALTO - 90f
            }
            y -= 4f
            actual += texto(MARGEN, y, TAMANIO_CUERPO, "F2", "Recargos", COLOR_GRIS)
            y -= INTERLINEADO + 2f
            for (recargo in doc.recargos) {
                if (y < 120f) {
                    paginas += actual
                    actual = mutableListOf()
                    y = ALTO - 90f
                }
                actual += texto(MARGEN, y, TAMANIO_CUERPO, "F1", recargo.nombre, COLOR_FONDO)
                actual += textoDerecha(
                    COL_SUBTOTAL, y, TAMANIO_CUERPO, "F1",
                    NumeroUtil.monedaDecimal(recargo.importe.toString()), COLOR_FONDO
                )
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

        // --- Totales del desglose (solo si hay renglones o recargos) ---
        if (doc.renglones.isNotEmpty() || doc.recargos.isNotEmpty()) {
            if (y < 150f) {
                paginas += actual
                actual = mutableListOf()
                y = ALTO - 90f
            }
            val subtotal = doc.renglones.sumOf { it.subtotal }
            val totalRecargos = doc.recargos.sumOf { it.importe }
            if (doc.renglones.isNotEmpty()) {
                actual += texto(MARGEN, y, TAMANIO_CUERPO, "F1", "Subtotal", COLOR_FONDO)
                actual += textoDerecha(
                    COL_SUBTOTAL, y, TAMANIO_CUERPO, "F1",
                    NumeroUtil.monedaDecimal(subtotal.toString()), COLOR_FONDO
                )
                y -= INTERLINEADO + 2f
            }
            if (doc.recargos.isNotEmpty()) {
                actual += texto(MARGEN, y, TAMANIO_CUERPO, "F1", "Recargos", COLOR_FONDO)
                actual += textoDerecha(
                    COL_SUBTOTAL, y, TAMANIO_CUERPO, "F1",
                    NumeroUtil.monedaDecimal(totalRecargos.toString()), COLOR_FONDO
                )
                y -= INTERLINEADO + 2f
            }
            y -= 4f
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
        actual += textoDerecha(
            ANCHO - MARGEN - 16, y + 3, TAMANIO_CUERPO + 3, "F2",
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

    /** Numero sin decimales sobrantes ni separador local: "1", "1.5". */
    private fun numero(valor: Double): String {
        if (valor == valor.toLong().toDouble()) return valor.toLong().toString()
        return String.format(Locale.US, "%.2f", valor).trimEnd('0').trimEnd('.')
    }

    /**
     * Ancho aproximado del texto en puntos. Se usan las metricas reales de
     * Helvetica para poder alinear los importes a la derecha.
     */
    private fun anchoTexto(valor: String, size: Float, negrita: Boolean): Float {
        val tabla = if (negrita) ANCHO_HELVETICA_BOLD else ANCHO_HELVETICA
        var unidades = 0
        for (caracter in valor) {
            unidades += tabla[caracter] ?: 556
        }
        return unidades * size / 1000f
    }

    /** Texto alineado al borde derecho en [xDerecha]. */
    private fun textoDerecha(
        xDerecha: Float,
        y: Float,
        size: Float,
        fuente: String,
        valor: String,
        color: String
    ): String {
        val x = xDerecha - anchoTexto(valor, size, fuente == "F2")
        return texto(x, y, size, fuente, valor, color)
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
