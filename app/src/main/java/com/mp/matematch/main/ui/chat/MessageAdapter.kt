package com.mp.matematch.main.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mp.matematch.R
import com.mp.matematch.main.ui.chat.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MessageAdapter(
    private val messageList: List<Message>,
    private val currentUserId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_LEFT = 0
        const val VIEW_TYPE_RIGHT = 1
    }

    override fun getItemViewType(position: Int): Int {
        val msg = messageList[position]
        return if (msg.senderId == currentUserId) VIEW_TYPE_RIGHT else VIEW_TYPE_LEFT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_RIGHT) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_right, parent, false)
            RightMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_left, parent, false)
            LeftMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messageList[position]
        if (holder is RightMessageViewHolder) {
            holder.bind(msg)
        } else if (holder is LeftMessageViewHolder) {
            holder.bind(msg)
        }
    }

    override fun getItemCount(): Int = messageList.size

    inner class LeftMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(message: Message) {
            itemView.findViewById<TextView>(R.id.tvMessageLeft).text = message.text
            itemView.findViewById<TextView>(R.id.tvTimeLeft).text = formatTime(message.timestamp)
        }
    }

    inner class RightMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(message: Message) {
            itemView.findViewById<TextView>(R.id.tvMessageRight).text = message.text
            itemView.findViewById<TextView>(R.id.tvTimeRight).text = formatTime(message.timestamp)
        }
    }

    private fun formatTime(timestamp: Long): String {
        // 시간 포맷은 자유롭게 수정 가능
        val sdf = SimpleDateFormat("a hh:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun updateMessages(newList: List<Message>) {
        (messageList as MutableList).clear()
        (messageList as MutableList).addAll(newList)
        notifyDataSetChanged()
    }



}
