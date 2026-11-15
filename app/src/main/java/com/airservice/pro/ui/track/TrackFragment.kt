package com.airservice.pro.ui.track

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.airservice.pro.R
import com.airservice.pro.data.AppDatabase
import com.airservice.pro.data.TechnicianEntity
import com.airservice.pro.data.TrabajoRow
import com.airservice.pro.data.WorkEntity
import com.airservice.pro.databinding.FragmentTrackBinding
import com.airservice.pro.ui.common.UiHelpers
import com.airservice.pro.ui.technician.TechnicianAdapter
import com.airservice.pro.ui.technician.TechnicianForm
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrackFragment : Fragment() {

    private var _binding: FragmentTrackBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: TrackAdapter
    private val dao by lazy { AppDatabase.getInstance(requireContext()).workDao() }
    private val technicianDao by lazy { AppDatabase.getInstance(requireContext()).technicianDao() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TrackAdapter(
            onAvanceChanged = { row, avance -> actualizarAvance(row, avance) },
            onClick = { abrirOpciones(it.trabajo) }
        )
        binding.rvTrack.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTrack.adapter = adapter

        val adapterTecnicos = TechnicianAdapter { abrirTecnico(it) }
        binding.rvTecnicos.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvTecnicos.adapter = adapterTecnicos

        binding.fabAgregar.setOnClickListener { WorkForm.show(this, null) }
        binding.btnAgregarTecnico.setOnClickListener { TechnicianForm.show(this, null) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    dao.observeAll().collect { lista ->
                        adapter.submitList(lista)
                        UiHelpers.emptyState(binding.tvEmpty, lista.isEmpty())
                    }
                }
                launch {
                    technicianDao.observeAll().collect { adapterTecnicos.submitList(it) }
                }
            }
        }
    }

    private fun actualizarAvance(row: TrabajoRow, avance: Int) {
        val trabajo = row.trabajo
        if (trabajo.avance == avance) return
        val nuevoEstado = if (avance >= 100) "Completado" else trabajo.estado
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                dao.update(trabajo.copy(avance = avance, estado = nuevoEstado))
            }
            Snackbar.make(binding.root, R.string.msg_work_updated, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun abrirOpciones(trabajo: WorkEntity) {
        val opciones = arrayOf(
            getString(R.string.action_edit),
            getString(R.string.action_delete)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(trabajo.titulo)
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> WorkForm.show(this, trabajo)
                    1 -> confirmarEliminar(trabajo)
                }
            }
            .show()
    }

    private fun confirmarEliminar(trabajo: WorkEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_message, trabajo.titulo))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) { dao.delete(trabajo) }
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun abrirTecnico(tecnico: TechnicianEntity) {
        val opciones = arrayOf(
            getString(R.string.action_edit),
            getString(R.string.action_delete)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(tecnico.nombre)
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> TechnicianForm.show(this, tecnico)
                    1 -> confirmarEliminarTecnico(tecnico)
                }
            }
            .show()
    }

    private fun confirmarEliminarTecnico(tecnico: TechnicianEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_message, tecnico.nombre))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) { technicianDao.delete(tecnico) }
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
