package com.mp.matematch.main.ui.chat


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mp.matematch.R
import com.bumptech.glide.Glide
import android.content.Intent



class ChatAdapter(private val chatList: List<ChatItem>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgProfile: ImageView = itemView.findViewById(R.id.imgProfile)
        val tvNameJob: TextView = itemView.findViewById(R.id.tvNameJob)
        val tvLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        val tvNewBadge: TextView = itemView.findViewById(R.id.tvNewBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chatList[position]

        holder.tvNameJob.text = "${chat.name} · ${chat.job}"
        holder.tvLastMessage.text = chat.lastMessage
        holder.tvTimestamp.text = chat.timestamp
        holder.tvNewBadge.visibility = if (chat.hasNewMessage) View.VISIBLE else View.GONE

        Glide.with(holder.itemView.context)
            .load(chat.profileImageUrl)
            .circleCrop()
            .placeholder(R.drawable.profile_sample)
            .into(holder.imgProfile)

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, ChatRoomActivity::class.java).apply {
                putExtra("receiverUid", chat.uid)
                putExtra("receiverName", chat.name)
                putExtra("receiverProfileImageUrl", chat.profileImageUrl)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = chatList.size
}
