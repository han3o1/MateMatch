package com.mp.matematch.main.ui.feed

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mp.matematch.databinding.FragmentFeedHouseBinding

class FeedHouseFragment : Fragment() {

    private var _binding: FragmentFeedHouseBinding? = null
    private val binding get() = _binding!!
    private lateinit var houseAdapter: HouseAdapter
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedHouseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        houseAdapter = HouseAdapter(mutableListOf())
        binding.recyclerViewHouse.apply {
            adapter = houseAdapter
            layoutManager = LinearLayoutManager(context)
        }

        // ✅ 기본 피드 로드 (roommate-provider 데이터)
        loadDefaultHouseFeed()

        // ✅ 검색창 클릭 시 필터 다이얼로그 열기
        binding.searchBoxHouse.setOnClickListener {
            FilterDialog(requireContext()) { filters ->
                applyFilters(filters)
            }.showStep1()
        }
    }

    private fun loadDefaultHouseFeed() {
        firestore.collection("users")
            .whereEqualTo("userType", "roommate-provider")
            .get()
            .addOnSuccessListener { result ->
                val houses = result.documents.mapNotNull { it.toObject(House::class.java) }
                houseAdapter.updateData(houses)
                Log.d("FeedHouse", "기본 피드 로드 완료: ${houses.size}개")
            }
            .addOnFailureListener {
                Log.e("FeedHouse", "기본 피드 로드 실패", it)
            }
    }

    private fun applyFilters(filters: Map<String, Any?>) {
        var query: Query = firestore.collection("users")
            .whereEqualTo("userType", "roommate-provider")

        val locations = filters["locations"] as? List<*> ?: emptyList<String>()
        if (locations.isNotEmpty()) query = query.whereIn("location", locations)

        val buildingTypes = filters["buildingTypes"] as? List<*> ?: emptyList<String>()
        if (buildingTypes.isNotEmpty() && !buildingTypes.contains("notLooking"))
            query = query.whereIn("buildingType", buildingTypes)

        query.get().addOnSuccessListener { result ->
            val filtered = result.documents.mapNotNull { it.toObject(House::class.java) }
            houseAdapter.updateData(filtered)
            Log.d("FeedHouse", "필터 적용 결과: ${filtered.size}개")
        }.addOnFailureListener {
            Log.e("FeedHouse", "필터 적용 실패", it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
