package com.airservice.pro.util

import android.content.Context
import java.io.File

/** Ruta de almacenamiento de los PDF generados. */
object PdfStorage {

    private const val CARPETA = "documentos"

    /** Devuelve (y crea) la carpeta donde se guardan los PDF. */
    fun carpeta(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, CARPETA).apply { mkdirs() }

    /** Construye la ruta del PDF a partir de un tipo y numero de documento. */
    fun archivo(context: Context, tipo: String, numero: String): File =
        File(carpeta(context), "$tipo-$numero.pdf")
}
