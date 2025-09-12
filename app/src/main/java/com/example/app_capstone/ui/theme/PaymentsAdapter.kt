package com.example.app_capstone


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PaymentsAdapter(private var pagosList: List<Payment>) : RecyclerView.Adapter<PaymentsAdapter.PagoViewHolder>() {

    class PagoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMonto: TextView = itemView.findViewById(R.id.tvMonto)
        val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        val tvEstado: TextView = itemView.findViewById(R.id.tvEstado)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagoViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_payment, parent, false)
        return PagoViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PagoViewHolder, position: Int) {
        val pago = pagosList[position]
        holder.tvMonto.text = "Monto: S/ ${pago.monto}"
        holder.tvFecha.text = "Fecha: ${pago.fecha}"
        holder.tvEstado.text = "Estado: ${pago.estado}"
        holder.tvDescripcion.text = "Descripción: ${pago.descripcion}"
    }

    override fun getItemCount() = pagosList.size

    fun updateData(newList: List<Payment>) {
        pagosList = newList
        notifyDataSetChanged()
    }
}