package com.airservice.pro.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertaYBuscaClientes() = runBlocking {
        val id = db.clientDao().insert(
            ClientEntity(
                nombre = "Ana",
                telefono = "1155550001",
                email = "ana@correo.com",
                direccion = "Calle 1",
                notas = ""
            )
        )
        assertTrue(id > 0)

        val cliente = db.clientDao().getById(id)
        assertNotNull(cliente)
        assertEquals("Ana", cliente!!.nombre)

        val resultados = db.clientDao().search("Ana").first()
        assertEquals(1, resultados.size)
    }

    @Test
    fun losJOINsDevuelvenLosDatosRelacionados() = runBlocking {
        val clienteId = db.clientDao().insert(
            ClientEntity("Taller", "1155550002", "taller@correo.com", "Ruta 8", "")
        )
        val equipoId = db.equipmentDao().insert(
            EquipmentEntity(
                clientId = clienteId,
                type = "Split",
                brand = "Samsung",
                model = "Wind",
                serial = "SN-1",
                installDate = "2024-01-01",
                capacityW = "3500",
                capacityBtu = "12000",
                gas = "R410A",
                location = "Oficina",
                direccion = "Ruta 8"
            )
        )
        db.serviceDao().insert(
            ServiceEntity(
                clientId = clienteId,
                equipmentId = equipoId,
                tipo = "Preventivo",
                descripcion = "Limpieza",
                estado = "Completado",
                fecha = "2024-01-02 10:00",
                technicianId = 0,
                costo = "10000"
            )
        )

        val equipos = db.equipmentDao().observeAll().first()
        assertEquals(1, equipos.size)
        assertEquals("Taller", equipos[0].clienteNombre)
        assertEquals("1155550002", equipos[0].clienteTelefono)

        val servicios = db.serviceDao().observeAll().first()
        assertEquals("Taller", servicios[0].clienteNombre)
        assertEquals("Samsung Wind", servicios[0].equipoDescripcion)
    }

    @Test
    fun elContadorGeneraNumerosConsecutivos() = runBlocking {
        assertEquals("PRE-0001", db.counterDao().siguienteNumero("presupuesto", "PRE-", 4))
        assertEquals("PRE-0002", db.counterDao().siguienteNumero("presupuesto", "PRE-", 4))
        assertEquals("PRE-0003", db.counterDao().siguienteNumero("presupuesto", "PRE-", 4))
        assertEquals("REC-0001", db.counterDao().siguienteNumero("comprobante", "REC-", 4))
    }

    @Test
    fun losTurnosSeFiltranPorFecha() = runBlocking {
        val clienteId = db.clientDao().insert(
            ClientEntity("Cliente", "1155550003", "c@correo.com", "", "")
        )
        db.turnDao().insert(
            TurnEntity(clienteId, 0, "Visita 1", "2024-05-01", "09:00", "Calle 1", "Pendiente", "")
        )
        db.turnDao().insert(
            TurnEntity(clienteId, 0, "Visita 2", "2024-05-02", "10:00", "Calle 2", "Pendiente", "")
        )

        val delDia = db.turnDao().observeByDate("2024-05-01").first()
        assertEquals(1, delDia.size)
        assertEquals("Visita 1", delDia[0].turno.titulo)
    }

    @Test
    fun eliminarClienteBorraEnCascadaLosRegistrosRelacionados() = runBlocking {
        val clienteId = db.clientDao().insert(
            ClientEntity("Cliente", "1155550004", "c@correo.com", "", "")
        )
        db.equipmentDao().insert(
            EquipmentEntity(clienteId, "Split", "LG", "Cool", "S-1", "2024-01-01", "", "", "R32", "", "")
        )
        db.serviceDao().insert(
            ServiceEntity(clienteId, 0, "Reparación", "Cambio", "Pendiente", "2024-01-02 10:00", 0, "")
        )

        db.serviceDao().deleteByClient(clienteId)
        db.equipmentDao().deleteByClient(clienteId)
        val cliente = db.clientDao().getById(clienteId)!!
        db.clientDao().delete(cliente)

        assertEquals(0, db.clientDao().count())
        assertEquals(0, db.equipmentDao().count())
        assertEquals(0, db.serviceDao().count())
    }
}
