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

class FeedPersonFragment : Fragment() {

    private var _binding: FragmentFeedPersonBinding? = null
    private val binding get() = _binding!!

    // ViewModel을 Fragment에 주입 (Firestore 직접 참조 삭제)
    private val viewModel: FeedViewModel by viewModels()

    // 'personAdapter' -> 'FeedPersonAdapter' (List<User>를 받는 어댑터)
    private lateinit var personAdapter: PersonAdapter // (파일 이름/클래스 이름도 수정 필요)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedPersonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 어댑터 초기화 (List<User> 사용)
        personAdapter = PersonAdapter(mutableListOf()) // (PersonAdapter -> FeedPersonAdapter)
        binding.recyclerViewPerson.apply {
            adapter = personAdapter
            layoutManager = LinearLayoutManager(context)
        }

        // ViewModel의 'personList' LiveData를 관찰
        viewModel.personList.observe(viewLifecycleOwner, Observer { people ->
            // LiveData가 변경될 때마다 어댑터에 새 리스트(List<User>)를 전달
            personAdapter.updateData(people)
            Log.d("FeedPerson", "피드 UI 업데이트: ${people.size}개")
        })

        // ViewModel에 기본 피드 로드를 요청
        viewModel.loadPersonFeed()

        // 필터 다이얼로그 (이 로직은 UI 로직이므로 Fragment에 두는 것이 맞음)
        binding.searchBoxPerson.setOnClickListener {
            FilterDialog(requireContext()) { filters ->
                // ViewModel에 필터 적용을 요청
                viewModel.applyPersonFilters(filters)
            }.showStep1()
        }
    }

    // (loadDefaultPersonFeed, applyFilters 함수는 ViewModel로 이동했으므로 삭제)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}