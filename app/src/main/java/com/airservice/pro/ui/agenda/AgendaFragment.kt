package com.airservice.pro.ui.agenda

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
import com.airservice.pro.data.TurnEntity
import com.airservice.pro.data.TurnoRow
import com.airservice.pro.databinding.FragmentAgendaBinding
import com.airservice.pro.ui.common.UiHelpers
import com.airservice.pro.util.FechaUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AgendaFragment : Fragment() {

    private var _binding: FragmentAgendaBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AgendaAdapter
    private val dao by lazy { AppDatabase.getInstance(requireContext()).turnDao() }
    private val diaSeleccionado = MutableStateFlow(FechaUtil.hoyIso())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAgendaBinding.inflate(inflater, container, false)
        return binding.root
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AgendaAdapter { abrirOpciones(it) }
        binding.rvTurnos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTurnos.adapter = adapter

        binding.btnDiaAnterior.setOnClickListener { moverDia(-1) }
        binding.btnDiaSiguiente.setOnClickListener { moverDia(1) }
        binding.fabAgregar.setOnClickListener { TurnForm.show(this, null) }

        actualizarCabecera(FechaUtil.hoyIso())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                diaSeleccionado
                    .flatMapLatest { dao.observeByDate(it) }
                    .collect { lista ->
                        adapter.submitList(lista)
                        UiHelpers.emptyState(binding.tvEmpty, lista.isEmpty())
                    }
            }
        }
    }

    private fun moverDia(delta: Int) {
        val nuevo = FechaUtil.sumarDias(diaSeleccionado.value, delta)
        diaSeleccionado.value = nuevo
        actualizarCabecera(nuevo)
    }

    private fun actualizarCabecera(isoDia: String) {
        val dia = FechaUtil.nombreDia(isoDia)
        val fecha = FechaUtil.mostrar(isoDia)
        binding.tvFecha.text = if (dia.isBlank()) fecha else "$dia · $fecha"
    }

    private fun abrirOpciones(row: TurnoRow) {
        val turno = row.turno
        val opciones = arrayOf(
            getString(R.string.action_edit),
            getString(R.string.action_delete)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("${turno.hora} · ${turno.titulo}")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> TurnForm.show(this, turno)
                    1 -> confirmarEliminar(turno)
                }
            }
            .show()
    }

    private fun confirmarEliminar(turno: TurnEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_message, turno.titulo))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) { dao.delete(turno) }
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
