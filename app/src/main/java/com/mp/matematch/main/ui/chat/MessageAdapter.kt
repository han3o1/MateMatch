package com.mp.matematch.main.ui.chat

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.mp.matematch.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.ImageView
import com.bumptech.glide.Glide

class MessageAdapter(
    private val messageList: MutableList<Message>,
    private val currentUserId: String,
    var receiverProfileImageUrl: String = "",

    var onLongPress: ((Message) -> Unit)? = null

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
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_right, parent, false)
            RightMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_left, parent, false)
            LeftMessageViewHolder(view)
        }
    }

    override fun getItemCount(): Int = messageList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messageList[position]
        if (holder is RightMessageViewHolder) {
            holder.bind(msg)
        } else if (holder is LeftMessageViewHolder) {
            holder.bind(msg)
        }
    }

    // ---------------- LEFT ----------------
    inner class LeftMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvMessage = itemView.findViewById<TextView>(R.id.tvMessageLeft)
        private val tvTime = itemView.findViewById<TextView>(R.id.tvTimeLeft)
        private val btnPlayAudio = itemView.findViewById<ImageButton>(R.id.btnPlayAudio)
        private val imgProfile = itemView.findViewById<ImageView>(R.id.ivProfileLeft)  // ← 추가됨

        fun bind(message: Message) {

            // 🔥 상대 프로필 이미지 로드
            Glide.with(itemView.context)
                .load(receiverProfileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.profile_sample)
                .into(imgProfile)

            tvMessage.text = message.text
            tvTime.text = formatTime(message.timestamp)

            btnPlayAudio.visibility =
                if (!message.audioUrl.isNullOrEmpty()) View.VISIBLE else View.GONE

            btnPlayAudio.setOnClickListener {
                playAudio(message.audioUrl!!, this)
            }

            // 삭제 long press도 그대로 유지
            itemView.setOnLongClickListener {
                onLongPress?.invoke(message)
                true
            }
        }
    }


    // ---------------- RIGHT ----------------
    inner class RightMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvMessage = itemView.findViewById<TextView>(R.id.tvMessageRight)
        private val tvTime = itemView.findViewById<TextView>(R.id.tvTimeRight)
        private val btnPlayAudio = itemView.findViewById<ImageButton>(R.id.btnPlayAudio)

        fun bind(message: Message) {
            tvMessage.text = message.text
            tvTime.text = formatTime(message.timestamp)

            btnPlayAudio.visibility =
                if (!message.audioUrl.isNullOrEmpty()) View.VISIBLE else View.GONE

            btnPlayAudio.setOnClickListener {
                playAudio(message.audioUrl!!, this)
            }
            itemView.setOnLongClickListener {
                onLongPress?.invoke(message)
                true
            }
        }
    }

    // ---------------- 재생 기능 ----------------
    private fun playAudio(url: String, holder: RecyclerView.ViewHolder) {
        val context = holder.itemView.context
        val mediaPlayer = MediaPlayer()

        try {
            mediaPlayer.setDataSource(url)
            mediaPlayer.prepare()
            mediaPlayer.start()

            Toast.makeText(context, "재생 시작", Toast.LENGTH_SHORT).show()

            mediaPlayer.setOnCompletionListener {
                mediaPlayer.release()
                Toast.makeText(context, "재생 종료", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "재생 실패", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------------- 시간 포맷 ----------------
    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("a hh:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // ---------------- 업데이트 ----------------
    fun updateMessages(newList: List<Message>) {
        messageList.clear()
        messageList.addAll(newList)
        notifyDataSetChanged()
    }
}
