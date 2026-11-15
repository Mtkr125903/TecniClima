package com.airservice.pro.ui.equipment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.airservice.pro.R
import com.airservice.pro.data.AppDatabase
import com.airservice.pro.databinding.FragmentEquipoBinding
import com.airservice.pro.ui.common.UiHelpers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class EquipmentFragment : Fragment() {

    private var _binding: FragmentEquipoBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: EquipmentAdapter
    private val dao by lazy { AppDatabase.getInstance(requireContext()).equipmentDao() }
    private val busqueda = MutableStateFlow("")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEquipoBinding.inflate(inflater, container, false)
        return binding.root
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = EquipmentAdapter { abrirDetalle(it.equipo.id) }
        binding.rvEquipos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEquipos.adapter = adapter

        binding.fabAgregar.setOnClickListener { EquipmentForm.show(this, null) }
        binding.etSearch.doAfterTextChanged { busqueda.value = it?.toString().orEmpty() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                busqueda
                    .debounce(150)
                    .flatMapLatest { q ->
                        if (q.isBlank()) dao.observeAll() else dao.search(q)
                    }
                    .collect { lista ->
                        adapter.submitList(lista)
                        UiHelpers.emptyState(binding.tvEmpty, lista.isEmpty())
                    }
            }
        }
    }

    private fun abrirDetalle(equipoId: Long) {
        findNavController().navigate(
            R.id.action_equipos_to_detalle,
            bundleOf("equipmentId" to equipoId)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
