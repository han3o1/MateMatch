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
    private val onApplyFilters: (Map<String, Any?>) -> Unit
) {
    private var step1Dialog: AlertDialog? = null
    private var step2Dialog: AlertDialog? = null

    private var selectedLocations = mutableListOf<String>()
    private var selectedBuildingTypes = mutableListOf<String>()

    fun showStep1() {
        val bindingStep1 = DialogFilterStep1Binding.inflate(LayoutInflater.from(context))
        val layoutLocation = bindingStep1.layoutLocation
        val layoutBuildingType = bindingStep1.layoutBuildingType
        val btnNext = bindingStep1.btnNextStep

        val locations = listOf("Seoul", "Busan", "Incheon", "Daejeon", "Daegu")
        val buildingTypes = listOf("난 집을 찾고 있지 않아요", "Apartment", "Villa", "Officetel", "Studio")

        // 🔹 지역 체크박스
        locations.forEach { city ->
            val cb = CheckBox(context)
            cb.text = city
            cb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedLocations.add(city) else selectedLocations.remove(city)
            }
            layoutLocation.addView(cb)
        }

        // 🔹 건물 타입 체크박스
        val checkBoxes = mutableListOf<CheckBox>()
        buildingTypes.forEach { type ->
            val cb = CheckBox(context)
            cb.text = type
            layoutBuildingType.addView(cb)
            checkBoxes.add(cb)
        }

        // 첫 번째 옵션: "난 집을 찾고 있지 않아요"
        checkBoxes[0].setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                for (i in 1 until checkBoxes.size) {
                    checkBoxes[i].isChecked = false
                    checkBoxes[i].isEnabled = false
                }
                selectedBuildingTypes.clear()
                selectedBuildingTypes.add("notLooking")
            } else {
                for (i in 1 until checkBoxes.size) checkBoxes[i].isEnabled = true
                selectedBuildingTypes.remove("notLooking")
            }
        }

        for (i in 1 until checkBoxes.size) {
            val cb = checkBoxes[i]
            cb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedBuildingTypes.add(cb.text.toString())
                else selectedBuildingTypes.remove(cb.text.toString())
            }
        }

        btnNext.setOnClickListener {
            step1Dialog?.dismiss()
            showStep2()
        }

        step1Dialog = AlertDialog.Builder(context).setView(bindingStep1.root).create()
        step1Dialog?.show()
    }


    // ============================================
    // 🔥 Step2: 힌트용 스피너(라벨만 보이고 펼치면 값 나오는 스피너)
    // ============================================
    private fun showStep2() {
        val bindingStep2 = DialogFilterStep2Binding.inflate(LayoutInflater.from(context))

        val btnApply = bindingStep2.btnApply
        val btnBack = bindingStep2.btnBack

        // 🔹 스피너 옵션 데이터
        val genderOptions = listOf("Any", "Male", "Female")
        val lifestyleOptions = listOf("Early Riser", "Night Owl", "Flexible")
        val smokingOptions = listOf("Doesn’t Matter", "No", "Yes")
        val petsOptions = listOf("Doesn’t Matter", "No", "Yes")
        val cleanlinessOptions = listOf("Clean", "Normal", "Messy")
        val occupationOptions = listOf("Any", "Student", "Office Worker")
        val moveInDateOptions = listOf("ASAP", "Within 1 month", "Within 3 months")
        val budgetOptions = listOf("Any", "500k", "700k", "1M", "1.5M+")

        // ===================================================
        // 🔥 공통 힌트 스피너 적용 함수
        // ===================================================
        fun attachHintSpinner(spinner: Spinner, options: List<String>, hint: String) {
            val items = mutableListOf(hint)
            items.addAll(options)

            val adapter = object : ArrayAdapter<String>(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                items
            ) {
                override fun isEnabled(position: Int): Boolean {
                    return position != 0 // hint 비활성화
                }
            }

            spinner.adapter = adapter
            spinner.setSelection(0) // hint 표시
        }

        // 🔥 여기서 hint 스피너 적용
        attachHintSpinner(bindingStep2.spinnerGender, genderOptions, "Gender")
        attachHintSpinner(bindingStep2.spinnerLifestyle, lifestyleOptions, "Lifestyle")
        attachHintSpinner(bindingStep2.spinnerSmoking, smokingOptions, "Smoking")
        attachHintSpinner(bindingStep2.spinnerPets, petsOptions, "Pets")
        attachHintSpinner(bindingStep2.spinnerCleanliness, cleanlinessOptions, "Cleanliness")
        attachHintSpinner(bindingStep2.spinnerOccupation, occupationOptions, "Occupation")
        attachHintSpinner(bindingStep2.spinnerMoveInDate, moveInDateOptions, "Move-in Date")
        attachHintSpinner(bindingStep2.spinnerBudget, budgetOptions, "Budget")

        btnBack.setOnClickListener {
            step2Dialog?.dismiss()
            showStep1()
        }

        btnApply.setOnClickListener {
            val filters = mapOf(
                "locations" to selectedLocations,
                "buildingTypes" to selectedBuildingTypes,

                "gender" to bindingStep2.spinnerGender.selectedItem,
                "lifestyle" to bindingStep2.spinnerLifestyle.selectedItem,
                "smoking" to bindingStep2.spinnerSmoking.selectedItem,
                "pets" to bindingStep2.spinnerPets.selectedItem,
                "cleanliness" to bindingStep2.spinnerCleanliness.selectedItem,
                "occupation" to bindingStep2.spinnerOccupation.selectedItem,
                "moveInDate" to bindingStep2.spinnerMoveInDate.selectedItem,
                "budget" to bindingStep2.spinnerBudget.selectedItem
            )

            step2Dialog?.dismiss()
            onApplyFilters(filters)
        }

        step2Dialog = AlertDialog.Builder(context).setView(bindingStep2.root).create()
        step2Dialog?.show()
    }
}
