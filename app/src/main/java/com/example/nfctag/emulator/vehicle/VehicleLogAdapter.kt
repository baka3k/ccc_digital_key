package com.example.nfctag.emulator.vehicle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.nfctag.R

class VehicleLogAdapter(private val data: MutableList<String> = mutableListOf()) :
    RecyclerView.Adapter<VehicleLogAdapter.ViewHolder>() {
    fun addLog(log: String) {
        data.add(log)
        notifyItemRangeInserted(data.size - 1, 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log_car, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val log = data[position]
        holder.textView.text = log
        val resource = holder.textView.resources
        if (log.startsWith("<<")) {
            holder.textView.setTextColor(resource.getColor(R.color.text_in))
        } else if (log.startsWith(">>")) {
            holder.textView.setTextColor(resource.getColor(R.color.text_out))
        } else {
            holder.textView.setTextColor(resource.getColor(R.color.text_normal))
        }
    }

    fun clear() {
        data.clear()
        notifyDataSetChanged()
    }

    // Holds the views for adding it to image and text
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.content)
    }

}