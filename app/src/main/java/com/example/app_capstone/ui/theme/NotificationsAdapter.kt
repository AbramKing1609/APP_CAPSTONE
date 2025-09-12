package com.example.app_capstone.ui.theme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.app_capstone.Notification  // Importa tu clase Notification
import com.example.app_capstone.R
import java.text.SimpleDateFormat
import java.util.*

class NotificationsAdapter(private var notificationsList: List<Notification>) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitulo: TextView = itemView.findViewById(R.id.tvTitulo)
        val tvMensaje: TextView = itemView.findViewById(R.id.tvMensaje)
        val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notificacion = notificationsList[position]
        holder.tvTitulo.text = notificacion.titulo
        holder.tvMensaje.text = notificacion.mensaje

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val fecha = dateFormat.format(notificacion.timestamp.toDate())
        holder.tvFecha.text = fecha
    }

    override fun getItemCount() = notificationsList.size

    fun updateData(newList: List<Notification>) {
        notificationsList = newList
        notifyDataSetChanged()
    }
}