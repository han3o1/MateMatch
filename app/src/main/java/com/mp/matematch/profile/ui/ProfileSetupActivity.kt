package com.mp.matematch.profile.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.mp.matematch.databinding.ActivityProfileSetupABinding

class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupABinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupABinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 날짜 선택기 (Move-in Date)
        binding.inputMoveInDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Move-in Date")
                .build()

            datePicker.addOnPositiveButtonClickListener {
                binding.inputMoveInDate.setText(datePicker.headerText)
            }
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }

        // 업로드 버튼 클릭 (갤러리에서 이미지 선택)
        binding.btnUploadPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
            startActivity(Intent.createChooser(intent, "Select Profile Image"))
        }



            binding.btnNext.setOnClickListener {
                val intent = Intent(this, ProfileSetupBActivity::class.java)
                startActivity(intent)
            }

        }
    }


