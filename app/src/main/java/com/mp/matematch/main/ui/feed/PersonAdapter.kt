package com.mp.matematch.main.ui.feed

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mp.matematch.R
import com.mp.matematch.databinding.ItemFeedPersonBinding
import com.mp.matematch.profile.model.User

class PersonAdapter(
    private val feedItemList: MutableList<FeedItem> = mutableListOf(),
    private val onMessageClick: (String) -> Unit
) : RecyclerView.Adapter<PersonAdapter.PersonViewHolder>() {

    class PersonViewHolder(val binding: ItemFeedPersonBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(feedItem: FeedItem, onMessageClick: (String) -> Unit) {
            val user = feedItem.user
            val matchScore = feedItem.matchScore

            with(binding) {

                textNameAge.text = "${user.name}, ${user.age}"
                textJob.text = user.occupation
                textQuote.text = "\"${user.statusMessage}\""
                textLocation.text = "${user.city}, ${user.district}"
                textTime.text = "Move-in: ${user.moveInDate}"

                // 메시지 버튼 클릭
                btnMessage.setOnClickListener {
                    onMessageClick(user.uid)
                }
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
