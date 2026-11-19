package com.airservice.pro.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM client ORDER BY nombre ASC")
    fun observeAll(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM client WHERE id = :id")
    fun observeById(id: Long): Flow<ClientEntity?>

    @Query("SELECT * FROM client WHERE id = :id")
    suspend fun getById(id: Long): ClientEntity?

    @Query("SELECT * FROM client ORDER BY nombre ASC")
    suspend fun getAll(): List<ClientEntity>

    @Query("SELECT * FROM client WHERE nombre LIKE '%' || :q || '%' OR telefono LIKE '%' || :q || '%' OR email LIKE '%' || :q || '%' ORDER BY nombre ASC")
    fun search(q: String): Flow<List<ClientEntity>>

    @Query("SELECT COUNT(*) FROM client")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(client: ClientEntity): Long

    @Update
    suspend fun update(client: ClientEntity)

    @Delete
    suspend fun delete(client: ClientEntity)
}

@Dao
interface TechnicianDao {
    @Query("SELECT * FROM technician ORDER BY activo DESC, nombre ASC")
    fun observeAll(): Flow<List<TechnicianEntity>>

    @Query("SELECT * FROM technician ORDER BY activo DESC, nombre ASC")
    suspend fun getAll(): List<TechnicianEntity>

    @Query("SELECT * FROM technician WHERE id = :id")
    suspend fun getById(id: Long): TechnicianEntity?

    @Query("SELECT COUNT(*) FROM technician")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(technician: TechnicianEntity): Long

    @Update
    suspend fun update(technician: TechnicianEntity)

    @Delete
    suspend fun delete(technician: TechnicianEntity)
}

@Dao
interface EquipmentDao {
    @Query(
        """
        SELECT e.*,
               COALESCE(c.nombre, '') AS clienteNombre,
               COALESCE(c.telefono, '') AS clienteTelefono
        FROM equipment e
        LEFT JOIN client c ON c.id = e.clientId
        ORDER BY e.brand ASC, e.model ASC
        """
    )
    fun observeAll(): Flow<List<EquipoRow>>

    @Query(
        """
        SELECT e.*,
               COALESCE(c.nombre, '') AS clienteNombre,
               COALESCE(c.telefono, '') AS clienteTelefono
        FROM equipment e
        LEFT JOIN client c ON c.id = e.clientId
        WHERE e.brand LIKE '%' || :q || '%' OR e.model LIKE '%' || :q || '%'
           OR e.serial LIKE '%' || :q || '%' OR e.type LIKE '%' || :q || '%'
           OR COALESCE(c.nombre, '') LIKE '%' || :q || '%'
        ORDER BY e.brand ASC, e.model ASC
        """
    )
    fun search(q: String): Flow<List<EquipoRow>>

    @Query(
        """
        SELECT e.*,
               COALESCE(c.nombre, '') AS clienteNombre,
               COALESCE(c.telefono, '') AS clienteTelefono
        FROM equipment e
        LEFT JOIN client c ON c.id = e.clientId
        WHERE e.clientId = :clientId
        ORDER BY e.brand ASC
        """
    )
    fun observeByClient(clientId: Long): Flow<List<EquipoRow>>

    @Query("SELECT * FROM equipment WHERE id = :id")
    suspend fun getById(id: Long): EquipmentEntity?

    @Query("SELECT * FROM equipment WHERE clientId = :clientId ORDER BY brand ASC")
    suspend fun listByClient(clientId: Long): List<EquipmentEntity>

    @Query("SELECT * FROM equipment ORDER BY brand ASC")
    suspend fun getAll(): List<EquipmentEntity>

    @Query("SELECT * FROM equipment WHERE serial = :serial LIMIT 1")
    suspend fun findBySerial(serial: String): EquipmentEntity?

    @Query("SELECT COUNT(*) FROM equipment")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(equipment: EquipmentEntity): Long

    @Update
    suspend fun update(equipment: EquipmentEntity)

    @Delete
    suspend fun delete(equipment: EquipmentEntity)

    @Query("DELETE FROM equipment WHERE clientId = :clientId")
    suspend fun deleteByClient(clientId: Long)
}

@Dao
interface ServiceDao {
    @Query(
        """
        SELECT s.*,
               COALESCE(c.nombre, '') AS clienteNombre,
               COALESCE(NULLIF(TRIM(e.brand || ' ' || e.model), ''), '') AS equipoDescripcion,
               COALESCE(t.nombre, '') AS tecnicoNombre
        FROM service s
        LEFT JOIN client c ON c.id = s.clientId
        LEFT JOIN equipment e ON e.id = s.equipmentId
        LEFT JOIN technician t ON t.id = s.technicianId
        ORDER BY s.fecha DESC
        """
    )
    fun observeAll(): Flow<List<ServicioRow>>

    @Query(
        """
        SELECT s.*,
               COALESCE(c.nombre, '') AS clienteNombre,
               COALESCE(NULLIF(TRIM(e.brand || ' ' || e.model), ''), '') AS equipoDescripcion,
               COALESCE(t.nombre, '') AS tecnicoNombre
        FROM service s
        LEFT JOIN client c ON c.id = s.clientId
        LEFT JOIN equipment e ON e.id = s.equipmentId
        LEFT JOIN technician t ON t.id = s.technicianId
        WHERE s.clientId = :clientId
        ORDER BY s.fecha DESC
        """
    )
    fun observeByClient(clientId: Long): Flow<List<ServicioRow>>

    @Query(
        """
        SELECT s.*,
               COALESCE(c.nombre, '') AS clienteNombre,
               COALESCE(NULLIF(TRIM(e.brand || ' ' || e.model), ''), '') AS equipoDescripcion,
               COALESCE(t.nombre, '') AS tecnicoNombre
        FROM service s
        LEFT JOIN client c ON c.id = s.clientId
        LEFT JOIN equipment e ON e.id = s.equipmentId
        LEFT JOIN technician t ON t.id = s.technicianId
        WHERE s.equipmentId = :equipmentId
        ORDER BY s.fecha DESC
        """
    )
    fun observeByEquipment(equipmentId: Long): Flow<List<ServicioRow>>

    @Query("SELECT * FROM service WHERE id = :id")
    suspend fun getById(id: Long): ServiceEntity?

    @Query("SELECT COUNT(*) FROM service")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(service: ServiceEntity): Long

    @Update
    suspend fun update(service: ServiceEntity)

    @Delete
    suspend fun delete(service: ServiceEntity)

    @Query("DELETE FROM service WHERE clientId = :clientId")
    suspend fun deleteByClient(clientId: Long)

    @Query("DELETE FROM service WHERE equipmentId = :equipmentId")
    suspend fun deleteByEquipment(equipmentId: Long)
}

@Dao
interface BudgetDao {
    @Query(
        """
        SELECT b.*,
               COALESCE(c.nombre, '') AS clienteNombre,
               COALESCE(c.telefono, '') AS clienteTelefono,
               COALESCE(NULLIF(TRIM(e.brand || ' ' || e.model), ''), '') AS equipoDescripcion
        FROM budget b
        LEFT JOIN client c ON c.id = b.clientId
        LEFT JOIN equipment e ON e.id = b.equipmentId
        ORDER BY b.fecha DESC
        """
    )
    fun observeAll(): Flow<List<PresupuestoRow>>

    @Query(
        """
        SELECT b.*,
               COALESCE(c.nombre, '') AS clienteNombre,
               COALESCE(c.telefono, '') AS clienteTelefono,
               COALESCE(NULLIF(TRIM(e.brand || ' ' || e.model), ''), '') AS equipoDescripcion
        FROM budget b
        LEFT JOIN client c ON c.id = b.clientId
        LEFT JOIN equipment e ON e.id = b.equipmentId
        WHERE b.clientId = :clientId
        ORDER BY b.fecha DESC
        """
    )
    fun observeByClient(clientId: Long): Flow<List<PresupuestoRow>>

    @Query("SELECT * FROM budget WHERE id = :id")
    suspend fun getById(id: Long): BudgetEntity?

    @Query("SELECT * FROM budget ORDER BY fecha DESC")
    suspend fun getAll(): List<BudgetEntity>

    @Query("SELECT COUNT(*) FROM budget")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity): Long

    @Update
    suspend fun update(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)

    @Query("DELETE FROM budget WHERE clientId = :clientId")
    suspend fun deleteByClient(clientId: Long)
}

@Dao
interface ReceiptDao {
    @Query(
        """
        SELECT r.*,
               COALESCE(c.nombre, '') AS clienteNombre,
               COALESCE(c.telefono, '') AS clienteTelefono,
               COALESCE(NULLIF(TRIM(e.brand || ' ' || e.model), ''), '') AS equipoDescripcion
        FROM receipt r
        LEFT JOIN client c ON c.id = r.clientId
        LEFT JOIN equipment e ON e.id = r.equipmentId
        ORDER BY r.fecha DESC
        """
    )
    fun observeAll(): Flow<List<ComprobanteRow>>

    @Query(
        """
        SELECT r.*,
               COALESCE(c.nombre, '') AS clienteNombre,
               COALESCE(c.telefono, '') AS clienteTelefono,
               COALESCE(NULLIF(TRIM(e.brand || ' ' || e.model), ''), '') AS equipoDescripcion
        FROM receipt r
        LEFT JOIN client c ON c.id = r.clientId
        LEFT JOIN equipment e ON e.id = r.equipmentId
        WHERE r.clientId = :clientId
        ORDER BY r.fecha DESC
        """
    )
    fun observeByClient(clientId: Long): Flow<List<ComprobanteRow>>

    @Query("SELECT * FROM receipt WHERE id = :id")
    suspend fun getById(id: Long): ReceiptEntity?

    @Query("SELECT * FROM receipt ORDER BY fecha DESC")
    suspend fun getAll(): List<ReceiptEntity>

    @Query("SELECT COUNT(*) FROM receipt")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(receipt: ReceiptEntity): Long

    @Update
    suspend fun update(receipt: ReceiptEntity)

    @Delete
    suspend fun delete(receipt: ReceiptEntity)

    @Query("DELETE FROM receipt WHERE clientId = :clientId")
    suspend fun deleteByClient(clientId: Long)
}

@Dao
interface WorkDao {
    @Query(
        """
        SELECT w.*,
               COALESCE(c.nombre, '') AS clienteNombre,
               COALESCE(c.telefono, '') AS clienteTelefono,
               COALESCE(t.nombre, '') AS tecnicoNombre,
               COALESCE(NULLIF(TRIM(e.brand || ' ' || e.model), ''), '') AS equipoDescripcion
        FROM work w
        LEFT JOIN client c ON c.id = w.clientId
        LEFT JOIN technician t ON t.id = w.technicianId
        LEFT JOIN equipment e ON e.id = w.equipmentId
        ORDER BY w.fecha DESC
        """
    )
    fun observeAll(): Flow<List<TrabajoRow>>

    @Query(
        """
        SELECT w.*,
               COALESCE(c.nombre, '') AS clienteNombre,
               COALESCE(c.telefono, '') AS clienteTelefono,
               COALESCE(t.nombre, '') AS tecnicoNombre,
               COALESCE(NULLIF(TRIM(e.brand || ' ' || e.model), ''), '') AS equipoDescripcion
        FROM work w
        LEFT JOIN client c ON c.id = w.clientId
        LEFT JOIN technician t ON t.id = w.technicianId
        LEFT JOIN equipment e ON e.id = w.equipmentId
        WHERE w.technicianId = :technicianId
        ORDER BY w.fecha DESC
        """
    )
    fun observeByTechnician(technicianId: Long): Flow<List<TrabajoRow>>

    @Query("SELECT * FROM work WHERE id = :id")
    suspend fun getById(id: Long): WorkEntity?

    @Query("DELETE FROM work WHERE clientId = :clientId")
    suspend fun deleteByClient(clientId: Long)

    @Query("SELECT COUNT(*) FROM work")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(work: WorkEntity): Long

    @Update
    suspend fun update(work: WorkEntity)

    @Delete
    suspend fun delete(work: WorkEntity)
}

@Dao
interface TurnDao {
    @Query(
        """
        SELECT t.*,
               COALESCE(c.nombre, '') AS clienteNombre,
               COALESCE(c.telefono, '') AS clienteTelefono,
               COALESCE(tec.nombre, '') AS tecnicoNombre
        FROM turn t
        LEFT JOIN client c ON c.id = t.clientId
        LEFT JOIN technician tec ON tec.id = t.technicianId
        ORDER BY t.fecha ASC, t.hora ASC
        """
    )
    fun observeAll(): Flow<List<TurnoRow>>

    @Query(
        """
        SELECT t.*,
               COALESCE(c.nombre, '') AS clienteNombre,
               COALESCE(c.telefono, '') AS clienteTelefono,
               COALESCE(tec.nombre, '') AS tecnicoNombre
        FROM turn t
        LEFT JOIN client c ON c.id = t.clientId
        LEFT JOIN technician tec ON tec.id = t.technicianId
        WHERE t.fecha LIKE :fecha || '%'
        ORDER BY t.hora ASC
        """
    )
    fun observeByDate(fecha: String): Flow<List<TurnoRow>>

    @Query("SELECT * FROM turn WHERE id = :id")
    suspend fun getById(id: Long): TurnEntity?

    @Query("DELETE FROM turn WHERE clientId = :clientId")
    suspend fun deleteByClient(clientId: Long)

    @Query("SELECT COUNT(*) FROM turn")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(turn: TurnEntity): Long

    @Update
    suspend fun update(turn: TurnEntity)

    @Delete
    suspend fun delete(turn: TurnEntity)
}

@Dao
abstract class CounterDao {
    @Query("SELECT value FROM counter WHERE clave = :clave")
    protected abstract suspend fun getValue(clave: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsert(counter: CounterEntity)

    @Transaction
    open suspend fun siguienteNumero(clave: String, prefijo: String, largo: Int): String {
        val nuevo = (getValue(clave) ?: 0L) + 1
        upsert(CounterEntity(clave, nuevo))
        return prefijo + nuevo.toString().padStart(largo, '0')
    }
}

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photo WHERE tipo = :tipo AND ownerId = :ownerId ORDER BY creado ASC")
    fun observeByOwner(tipo: String, ownerId: Long): Flow<List<PhotoEntity>>

    @Query("SELECT COUNT(*) FROM photo WHERE tipo = :tipo AND ownerId = :ownerId")
    suspend fun countByOwner(tipo: String, ownerId: Long): Int

    @Query("SELECT * FROM photo WHERE tipo = :tipo AND ownerId = :ownerId ORDER BY creado ASC")
    suspend fun getByOwner(tipo: String, ownerId: Long): List<PhotoEntity>

    @Query("SELECT * FROM photo WHERE id = :id")
    suspend fun getById(id: Long): PhotoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: PhotoEntity): Long

    @Delete
    suspend fun delete(photo: PhotoEntity)

    @Query("DELETE FROM photo WHERE tipo = :tipo AND ownerId = :ownerId")
    suspend fun deleteByOwner(tipo: String, ownerId: Long)
}

/** Perfil de la empresa: una sola fila (id = 1). */
@Dao
interface CompanyProfileDao {
    @Query("SELECT * FROM company_profile WHERE id = 1")
    fun observe(): Flow<CompanyProfileEntity?>

    @Query("SELECT * FROM company_profile WHERE id = 1")
    suspend fun get(): CompanyProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: CompanyProfileEntity)
}

/** Catalogo de trabajos con precio de referencia. */
@Dao
interface CatalogDao {
    @Query("SELECT * FROM catalog_item WHERE activo = 1 ORDER BY familia ASC, concepto ASC")
    suspend fun getActivos(): List<CatalogItemEntity>

    @Query("SELECT * FROM catalog_item ORDER BY familia ASC, concepto ASC")
    fun observeAll(): Flow<List<CatalogItemEntity>>

    @Query("SELECT COUNT(*) FROM catalog_item")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CatalogItemEntity): Long
}

/** Recargos disponibles para sumar a un presupuesto. */
@Dao
interface ChargeCatalogDao {
    @Query("SELECT * FROM charge_catalog WHERE activo = 1 ORDER BY nombre ASC")
    suspend fun getActivos(): List<ChargeCatalogEntity>

    @Query("SELECT COUNT(*) FROM charge_catalog")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(charge: ChargeCatalogEntity): Long
}

/** Renglones de un presupuesto. */
@Dao
interface BudgetItemDao {
    @Query("SELECT * FROM budget_item WHERE budgetId = :budgetId ORDER BY orden ASC, id ASC")
    suspend fun getByBudget(budgetId: Long): List<BudgetItemEntity>

    @Query("SELECT COUNT(*) FROM budget_item")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: BudgetItemEntity): Long

    @Update
    suspend fun update(item: BudgetItemEntity)

    @Delete
    suspend fun delete(item: BudgetItemEntity)

    @Query("DELETE FROM budget_item WHERE budgetId = :budgetId")
    suspend fun deleteByBudget(budgetId: Long)
}

/** Recargos aplicados a un presupuesto. */
@Dao
interface BudgetChargeDao {
    @Query("SELECT * FROM budget_charge WHERE budgetId = :budgetId ORDER BY orden ASC, id ASC")
    suspend fun getByBudget(budgetId: Long): List<BudgetChargeEntity>

    @Query("SELECT COUNT(*) FROM budget_charge")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(charge: BudgetChargeEntity): Long

    @Update
    suspend fun update(charge: BudgetChargeEntity)

    @Delete
    suspend fun delete(charge: BudgetChargeEntity)

    @Query("DELETE FROM budget_charge WHERE budgetId = :budgetId")
    suspend fun deleteByBudget(budgetId: Long)
}

/** Plantillas de presupuesto y sus renglones. */
@Dao
interface BudgetTemplateDao {
    @Query("SELECT * FROM budget_template WHERE activo = 1 ORDER BY familia ASC, nombre ASC")
    suspend fun getActivos(): List<BudgetTemplateEntity>

    @Query("SELECT * FROM budget_template_item WHERE templateId = :templateId ORDER BY orden ASC, id ASC")
    suspend fun getItems(templateId: Long): List<BudgetTemplateItemEntity>

    @Query("SELECT COUNT(*) FROM budget_template")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: BudgetTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: BudgetTemplateItemEntity): Long
}
