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
        PhotoEntity::class,
        CompanyProfileEntity::class,
        CatalogItemEntity::class,
        ChargeCatalogEntity::class,
        BudgetItemEntity::class,
        BudgetChargeEntity::class,
        BudgetTemplateEntity::class,
        BudgetTemplateItemEntity::class
    ],
    version = 5,
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
    abstract fun companyProfileDao(): CompanyProfileDao
    abstract fun catalogDao(): CatalogDao
    abstract fun chargeCatalogDao(): ChargeCatalogDao
    abstract fun budgetItemDao(): BudgetItemDao
    abstract fun budgetChargeDao(): BudgetChargeDao
    abstract fun budgetTemplateDao(): BudgetTemplateDao

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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // --- Tablas nuevas para empresa y catalogo ---
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `company_profile` (" +
                        "`id` INTEGER PRIMARY KEY NOT NULL, " +
                        "`nombre` TEXT NOT NULL, " +
                        "`matricula` TEXT NOT NULL, " +
                        "`cuit` TEXT NOT NULL, " +
                        "`telefono` TEXT NOT NULL, " +
                        "`whatsapp` TEXT NOT NULL, " +
                        "`aliasPago` TEXT NOT NULL, " +
                        "`direccion` TEXT NOT NULL, " +
                        "`logoPath` TEXT NOT NULL, " +
                        "`validezDias` INTEGER NOT NULL)"
                )

                db.execSQL("ALTER TABLE `budget` ADD COLUMN `validezDias` INTEGER NOT NULL DEFAULT 15")
                db.execSQL("ALTER TABLE `budget` ADD COLUMN `vence` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `budget` ADD COLUMN `sena` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `budget` ADD COLUMN `garantia` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `budget` ADD COLUMN `incluye` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `budget` ADD COLUMN `noIncluye` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // --- Catalogos y plantillas ---
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `catalog_item` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`familia` TEXT NOT NULL, " +
                        "`concepto` TEXT NOT NULL, " +
                        "`precio` TEXT NOT NULL, " +
                        "`duracionHs` TEXT NOT NULL, " +
                        "`activo` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_catalog_item_familia` ON `catalog_item` (`familia`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `charge_catalog` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`nombre` TEXT NOT NULL, " +
                        "`modo` TEXT NOT NULL, " +
                        "`valor` TEXT NOT NULL, " +
                        "`temporada` INTEGER NOT NULL, " +
                        "`activo` INTEGER NOT NULL)"
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `budget_item` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`budgetId` INTEGER NOT NULL, " +
                        "`concepto` TEXT NOT NULL, " +
                        "`cantidad` REAL NOT NULL, " +
                        "`precio` TEXT NOT NULL, " +
                        "`subtotal` TEXT NOT NULL, " +
                        "`orden` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_budget_item_budgetId` ON `budget_item` (`budgetId`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `budget_charge` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`budgetId` INTEGER NOT NULL, " +
                        "`nombre` TEXT NOT NULL, " +
                        "`modo` TEXT NOT NULL, " +
                        "`valor` TEXT NOT NULL, " +
                        "`importe` TEXT NOT NULL, " +
                        "`orden` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_budget_charge_budgetId` ON `budget_charge` (`budgetId`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `budget_template` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`nombre` TEXT NOT NULL, " +
                        "`familia` TEXT NOT NULL, " +
                        "`descripcion` TEXT NOT NULL, " +
                        "`activo` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_budget_template_familia` ON `budget_template` (`familia`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `budget_template_item` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`templateId` INTEGER NOT NULL, " +
                        "`concepto` TEXT NOT NULL, " +
                        "`cantidad` REAL NOT NULL, " +
                        "`precio` TEXT NOT NULL, " +
                        "`orden` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_budget_template_item_templateId` ON `budget_template_item` (`templateId`)")
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "airservice.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}