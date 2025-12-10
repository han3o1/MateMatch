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
                viewModel.sendMessage(chatId, "📐 Tilt Measure:\n$levelMsg")
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

        //  1. Intent 값 가져오기
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

        // 2. 이름이나 프로필이 비어있으면 Firestore에서 가져오기
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

        //  3. 메시지 목록 초기화
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        adapter = MessageAdapter(mutableListOf(), currentUserId)
        rvMessages.adapter = adapter
        rvMessages.layoutManager = LinearLayoutManager(this)

        // 4. 메시지 불러오기
        viewModel.loadMessages(chatId)

        viewModel.messages.observe(this) { messages ->
            adapter.updateMessages(messages)
            rvMessages.scrollToPosition(messages.size - 1)
        }

        //  5. 메시지 보내기
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
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }
        if (!checkAudioPermission()) {
            requestAudioPermission()
        }
    }

    private fun startRecording() {
        try {
            val outputDir = externalCacheDir
            audioFile = File.createTempFile("audio_", ".3gp", outputDir)

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
                    Toast.makeText(this@ChatRoomActivity, "Failed to prepare recording", Toast.LENGTH_SHORT).show()
                    return

                }
            }

            isRecording = true
            tvRecordingStatus.text = "🎙️ Recording voice..."
            tvRecordingStatus.visibility = View.VISIBLE

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "An error occurred while starting the recording", Toast.LENGTH_SHORT).show()
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





    private fun getChatId(uid1: String, uid2: String): String {
        return listOf(uid1, uid2).sorted().joinToString("_")
    }

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
                    "lastMessage" to "[Voice Message]",
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
            Toast.makeText(this, "Recording permission granted", Toast.LENGTH_SHORT).show()

        } else {
            Toast.makeText(this, "Recording permission is required", Toast.LENGTH_SHORT).show()

        }
    }



}
