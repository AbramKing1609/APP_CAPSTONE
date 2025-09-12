package com.example.app_capstone

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PatientsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var patientsAdapter: PatientsAdapter
    private lateinit var tvEmpty: TextView
    private lateinit var btnBack: Button
    private lateinit var btnAddPatient: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patients)

        recyclerView = findViewById(R.id.rvPatients)
        tvEmpty = findViewById(R.id.tvEmptyPatients)
        btnBack = findViewById(R.id.btnBackPatients)
        btnAddPatient = findViewById(R.id.btnAddPatient)

        // Configurar RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        patientsAdapter = PatientsAdapter(emptyList()) { patient ->
            // Abrir detalles del paciente
            //val intent = Intent(this, PatientDetailActivity::class.java)
            intent.putExtra("patientId", patient.id)
            startActivity(intent)
        }
        recyclerView.adapter = patientsAdapter

        // Cargar pacientes del usuario actual
        loadPatients()

        btnBack.setOnClickListener {
            finish()
        }

        btnAddPatient.setOnClickListener {
            //val intent = Intent(this, AddPatientActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadPatients() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("pacientes")
                .whereEqualTo("doctorId", userId)
                .get()
                .addOnSuccessListener { documents ->
                    val patientsList = mutableListOf<Patient>()
                    for (document in documents) {
                        val patient = document.toObject(Patient::class.java)
                        patient.id = document.id
                        patientsList.add(patient)
                    }

                    if (patientsList.isEmpty()) {
                        tvEmpty.visibility = TextView.VISIBLE
                        recyclerView.visibility = RecyclerView.GONE
                    } else {
                        tvEmpty.visibility = TextView.GONE
                        recyclerView.visibility = RecyclerView.VISIBLE
                        patientsAdapter.updateData(patientsList)
                    }
                }
                .addOnFailureListener { exception ->
                    tvEmpty.text = "Error cargando pacientes"
                    tvEmpty.visibility = TextView.VISIBLE
                    recyclerView.visibility = RecyclerView.GONE
                }
        }
    }
}