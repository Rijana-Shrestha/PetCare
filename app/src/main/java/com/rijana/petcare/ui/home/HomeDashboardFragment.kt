package com.rijana.petcare.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.rijana.petcare.databinding.FragmentHomeDashboardBinding

class HomeDashboardFragment : Fragment() {

    private var _binding: FragmentHomeDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: replace with the real signed-in user's name once we hook up
        // UserRepository.getUserProfile() here
        binding.tvGreeting.text = "Good morning!"

        binding.btnAddPet.setOnClickListener {
            // TODO: navigate to Add/Edit Pet once Phase 4 builds it
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}