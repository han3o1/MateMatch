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
import com.mp.matematch.databinding.FragmentFeedPersonBinding

class FeedPersonalFragment : Fragment() {

    private var _binding: FragmentFeedPersonBinding? = null
    private val binding get() = _binding!!
    private lateinit var personAdapter: PersonAdapter
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedPersonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        personAdapter = PersonAdapter(mutableListOf())
        binding.recyclerView.apply {
            adapter = personAdapter
            layoutManager = LinearLayoutManager(context)
        }

        // ✅ 기본 피드 로드
        loadDefaultPersonFeed()

        // ✅ 검색창 클릭 시 필터 다이얼로그 열기
        binding.searchBox.setOnClickListener {
            FilterDialog(requireContext()) { filters ->
                applyFilters(filters)
            }.showStep1()
        }
    }

    private fun loadDefaultPersonFeed() {
        val currentUserType = getCurrentUserType()
        var query: Query = firestore.collection("users")

        when (currentUserType) {
            "roommate-provider" -> query = query.whereEqualTo("userType", "houseSeeker")
            "roommate-seeker" -> query = query.whereEqualTo("userType", "roommate-seeker")
        }

        query.get().addOnSuccessListener { result ->
            val persons = result.documents.mapNotNull { it.toObject(Person::class.java) }
            personAdapter.updateData(persons)
            Log.d("FeedPersonal", "기본 피드 로드 완료: ${persons.size}개")
        }
    }

    private fun applyFilters(filters: Map<String, Any?>) {
        val currentUserType = getCurrentUserType()
        var query: Query = firestore.collection("users")

        when (currentUserType) {
            "roommate-provider" -> query = query.whereEqualTo("userType", "houseSeeker")
            "roommate-seeker" -> query = query.whereEqualTo("userType", "roommate-seeker")
        }

        val locations = filters["locations"] as? List<*> ?: emptyList<String>()
        if (locations.isNotEmpty()) {
            query = query.whereIn("location", locations)
        }

        query.get().addOnSuccessListener { result ->
            val filtered = result.documents.mapNotNull { it.toObject(Person::class.java) }
            personAdapter.updateData(filtered)
            Log.d("FeedPersonal", "필터 적용 결과: ${filtered.size}개")
        }
    }

    private fun getCurrentUserType(): String {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return ""
        var type = ""

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                type = doc.getString("userType") ?: ""
            }

        return type
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
