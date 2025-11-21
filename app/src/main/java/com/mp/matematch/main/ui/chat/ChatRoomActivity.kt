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
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.view.View
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts



class ChatRoomActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: MessageAdapter
    private lateinit var chatId: String
    private lateinit var receiverUid: String

    private lateinit var tvRecordingStatus: TextView

    // 음성 변수
    private var isRecording = false
    private var mediaRecorder: MediaRecorder? = null
    private lateinit var audioFile: File

    private val levelMeterLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == RESULT_OK) {
                val levelMsg = result.data?.getStringExtra("levelResult") ?: return@registerForActivityResult

                // 채팅으로 보내기
                viewModel.sendMessage(chatId, "📐 수평계 결과:\n$levelMsg")
            }
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

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

        // 2. Firestore에서 유저 정보 보충
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
            tvName.text = receiverName
            Glide.with(this)
                .load(receiverProfileImageUrl)
                .circleCrop()
                .into(imgProfile)
        }

        // 메시지 리스트
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        adapter = MessageAdapter(mutableListOf(), currentUserId)
        rvMessages.adapter = adapter
        rvMessages.layoutManager = LinearLayoutManager(this)

        viewModel.loadMessages(chatId)
        viewModel.messages.observe(this) { messages ->
            adapter.updateMessages(messages)
            rvMessages.scrollToPosition(messages.size - 1)
        }

        btnSend.setOnClickListener {
            val text = edtMessage.text.toString()
            if (text.isNotBlank()) {
                viewModel.sendMessage(chatId, text)
                edtMessage.text.clear()
            }
        }

        val btnLevel = findViewById<ImageButton>(R.id.btnLevel)
        btnLevel.setOnClickListener {
            val intent = Intent(this, LevelMeterActivity::class.java)
            levelMeterLauncher.launch(intent)
        }





        tvRecordingStatus = findViewById(R.id.tvRecordingStatus)

        val btnRecord = findViewById<ImageButton>(R.id.btnRecord)
        btnRecord.setOnClickListener {
            if (isRecording) stopRecording()
            else startRecording()
        }

        if (!checkAudioPermission()) requestAudioPermission()

        adapter.onMessageLongClick = { message ->
            showDeleteDialog(message)
        }


    }

    private fun showDeleteDialog(message: Message) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("메시지 삭제")
            .setMessage("이 메시지를 삭제할까요?")
            .setPositiveButton("삭제") { _, _ ->
                deleteMessage(message)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteMessage(message: Message) {
        val db = FirebaseFirestore.getInstance()

        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .document(message.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "삭제됨", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "삭제 실패", Toast.LENGTH_SHORT).show()
            }
    }




    private fun startRecording() {
        try {
            val outputDir = externalCacheDir ?: cacheDir
            audioFile = File.createTempFile("audio_", ".m4a", outputDir)

            mediaRecorder = MediaRecorder()
            mediaRecorder?.apply {

                // 순서 매우 중요!
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

                setOutputFile(audioFile.absolutePath)

                try {
                    prepare()
                    start()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@ChatRoomActivity, "녹음 준비 실패", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            isRecording = true
            tvRecordingStatus.text = "🎙️ 음성 녹음 중..."
            tvRecordingStatus.visibility = View.VISIBLE

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "녹음 시작 오류 발생", Toast.LENGTH_SHORT).show()
        }
    }


    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mediaRecorder = null
        isRecording = false
        tvRecordingStatus.visibility = View.GONE

        if (audioFile.exists() && audioFile.length() > 1000) {
            uploadToStorage(audioFile)
        } else {
            Toast.makeText(this, "녹음 실패. 파일 없음", Toast.LENGTH_SHORT).show()
        }
    }


    private fun getChatId(uid1: String, uid2: String): String {
        return listOf(uid1, uid2).sorted().joinToString("_")
    }

    // ✅ 최종 통합된 업로드 함수
    private fun uploadToStorage(file: File) {
        val currentUid = FirebaseAuth.getInstance().currentUser!!.uid
        val timestamp = System.currentTimeMillis()

        val storageRef = FirebaseStorage.getInstance().reference
        val audioRef =
            storageRef.child("audio_messages/${chatId}_${currentUid}_${timestamp}.m4a")

        audioRef.putFile(Uri.fromFile(file))
            .addOnSuccessListener {
                audioRef.downloadUrl.addOnSuccessListener { uri ->
                    val audioUrl = uri.toString()
                    sendAudioMessage(chatId, audioUrl)
                }
            }
            .addOnFailureListener {
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "녹음 권한 허용됨", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "녹음 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }
}
