package com.example.app_capstone

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
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
    private lateinit var mainContentFrame: FrameLayout

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
        mainContentFrame = findViewById(R.id.main_content_frame)
    }

    private fun setupNavigation() {
        // Botón de inicio ya está seleccionado por defecto
        selectButton(btnHome)

        btnHome.setOnClickListener {
            selectButton(btnHome)
            displayContent(R.layout.content_home)
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
            displayContent(R.layout.content_profile)
        }

        btnSettings.setOnClickListener {
            displayContent(R.layout.content_settings)
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
                    displayContent(R.layout.content_home, name, lastName)
                }
            }
            .addOnFailureListener {
                displayContent(R.layout.content_home, "Doctor", "")
            }
    }

    /**
     * Muestra el contenido del layout especificado en el FrameLayout principal.
     */
    private fun displayContent(layoutId: Int, name: String = "", lastName: String = "") {
        // Limpia cualquier vista anterior en el contenedor
        mainContentFrame.removeAllViews()

        // Infla el nuevo layout y lo agrega al FrameLayout
        val newLayout = LayoutInflater.from(this).inflate(layoutId, mainContentFrame, false)
        mainContentFrame.addView(newLayout)

        // Si el layout es el de inicio, actualiza los datos del usuario
        if (layoutId == R.layout.content_home) {
            val tvWelcome = newLayout.findViewById<TextView>(R.id.tvWelcome)
            val tvDoctorName = newLayout.findViewById<TextView>(R.id.tvDoctorName)
            val btnLogout = newLayout.findViewById<Button>(R.id.btnLogout)

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

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
