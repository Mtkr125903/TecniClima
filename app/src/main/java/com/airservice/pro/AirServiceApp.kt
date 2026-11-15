package com.airservice.pro

import android.app.Application
import com.airservice.pro.data.AppDatabase
import com.airservice.pro.data.BudgetEntity
import com.airservice.pro.data.ClientEntity
import com.airservice.pro.data.EquipmentEntity
import com.airservice.pro.data.ReceiptEntity
import com.airservice.pro.data.ServiceEntity
import com.airservice.pro.data.TechnicianEntity
import com.airservice.pro.data.TurnEntity
import com.airservice.pro.data.WorkEntity
import com.airservice.pro.util.FechaUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AirServiceApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch { DataSeeder.sembrar(AppDatabase.getInstance(this@AirServiceApp)) }
    }
}

/**
 * Carga un conjunto minimo de datos de demostracion la primera vez.
 * Si la base ya tiene clientes no se inserta nada.
 */
object DataSeeder {

    suspend fun sembrar(db: AppDatabase) {
        if (db.clientDao().count() > 0) return

        val clientes = listOf(
            ClientEntity(
                nombre = "María Fernández",
                telefono = "+54 9 11 5555 1010",
                email = "maria.fernandez@correo.com",
                direccion = "Av. Corrientes 2450, CABA",
                notas = "Prefiere visitas por la mañana."
            ),
            ClientEntity(
                nombre = "Taller Norte Industrial",
                telefono = "+54 9 11 5555 2020",
                email = "compras@tnorte.com",
                direccion = "Ruta 8 Km 32, Pilar",
                notas = "Empresa con 6 equipos splits."
            ),
            ClientEntity(
                nombre = "Jorge Ramírez",
                telefono = "+54 9 11 5555 3030",
                email = "jorge.r@correo.com",
                direccion = "Calle Tucumán 1204, CABA",
                notas = ""
            )
        ).map { db.clientDao().insert(it) }

        val tecnicos = listOf(
            TechnicianEntity(
                nombre = "Juan Pérez",
                telefono = "+54 9 11 5555 4001",
                especialidad = "Split y制冷",
                activo = true
            ),
            TechnicianEntity(
                nombre = "Lucía Gómez",
                telefono = "+54 9 11 5555 4002",
                especialidad = "Cámaras frigoríficas",
                activo = true
            ),
            TechnicianEntity(
                nombre = "Diego Soto",
                telefono = "+54 9 11 5555 4003",
                especialidad = "Calderas",
                activo = false
            )
        ).map { db.technicianDao().insert(it) }

        val equipos = listOf(
            EquipmentEntity(
                clientId = clientes[0],
                type = "Split",
                brand = "Samsung",
                model = "Wind-Free 12.000",
                serial = "SN-8A21-0092",
                installDate = "2023-03-14",
                capacityW = "3500",
                capacityBtu = "12000",
                gas = "R410A",
                location = "Dormitorio principal",
                direccion = "Av. Corrientes 2450, CABA"
            ),
            EquipmentEntity(
                clientId = clientes[0],
                type = "Split",
                brand = "LG",
                model = "Cool 9000",
                serial = "LG-4410-2211",
                installDate = "2024-01-22",
                capacityW = "2600",
                capacityBtu = "9000",
                gas = "R32",
                location = "Living",
                direccion = "Av. Corrientes 2450, CABA"
            ),
            EquipmentEntity(
                clientId = clientes[1],
                type = "Cámara frigorífica",
                brand = "Cold Point",
                model = "CP-1200",
                serial = "CP-88-4402",
                installDate = "2022-11-02",
                capacityW = "5200",
                capacityBtu = "",
                gas = "R404A",
                location = "Depósito 1",
                direccion = "Ruta 8 Km 32, Pilar"
            ),
            EquipmentEntity(
                clientId = clientes[2],
                type = "Aire acondicionado",
                brand = "BGH",
                model = "Vita 3500",
                serial = "BGH-0099-5512",
                installDate = "2021-06-18",
                capacityW = "1000",
                capacityBtu = "3500",
                gas = "R410A",
                location = "Habitación",
                direccion = "Calle Tucumán 1204, CABA"
            )
        ).map { db.equipmentDao().insert(it) }

        val hoy = FechaUtil.hoyIso()
        val hace3 = FechaUtil.sumarDias(hoy, -3)
        val hace5 = FechaUtil.sumarDias(hoy, -5)
        val hace10 = FechaUtil.sumarDias(hoy, -10)
        val hace12 = FechaUtil.sumarDias(hoy, -12)
        val hace25 = FechaUtil.sumarDias(hoy, -25)

        listOf(
            ServiceEntity(
                clientId = clientes[0],
                equipmentId = equipos[0],
                tipo = "Preventivo",
                descripcion = "Limpieza de filtros y revisión de presión de gas.",
                estado = "Completado",
                fecha = "$hace25 10:30",
                technicianId = tecnicos[0],
                costo = "45000"
            ),
            ServiceEntity(
                clientId = clientes[0],
                equipmentId = equipos[1],
                tipo = "Reparación",
                descripcion = "Reemplazo de turbina y control remoto.",
                estado = "Completado",
                fecha = "$hace10 15:45",
                technicianId = tecnicos[0],
                costo = "98000"
            ),
            ServiceEntity(
                clientId = clientes[1],
                equipmentId = equipos[2],
                tipo = "Fuga",
                descripcion = "Detección de fuga de refrigerante en la cámara 1.",
                estado = "En proceso",
                fecha = "$hace3 08:15",
                technicianId = tecnicos[1],
                costo = "135000"
            ),
            ServiceEntity(
                clientId = clientes[2],
                equipmentId = equipos[3],
                tipo = "Limpieza",
                descripcion = "Lavado de serpentina y control de filtros.",
                estado = "Pendiente",
                fecha = "$hoy 09:00",
                technicianId = tecnicos[0],
                costo = "32000"
            ),
            ServiceEntity(
                clientId = clientes[1],
                equipmentId = equipos[2],
                tipo = "Preventivo",
                descripcion = "Mantenimiento programado trimestral.",
                estado = "Completado",
                fecha = "$hace3 17:00",
                technicianId = tecnicos[1],
                costo = "78000"
            ),
            ServiceEntity(
                clientId = clientes[2],
                equipmentId = equipos[3],
                tipo = "Instalación",
                descripcion = "Traslado de equipo a nueva habitación.",
                estado = "Cancelado",
                fecha = "$hace10 11:20",
                technicianId = tecnicos[2],
                costo = "0"
            )
        ).map { db.serviceDao().insert(it) }

        val presupuestos = listOf(
            BudgetEntity(
                clientId = clientes[1],
                equipmentId = equipos[2],
                numero = db.counterDao().siguienteNumero("presupuesto", "PRE-", 4),
                monto = "158000",
                descripcion = "Reparación de cámara frigorífica 1",
                detalle = "Carga de gas R404A\nCambio de presostato\nPrueba de estanqueidad",
                estado = "Pendiente de aprobación",
                fecha = "$hace3 12:00"
            ),
            BudgetEntity(
                clientId = clientes[0],
                equipmentId = equipos[1],
                numero = db.counterDao().siguienteNumero("presupuesto", "PRE-", 4),
                monto = "87000",
                descripcion = "Cambio de turbina y tablero",
                detalle = "Turbina original\nTablero de control\nMano de obra",
                estado = "Aprobado",
                fecha = "$hace12 09:30"
            ),
            BudgetEntity(
                clientId = clientes[2],
                equipmentId = equipos[3],
                numero = db.counterDao().siguienteNumero("presupuesto", "PRE-", 4),
                monto = "42000",
                descripcion = "Mantenimiento preventivo anual",
                detalle = "Limpieza\nControl de presiones\nCertificado",
                estado = "Enviado",
                fecha = "$hace5 16:00"
            )
        ).map { db.budgetDao().insert(it) }

        listOf(
            ReceiptEntity(
                clientId = clientes[0],
                equipmentId = equipos[1],
                budgetId = presupuestos[1],
                numero = db.counterDao().siguienteNumero("comprobante", "REC-", 4),
                monto = "87000",
                observacion = "Pago en efectivo contra entrega.",
                detalle = "Cambio de turbina y tablero",
                estado = "Pagado",
                fecha = "$hace10 16:10",
                metodoPago = "Efectivo",
                pdfPath = ""
            ),
            ReceiptEntity(
                clientId = clientes[2],
                equipmentId = equipos[3],
                budgetId = presupuestos[2],
                numero = db.counterDao().siguienteNumero("comprobante", "REC-", 4),
                monto = "42000",
                observacion = "",
                detalle = "Mantenimiento preventivo anual",
                estado = "Pendiente",
                fecha = "$hace5 16:30",
                metodoPago = "Transferencia",
                pdfPath = ""
            )
        ).map { db.receiptDao().insert(it) }

        listOf(
            WorkEntity(
                clientId = clientes[1],
                equipmentId = equipos[2],
                technicianId = tecnicos[1],
                titulo = "Reparación cámara frigorífica 1",
                descripcion = "Carga de gas y cambio de presostato.",
                avance = 65,
                estado = "En proceso",
                fecha = "$hace3 08:00"
            ),
            WorkEntity(
                clientId = clientes[0],
                equipmentId = equipos[0],
                technicianId = tecnicos[0],
                titulo = "Preventivo Samsung Wind-Free",
                descripcion = "Limpieza de filtros y control de presión.",
                avance = 100,
                estado = "Completado",
                fecha = "$hace25 10:00"
            ),
            WorkEntity(
                clientId = clientes[2],
                equipmentId = equipos[3],
                technicianId = tecnicos[0],
                titulo = "Lavado de serpentina BGH",
                descripcion = "Lavado de serpentina y cambio de filtros.",
                avance = 20,
                estado = "En proceso",
                fecha = "$hoy 09:00"
            ),
            WorkEntity(
                clientId = clientes[1],
                equipmentId = equipos[2],
                technicianId = tecnicos[1],
                titulo = "Mantenimiento trimestral",
                descripcion = "Control general de las 2 cámaras.",
                avance = 100,
                estado = "Completado",
                fecha = "$hace3 17:00"
            )
        ).map { db.workDao().insert(it) }

        val manana = FechaUtil.sumarDias(hoy, 1)
        val pasado = FechaUtil.sumarDias(hoy, 2)
        listOf(
            TurnEntity(
                clientId = clientes[1],
                technicianId = tecnicos[1],
                titulo = "Carga de gas R404A",
                fecha = hoy,
                hora = "08:30",
                direccion = "Ruta 8 Km 32, Pilar",
                estado = "Confirmado",
                notas = "Llevar manómetro y balanza."
            ),
            TurnEntity(
                clientId = clientes[2],
                technicianId = tecnicos[0],
                titulo = "Lavado de serpentina",
                fecha = hoy,
                hora = "10:15",
                direccion = "Calle Tucumán 1204, CABA",
                estado = "Pendiente",
                notas = ""
            ),
            TurnEntity(
                clientId = clientes[0],
                technicianId = tecnicos[0],
                titulo = "Control de preventivo",
                fecha = manana,
                hora = "09:00",
                direccion = "Av. Corrientes 2450, CABA",
                estado = "Pendiente",
                notas = "Cliente propone por la mañana."
            ),
            TurnEntity(
                clientId = clientes[1],
                technicianId = tecnicos[1],
                titulo = "Revisión cámara 2",
                fecha = pasado,
                hora = "14:30",
                direccion = "Ruta 8 Km 32, Pilar",
                estado = "Pendiente",
                notas = ""
            ),
            TurnEntity(
                clientId = clientes[0],
                technicianId = tecnicos[2],
                titulo = "Relevamiento de equipos",
                fecha = pasado,
                hora = "11:00",
                direccion = "Av. Corrientes 2450, CABA",
                estado = "Cancelado",
                notas = " reprogramado."
            )
        ).forEach { db.turnDao().insert(it) }
    }
}
