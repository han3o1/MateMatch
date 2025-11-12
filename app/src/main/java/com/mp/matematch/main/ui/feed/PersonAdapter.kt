package com.mp.matematch.main.ui.feed

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mp.matematch.R
import com.mp.matematch.databinding.ItemFeedPersonBinding
import com.mp.matematch.profile.model.User
// import com.bumptech.glide.Glide

class PersonAdapter(private val userList: MutableList<User> = mutableListOf()) :
    RecyclerView.Adapter<PersonAdapter.PersonViewHolder>() {

    class PersonViewHolder(val binding: ItemFeedPersonBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            with(binding) {
                // 이미지 (TODO: Glide/Coil 라이브러리 필요)
                // .setImageResource -> .load(URL)
                // Glide.with(root.context).load(user.profileImageUrl).into(imageProfile)
                textNameAge.text = "${user.name}, ${user.age}"
                textJob.text = user.occupation
                textQuote.text = "\"${user.statusMessage}\""
                textLocation.text = "${user.city}, ${user.district}"
                textTime.text = "Move-in: ${user.moveInDate}"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonViewHolder {
        val binding = ItemFeedPersonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PersonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
        val user = userList[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int = userList.size

    fun updateData(newList: List<User>) {
        userList.clear()
        userList.addAll(newList)
        notifyDataSetChanged()
    }
}