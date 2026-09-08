package com.rijana.petcare.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.rijana.petcare.PetCareApplication
import com.rijana.petcare.R
import com.rijana.petcare.data.firebase.AuthManager
import com.rijana.petcare.data.repository.UserRepository
import com.rijana.petcare.databinding.FragmentSplashBinding

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as PetCareApplication
        val userRepository = UserRepository(AuthManager(), app.database.userDao())

        if (userRepository.currentFirebaseUid != null) {
            findNavController().navigate(R.id.action_splash_to_home)
        } else {
            findNavController().navigate(R.id.action_splash_to_getStarted)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}