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
    val fecha: String
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

// ---------- Filas de consulta (JOIN) ----------

data class EquipoRow(
    @Embedded val equipo: EquipmentEntity,
    val clienteNombre: String,
    val clienteTelefono: String
)

data class ServicioRow(
    @Embedded val servicio: ServiceEntity,
    val clienteNombre: String,
    val equipoDescripcion: String,
    val tecnicoNombre: String
)

data class PresupuestoRow(
    @Embedded val presupuesto: BudgetEntity,
    val clienteNombre: String,
    val clienteTelefono: String,
    val equipoDescripcion: String
)

data class ComprobanteRow(
    @Embedded val comprobante: ReceiptEntity,
    val clienteNombre: String,
    val clienteTelefono: String,
    val equipoDescripcion: String
)

data class TrabajoRow(
    @Embedded val trabajo: WorkEntity,
    val clienteNombre: String,
    val tecnicoNombre: String,
    val equipoDescripcion: String
)

data class TurnoRow(
    @Embedded val turno: TurnEntity,
    val clienteNombre: String,
    val clienteTelefono: String,
    val tecnicoNombre: String
)
