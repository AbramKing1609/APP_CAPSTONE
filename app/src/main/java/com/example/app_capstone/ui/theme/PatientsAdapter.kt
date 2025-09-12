package com.example.app_capstone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PatientsAdapter(
    private var patientsList: List<Patient>,
    private val onItemClick: (Patient) -> Unit
) : RecyclerView.Adapter<PatientsAdapter.PatientViewHolder>() {

    class PatientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        val tvEdad: TextView = itemView.findViewById(R.id.tvEdad)
        val tvTelefono: TextView = itemView.findViewById(R.id.tvTelefono)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_patient, parent, false)
        return PatientViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        val paciente = patientsList[position]
        holder.tvNombre.text = "${paciente.nombre} ${paciente.apellido}"
        holder.tvEdad.text = "Edad: ${paciente.edad} años"
        holder.tvTelefono.text = "Teléfono: ${paciente.telefono}"

        holder.itemView.setOnClickListener {
            onItemClick(paciente)
        }
    }

    override fun getItemCount() = patientsList.size

    fun updateData(newList: List<Patient>) {
        patientsList = newList
        notifyDataSetChanged()
    }
}