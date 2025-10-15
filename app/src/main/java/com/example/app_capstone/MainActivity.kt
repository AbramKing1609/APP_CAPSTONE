package com.example.app_capstone

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
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
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

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
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
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
        db.collection("medicos").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("NOMBRE") ?: "Doctor"
                    val lastName = document.getString("APELLIDO") ?: ""

                    // Mostrar contenido de inicio con los datos del usuario
                    displayContent(R.layout.content_home, name, lastName)
                } else {
                    displayContent(R.layout.content_home, "Doctor", "")
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar datos del médico.", Toast.LENGTH_SHORT).show()
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
                    db.collection("medicos").document(currentUser.uid).get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val name = document.getString("NOMBRE") ?: "Doctor"
                                val lastName = document.getString("APELLIDO") ?: ""
                                val tvWelcome = newLayout.findViewById<TextView>(R.id.tvWelcome)
                                val tvDoctorName = newLayout.findViewById<TextView>(R.id.tvDoctorName)
                                val btnLogout = newLayout.findViewById<Button>(R.id.btnLogout)

                                tvWelcome?.text = "Hola Doctor/a,"
                                tvDoctorName?.text = "$name $lastName"

                                btnLogout?.setOnClickListener {
                                    showLogoutDialog()
                                }
                            }
                        }
                }
            }

            R.layout.content_settings -> {
                val btnChangePassword = newLayout.findViewById<Button>(R.id.btnChangePassword)
                val btnAbout = newLayout.findViewById<Button>(R.id.btnAbout)

                btnChangePassword?.setOnClickListener {
                    showChangePasswordDialog()
                }

                btnAbout?.setOnClickListener {
                    showAboutDialog()
                }
            }
        }
    }

    /**
     * Muestra un diálogo para editar la información del perfil del doctor.
     * @param doctorData El mapa de datos del doctor, que ahora incluye los nombres de las colecciones de lookup.
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

        // --- PRECARGA DE DATOS ---
        etName.setText(doctorData?.get("NOMBRE") as? String ?: "")
        etLastName.setText(doctorData?.get("APELLIDO") as? String ?: "")

        // Usamos los nombres traídos de las lookups y hacemos estos campos de solo lectura
        etSpecialty.setText(doctorData?.get("NOMBRE_ESPECIALIDAD") as? String ?: "N/A")
        etSpecialty.isEnabled = false // No se puede editar como texto libre

        etUniversity.setText(doctorData?.get("NOMBRE_UNIVERSIDAD") as? String ?: "N/A")
        etUniversity.isEnabled = false // No se puede editar como texto libre

        etHospital.setText(doctorData?.get("NOMBRE_HOSPITAL") as? String ?: "N/A")
        etHospital.isEnabled = false // No se puede editar como texto libre

        etExperienceYears.setText(doctorData?.get("EXP_ANIOS")?.toString() ?: "")
        // Asumiendo que INFO_ADIC es el campo de la base de datos para Información Adicional
        etAdditionalInfo.setText(doctorData?.get("INFO_ADIC") as? String ?: "")


        // Listener para el botón Cancelar
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Listener para el botón Guardar
        btnSave.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                // Crear un mapa con los datos a actualizar (SOLO campos que son editables)
                // Usamos Any? para permitir el Int? de toIntOrNull()
                val updatedData = hashMapOf<String, Any?>(
                    "NOMBRE" to etName.text.toString(),
                    "APELLIDO" to etLastName.text.toString(),
                    "EXP_ANIOS" to etExperienceYears.text.toString().toIntOrNull(),
                    // Usamos INFO_ADIC basado en el esquema de Firestore
                    "INFO_ADIC" to etAdditionalInfo.text.toString()
                )

                // Actualizar el documento en la colección 'medicos'
                // La conversión a Map<String, Any> es segura aquí porque Firestore ignora los valores null en un update.
                db.collection("medicos").document(userId).update(updatedData as Map<String, Any>)
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

            tvWelcome?.text = "Hola Doctor/a,"
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
            .apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        startActivity(intent)
        finish()
    }

    /**
     * Muestra el diálogo para cambiar la contraseña.
     */
    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)
        val builder = AlertDialog.Builder(this)
            .setTitle("Cambiar Contraseña")
            .setView(dialogView)

        val dialog = builder.create()
        dialog.show()

        // Referencia a los nuevos campos de texto
        val etCurrentPassword = dialogView.findViewById<EditText>(R.id.etCurrentPassword)
        val etNewPassword = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<EditText>(R.id.etConfirmPassword)
        val btnCancelPassword = dialogView.findViewById<Button>(R.id.btnCancelPassword)
        val btnSavePassword = dialogView.findViewById<Button>(R.id.btnSavePassword)

        btnCancelPassword.setOnClickListener {
            dialog.dismiss()
        }

        btnSavePassword.setOnClickListener {
            val currentPassword = etCurrentPassword.text.toString()
            val newPassword = etNewPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = auth.currentUser
            if (user != null) {
                // Reautenticar al usuario con su contraseña actual
                val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

                user.reauthenticate(credential)
                    .addOnCompleteListener { reauthTask ->
                        if (reauthTask.isSuccessful) {
                            // Si la reautenticación es exitosa, actualizar la contraseña
                            user.updatePassword(newPassword)
                                .addOnCompleteListener { updateTask ->
                                    if (updateTask.isSuccessful) {
                                        Toast.makeText(this, "Contraseña actualizada correctamente.", Toast.LENGTH_SHORT).show()
                                        dialog.dismiss()
                                    } else {
                                        Toast.makeText(this, "Error al actualizar la contraseña: ${updateTask.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                        } else {
                            // Si la reautenticación falla (contraseña incorrecta)
                            Toast.makeText(this, "La contraseña actual es incorrecta.", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
    }

    /**
     * Muestra el diálogo de "Acerca de la Aplicación".
     */
    private fun showAboutDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_about, null)
        val builder = AlertDialog.Builder(this)
            .setTitle("Acerca de la Aplicación")
            .setView(dialogView)

        val dialog = builder.create()
        dialog.show()

        val btnCloseAbout = dialogView.findViewById<Button>(R.id.btnCloseAbout)
        btnCloseAbout.setOnClickListener {
            dialog.dismiss()
        }
    }
}
