package com.mp.matematch.main.ui.feed

import android.widget.TextView
import android.widget.LinearLayout
import android.util.TypedValue
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mp.matematch.databinding.ItemFeedPersonBinding
import com.mp.matematch.R

class PersonAdapter(private val personList: List<Person>) :
    RecyclerView.Adapter<PersonAdapter.PersonViewHolder>() {

    class PersonViewHolder(val binding: ItemFeedPersonBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonViewHolder {
        val binding = ItemFeedPersonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PersonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
        val person = personList[position]
        with(holder.binding) {
            imageProfile.setImageResource(person.profileImageResId)

            // 이름, 나이, 직업 - textNameAge, textJob
            textNameAge.text = "${person.name}, ${person.age}"
            textJob.text = person.job

            // 소개 문구
            textQuote.text = person.description

            // 위치 + 렌트 범위
            textLocation.text = person.location
            textTime.text = person.rentRange

            // 태그 chip
            tagContainer.removeAllViews()
            person.tags.forEach { tag ->
                val tagView = TextView(root.context).apply {
                    text = tag
                    setBackgroundResource(R.drawable.bg_feed_tag_chip)
                    setTextColor(ContextCompat.getColor(root.context, R.color.tag_text))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setPadding(24, 12, 24, 12)
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(8, 0, 8, 0)
                    layoutParams = params
                }
                tagContainer.addView(tagView)
            }
        }


    }

    override fun getItemCount() = personList.size
}
