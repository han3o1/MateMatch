package com.mp.matematch.main.ui.feed

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mp.matematch.databinding.ItemFeedHouseBinding
import com.mp.matematch.profile.model.User

class HouseAdapter(
    private val feedItemList: MutableList<FeedItem> = mutableListOf(),
    private val onMessageClick: (String) -> Unit
) : RecyclerView.Adapter<HouseAdapter.HouseViewHolder>() {

    inner class HouseViewHolder(private val binding: ItemFeedHouseBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(feedItem: FeedItem) {
            val user = feedItem.user
            val matchScore = feedItem.matchScore

            with(binding) {
                textTitle.text = user.buildingType ?: "Building Type N/A"
                textPrice.text = "₩${user.monthlyRent ?: 0} / mo"
                val location = "${user.city}, ${user.district}"
                val roomType = "Room: ${user.buildingType ?: "N/A"}"
                val fee = "Fee: ₩${user.maintenanceFee ?: 0}"
                val moveIn = "📅 ${user.moveInDate}"
                textDetails.text = "$location\n$roomType\n$fee\n$moveIn"
                textDescription.text = user.bio

                // 태그
                tagContainer.removeAllViews()
                user.amenities?.forEach { tag ->
                    val tagView = TextView(root.context).apply {
                        text = tag
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        setPadding(24, 12, 24, 12)
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.setMargins(8, 0, 8, 0)
                        layoutParams = params
                    }
                    tagContainer.addView(tagView)
                }

                textMatchRate.text = "${matchScore}% Match"

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
        val feedItem = feedItemList[position]
        holder.bind(feedItem)
    }

    override fun getItemCount(): Int = feedItemList.size

    fun updateData(newList: List<FeedItem>) {
        feedItemList.clear()
        feedItemList.addAll(newList)
        notifyDataSetChanged()
    }
}
