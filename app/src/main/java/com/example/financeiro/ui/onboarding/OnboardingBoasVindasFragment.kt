package com.example.financeiro.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.financeiro.databinding.FragmentOnboardingBoasVindasBinding

class OnboardingBoasVindasFragment : Fragment() {

    private var _binding: FragmentOnboardingBoasVindasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OnboardingViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBoasVindasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnComecar.setOnClickListener {
            viewModel.irParaProximoPasso()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}