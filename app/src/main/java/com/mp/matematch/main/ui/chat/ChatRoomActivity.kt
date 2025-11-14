package com.mp.matematch.main.ui.chat

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mp.matematch.R
import android.media.MediaRecorder
import java.io.File
import java.io.IOException
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.FieldValue
import android.widget.Toast




class ChatRoomActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: MessageAdapter
    private lateinit var chatId: String
    private lateinit var receiverUid: String

    //음성 변수
    private var isRecording = false
    private var mediaRecorder: MediaRecorder? = null
    private lateinit var audioFile: File


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        // 📌 1. Intent 값 가져오기
        receiverUid = intent.getStringExtra("receiverUid") ?: ""
        chatId = intent.getStringExtra("chatId")
            ?: getChatId(FirebaseAuth.getInstance().currentUser!!.uid, receiverUid)

        var receiverName = intent.getStringExtra("receiverName") ?: ""
        var receiverProfileImageUrl = intent.getStringExtra("receiverProfileImageUrl") ?: ""

        val tvName = findViewById<TextView>(R.id.tvUserName)
        val imgProfile = findViewById<ImageView>(R.id.profileImageView)
        val rvMessages = findViewById<RecyclerView>(R.id.recyclerViewMessages)
        val edtMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)

        // 📌 2. 이름이나 프로필이 비어있으면 Firestore에서 가져오기
        if (receiverName.isEmpty() || receiverProfileImageUrl.isEmpty()) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(receiverUid)
                .get()
                .addOnSuccessListener { doc ->
                    receiverName = doc.getString("name") ?: "Unknown"
                    receiverProfileImageUrl = doc.getString("profileImageUrl") ?: ""

                    tvName.text = receiverName

                    Glide.with(this)
                        .load(receiverProfileImageUrl)
                        .circleCrop()
                        .into(imgProfile)
                }
        } else {
            // Intent 값으로 UI 바인딩
            tvName.text = receiverName
            Glide.with(this)
                .load(receiverProfileImageUrl)
                .circleCrop()
                .into(imgProfile)
        }

        // 📌 3. 메시지 목록 초기화
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        adapter = MessageAdapter(mutableListOf(), currentUserId)
        rvMessages.adapter = adapter
        rvMessages.layoutManager = LinearLayoutManager(this)

        // 📌 4. 메시지 불러오기
        viewModel.loadMessages(chatId)

        viewModel.messages.observe(this) { messages ->
            adapter.updateMessages(messages)
            rvMessages.scrollToPosition(messages.size - 1)
        }

        // 📌 5. 메시지 보내기
        btnSend.setOnClickListener {
            val text = edtMessage.text.toString()
            if (text.isNotBlank()) {
                viewModel.sendMessage(chatId, text)
                edtMessage.text.clear()
            }
        }

        val btnRecord = findViewById<ImageButton>(R.id.btnRecord)
        btnRecord.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }
    }

    private fun startRecording() {
        try {
            val outputDir = externalCacheDir
            audioFile = File.createTempFile("audio_", ".3gp", outputDir)

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }

            isRecording = true
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        isRecording = false

        uploadToStorage(audioFile)
    }





    private fun getChatId(uid1: String, uid2: String): String {
        return listOf(uid1, uid2).sorted().joinToString("_")
    }

    private fun uploadToStorage(file: File) {
        val storageRef = FirebaseStorage.getInstance().reference
        val audioRef = storageRef.child("audio_messages/${file.name}")

        val uploadTask = audioRef.putFile(Uri.fromFile(file))

        uploadTask.addOnSuccessListener {
            audioRef.downloadUrl.addOnSuccessListener { uri ->
                val audioUrl = uri.toString()
                sendAudioMessage(chatId, audioUrl)
            }
        }.addOnFailureListener {
            it.printStackTrace()
            Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendAudioMessage(chatId: String, audioUrl: String) {
        val currentUid = FirebaseAuth.getInstance().currentUser!!.uid

        val msg = mapOf(
            "senderId" to currentUid,
            "audioUrl" to audioUrl,
            "timestamp" to System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance()
            .collection("chats")
            .document(chatId)
            .collection("messages")
            .add(msg)

        FirebaseFirestore.getInstance()
            .collection("chats")
            .document(chatId)
            .update(
                mapOf(
                    "lastMessage" to "[음성 메시지]",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
    }


}
