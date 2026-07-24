package com.example.mahubephamarcy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ConsultationAdapter(
    private var consultations: List<DBConnection.ConsultationData>
) : RecyclerView.Adapter<ConsultationAdapter.ConsultationViewHolder>() {

    class ConsultationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val type: TextView = view.findViewById(R.id.consultationType)
        val date: TextView = view.findViewById(R.id.consultationDate)
        val desc: TextView = view.findViewById(R.id.consultationDesc)
        val feedback: TextView = view.findViewById(R.id.pharmacyFeedback)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConsultationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_consultation, parent, false)
        return ConsultationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConsultationViewHolder, position: Int) {
        val item = consultations[position]
        holder.type.text = item.type
        holder.date.text = item.date
        holder.desc.text = item.desc

        android.util.Log.d("ConsultationAdapter", "Binding item: ${item.type}, Feedback: '${item.feedback}'")

        if (!item.feedback.isNullOrEmpty() && item.feedback.trim().isNotEmpty()) {
            holder.feedback.text = "Response: ${item.feedback}"
            if (item.feedback.contains("Approved", ignoreCase = true)) {
                holder.feedback.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.verified_green_text))
            } else {
                holder.feedback.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.logout_red))
            }
        } else {
            holder.feedback.text = "Status: Pending review"
            holder.feedback.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.secondary_textColor))
        }
    }

    override fun getItemCount() = consultations.size

    fun updateData(newConsultations: List<DBConnection.ConsultationData>) {
        this.consultations = newConsultations
        notifyDataSetChanged()
    }
}
