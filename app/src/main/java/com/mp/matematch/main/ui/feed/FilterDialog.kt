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
        val locations = listOf("Seoul", "Busan", "Incheon", "Daejeon", "Daegu") // TODO: R.array.cities
        val buildingTypes = listOf("난 집을 찾고 있지 않아요", "Apartment", "Villa", "Officetel", "Studio") // TODO: R.array

        locations.forEach { city ->
            val cb = CheckBox(context)
            cb.text = city
            cb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedLocations.add(city) else selectedLocations.remove(city)
            }
            layoutLocation.addView(cb)
        }

        val checkBoxes = mutableListOf<CheckBox>()
        buildingTypes.forEach { type ->
            val cb = CheckBox(context)
            cb.text = type
            layoutBuildingType.addView(cb)
            checkBoxes.add(cb)
        }

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

    private fun showStep2() {
        val bindingStep2 = DialogFilterStep2Binding.inflate(LayoutInflater.from(context))
        val btnApply = bindingStep2.btnApply
        val btnBack = bindingStep2.btnBack

        btnBack.setOnClickListener {
            step2Dialog?.dismiss()
            showStep1()
        }

        btnApply.setOnClickListener {
            val filters = mutableMapOf<String, Any?>(
                "locations" to selectedLocations,
                "buildingTypes" to selectedBuildingTypes
            )
            step2Dialog?.dismiss()
            onApplyFilters(filters)
        }

        step2Dialog = AlertDialog.Builder(context).setView(bindingStep2.root).create()
        step2Dialog?.show()
    }
}