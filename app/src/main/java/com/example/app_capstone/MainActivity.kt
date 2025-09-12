package com.example.app_capstone

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var btnNotifications: LinearLayout
    private lateinit var btnPatients: LinearLayout
    private lateinit var btnHome: LinearLayout
    private lateinit var btnPagos: LinearLayout
    private lateinit var btnProfile: ImageButton
    private lateinit var btnSettings: ImageButton

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupNavigation()
        checkCurrentUserAndShowHome()
    }

    private fun initViews() {
        btnNotifications = findViewById(R.id.btnNotifications)
        btnPatients = findViewById(R.id.btnPatients)
        btnHome = findViewById(R.id.btnHome)
        btnPagos = findViewById(R.id.btnPagos)
        btnProfile = findViewById(R.id.btnProfile)
        btnSettings = findViewById(R.id.btnSettings)
    }

    private fun setupNavigation() {
        // Botón de inicio ya está seleccionado por defecto
        selectButton(btnHome)

        btnHome.setOnClickListener {
            selectButton(btnHome)
        }

        btnPagos.setOnClickListener {
            selectButton(btnPagos)
            val intent = Intent(this, PaymentsActivity::class.java)
            startActivity(intent)
        }

        btnNotifications.setOnClickListener {
            selectButton(btnNotifications)
            val intent = Intent(this, NotificationsActivity::class.java)
            startActivity(intent)
        }

        btnPatients.setOnClickListener {
            selectButton(btnPatients)
            val intent = Intent(this, PatientsActivity::class.java)
            startActivity(intent)
        }

        btnProfile.setOnClickListener {
            showProfileDialog()
        }

        btnSettings.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun selectButton(selectedButton: LinearLayout) {
        // Restablecer todos los botones
        val buttons = listOf(btnHome, btnPagos, btnNotifications, btnPatients)
        for (button in buttons) {
            val imageView = button.getChildAt(0) as ImageView
            val textView = button.getChildAt(1) as TextView

            if (button == selectedButton) {
                // Cambiar el color para el botón seleccionado
                imageView.setColorFilter(ContextCompat.getColor(this, R.color.selected_nav_color))
                textView.setTextColor(ContextCompat.getColor(this, R.color.selected_nav_color))
            } else {
                // Restablecer el color para los botones no seleccionados
                imageView.setColorFilter(ContextCompat.getColor(this, android.R.color.white))
                textView.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            }
        }
    }

    private fun checkCurrentUserAndShowHome() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            goToLogin()
            return
        }

        // Recuperar datos de Firestore
        val userId = currentUser.uid
        db.collection("doctores").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: "Doctor"
                    val lastName = document.getString("lastName") ?: ""

                    // Mostrar contenido de inicio con los datos del usuario
                    showHomeContent(name, lastName)
                }
            }
            .addOnFailureListener {
                showHomeContent("Doctor", "")
            }
    }

    private fun showHomeContent(name: String = "", lastName: String = "") {
        // Primero, encuentra la vista que incluye el diseño content_home
        val contentHomeView = findViewById<View>(R.id.contentHome)

        // Verifica si la vista existe antes de buscar elementos en ella
        contentHomeView?.let {
            val tvWelcome = it.findViewById<TextView>(R.id.tvWelcome)
            val tvDoctorName = it.findViewById<TextView>(R.id.tvDoctorName)
            val btnLogout = it.findViewById<Button>(R.id.btnLogout)

            tvWelcome?.text = "Bienvenido,"
            tvDoctorName?.text = "$name $lastName"

            btnLogout?.setOnClickListener {
                showLogoutDialog()
            }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Sí") { _: DialogInterface, _: Int ->
                auth.signOut()
                goToLogin()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showProfileDialog() {
        AlertDialog.Builder(this)
            .setTitle("Perfil")
            .setMessage("Aquí se mostrará la información de tu perfil.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Ajustes")
            .setMessage("Aquí estarán las opciones de configuración de la aplicación.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
