package com.airservice.pro.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "client")
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val telefono: String,
    val email: String,
    val direccion: String,
    val notas: String
)

@Entity(tableName = "technician")
data class TechnicianEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val telefono: String,
    val especialidad: String,
    val activo: Boolean
)

@Entity(tableName = "equipment", indices = [Index("clientId")])
data class EquipmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val type: String,
    val brand: String,
    val model: String,
    val serial: String,
    val installDate: String,
    val capacityW: String,
    val capacityBtu: String,
    val gas: String,
    val location: String,
    val direccion: String
)

@Entity(tableName = "service", indices = [Index("clientId"), Index("equipmentId")])
data class ServiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val equipmentId: Long,
    val tipo: String,
    val descripcion: String,
    val estado: String,
    val fecha: String,
    val technicianId: Long,
    val costo: String
)

@Entity(tableName = "receipt", indices = [Index("clientId")])
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val equipmentId: Long,
    val budgetId: Long,
    val numero: String,
    val monto: String,
    val observacion: String,
    val detalle: String,
    val estado: String,
    val fecha: String,
    val metodoPago: String,
    val pdfPath: String
)

@Entity(tableName = "work", indices = [Index("clientId"), Index("technicianId")])
data class WorkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val equipmentId: Long,
    val technicianId: Long,
    val titulo: String,
    val descripcion: String,
    val avance: Int,
    val estado: String,
    val fecha: String
)

@Entity(tableName = "turn", indices = [Index("clientId"), Index("technicianId")])
data class TurnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val technicianId: Long,
    val titulo: String,
    val fecha: String,
    val hora: String,
    val direccion: String,
    val estado: String,
    val notas: String
)

@Entity(tableName = "counter")
data class CounterEntity(
    @PrimaryKey val clave: String,
    val value: Long
)

/**
 * Datos de la empresa que imprime los documentos. Es una tabla de una sola
 * fila (id = 1) con los datos de contacto, la matricula, el alias de pago
 * y la validez por defecto de los presupuestos.
 */
@Entity(tableName = "company_profile")
data class CompanyProfileEntity(
    @PrimaryKey val id: Long = 1,
    val nombre: String = "",
    val matricula: String = "",
    val cuit: String = "",
    val telefono: String = "",
    val whatsapp: String = "",
    val aliasPago: String = "",
    val direccion: String = "",
    val logoPath: String = "",
    val validezDias: Int = 15
)

/**
 * Catalogo de trabajos con precio de referencia. Sirve para cargar un
 * presupuesto en segundos y para renombrar el tipo de los servicios.
 */
@Entity(tableName = "catalog_item", indices = [Index("familia")])
data class CatalogItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val familia: String,
    val concepto: String,
    val precio: String,
    val duracionHs: String,
    val activo: Boolean = true
)

/**
 * Recargo disponible (urgencia, fin de semana, pico de temporada, etc.).
 * Los de temporada solo se aplican automaticamente entre octubre y marzo.
 */
@Entity(tableName = "charge_catalog")
data class ChargeCatalogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val modo: String,
    val valor: String,
    val temporada: Boolean = false,
    val activo: Boolean = true
)

/** Renglon de un presupuesto: concepto, cantidad y precio unitario. */
@Entity(tableName = "budget_item", indices = [Index("budgetId")])
data class BudgetItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val budgetId: Long,
    val concepto: String,
    val cantidad: Double,
    val precio: String,
    val subtotal: String,
    val orden: Int
)

/** Recargo aplicado a un presupuesto (con su importe ya calculado). */
@Entity(tableName = "budget_charge", indices = [Index("budgetId")])
data class BudgetChargeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val budgetId: Long,
    val nombre: String,
    val modo: String,
    val valor: String,
    val importe: String,
    val orden: Int
)

/** Plantilla de presupuesto: al usarla se cargan sus renglones. */
@Entity(tableName = "budget_template", indices = [Index("familia")])
data class BudgetTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val familia: String,
    val descripcion: String,
    val activo: Boolean = true
)

/** Renglon de una plantilla de presupuesto. */
@Entity(tableName = "budget_template_item", indices = [Index("templateId")])
data class BudgetTemplateItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val concepto: String,
    val cantidad: Double,
    val precio: String,
    val orden: Int
)

/** BudgetEntity con campos extendidos (validez, vencimiento, condiciones). */
@Entity(tableName = "budget", indices = [Index("clientId"), Index("equipmentId")])
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val equipmentId: Long,
    val numero: String,
    val monto: String,
    val descripcion: String,
    val detalle: String,
    val estado: String,
    val fecha: String,
    val validezDias: Int = 15,
    val vence: String = "",
    val sena: String = "",
    val garantia: String = "",
    val incluye: String = "",
    val noIncluye: String = ""
)

/** Fila de consulta para equipos (JOIN equipment + client). */
data class EquipoRow(
    @Embedded val equipo: EquipmentEntity,
    val clienteNombre: String,
    val clienteTelefono: String
)

/** Fila de consulta para servicios (JOIN service + client + equipment + technician). */
data class ServicioRow(
    @Embedded val servicio: ServiceEntity,
    val clienteNombre: String,
    val equipoDescripcion: String,
    val tecnicoNombre: String
)

/** Fila de consulta para presupuestos (JOIN budget + client + equipment). */
data class PresupuestoRow(
    @Embedded val presupuesto: BudgetEntity,
    val clienteNombre: String,
    val clienteTelefono: String,
    val equipoDescripcion: String
)

/** Fila de consulta para comprobantes (JOIN receipt + client + equipment). */
data class ComprobanteRow(
    @Embedded val comprobante: ReceiptEntity,
    val clienteNombre: String,
    val clienteTelefono: String,
    val equipoDescripcion: String
)

/** Fila de consulta para turnos (JOIN turn + client + technician). */
data class TurnoRow(
    @Embedded val turno: TurnEntity,
    val clienteNombre: String,
    val clienteTelefono: String,
    val tecnicoNombre: String
)

/** Fila de consulta para trabajos (JOIN work + client + equipment + technician). */
data class TrabajoRow(
    @Embedded val trabajo: WorkEntity,
    val clienteNombre: String,
    val clienteTelefono: String,
    val equipoDescripcion: String,
    val tecnicoNombre: String
)

/** Foto adjunta a un equipo, un servicio o un trabajo. */
@Entity(tableName = "photo", indices = [Index("ownerId")])
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipo: String,
    val ownerId: Long,
    val uri: String,
    val creado: String,
    val nota: String = ""
)
