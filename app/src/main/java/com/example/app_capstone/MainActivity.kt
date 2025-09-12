package com.example.app_capstone

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
     * Muestra el contenido del layout especificado en el FrameLayout principal y actualiza los datos.
     */
    private fun displayContent(layoutId: Int) {
        // Limpia cualquier vista anterior en el contenedor
        mainContentFrame.removeAllViews()

        // Infla el nuevo layout y lo agrega al FrameLayout
        val newLayout = LayoutInflater.from(this).inflate(layoutId, mainContentFrame, false)
        mainContentFrame.addView(newLayout)

        when (layoutId) {
            R.layout.content_home -> {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    db.collection("doctores").document(currentUser.uid).get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val name = document.getString("name") ?: "Doctor"
                                val lastName = document.getString("lastName") ?: ""
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
                }
            }
            R.layout.content_profile -> {
                val currentUser = auth.currentUser
                val tvProfileName = newLayout.findViewById<TextView>(R.id.tvProfileName)
                val tvProfileSpecialty = newLayout.findViewById<TextView>(R.id.tvProfileSpecialty)
                val tvUniversity = newLayout.findViewById<TextView>(R.id.tvUniversity)
                val tvExperienceYears = newLayout.findViewById<TextView>(R.id.tvExperienceYears)
                val tvHospital = newLayout.findViewById<TextView>(R.id.tvHospital)
                val tvAdditionalInfo = newLayout.findViewById<TextView>(R.id.tvAdditionalInfo)
                val ivProfilePicture = newLayout.findViewById<ImageView>(R.id.ivProfilePicture)
                // Encuentra el botón de edición y le añade un listener
                val btnEditProfile = newLayout.findViewById<Button>(R.id.btnEditProfile)

                if (currentUser != null) {
                    db.collection("doctores").document(currentUser.uid).get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val name = document.getString("name") ?: "N/A"
                                val lastName = document.getString("lastName") ?: ""
                                val specialty = document.getString("especialidad") ?: "N/A"
                                val university = document.getString("university") ?: "N/A"
                                val experienceYears = document.get("experience_years")?.toString() ?: "N/A"
                                val hospital = document.getString("hospital") ?: "N/A"
                                val additionalInfo = document.getString("additional_info") ?: "N/A"

                                tvProfileName?.text = "$name $lastName"
                                tvProfileSpecialty?.text = specialty
                                tvUniversity?.text = "Universidad: $university"
                                tvExperienceYears?.text = "Años de experiencia: $experienceYears"
                                tvHospital?.text = "Hospital: $hospital"
                                tvAdditionalInfo?.text = "Información Adicional: $additionalInfo"
                                // Note: For the image, you would need to use a library like Glide or Picasso
                                // to load it from a URL if you have one stored in Firestore.

                                // Aquí se añade el listener al botón de edición
                                btnEditProfile?.setOnClickListener {
                                    showEditProfileDialog(document.data)
                                }
                            } else {
                                // In case the document doesn't exist
                                tvProfileName?.text = "N/A"
                                tvProfileSpecialty?.text = "N/A"
                                tvUniversity?.text = "Universidad: N/A"
                                tvExperienceYears?.text = "Años de experiencia: N/A"
                                tvHospital?.text = "Hospital: N/A"
                                tvAdditionalInfo?.text = "Información Adicional: N/A"
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error al cargar datos: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    // No user logged in
                    tvProfileName?.text = "N/A"
                    tvProfileSpecialty?.text = "N/A"
                    tvUniversity?.text = "Universidad: N/A"
                    tvExperienceYears?.text = "Años de experiencia: N/A"
                    tvHospital?.text = "Hospital: N/A"
                    tvAdditionalInfo?.text = "Información Adicional: N/A"
                }
            }
            // Agrega más casos para otros layouts si es necesario
        }
    }

    /**
     * Muestra un diálogo para editar la información del perfil del doctor.
     * @param doctorData El mapa de datos del doctor recuperado de Firestore.
     */
    private fun showEditProfileDialog(doctorData: Map<String, Any>?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null)
        val builder = AlertDialog.Builder(this)
            .setTitle("Editar Perfil")
            .setView(dialogView)

        val dialog = builder.create()
        dialog.show()

        // Asignar las vistas del diálogo
        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etLastName = dialogView.findViewById<EditText>(R.id.etLastName)
        val etSpecialty = dialogView.findViewById<EditText>(R.id.etSpecialty)
        val etUniversity = dialogView.findViewById<EditText>(R.id.etUniversity)
        val etExperienceYears = dialogView.findViewById<EditText>(R.id.etExperienceYears)
        val etHospital = dialogView.findViewById<EditText>(R.id.etHospital)
        val etAdditionalInfo = dialogView.findViewById<EditText>(R.id.etAdditionalInfo)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        // Precargar los datos actuales en los EditText
        etName.setText(doctorData?.get("name") as? String ?: "")
        etLastName.setText(doctorData?.get("lastName") as? String ?: "")
        etSpecialty.setText(doctorData?.get("especialidad") as? String ?: "")
        etUniversity.setText(doctorData?.get("university") as? String ?: "")
        etExperienceYears.setText(doctorData?.get("experience_years")?.toString() ?: "")
        etHospital.setText(doctorData?.get("hospital") as? String ?: "")
        etAdditionalInfo.setText(doctorData?.get("additional_info") as? String ?: "")

        // Listener para el botón Cancelar
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Listener para el botón Guardar
        btnSave.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                // Crear un mapa con los datos a actualizar
                val updatedData = hashMapOf(
                    "name" to etName.text.toString(),
                    "lastName" to etLastName.text.toString(),
                    "especialidad" to etSpecialty.text.toString(),
                    "university" to etUniversity.text.toString(),
                    "experience_years" to etExperienceYears.text.toString().toIntOrNull(),
                    "hospital" to etHospital.text.toString(),
                    "additional_info" to etAdditionalInfo.text.toString()
                )

                // Actualizar el documento en Firestore
                db.collection("doctores").document(userId).update(updatedData as Map<String, Any>)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Perfil actualizado correctamente.", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        // Refrescar la pantalla de perfil para mostrar los datos actualizados
                        displayContent(R.layout.content_profile)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al actualizar el perfil: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // He mantenido la sobrecarga del método displayContent para la pantalla de inicio
    private fun displayContent(layoutId: Int, name: String = "", lastName: String = "") {
        mainContentFrame.removeAllViews()
        val newLayout = LayoutInflater.from(this).inflate(layoutId, mainContentFrame, false)
        mainContentFrame.addView(newLayout)

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
