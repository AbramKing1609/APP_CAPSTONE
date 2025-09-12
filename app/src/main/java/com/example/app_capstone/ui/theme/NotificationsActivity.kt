package com.example.app_capstone

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app_capstone.ui.theme.NotificationsAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class NotificationsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var notificationsAdapter: NotificationsAdapter
    private lateinit var tvEmpty: TextView
    private lateinit var btnBack: Button
    private var notificationsListener: ListenerRegistration? = null

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        recyclerView = findViewById(R.id.rvNotifications)
        tvEmpty = findViewById(R.id.tvEmptyNotifications)
        btnBack = findViewById(R.id.btnBackNotifications)

        // Configurar RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        notificationsAdapter = NotificationsAdapter(emptyList())
        recyclerView.adapter = notificationsAdapter

        // Cargar notificaciones del usuario actual
        loadNotifications()

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadNotifications() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            notificationsListener = db.collection("notificaciones")
                .whereEqualTo("doctorId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        tvEmpty.text = "Error cargando notificaciones"
                        tvEmpty.visibility = TextView.VISIBLE
                        recyclerView.visibility = RecyclerView.GONE
                        return@addSnapshotListener
                    }

                    val notificationsList = mutableListOf<Notification>()
                    if (snapshot != null) {
                        for (document in snapshot) {
                            val notificacion = document.toObject(Notification::class.java)
                            notificacion.id = document.id
                            notificationsList.add(notificacion)
                        }
                    }

                    if (notificationsList.isEmpty()) {
                        tvEmpty.visibility = TextView.VISIBLE
                        recyclerView.visibility = RecyclerView.GONE
                    } else {
                        tvEmpty.visibility = TextView.GONE
                        recyclerView.visibility = RecyclerView.VISIBLE
                        notificationsAdapter.updateData(notificationsList)
                    }
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationsListener?.remove()
    }
}