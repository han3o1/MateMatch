package com.mp.matematch.main.ui.feed

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.*
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
        val binding = DialogFilterStep1Binding.inflate(LayoutInflater.from(context))

        val locations = listOf("Seoul", "Busan", "Incheon", "Daejeon", "Daegu")
        val buildingTypes = listOf(
            "난 집을 찾고 있지 않아요",
            "Apartment", "Villa", "Officetel", "Studio"
        )

        // ✔ Step1 체크박스 자동 구성
        locations.forEach { city ->
            val cb = CheckBox(context)
            cb.text = city
            cb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedLocations.add(city)
                else selectedLocations.remove(city)
            }
            binding.layoutLocation.addView(cb)
        }

        val checkBoxes = mutableListOf<CheckBox>()
        buildingTypes.forEach { type ->
            val cb = CheckBox(context)
            cb.text = type
            binding.layoutBuildingType.addView(cb)
            checkBoxes.add(cb)

            cb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedBuildingTypes.add(type)
                else selectedBuildingTypes.remove(type)
            }
        }

        // ✔ 첫 번째 옵션 '난 집 찾지 않아요' 동작
        checkBoxes[0].setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                for (i in 1 until checkBoxes.size) {
                    checkBoxes[i].isEnabled = false
                    checkBoxes[i].isChecked = false
                }
                selectedBuildingTypes.clear()
                selectedBuildingTypes.add("notLooking")
            } else {
                for (i in 1 until checkBoxes.size) {
                    checkBoxes[i].isEnabled = true
                }
                selectedBuildingTypes.remove("notLooking")
            }
        }

        binding.btnNextStep.setOnClickListener {
            step1Dialog?.dismiss()
            showStep2()
        }

        step1Dialog = AlertDialog.Builder(context).setView(binding.root).create()
        step1Dialog?.show()
    }


    private fun showStep2() {
        val binding = DialogFilterStep2Binding.inflate(LayoutInflater.from(context))

        fun setup(spinner: Spinner, items: List<String>) {
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, items)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        // Spinner 목록 설정
        setup(binding.spinnerBudget, listOf("","$300~500","$500~700","$700~900","$900+"))
        setup(binding.spinnerLifestyle, listOf("","Quiet","Moderate","Active"))
        setup(binding.spinnerSmoking, listOf("","No","Yes"))
        setup(binding.spinnerPets, listOf("","No","Yes"))
        setup(binding.spinnerCleanliness, listOf("","Low","Medium","High"))
        setup(binding.spinnerGender, listOf("","Male","Female"))
        setup(binding.spinnerOccupation, listOf("","Student","Office Worker","Self-employed"))
        setup(binding.spinnerMBTI, listOf("","INTJ","INTP","ENTJ","ENTP","INFJ","INFP"))
        setup(binding.spinnerMoveInDate, listOf("","ASAP","1~2 weeks","1 month","Flexible"))

        binding.btnApply.setOnClickListener {

            val filterMap = mapOf(
                "locations" to selectedLocations,
                "buildingTypes" to selectedBuildingTypes,
                "budget" to binding.spinnerBudget.selectedItem.toString(),
                "lifestyle" to binding.spinnerLifestyle.selectedItem.toString(),
                "smoking" to binding.spinnerSmoking.selectedItem.toString(),
                "pets" to binding.spinnerPets.selectedItem.toString(),
                "cleanliness" to binding.spinnerCleanliness.selectedItem.toString(),
                "gender" to binding.spinnerGender.selectedItem.toString(),
                "occupation" to binding.spinnerOccupation.selectedItem.toString(),
                "mbti" to binding.spinnerMBTI.selectedItem.toString(),
                "moveInDate" to binding.spinnerMoveInDate.selectedItem.toString()
            )

            step2Dialog?.dismiss()
            onApplyFilters(filterMap)
        }

        binding.btnBack.setOnClickListener {
            step2Dialog?.dismiss()
            showStep1()
        }

        step2Dialog = AlertDialog.Builder(context).setView(binding.root).create()
        step2Dialog?.show()
    }
}
