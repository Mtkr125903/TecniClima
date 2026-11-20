package com.airservice.pro.ui

import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.airservice.pro.R
import com.airservice.pro.data.AppDatabase
import com.airservice.pro.databinding.ActivityMainBinding
import com.airservice.pro.util.WhatsAppHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    /** Evita que el listener del destino vuelva a disparar la navegacion de la pestana. */
    private var sincronizandoTab = false
    private var ultimoAtras = 0L

    /** Destino de navegacion, titulo e icono de cada pestana. */
    private data class TabData(
        val grafo: Int,
        val raiz: Int,
        val titulo: Int,
        val icono: Int
    )

    private val tabsData: List<TabData> by lazy {
        listOf(
            TabData(R.id.clientes_nav, R.id.clientesFragment, R.string.module_client, R.drawable.ic_client),
            TabData(R.id.equipos_nav, R.id.equiposFragment, R.string.module_equipment, R.drawable.ic_equipment),
            TabData(R.id.historial_nav, R.id.historialFragment, R.string.module_history, R.drawable.ic_history),
            TabData(R.id.presupuestos_nav, R.id.presupuestosFragment, R.string.module_budget, R.drawable.ic_budget),
            TabData(R.id.comprobantes_nav, R.id.comprobantesFragment, R.string.module_receipt, R.drawable.ic_receipt),
            TabData(R.id.seguimiento_nav, R.id.seguimientoFragment, R.string.module_track, R.drawable.ic_work),
            TabData(R.id.agenda_nav, R.id.agendaFragment, R.string.module_agenda, R.drawable.ic_calendar),
            TabData(R.id.mollier_nav, R.id.mollierFragment, R.string.module_mollier, R.drawable.ic_mollier)
        )
    }

    /** Raices de cada pestana: son las pantallas de nivel superior (sin flecha "atras"). */
    private val raizPestanas: Set<Int> by lazy {
        setOf(
            R.id.clientesFragment,
            R.id.equiposFragment,
            R.id.historialFragment,
            R.id.presupuestosFragment,
            R.id.comprobantesFragment,
            R.id.seguimientoFragment,
            R.id.agendaFragment,
            R.id.mollierFragment,
            R.id.companyProfileFragment
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHost.navController
        appBarConfiguration = AppBarConfiguration(raizPestanas)

        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)

        configurarInsets()
        configurarTabs()
        configurarDestinos()
        configurarAtras()
        configurarAccesosDirectos()
    }

    // ------------------------------------------------------------------
    // Insets: deja libre la zona del gesto inferior para los FAB
    // ------------------------------------------------------------------
    private fun configurarInsets() {
        val baseBottom = binding.navHostFragment.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val gestures = insets.getInsets(
                WindowInsetsCompat.Type.systemGestures() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.navHostFragment.setPadding(
                binding.navHostFragment.paddingLeft,
                binding.navHostFragment.paddingTop,
                binding.navHostFragment.paddingRight,
                baseBottom + gestures.bottom
            )
            insets
        }
    }

    // ------------------------------------------------------------------
    // Pestanas
    // ------------------------------------------------------------------
    private fun configurarTabs() {
        tabsData.forEach { (_, _, tituloRes, iconoRes) ->
            val tab = binding.tabLayout.newTab()
                .setText(tituloRes)
                .setIcon(iconoRes)
            binding.tabLayout.addTab(tab)
        }

        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                if (sincronizandoTab) return
                val position = tab?.position ?: 0
                if (position !in tabsData.indices) return
                irAPestana(position)
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}

            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                // Tocar la pestana actual vuelve a su raiz en vez de no hacer nada.
                val position = tab?.position ?: 0
                if (position !in tabsData.indices) return
                navController.popBackStack(tabsData[position].raiz, false)
            }
        })
    }

    /**
     * Mantiene la pestana seleccionada sincronizada con la pantalla actual y
     * habilita el manejo propio del boton atras cuando no hay historial.
     */
    private fun configurarDestinos() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val destinoPestana = destination.hierarchy
                .firstOrNull { it.id in tabsData.map { tab -> tab.grafo } }
                ?.id
            if (destinoPestana != null) {
                val position = tabsData.indexOfFirst { it.grafo == destinoPestana }
                if (position >= 0 && position != binding.tabLayout.selectedTabPosition) {
                    sincronizandoTab = true
                    binding.tabLayout.getTabAt(position)?.select()
                    sincronizandoTab = false
                }
            }
            callbackAtras.isEnabled = navController.previousBackStackEntry == null
        }
    }

    /** Doble pulsacion para salir cuando ya no hay historial de navegacion. */
    private val callbackAtras = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            val ahora = SystemClock.elapsedRealtime()
            if (ahora - ultimoAtras < 2000) {
                finish()
            } else {
                ultimoAtras = ahora
                Snackbar.make(binding.root, R.string.msg_back_again, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun configurarAtras() {
        onBackPressedDispatcher.addCallback(this, callbackAtras)
    }

    /** Cambia a la pestana indicada desde cualquier punto de la app. */
    fun irAPestana(posicion: Int) {
        if (posicion !in tabsData.indices) return
        val tab = tabsData[posicion]
        val yaEstoy = navController.currentDestination?.hierarchy?.any { it.id == tab.grafo } == true
        if (yaEstoy) {
            // Ya estamos en esa pestana: subir al fragmento raiz
            navController.popBackStack(tab.raiz, false)
            return
        }
        navController.navigate(
            tab.grafo,
            null,
            NavOptions.Builder()
                // saveState=true guarda la pila de la pestaña que se abandona;
                // restoreState la recupera al volver (por ejemplo, una ficha abierta).
                .setPopUpTo(navController.graph.findStartDestination().id, false, true)
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .build()
        )
    }

    // ------------------------------------------------------------------
    // Accesos directos del header (spec §2)
    // ------------------------------------------------------------------
    private fun configurarAccesosDirectos() {
        binding.btnAgenda.setOnClickListener { irAPestana(6) }
        binding.btnClientesEquipos.setOnClickListener { irAPestana(0) }
        binding.btnWhatsappRapido.setOnClickListener { mostrarSelectorWhatsApp() }
        binding.btnEmpresa.setOnClickListener { abrirPerfilEmpresa() }

        verificarPerfilPrimeraVez()
    }

    private fun abrirPerfilEmpresa() {
        navController.navigate(R.id.companyProfileFragment)
    }

    private fun verificarPerfilPrimeraVez() {
        val prefs = getSharedPreferences("prefs_profile_first_run", MODE_PRIVATE)
        val avisado = prefs.getBoolean("avisado", false)
        if (!avisado) {
            lifecycleScope.launch {
                val perfil = withContext(Dispatchers.IO) {
                    AppDatabase.getInstance(applicationContext).companyProfileDao().get()
                }
                if (perfil == null || perfil.nombre.isBlank()) {
                    Snackbar.make(binding.root, R.string.msg_first_profile, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.module_company) { abrirPerfilEmpresa() }
                        .show()
                }
                prefs.edit().putBoolean("avisado", true).apply()
            }
        }
    }

    private fun mostrarSelectorWhatsApp() {
        lifecycleScope.launch {
            val clientes = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(applicationContext).clientDao().getAll()
            }.filter { it.telefono.isNotBlank() }

            if (clientes.isEmpty()) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(R.string.action_whatsapp)
                    .setMessage(R.string.msg_no_phone)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                return@launch
            }

            val nombres = clientes.map { it.nombre }.toTypedArray()
            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle(R.string.action_whatsapp)
                .setItems(nombres) { _, which ->
                    val cliente = clientes[which]
                    WhatsAppHelper.enviarWhatsApp(
                        this@MainActivity,
                        cliente.telefono,
                        getString(R.string.whatsapp_generic_body, cliente.nombre)
                    )
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    /** Indica si el sistema esta en modo oscuro (para la paleta adaptativa). */
    fun esModoOscuro(): Boolean =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
}
