package com.mp.matematch.main.ui.feed

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.*
import com.mp.matematch.R
import com.mp.matematch.databinding.DialogFilterStep1Binding
import com.mp.matematch.databinding.DialogFilterStep2Binding

class FilterDialog(
    private val context: Context,
    private val options: FilterOptions,
    private val onApplyFilters: (Map<String, Any?>) -> Unit
) {
    private var step1Dialog: AlertDialog? = null
    private var step2Dialog: AlertDialog? = null

    private val selectedLocations = mutableListOf<String>()
    private val selectedBuildingTypes = mutableListOf<String>()

    fun showStep1() {
        val binding = DialogFilterStep1Binding.inflate(LayoutInflater.from(context))

        val layoutLocation = binding.layoutLocation
        val layoutBuildingType = binding.layoutBuildingType

        // -----------------------------
        // 🔥 1) 지역 옵션 — DB에서 동적 생성
        // -----------------------------
        options.cities.forEach { city ->
            val cb = CheckBox(context).apply { text = city }
            cb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedLocations.add(city)
                else selectedLocations.remove(city)
            }
            layoutLocation.addView(cb)
        }

        // -----------------------------
        // 🔥 2) 건물 타입 — DB에서 동적 생성
        // -----------------------------
        options.buildingTypes.forEach { type ->
            val cb = CheckBox(context).apply { text = type }
            cb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedBuildingTypes.add(type)
                else selectedBuildingTypes.remove(type)
            }
            layoutBuildingType.addView(cb)
        }

        // -----------------------------
        // 다음 단계
        // -----------------------------
        binding.btnNextStep.setOnClickListener {
            step1Dialog?.dismiss()
            showStep2()
        }

        step1Dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .create()

        step1Dialog?.show()
    }


    // ===========================================================
    // 🔥 Step2 Dialog — DB 기반 Spinner 옵션 적용
    // ===========================================================
    private fun showStep2() {
        val binding = DialogFilterStep2Binding.inflate(LayoutInflater.from(context))

        // 공통 hint + adapter 생성 함수
        fun attachSpinner(spinner: Spinner, hint: String, items: List<String>) {
            val list = listOf(hint) + items
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, list)
            spinner.adapter = adapter
        }

        // -----------------------------
        // 🔥 Step2 각 필터 옵션 — DB 값 기반
        // -----------------------------
        attachSpinner(binding.spinnerGender, "Any", listOf("Male", "Female"))
        attachSpinner(binding.spinnerLifestyle, "Any", options.sleepSchedules)
        attachSpinner(binding.spinnerSmoking, "Any", options.smoking)
        attachSpinner(binding.spinnerPets, "Any", options.pets)
        attachSpinner(binding.spinnerCleanliness, "Any", options.cleanliness)
        attachSpinner(binding.spinnerOccupation, "Any", options.occupations)

        attachSpinner(
            binding.spinnerMoveInDate,
            "Any",
            listOf("Within 1 month", "Within 3 months")
        )
        // Move-in date는 3개 옵션 유지
        attachSpinner(
            binding.spinnerMoveInDate,
            "Move-in Date",
            listOf("Any", "Within 1 month", "Within 3 months")
        )

        // -----------------------------
        // 🔙 이전 페이지
        // -----------------------------
        binding.btnBack.setOnClickListener {
            step2Dialog?.dismiss()
            showStep1()
        }

        // -----------------------------
        // 🔥 필터 적용 버튼
        // -----------------------------
        binding.btnApply.setOnClickListener {
            val filters = mapOf(
                "locations" to selectedLocations,
                "buildingTypes" to selectedBuildingTypes,

                "gender" to binding.spinnerGender.selectedItem.toString(),
                "sleepSchedule" to binding.spinnerLifestyle.selectedItem.toString(),
                "smoking" to binding.spinnerSmoking.selectedItem.toString(),
                "pets" to binding.spinnerPets.selectedItem.toString(),
                "cleanliness" to binding.spinnerCleanliness.selectedItem.toString(),
                "occupation" to binding.spinnerOccupation.selectedItem.toString(),
                "moveInDate" to binding.spinnerMoveInDate.selectedItem.toString(),
                "mbti" to null  // 현재 UI에 없으므로 기본 null
            )

            step2Dialog?.dismiss()
            onApplyFilters(filters)
        }

        step2Dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .create()

        step2Dialog?.show()
    }
}
