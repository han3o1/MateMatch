package com.mp.matematch.settings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import com.mp.matematch.databinding.FragmentSettingsBinding
import com.mp.matematch.settings.SettingsRepository

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val settingsRepo by lazy { SettingsRepository }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadSettings()
        setupListeners()

        binding.toolbarSettings.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadSettings() {
        val isPushEnabled = settingsRepo.isPushEnabled(requireContext())
        binding.switchPush.isChecked = isPushEnabled

        val viewMode = settingsRepo.getFeedViewMode(requireContext())
        if (viewMode == SettingsRepository.VIEW_MODE_CARD) {
            binding.rbCardMode.isChecked = true
        } else {
            binding.rbListMode.isChecked = true
        }
    }

    private fun setupListeners() {
        binding.switchPush.setOnCheckedChangeListener { _, isChecked ->
            settingsRepo.setPushEnabled(requireContext(), isChecked)
        }

        binding.rgViewMode.setOnCheckedChangeListener { _, checkedId ->
            val newMode = if (checkedId == binding.rbCardMode.id) {
                SettingsRepository.VIEW_MODE_CARD
            } else {
                SettingsRepository.VIEW_MODE_LIST
            }
            settingsRepo.setFeedViewMode(requireContext(), newMode)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}