package com.mp.matematch.main.ui.chat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import com.mp.matematch.R
import java.io.File
import java.io.IOException

class ChatRoomActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: MessageAdapter
    private lateinit var chatId: String
    private lateinit var receiverUid: String

    private lateinit var tvRecordingStatus: TextView
    private var isRecording = false
    private var mediaRecorder: MediaRecorder? = null
    private lateinit var audioFile: File

    private val levelMeterLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val levelMsg = result.data?.getStringExtra("levelResult") ?: return@registerForActivityResult
                viewModel.sendMessage(chatId, "📐 Tilt Measure:\n$levelMsg")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room)

        // ----------------------------
        // 1) 알림 채널 생성 + 권한 요청
        // ----------------------------
        createNotificationChannel()
        requestNotificationPermission()

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        // Intent 값
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

        // Firestore에서 유저 정보 불러오기 (if 필요)
        if (receiverName.isEmpty() || receiverProfileImageUrl.isEmpty()) {
            FirebaseFirestore.getInstance()
                .collection("chats")
                .document(chatId)
                .update("updatedAt", FieldValue.serverTimestamp())

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
            tvName.text = receiverName
            Glide.with(this)
                .load(receiverProfileImageUrl)
                .circleCrop()
                .into(imgProfile)
        }

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        adapter = MessageAdapter(mutableListOf(), currentUserId)
        rvMessages.adapter = adapter
        rvMessages.layoutManager = LinearLayoutManager(this)

        // ----------------------------
        // 메시지 실시간 로드
        // ----------------------------
        viewModel.loadMessages(chatId)

        viewModel.messages.observe(this) { messages ->
            adapter.updateMessages(messages)
            rvMessages.scrollToPosition(messages.size - 1)

            // 알림 트리거
            val lastMsg = messages.lastOrNull() ?: return@observe

            // 내가 보낸 메시지는 알림 안 띄움
            if (lastMsg.senderId == currentUserId) return@observe

            // 채팅방 안에 있을 때도 교수님 요구사항 따라 알림 표시 필요
            showChatNotification(
                message = lastMsg.text ?: "[Voice Message]",
                sender = receiverName
            )
        }

        // 메시지 보내기
        btnSend.setOnClickListener {
            val text = edtMessage.text.toString()
            if (text.isNotBlank()) {
                viewModel.sendMessage(chatId, text)
                edtMessage.text.clear()
            }
        }

        // 기울기 측정
        val btnLevel = findViewById<ImageButton>(R.id.btnLevel)
        btnLevel.setOnClickListener {
            val intent = Intent(this, LevelMeterActivity::class.java)
            levelMeterLauncher.launch(intent)
        }

        // 음성 녹음 UI
        tvRecordingStatus = findViewById(R.id.tvRecordingStatus)
        val btnRecord = findViewById<ImageButton>(R.id.btnRecord)
        btnRecord.setOnClickListener {
            if (isRecording) stopRecording() else startRecording()
        }

        if (!checkAudioPermission()) requestAudioPermission()
    }

    // ----------------------------
    // 알림 채널 생성 (필수)
    // ----------------------------
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "chat_channel",
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 2001)
            }
        }
    }

    private fun showChatNotification(message: String, sender: String) {

        val intent = Intent(this, ChatRoomActivity::class.java).apply {
            putExtra("receiverUid", receiverUid)
            putExtra("chatId", chatId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, "chat_channel")
            .setSmallIcon(R.drawable.ic_logo)   // ← 여기 아이콘 문제 해결
            .setContentTitle("$sender 님의 메시지")             // ← 여기 오류 제거
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val manager = NotificationManagerCompat.from(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        manager.notify(10001, builder.build())
    }


    // ----------------------------
    // 아래는 기존 음성 녹음/전송 기능 (수정 X)
    // ----------------------------
    private fun startRecording() {
        try {
            val outputDir = externalCacheDir
            audioFile = File.createTempFile("audio_", ".3gp", outputDir)

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile.absolutePath)

                prepare()
                start()
            }

            isRecording = true
            tvRecordingStatus.text = "🎙️ Recording voice..."
            tvRecordingStatus.visibility = View.VISIBLE

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        isRecording = false

        tvRecordingStatus.text = ""
        tvRecordingStatus.visibility = View.GONE

        uploadToStorage(audioFile)
    }

    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.RECORD_AUDIO),
            1001
        )
    }

    private fun uploadToStorage(file: File) {
        val currentUid = FirebaseAuth.getInstance().currentUser!!.uid
        val timestamp = System.currentTimeMillis()

        val storageRef = FirebaseStorage.getInstance().reference
        val audioRef = storageRef.child("audio_messages/${chatId}_${currentUid}_${timestamp}.m4a")

        audioRef.putFile(Uri.fromFile(file))
            .addOnSuccessListener {
                audioRef.downloadUrl.addOnSuccessListener { uri ->
                    sendAudioMessage(chatId, uri.toString())
                }
            }
            .addOnFailureListener {
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
                    "lastMessage" to "[Voice Message]",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
    }
    private fun getChatId(uid1: String, uid2: String): String {
        return listOf(uid1, uid2).sorted().joinToString("_")
    }

}
