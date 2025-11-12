package com.mp.matematch.main.ui.feed

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mp.matematch.R
import com.mp.matematch.databinding.ItemFeedHouseBinding
import com.mp.matematch.profile.model.User

class HouseAdapter(private val userList: MutableList<User> = mutableListOf()) :
    RecyclerView.Adapter<HouseAdapter.HouseViewHolder>() {

    class HouseViewHolder(private val binding: ItemFeedHouseBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            with(binding) {
                textTitle.text = user.buildingType ?: "Building Type N/A"
                textPrice.text = "₩${user.monthlyRent ?: 0} / mo"
                val location = "${user.city}, ${user.district}"
                val roomType = "Room: ${user.buildingType ?: "N/A"}"
                val fee = "Fee: ₩${user.maintenanceFee ?: 0}"
                val moveIn = "📅 ${user.moveInDate}"
                textDetails.text = "$location\n$roomType\n$fee\n$moveIn"
                textDescription.text = user.bio

                tagContainer.removeAllViews()
                user.amenities?.forEach { tag ->
                    val tagView = TextView(root.context).apply {
                        text = tag
                        // (TODO: 태그 스타일 설정 - bg_feed_tag_chip, tag_text 등이 필요)
                        // setBackgroundResource(R.drawable.bg_feed_tag_chip)
                        // setTextColor(ContextCompat.getColor(root.context, R.color.tag_text))
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
                textMatchRate.text = "90% Match" // (임시)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HouseViewHolder {
        val binding =
            ItemFeedHouseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HouseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HouseViewHolder, position: Int) {
        val user = userList[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int = userList.size

    fun updateData(newList: List<User>) {
        userList.clear()
        userList.addAll(newList)
        notifyDataSetChanged()
    }
}
