package com.airservice.pro.ui.equipment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.airservice.pro.R
import com.airservice.pro.data.AppDatabase
import com.airservice.pro.data.EquipmentEntity
import com.airservice.pro.data.PhotoEntity
import com.airservice.pro.databinding.FragmentEquipoFichaBinding
import com.airservice.pro.ui.common.UiHelpers
import com.airservice.pro.ui.history.ServiceHistoryAdapter
import com.airservice.pro.util.FechaUtil
import com.airservice.pro.util.PhotoStorage
import com.airservice.pro.util.WhatsAppHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EquipmentDetailFragment : Fragment() {

    private var _binding: FragmentEquipoFichaBinding? = null
    private val binding get() = _binding!!

    private val equipmentId: Long by lazy { requireArguments().getLong(ARG_EQUIPMENT_ID) }
    private val db by lazy { AppDatabase.getInstance(requireContext()) }

    private lateinit var adapterFotos: PhotoAdapter
    private var uriCamara: Uri? = null

    /** Camara: se crea el archivo antes de abrir la app de camara. */
    private val tomarFoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = uriCamara
        uriCamara = null
        if (ok && uri != null) {
            guardarFoto(uri.toString())
        }
    }

    /** Galeria: se copia la imagen al almacenamiento propio de la app. */
    private val elegirFoto = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { origen ->
        if (origen == null) return@registerForActivityResult
        viewLifecycleOwner.lifecycleScope.launch {
            val uri = withContext(Dispatchers.IO) {
                PhotoStorage.copiarDesde(requireContext(), origen)
            }
            if (uri == null) {
                Snackbar.make(binding.root, R.string.msg_photo_error, Snackbar.LENGTH_SHORT).show()
            } else {
                guardarFoto(uri.toString())
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEquipoFichaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapterServicios = ServiceHistoryAdapter { }
        binding.rvServicios.layoutManager = LinearLayoutManager(requireContext())
        binding.rvServicios.isNestedScrollingEnabled = false
        binding.rvServicios.adapter = adapterServicios

        adapterFotos = PhotoAdapter(
            onClick = { PhotoViewerDialog.newInstance(it.uri).show(parentFragmentManager, "foto") },
            onDelete = { confirmarBorrarFoto(it) }
        )
        binding.rvFotos.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvFotos.adapter = adapterFotos
        binding.btnAgregarFoto.setOnClickListener { agregarFoto() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    val equipo = db.equipmentDao().getById(equipmentId)
                    if (equipo == null) {
                        findNavController().navigateUp()
                    } else {
                        bindEquipo(equipo)
                    }
                }
                launch {
                    db.serviceDao().observeByEquipment(equipmentId).collect {
                        adapterServicios.submitList(it)
                        UiHelpers.emptyState(binding.tvEmptyServicios, it.isEmpty())
                    }
                }
                launch {
                    db.photoDao().observeByOwner(TIPO_FOTO, equipmentId).collect { fotos ->
                        adapterFotos.submitList(fotos)
                        binding.rvFotos.visibility = if (fotos.isEmpty()) View.GONE else View.VISIBLE
                        binding.tvSinFotos.visibility = if (fotos.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun bindEquipo(equipo: EquipmentEntity) {
        binding.tvTipo.text = equipo.type.ifBlank { getString(R.string.label_equipment) }
        binding.tvMarca.text = listOf(equipo.brand, equipo.model)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { getString(R.string.hint_brand) }

        viewLifecycleOwner.lifecycleScope.launch {
            val cliente = withContext(Dispatchers.IO) {
                db.clientDao().getById(equipo.clientId)
            }
            binding.tvCliente.text = cliente?.nombre ?: getString(R.string.msg_no_client_selected)
            binding.btnLlamar.setOnClickListener {
                WhatsAppHelper.abrirLlamada(requireContext(), cliente?.telefono)
            }
            binding.btnWhatsapp.setOnClickListener {
                WhatsAppHelper.enviarWhatsApp(
                    requireContext(),
                    cliente?.telefono,
                    getString(R.string.whatsapp_generic_body, cliente?.nombre.orEmpty())
                )
            }
        }

        binding.tvSerial.text = getString(R.string.label_serial) + ": " +
            equipo.serial.ifBlank { "-" }
        binding.tvInstalacion.text = getString(R.string.label_install_date) + ": " +
            equipo.installDate.ifBlank { "-" }
        binding.tvCapacidad.text = getString(R.string.label_capacity) + ": " +
            listOf(
                equipo.capacityW.takeIf { it.isNotBlank() }?.let { "$it W" },
                equipo.capacityBtu.takeIf { it.isNotBlank() }?.let { "$it BTU" }
            ).joinToString(" · ").ifBlank { "-" }
        binding.tvGas.text = getString(R.string.label_gas) + ": " + equipo.gas.ifBlank { "-" }
        binding.tvUbicacion.text = equipo.location.ifBlank { "-" }
        binding.tvDireccion.text = equipo.direccion.ifBlank { "-" }

        binding.btnEditar.setOnClickListener { EquipmentForm.show(this, equipo) }
        binding.btnEliminar.setOnClickListener { confirmarEliminar(equipo) }
    }

    private fun confirmarEliminar(equipo: EquipmentEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_message, equipo.brand))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        val fotos = db.photoDao().getByOwner(TIPO_FOTO, equipo.id)
                        fotos.forEach { PhotoStorage.borrarArchivo(requireContext(), it.uri) }
                        db.photoDao().deleteByOwner(TIPO_FOTO, equipo.id)
                        db.serviceDao().deleteByEquipment(equipo.id)
                        db.equipmentDao().delete(equipo)
                    }
                    findNavController().navigateUp()
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    // ------------------------------------------------------------------
    // Fotos
    // ------------------------------------------------------------------
    private fun agregarFoto() {
        viewLifecycleOwner.lifecycleScope.launch {
            val actuales = withContext(Dispatchers.IO) {
                db.photoDao().countByOwner(TIPO_FOTO, equipmentId)
            }
            if (actuales >= MAX_FOTOS) {
                Snackbar.make(
                    binding.root,
                    resources.getQuantityString(R.plurals.msg_photo_limit, MAX_FOTOS, MAX_FOTOS),
                    Snackbar.LENGTH_SHORT
                ).show()
                return@launch
            }
            val opciones = arrayOf(
                getString(R.string.action_take_photo),
                getString(R.string.action_pick_photo)
            )
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.action_add_photo)
                .setItems(opciones) { _, which ->
                    if (which == 0) tomarFotoConCamara() else {
                        elegirFoto.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        }
    }

    private fun tomarFotoConCamara() {
        val (_, uri) = PhotoStorage.nuevoArchivo(requireContext())
        uriCamara = uri
        runCatching { tomarFoto.launch(uri) }
            .onFailure {
                uriCamara = null
                Snackbar.make(binding.root, R.string.msg_no_camera, Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun guardarFoto(uri: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.photoDao().insert(
                    PhotoEntity(
                        tipo = TIPO_FOTO,
                        ownerId = equipmentId,
                        uri = uri,
                        creado = FechaUtil.ahoraIso()
                    )
                )
            }
            Snackbar.make(binding.root, R.string.msg_photo_added, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun confirmarBorrarFoto(foto: PhotoEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_delete_photo)
            .setMessage(R.string.dialog_delete_photo_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        db.photoDao().delete(foto)
                        PhotoStorage.borrarArchivo(requireContext(), foto.uri)
                    }
                    Snackbar.make(binding.root, R.string.msg_photo_deleted, Snackbar.LENGTH_SHORT).show()
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
        private const val ARG_EQUIPMENT_ID = "equipmentId"
        private const val TIPO_FOTO = "EQUIPMENT"
        private const val MAX_FOTOS = 6

        fun newInstance(equipmentId: Long) = EquipmentDetailFragment().apply {
            arguments = Bundle().apply { putLong(ARG_EQUIPMENT_ID, equipmentId) }
        }
    }
}
