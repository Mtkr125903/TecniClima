package com.airservice.pro.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Verifica que la migracion 1 -> 2 conserve los datos y agregue las columnas nuevas. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration-test.db"
    private lateinit var archivo: File

    @Before
    fun setUp() {
        archivo = context.getDatabasePath(dbName)
        if (archivo.exists()) archivo.delete()
        crearVersion1(archivo.absolutePath)
    }

    @After
    fun tearDown() {
        if (archivo.exists()) archivo.delete()
    }

    private fun crearVersion1(path: String) {
        val db = SQLiteDatabase.openOrCreateDatabase(path, null)
        try {
            db.version = 1
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `client` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`nombre` TEXT NOT NULL, `telefono` TEXT NOT NULL, `email` TEXT NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `equipment` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`clientId` INTEGER NOT NULL, `type` TEXT NOT NULL, `brand` TEXT NOT NULL, " +
                    "`model` TEXT NOT NULL, `serial` TEXT NOT NULL, `installDate` TEXT NOT NULL, " +
                    "`capacityW` TEXT NOT NULL, `capacityBtu` TEXT NOT NULL, `gas` TEXT NOT NULL, " +
                    "`location` TEXT NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `service` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`clientId` INTEGER NOT NULL, `equipmentId` INTEGER NOT NULL, " +
                    "`tipo` TEXT NOT NULL, `descripcion` TEXT NOT NULL, `estado` TEXT NOT NULL, " +
                    "`fecha` TEXT NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `budget` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`clientId` INTEGER NOT NULL, `numero` TEXT NOT NULL, `monto` TEXT NOT NULL, " +
                    "`descripcion` TEXT NOT NULL, `estado` TEXT NOT NULL, `fecha` TEXT NOT NULL)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `receipt` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`clientId` INTEGER NOT NULL, `numero` TEXT NOT NULL, `monto` TEXT NOT NULL, " +
                    "`observacion` TEXT NOT NULL, `estado` TEXT NOT NULL, `fecha` TEXT NOT NULL)"
            )
            db.execSQL(
                "INSERT INTO `client` (`id`, `nombre`, `telefono`, `email`) " +
                    "VALUES (1, 'Cliente Existente', '1155550000', 'existente@correo.com')"
            )
            db.execSQL(
                "INSERT INTO `service` (`id`, `clientId`, `equipmentId`, `tipo`, `descripcion`, `estado`, `fecha`) " +
                    "VALUES (1, 1, 0, 'Preventivo', 'Servicio previo', 'Completado', '2024-01-01 10:00')"
            )
        } finally {
            db.close()
        }
    }

    @Test
    fun laMigracionConservaLosDatosYAgregaLasColumnasNuevas() = runBlocking {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

        val cliente = db.clientDao().getById(1)
        assertEquals("Cliente Existente", cliente?.nombre)
        assertEquals("", cliente?.direccion)
        assertEquals("", cliente?.notas)

        val servicio = db.serviceDao().getById(1)
        assertEquals("Servicio previo", servicio?.descripcion)
        assertEquals(0L, servicio?.technicianId)
        assertEquals("", servicio?.costo)

        assertEquals(1, db.technicianDao().count())
        assertEquals(1, db.workDao().count())
        assertEquals(1, db.turnDao().count())
        assertEquals(1, db.receiptDao().count())

        db.close()
    }

    @Test
    fun laMigracionCreaLosIndicesEsperados() {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

        val readable = db.openHelper.readableDatabase
        val indices = mutableListOf<String>()
        readable.query("PRAGMA index_list(`client`)").use { cursor ->
            while (cursor.moveToNext()) indices.add(cursor.getString(1))
        }
        assertTrue(indices.isNotEmpty())

        db.close()
    }
}
