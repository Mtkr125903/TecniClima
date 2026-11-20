package com.airservice.pro.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Verifica que las migraciones conserven los datos y agreguen las columnas y tablas nuevas. */
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

    /** Aplica las migraciones reales sobre la base para dejar el archivo en la version 3. */
    private fun subirAVersion3(nombre: String) {
        val callback = object : SupportSQLiteOpenHelper.Callback(3) {
            override fun onCreate(db: SupportSQLiteDatabase) = Unit

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                if (oldVersion <= 1 && newVersion >= 2) AppDatabase.MIGRATION_1_2.migrate(db)
                if (oldVersion <= 2 && newVersion >= 3) AppDatabase.MIGRATION_2_3.migrate(db)
            }
        }
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(nombre)
                .callback(callback)
                .build()
        )
        helper.writableDatabase
        helper.close()
    }

    @Test
    fun laMigracionConservaLosDatosYAgregaLasColumnasNuevas() = runBlocking {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5
            )
            .build()

        val cliente = db.clientDao().getById(1)
        assertEquals("Cliente Existente", cliente?.nombre)
        assertEquals("", cliente?.direccion)
        assertEquals("", cliente?.notas)

        val servicio = db.serviceDao().getById(1)
        assertEquals("Servicio previo", servicio?.descripcion)
        assertEquals(0L, servicio?.technicianId)
        assertEquals("", servicio?.costo)

        assertEquals(1, db.clientDao().count())
        assertEquals(1, db.serviceDao().count())

        db.close()
    }

    @Test
    fun laMigracionCreaLosIndicesEsperados() {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5
            )
            .build()

        val readable = db.openHelper.readableDatabase
        assertTrue(indicesDe(readable, "equipment").contains("index_equipment_clientId"))
        assertTrue(indicesDe(readable, "service").contains("index_service_equipmentId"))
        assertTrue(indicesDe(readable, "photo").contains("index_photo_ownerId"))
        assertTrue(indicesDe(readable, "catalog_item").contains("index_catalog_item_familia"))
        assertTrue(indicesDe(readable, "budget_item").contains("index_budget_item_budgetId"))
        assertTrue(indicesDe(readable, "budget_charge").contains("index_budget_charge_budgetId"))
        assertTrue(indicesDe(readable, "budget_template").contains("index_budget_template_familia"))
        assertTrue(indicesDe(readable, "budget_template_item").contains("index_budget_template_item_templateId"))

        db.close()
    }

    private fun indicesDe(db: SupportSQLiteDatabase, tabla: String): List<String> {
        val indices = mutableListOf<String>()
        db.query("PRAGMA index_list(`$tabla`)").use { cursor ->
            while (cursor.moveToNext()) indices.add(cursor.getString(1))
        }
        return indices
    }

    @Test
    fun laMigracionDesdeVersion3CreaLasTablasNuevasYConservaLosDatos() = runBlocking {
        val nombreV3 = "migration-test-v3.db"
        val archivoV3 = context.getDatabasePath(nombreV3)
        if (archivoV3.exists()) archivoV3.delete()

        try {
            crearVersion1(archivoV3.absolutePath)
            subirAVersion3(nombreV3)

            val db = Room.databaseBuilder(context, AppDatabase::class.java, nombreV3)
                .addMigrations(AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
                .build()

            val cliente = db.clientDao().getById(1)
            assertNotNull(cliente)
            assertEquals("Cliente Existente", cliente?.nombre)

            assertNull(db.companyProfileDao().get())
            assertEquals(0, db.catalogDao().count())
            assertEquals(0, db.chargeCatalogDao().count())
            assertEquals(0, db.budgetItemDao().count())
            assertEquals(0, db.budgetChargeDao().count())
            assertEquals(0, db.budgetTemplateDao().count())

            val columnas = mutableListOf<String>()
            db.openHelper.readableDatabase.query("PRAGMA table_info(`budget`)").use { cursor ->
                val indiceNombre = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) columnas.add(cursor.getString(indiceNombre))
            }
            assertTrue(columnas.contains("validezDias"))
            assertTrue(columnas.contains("vence"))
            assertTrue(columnas.contains("sena"))
            assertTrue(columnas.contains("garantia"))
            assertTrue(columnas.contains("incluye"))
            assertTrue(columnas.contains("noIncluye"))

            db.close()
        } finally {
            if (archivoV3.exists()) archivoV3.delete()
        }
    }
}
