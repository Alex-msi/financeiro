package com.example.financeiro

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity — ponto de entrada do app.
 *
 * Ao iniciar, verifica a flag "onboarding_completo" em SharedPreferences:
 *  - false (ou ausente) → navega para OnboardingFragment
 *  - true              → navega direto para DashboardFragment (implementado na S21)
 *
 * NOTA: A navegação completa (nav_graph.xml + BottomNavigationView) será
 * implementada na S21. Por ora, o startDestination do nav_graph aponta para
 * o OnboardingFragment e, ao concluir o onboarding, o app navega para o
 * Dashboard via action definida no grafo.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        configurarBarrasDoSistema()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        redirecionarSeNecessario()
    }

    /**
     * Se o onboarding já foi concluído em uma execução anterior,
     * pula direto para o Dashboard sem exibir o onboarding novamente.
     *
     * Implementação completa na S22 (seed de categorias + verificação integrada).
     */
    private fun redirecionarSeNecessario() {
        val prefs = getSharedPreferences("saldo_tenho_prefs", Context.MODE_PRIVATE)
        val onboardingCompleto = prefs.getBoolean("onboarding_completo", false)

        if (onboardingCompleto && navController.currentDestination?.id == R.id.onboardingFragment) {
            // S21 definirá a action correta no nav_graph
            navController.navigate(R.id.action_global_dashboard)
        }
        // Caso contrário: permanece no startDestination (OnboardingFragment)
    }

    private fun configurarBarrasDoSistema() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        val root = findViewById<android.view.View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
    }
}
