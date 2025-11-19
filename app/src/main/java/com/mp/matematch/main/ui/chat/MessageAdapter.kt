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

class MessageAdapter(
    private val messageList: MutableList<Message>,
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

    // ------------------ LEFT ------------------
    inner class LeftMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvMessage = itemView.findViewById<TextView>(R.id.tvMessageLeft)
        private val tvTime = itemView.findViewById<TextView>(R.id.tvTimeLeft)
        private val btnPlayAudio = itemView.findViewById<ImageButton>(R.id.btnPlayAudio)

        fun bind(message: Message) {
            // 텍스트 메시지
            tvMessage.text = message.text
            tvTime.text = formatTime(message.timestamp)

            // 음성 메시지 버튼 표시 여부
            btnPlayAudio.visibility =
                if (!message.audioUrl.isNullOrEmpty()) View.VISIBLE else View.GONE

            btnPlayAudio.setOnClickListener {
                playAudio(message.audioUrl)
            }
        }
    }

    // ------------------ RIGHT ------------------
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
                playAudio(message.audioUrl)
            }
        }
    }

    // ------------------ 재생 기능 ------------------
    private fun playAudio(url: String?) {
        if (url.isNullOrEmpty()) return

        val mediaPlayer = MediaPlayer()

        try {
            mediaPlayer.setDataSource(url)
            mediaPlayer.prepare()
            mediaPlayer.start()

            Toast.makeText(
                contextFromAdapter(),
                "음성 재생 시작",
                Toast.LENGTH_SHORT
            ).show()

            mediaPlayer.setOnCompletionListener {
                mediaPlayer.release()
                Toast.makeText(
                    contextFromAdapter(),
                    "재생 종료",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                contextFromAdapter(),
                "재생 실패",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun contextFromAdapter() =
    // adapter 사용 시 Context가 없으니까 itemView의 context 사용
        // (항상 리스트 중 첫 번째 item 기준으로 안전)
        if (messageList.isNotEmpty())
            messageListViewHolder?.itemView?.context
        else null

    private val messageListViewHolder: RecyclerView.ViewHolder?
        get() = null // 안전용 placeholder (필요 없음)

    // ------------------ 시간 포맷 ------------------
    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("a hh:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // ------------------ 업데이트 ------------------
    fun updateMessages(newList: List<Message>) {
        messageList.clear()
        messageList.addAll(newList)
        notifyDataSetChanged()
    }
}
