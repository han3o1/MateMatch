package com.mp.matematch.main.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mp.matematch.R
import com.mp.matematch.databinding.FragmentFeedPersonBinding

class FeedPersonalFragment : Fragment() {

    private var _binding: FragmentFeedPersonBinding? = null
    private val binding get() = _binding!!
    private lateinit var personAdapter: PersonAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedPersonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dummyData = listOf(
            Person(
                name = "Sarah Chen",
                age = 25,
                job = "Designer",
                location = "Downtown Seattle",
                rentRange = "$1200 ~ $1600",
                description = "Looking for a clean and quiet place! 🧹",
                tags = listOf("Night Owl", "Clean", "Private"),
                profileImageResId = R.drawable.ic_profile_placeholder
            )
        )

        personAdapter = PersonAdapter(dummyData)

        binding.recyclerView.apply {
            adapter = personAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
