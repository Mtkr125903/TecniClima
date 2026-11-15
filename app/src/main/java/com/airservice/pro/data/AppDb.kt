package com.airservice.pro.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ClientEntity::class,
        TechnicianEntity::class,
        EquipmentEntity::class,
        ServiceEntity::class,
        BudgetEntity::class,
        ReceiptEntity::class,
        WorkEntity::class,
        TurnEntity::class,
        CounterEntity::class,
        PhotoEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun technicianDao(): TechnicianDao
    abstract fun equipmentDao(): EquipmentDao
    abstract fun serviceDao(): ServiceDao
    abstract fun budgetDao(): BudgetDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun workDao(): WorkDao
    abstract fun turnDao(): TurnDao
    abstract fun counterDao(): CounterDao
    abstract fun photoDao(): PhotoDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // --- Columnas nuevas en tablas existentes ---
                db.execSQL("ALTER TABLE `client` ADD COLUMN `direccion` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `client` ADD COLUMN `notas` TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE `equipment` ADD COLUMN `direccion` TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE `service` ADD COLUMN `technicianId` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `service` ADD COLUMN `costo` TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE `budget` ADD COLUMN `equipmentId` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `budget` ADD COLUMN `detalle` TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE `receipt` ADD COLUMN `equipmentId` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `receipt` ADD COLUMN `budgetId` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `receipt` ADD COLUMN `detalle` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `receipt` ADD COLUMN `metodoPago` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `receipt` ADD COLUMN `pdfPath` TEXT NOT NULL DEFAULT ''")

                // --- Tablas nuevas ---
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `technician` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`nombre` TEXT NOT NULL, " +
                        "`telefono` TEXT NOT NULL, " +
                        "`especialidad` TEXT NOT NULL, " +
                        "`activo` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `work` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`clientId` INTEGER NOT NULL, " +
                        "`equipmentId` INTEGER NOT NULL, " +
                        "`technicianId` INTEGER NOT NULL, " +
                        "`titulo` TEXT NOT NULL, " +
                        "`descripcion` TEXT NOT NULL, " +
                        "`avance` INTEGER NOT NULL, " +
                        "`estado` TEXT NOT NULL, " +
                        "`fecha` TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `turn` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`clientId` INTEGER NOT NULL, " +
                        "`technicianId` INTEGER NOT NULL, " +
                        "`titulo` TEXT NOT NULL, " +
                        "`fecha` TEXT NOT NULL, " +
                        "`hora` TEXT NOT NULL, " +
                        "`direccion` TEXT NOT NULL, " +
                        "`estado` TEXT NOT NULL, " +
                        "`notas` TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `counter` (" +
                        "`clave` TEXT PRIMARY KEY NOT NULL, " +
                        "`value` INTEGER NOT NULL)"
                )

                // --- Indices ---
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_equipment_clientId` ON `equipment` (`clientId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_clientId` ON `service` (`clientId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_equipmentId` ON `service` (`equipmentId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_budget_clientId` ON `budget` (`clientId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_budget_equipmentId` ON `budget` (`equipmentId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_receipt_clientId` ON `receipt` (`clientId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_work_clientId` ON `work` (`clientId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_work_technicianId` ON `work` (`technicianId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_turn_clientId` ON `turn` (`clientId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_turn_technicianId` ON `turn` (`technicianId`)")
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `photo` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`tipo` TEXT NOT NULL, " +
                        "`ownerId` INTEGER NOT NULL, " +
                        "`uri` TEXT NOT NULL, " +
                        "`creado` TEXT NOT NULL, " +
                        "`nota` TEXT NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_photo_ownerId` ON `photo` (`ownerId`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "airservice.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
