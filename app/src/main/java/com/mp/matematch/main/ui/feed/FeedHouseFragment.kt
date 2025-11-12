package com.mp.matematch.main.ui.feed

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.mp.matematch.databinding.FragmentFeedHouseBinding
import com.mp.matematch.main.ui.feed.FilterDialog
import com.mp.matematch.profile.model.User

class FeedHouseFragment : Fragment() {

    private var _binding: FragmentFeedHouseBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FeedViewModel by viewModels()
    private lateinit var houseAdapter: HouseAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedHouseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 어댑터 초기화
        houseAdapter = HouseAdapter(mutableListOf())
        binding.recyclerViewHouse.apply {
            adapter = houseAdapter
            layoutManager = LinearLayoutManager(context)
        }

        // ViewModel의 'houseList' LiveData를 관찰
        viewModel.houseList.observe(viewLifecycleOwner, Observer { users ->
            houseAdapter.updateData(users)
            Log.d("FeedHouse", "피드 UI 업데이트: ${users.size}개")
        })

        viewModel.loadHouseFeed()

        // 필터 다이얼로그
        binding.searchBoxHouse.setOnClickListener {
            FilterDialog(requireContext()) { filters ->
                viewModel.applyHouseFilters(filters)
            }.showStep1() // (FilterDialog 구현 필요)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}