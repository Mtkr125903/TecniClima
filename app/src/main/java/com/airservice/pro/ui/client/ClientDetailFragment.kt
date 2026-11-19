package com.airservice.pro.ui.client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.withTransaction
import com.airservice.pro.R
import com.airservice.pro.data.AppDatabase
import com.airservice.pro.data.ClientEntity
import com.airservice.pro.databinding.FragmentClienteFichaBinding
import com.airservice.pro.ui.budget.PresupuestosAdapter
import com.airservice.pro.ui.common.UiHelpers
import com.airservice.pro.ui.equipment.EquipmentAdapter
import com.airservice.pro.ui.equipment.EquipmentForm
import com.airservice.pro.ui.history.ServiceHistoryAdapter
import com.airservice.pro.ui.receipt.ReceiptsAdapter
import com.airservice.pro.util.PhotoStorage
import com.airservice.pro.util.WhatsAppHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClientDetailFragment : Fragment() {

    private var _binding: FragmentClienteFichaBinding? = null
    private val binding get() = _binding!!

    private var clienteActual: ClientEntity? = null

    private val clientId: Long by lazy { requireArguments().getLong(ARG_CLIENT_ID) }
    private val db by lazy { AppDatabase.getInstance(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClienteFichaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapterEquipos = EquipmentAdapter { }
        val adapterServicios = ServiceHistoryAdapter { }
        val adapterPresupuestos = PresupuestosAdapter({}, {}, {}, {}, {})
        val adapterComprobantes = ReceiptsAdapter({}, {}, {})

        listOf(
            binding.rvEquipos to adapterEquipos,
            binding.rvServicios to adapterServicios,
            binding.rvPresupuestos to adapterPresupuestos,
            binding.rvComprobantes to adapterComprobantes
        ).forEach { (rv, adapter) ->
            rv.layoutManager = LinearLayoutManager(requireContext())
            rv.isNestedScrollingEnabled = false
            rv.adapter = adapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    db.clientDao().observeById(clientId).collect { cliente ->
                        if (cliente == null) {
                            findNavController().navigateUp()
                        } else {
                            bindCliente(cliente)
                        }
                    }
                }
                launch {
                    db.equipmentDao().observeByClient(clientId).collect {
                        adapterEquipos.submitList(it)
                        UiHelpers.emptyState(binding.tvEmptyEquipos, it.isEmpty())
                        binding.btnAgregarEquipo.visibility =
                            if (it.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    db.serviceDao().observeByClient(clientId).collect {
                        adapterServicios.submitList(it)
                        UiHelpers.emptyState(binding.tvEmptyServicios, it.isEmpty())
                    }
                }
                launch {
                    db.budgetDao().observeByClient(clientId).collect {
                        adapterPresupuestos.submitList(it)
                        UiHelpers.emptyState(binding.tvEmptyPresupuestos, it.isEmpty())
                    }
                }
                launch {
                    db.receiptDao().observeByClient(clientId).collect {
                        adapterComprobantes.submitList(it)
                        UiHelpers.emptyState(binding.tvEmptyComprobantes, it.isEmpty())
                    }
                }
            }
        }
    }

    private fun bindCliente(cliente: ClientEntity) {
        clienteActual = cliente
        binding.tvNombre.text = cliente.nombre
        binding.tvContacto.text = listOfNotNull(
            cliente.telefono.takeIf { it.isNotBlank() },
            cliente.email.takeIf { it.isNotBlank() }
        ).joinToString(" · ").ifBlank { getString(R.string.msg_no_phone) }
        binding.tvDireccion.text = cliente.direccion.ifBlank { getString(R.string.hint_address) }
        binding.tvNotas.text = cliente.notas
        binding.tvNotas.visibility = if (cliente.notas.isBlank()) View.GONE else View.VISIBLE

        binding.btnLlamar.setOnClickListener {
            WhatsAppHelper.abrirLlamada(requireContext(), cliente.telefono)
        }
        binding.btnWhatsapp.setOnClickListener {
            WhatsAppHelper.enviarWhatsApp(
                requireContext(),
                cliente.telefono,
                getString(R.string.whatsapp_generic_body, cliente.nombre)
            )
        }
        binding.btnEditar.setOnClickListener { ClientForm.show(this, cliente) }
        binding.btnEliminar.setOnClickListener { confirmarEliminar(cliente) }
        binding.btnAgregarEquipo.setOnClickListener {
            clienteActual?.let { EquipmentForm.show(this, null, it) }
        }
    }

    private fun confirmarEliminar(cliente: ClientEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(
                getString(R.string.dialog_delete_message, cliente.nombre) + "\n\n" +
                    getString(R.string.dialog_delete_client_message)
            )
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        val equipos = db.equipmentDao().listByClient(cliente.id)
                        val fotos = equipos.flatMap {
                            db.photoDao().getByOwner(PhotoStorage.TIPO_FOTO_EQUIPO, it.id)
                        }
                        db.withTransaction {
                            db.workDao().deleteByClient(cliente.id)
                            db.turnDao().deleteByClient(cliente.id)
                            db.serviceDao().deleteByClient(cliente.id)
                            db.budgetDao().deleteByClient(cliente.id)
                            db.receiptDao().deleteByClient(cliente.id)
                            equipos.forEach {
                                db.photoDao().deleteByOwner(PhotoStorage.TIPO_FOTO_EQUIPO, it.id)
                            }
                            db.equipmentDao().deleteByClient(cliente.id)
                            db.clientDao().delete(cliente)
                        }
                        // Los archivos fisicos se borran recien despues de confirmar la
                        // transaccion: si la DB falla, no se pierden las fotos.
                        fotos.forEach { PhotoStorage.borrarArchivo(requireContext(), it.uri) }
                    }
                    findNavController().navigateUp()
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CLIENT_ID = "clientId"

        fun newInstance(clientId: Long) = ClientDetailFragment().apply {
            arguments = Bundle().apply { putLong(ARG_CLIENT_ID, clientId) }
        }
    }
}
