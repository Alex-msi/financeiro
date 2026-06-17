package com.example.financeiro.ui.onboarding

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.financeiro.R
import com.example.financeiro.databinding.FragmentOnboardingBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OnboardingViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
        observeUiState()
    }

    private fun setupViewPager() {
        val adapter = OnboardingPagerAdapter(this)
        binding.viewPager.adapter = adapter
        // Desabilita swipe manual — navegação só pelos botões do ViewModel
        binding.viewPager.isUserInputEnabled = false

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                atualizarIndicadores(position)
            }
        })
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.viewPager.currentItem = state.currentStep
                    atualizarIndicadores(state.currentStep)

                    if (state.onboardingConcluido) {
                        navegarParaDashboard()
                    }
                }
            }
        }
    }

    private fun atualizarIndicadores(passo: Int) {
        val indicadores = listOf(
            binding.dot0,
            binding.dot1,
            binding.dot2,
            binding.dot3,
            binding.dot4
        )
        indicadores.forEachIndexed { index, view ->
            view.isSelected = index == passo
        }
    }

    private fun navegarParaDashboard() {
        findNavController().navigate(R.id.action_onboarding_to_dashboard)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/** Adapter com 5 páginas fixas */
class OnboardingPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 5
    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> OnboardingBoasVindasFragment()
        1 -> OnboardingContasFragment()
        2 -> OnboardingCartoesFragment()
        3 -> OnboardingParcelamentosFragment()
        4 -> OnboardingConclusaoFragment()
        else -> OnboardingBoasVindasFragment()
    }
}
