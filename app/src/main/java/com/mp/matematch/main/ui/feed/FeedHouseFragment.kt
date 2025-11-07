package com.mp.matematch.main.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mp.matematch.R
import com.mp.matematch.databinding.FragmentFeedHouseBinding

class FeedHouseFragment : Fragment() {

    private var _binding: FragmentFeedHouseBinding? = null
    private val binding get() = _binding!!

    private lateinit var houseAdapter: HouseAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedHouseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dummyData = listOf(
            House(
                title = "Sunny 2BR in Gangnam",
                price = "₩1,200,000/month",
                location = "Gangnam-gu, Seoul",
                description = "Spacious apartment with sunlight ☀️",
                tags = listOf("Near Subway", "Pet Friendly", "Fully Furnished"),
                imageResId = R.drawable.sample_house,
                moveInDate = "2024-12-01",
                roomType = "One Room"
            )
        )

        houseAdapter = HouseAdapter(dummyData)

        binding.recyclerViewHouse.apply {
            adapter = houseAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
