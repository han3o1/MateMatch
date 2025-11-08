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

class HouseAdapter(private val houseList: MutableList<House> = mutableListOf()) :

    RecyclerView.Adapter<HouseAdapter.HouseViewHolder>() {

    class HouseViewHolder(val binding: ItemFeedHouseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HouseViewHolder {
        val binding = ItemFeedHouseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HouseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HouseViewHolder, position: Int) {
        val house = houseList[position]
        with(holder.binding) {
            imageHouse.setImageResource(house.imageResId)
            textTitle.text = house.title
            textPrice.text = house.price
            textDescription.text = house.description
            textDetails.text = "${house.location}\n${house.roomType}\n📅 ${house.moveInDate}"

            // 태그
            tagContainer.removeAllViews()
            house.tags.forEach { tag ->
                val tagView = TextView(root.context).apply {
                    text = tag
                    setBackgroundResource(R.drawable.bg_feed_tag_chip)
                    setTextColor(ContextCompat.getColor(root.context, R.color.tag_text))
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
        }
    }

    override fun getItemCount(): Int = houseList.size

    fun updateData(newList: List<House>) {
        houseList.clear()
        houseList.addAll(newList)
        notifyDataSetChanged()
    }
}
