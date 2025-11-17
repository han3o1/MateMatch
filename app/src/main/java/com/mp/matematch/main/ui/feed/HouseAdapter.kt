package com.mp.matematch.main.ui.feed

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.mp.matematch.R
import com.mp.matematch.databinding.ItemFeedHouseBinding

class HouseAdapter(
    private val feedItemList: MutableList<FeedItem> = mutableListOf(),
    private val onMessageClick: (String?) -> Unit
) : RecyclerView.Adapter<HouseAdapter.HouseViewHolder>() {

    inner class HouseViewHolder(private val binding: ItemFeedHouseBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(feedItem: FeedItem, onMessageClick: (String?) -> Unit) {
            val user = feedItem.user
            val matchScore = feedItem.matchScore

            with(binding) {

                // ⭐ 궁합 퍼센트
                textMatchRate.text = "★ ${matchScore}% Match"

                // ⭐ 집 사진
                if (!user.profileImageUrl.isNullOrEmpty()) {
                    Glide.with(root.context)
                        .load(user.profileImageUrl)
                        .into(imageHouse)
                } else {
                    imageHouse.setImageResource(R.drawable.sample_house)
                }

                // ⭐ 집 종류
                textTitle.text = user.buildingType ?: "N/A"

                // ⭐ 월세
                textPrice.text = "₩${user.monthlyRent ?: 0} / mo"

                // ⭐ 위치
                textLocation.text = "📍 ${user.city}, ${user.district}"

                // ⭐ 관리비
                textMaintenanceFee.text = "💵 Maintenance Cost: ₩${user.maintenanceFee ?: 0}"

                // ⭐ 입주 가능 날짜
                textMoveIn.text = "📅 Available: ${user.moveInDate ?: "N/A"}"

                // ⭐ 방 주인 정보
                textOwnerInfo.text = "${user.name}, ${user.age} | ${user.occupation}"

                // ⭐ 소개
                textDescription.text = user.bio ?: ""

                // ⭐ 태그
                tagContainer.removeAllViews()
                user.amenities?.forEach { tag ->
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
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HouseViewHolder {
        val binding =
            ItemFeedHouseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HouseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HouseViewHolder, position: Int) {
        holder.bind(feedItemList[position], onMessageClick)
    }

    override fun getItemCount(): Int = feedItemList.size

    fun updateData(newList: List<FeedItem>) {
        feedItemList.clear()
        feedItemList.addAll(newList)
        notifyDataSetChanged()
    }
}