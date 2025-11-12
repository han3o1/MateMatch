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
import com.mp.matematch.databinding.FragmentFeedPersonBinding
import com.mp.matematch.main.ui.feed.FilterDialog
import com.mp.matematch.profile.model.User

class FeedPersonFragment : Fragment() {

    private var _binding: FragmentFeedPersonBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FeedViewModel by viewModels()
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

        // 어댑터 초기화
        personAdapter = PersonAdapter(mutableListOf())
        binding.recyclerViewPerson.apply {
            adapter = personAdapter
            layoutManager = LinearLayoutManager(context)
        }

        // ViewModel의 'personList' LiveData를 관찰
        viewModel.personList.observe(viewLifecycleOwner, Observer { people ->
            personAdapter.updateData(people)
            Log.d("FeedPerson", "피드 UI 업데이트: ${people.size}개")
        })

        viewModel.loadPersonFeed()

        // 필터 다이얼로그
        binding.searchBoxPerson.setOnClickListener {
            FilterDialog(requireContext()) { filters ->
                viewModel.applyPersonFilters(filters)
            }.showStep1() // (FilterDialog 구현 필요)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}