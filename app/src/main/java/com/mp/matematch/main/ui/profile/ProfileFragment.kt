package com.mp.matematch.main.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.mp.matematch.R
import com.mp.matematch.databinding.FragmentProfileBinding
import com.mp.matematch.databinding.ItemProfileDetailBinding
import com.mp.matematch.profile.model.User
import com.mp.matematch.purpose.ui.PurposeSelectionActivity

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // ViewModel 초기화
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewModel의 LiveData 구독
        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            updateUi(user)
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
        }

        // ViewModel에 프로필 로드 요청
        viewModel.loadUserProfile()

        // "Edit Profile" 버튼 클릭 리스너 (나중에 구현)
        binding.btnEditProfile.setOnClickListener {
            val intent = Intent(requireContext(), PurposeSelectionActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * User 객체의 데이터로 UI를 업데이트하는 함수
     */
    private fun updateUi(user: User) {
        binding.apply {
            // 1. 이미지 (Glide 사용)
            // (Glide 의존성이 build.gradle에 추가되어 있어야 함)
            Glide.with(this@ProfileFragment)
                .load(user.profileImageUrl)
                .placeholder(R.drawable.ic_profile_placeholder) // 로딩 중 이미지
                .error(R.drawable.ic_profile_placeholder) // 오류 시 이미지
                .circleCrop() // 원형으로 자르기
                .into(ivProfileImage)

            // 2. 기본 정보
            tvName.text = user.name
            tvOccupation.text = user.occupation
            tvStatusMessage.text = "\"${user.statusMessage}\""
            tvBio.text = user.bio

            // 3. 사용자 유형 태그
            tvUserTypeTag.text = when (user.userType) {
                "Provider" -> "Looking for a Roommate"
                "HouseSeeker" -> "Looking for a House"
                "Seeker" -> "Looking for a Roommate"
                else -> ""
            }

            // 4. 상세 정보 (include된 레이아웃 접근)
            // (key, value, icon을 설정하는 헬퍼 함수 사용)
            setupDetailItem(itemAge, R.drawable.ic_profile_placeholder, "Age:", "${user.age} years")
            setupDetailItem(itemGender, R.drawable.ic_profile_placeholder, "Gender:", user.gender)
            setupDetailItem(itemLocation, R.drawable.ic_profile_placeholder, "Location:", "${user.city}, ${user.district}")

            // userType에 따라 Budget(월세) 또는 Building Type을 보여주거나 숨김
            if (user.userType == "Provider") {
                itemBudget.root.visibility = View.VISIBLE
                itemBuildingType.root.visibility = View.VISIBLE
                setupDetailItem(itemBudget, R.drawable.ic_profile_placeholder, "Budget:", user.monthlyRent.toString())
                setupDetailItem(itemBuildingType, R.drawable.ic_profile_placeholder, "Building Type:", user.buildingType)
            } else {
                // Seeker나 HouseSeeker는 본인 집 정보가 없으므로 숨김
                itemBudget.root.visibility = View.GONE
                itemBuildingType.root.visibility = View.GONE
            }
        }
    }

    /**
     * item_profile_detail.xml의 뷰들을 설정하는 헬퍼 함수
     */
    private fun setupDetailItem(itemBinding: ItemProfileDetailBinding, iconRes: Int, key: String, value: String?) {
        itemBinding.ivIcon.setImageResource(iconRes)
        itemBinding.tvKey.text = key
        itemBinding.tvValue.text = value
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}