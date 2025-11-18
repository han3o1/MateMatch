package com.mp.matematch.main.ui.feed

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.mp.matematch.R
import com.mp.matematch.databinding.ItemFeedPersonBinding
import com.mp.matematch.profile.model.User

class PersonAdapter(
    private val feedItemList: MutableList<FeedItem> = mutableListOf(),
    private val onMessageClick: (String?) -> Unit
) : RecyclerView.Adapter<PersonAdapter.PersonViewHolder>() {

    class PersonViewHolder(val binding: ItemFeedPersonBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(feedItem: FeedItem, onMessageClick: (String?) -> Unit) {
            val user = feedItem.user
            val matchScore = feedItem.matchScore

            with(binding) {

                // ⭐ 프로필 사진 (Glide)
                if (!user.profileImageUrl.isNullOrEmpty()) {
                    Glide.with(root.context)
                        .load(user.profileImageUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .into(imageProfile)
                } else {
                    imageProfile.setImageResource(R.drawable.ic_profile_placeholder)
                }

                // ⭐ 기본 정보
                textNameAge.text = "${user.name}, ${user.age}"
                textJob.text = user.occupation
                textQuote.text = "\"${user.statusMessage}\""

                // ⭐ 거주 지역
                textLocation.text = "📍 ${user.city}, ${user.district}"

                // ⭐ 월세
                val monthlyRent = user.monthlyRent
                if (monthlyRent != null && monthlyRent > 0) {
                    textMonthlyRent.text = "💵 Maintenance Cost: ₩$monthlyRent"
                    textMonthlyRent.visibility = View.VISIBLE
                } else {
                    textMonthlyRent.visibility = View.GONE
                }

                // ⭐ 입주 가능 날짜
                textTime.text = "📅 Available: ${user.moveInDate ?: "N/A"}"

                // ⭐ 자기소개
                textIntro.text = user.bio ?: ""

                // ⭐ 태그
                tagContainer.removeAllViews()

                val lifestyleTags = listOf(
                    user.sleepSchedule,
                    user.smoking,
                    user.pets,
                    user.cleanliness
                ).filter { it.isNotEmpty() }

                lifestyleTags.forEach { tag ->
                    val chip = Chip(root.context).apply {
                        text = tag
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    }
                    tagContainer.addView(chip)
                }

                // 메시지 버튼 클릭
                btnMessage.setOnClickListener {
                    onMessageClick(user.uid)
                }

                // 궁합 퍼센트
                textMatchRate.text = "★ ${matchScore}% Match"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonViewHolder {
        val binding = ItemFeedPersonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PersonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
        val feedItem = feedItemList[position]
        holder.bind(feedItem, onMessageClick)
    }

    override fun getItemCount(): Int = feedItemList.size

    fun updateData(newList: List<FeedItem>) {
        feedItemList.clear()
        feedItemList.addAll(newList)
        notifyDataSetChanged()
    }
}