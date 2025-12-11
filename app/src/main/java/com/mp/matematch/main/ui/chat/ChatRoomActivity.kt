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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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

class ChatRoomActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: MessageAdapter   // 초기화❌ (나중에 Firestore 로딩 후)
    private lateinit var chatId: String
    private lateinit var receiverUid: String

    private var receiverProfileUrl: String = ""
    private var receiverName: String = ""

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

        // 알림 채널 및 권한 요청
        createNotificationChannel()
        requestNotificationPermission()

        receiverUid = intent.getStringExtra("receiverUid") ?: ""
        chatId = intent.getStringExtra("chatId")
            ?: getChatId(FirebaseAuth.getInstance().currentUser!!.uid, receiverUid)

        val tvName = findViewById<TextView>(R.id.tvUserName)
        val imgProfile = findViewById<ImageView>(R.id.profileImageView)
        val rvMessages = findViewById<RecyclerView>(R.id.recyclerViewMessages)
        val edtMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)

        // ---------------------------
        // 1) 상대 유저 정보 Firestore에서 가져오기
        // ---------------------------
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(receiverUid)
            .get()
            .addOnSuccessListener { doc ->
                receiverName = doc.getString("name") ?: "Unknown"
                receiverProfileUrl = doc.getString("profileImageUrl") ?: ""

                // 상단 헤더 업데이트
                tvName.text = receiverName
                Glide.with(this)
                    .load(receiverProfileUrl)
                    .circleCrop()
                    .into(imgProfile)

                // ---------------------------
                // 2) 이제 adapter 초기화 (정답)
                // ---------------------------
                val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
                adapter = MessageAdapter(mutableListOf(), currentUserId).apply {
                    this.receiverProfileImageUrl = receiverProfileUrl
                }

                rvMessages.adapter = adapter
                rvMessages.layoutManager = LinearLayoutManager(this)

                // 3) 메시지 로드 시작
                observeMessages(rvMessages)
            }

        btnSend.setOnClickListener {
            val text = edtMessage.text.toString()
            if (text.isNotBlank()) {
                viewModel.sendMessage(chatId, text)
                edtMessage.text.clear()
            }
        }

        // 음성 녹음 UI
        tvRecordingStatus = findViewById(R.id.tvRecordingStatus)
        val btnRecord = findViewById<ImageButton>(R.id.btnRecord)
        btnRecord.setOnClickListener {
            if (isRecording) stopRecording() else startRecording()
        }

        if (!checkAudioPermission()) requestAudioPermission()
        findViewById<View>(R.id.topBar).bringToFront()

    }

    // ---------------------------
    // 메시지 옵저버
    // ---------------------------
    private fun observeMessages(rvMessages: RecyclerView) {
        val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid

        viewModel.loadMessages(chatId)
        viewModel.messages.observe(this) { messages ->
            adapter.updateMessages(messages)
            rvMessages.scrollToPosition(messages.size - 1)

            val lastMsg = messages.lastOrNull() ?: return@observe
            if (lastMsg.senderId == currentUserId) return@observe

            showChatNotification(
                message = lastMsg.text ?: "[Voice Message]",
                sender = receiverName
            )
        }
    }

    // ---------------------------
    // 알림 생성
    // ---------------------------
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("chat_channel", "Chat Messages", NotificationManager.IMPORTANCE_HIGH)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
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
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, "chat_channel")
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle("$sender 님의 메시지")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        NotificationManagerCompat.from(this).notify(10001, builder.build())
    }

    // ---------------------------
    // 음성 메시지 녹음
    // ---------------------------
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
            tvRecordingStatus.text = "🎙 Recording..."
            tvRecordingStatus.visibility = View.VISIBLE

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Recording failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        isRecording = false

        tvRecordingStatus.visibility = View.GONE
        uploadToStorage(audioFile)
    }

    private fun checkAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.RECORD_AUDIO),
            1001
        )
    }

    private fun uploadToStorage(file: File) {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val timestamp = System.currentTimeMillis()
        val audioRef = FirebaseStorage.getInstance().reference
            .child("audio_messages/${chatId}_${uid}_${timestamp}.m4a")

        audioRef.putFile(Uri.fromFile(file))
            .addOnSuccessListener {
                audioRef.downloadUrl.addOnSuccessListener { uri ->
                    sendAudioMessage(uri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendAudioMessage(audioUrl: String) {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val msg = mapOf(
            "senderId" to uid,
            "audioUrl" to audioUrl,
            "timestamp" to System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance()
            .collection("chats").document(chatId)
            .collection("messages").add(msg)

        FirebaseFirestore.getInstance()
            .collection("chats").document(chatId)
            .update(
                mapOf(
                    "lastMessage" to "[Voice Message]",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
    }

    private fun getChatId(uid1: String, uid2: String): String =
        listOf(uid1, uid2).sorted().joinToString("_")
}
