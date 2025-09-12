package com.example.app_capstone

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PaymentsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var pagosAdapter: PaymentsAdapter
    private lateinit var tvEmpty: TextView
    private lateinit var btnBack: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payments)

        recyclerView = findViewById(R.id.rvPagos)
        tvEmpty = findViewById(R.id.tvEmptyPagos)
        btnBack = findViewById(R.id.btnBackPagos)

        // Configurar RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        pagosAdapter = PaymentsAdapter(emptyList())
        recyclerView.adapter = pagosAdapter

        // Cargar pagos del usuario actual
        loadPagos()

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadPagos() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("pagos")
                .whereEqualTo("doctorId", userId)
                .get()
                .addOnSuccessListener { documents ->
                    val pagosList = mutableListOf<Payment>()
                    for (document in documents) {
                        val pago = document.toObject(Payment::class.java)
                        pago.id = document.id
                        pagosList.add(pago)
                    }

                    if (pagosList.isEmpty()) {
                        tvEmpty.visibility = TextView.VISIBLE
                        recyclerView.visibility = RecyclerView.GONE
                    } else {
                        tvEmpty.visibility = TextView.GONE
                        recyclerView.visibility = RecyclerView.VISIBLE
                        pagosAdapter.updateData(pagosList)
                    }
                }
                .addOnFailureListener { exception ->
                    tvEmpty.text = "Error cargando pagos"
                    tvEmpty.visibility = TextView.VISIBLE
                    recyclerView.visibility = RecyclerView.GONE
                }
        }
    }
}