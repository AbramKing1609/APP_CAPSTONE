package com.example.app_capstone

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.graphics.Color
import androidx.cardview.widget.CardView

// Clase de datos genérica para listas (Notificaciones/Pacientes)
data class ItemData(val name: String, val type: String)

// 🔹 NUEVA CLASE DE DATOS para el cronograma del calendario
data class CalendarItemData(val hour: String, val patientName: String, val cardColor: String)

class MainActivity : AppCompatActivity() {

    private lateinit var btnNotifications: LinearLayout
    private lateinit var btnPatients: LinearLayout
    private lateinit var btnHome: LinearLayout
    private lateinit var btnCalendars: LinearLayout
    private lateinit var btnLogout: LinearLayout
    private lateinit var mainContentFrame: FrameLayout

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupNavigation()
        setupLogoutButton()
        checkCurrentUserAndShowHome()
    }

    private fun initViews() {
        btnNotifications = findViewById(R.id.btnNotifications)
        btnPatients = findViewById(R.id.btnPatients)
        btnHome = findViewById(R.id.btnHome)
        btnCalendars = findViewById(R.id.btnCalendars)
        btnLogout = findViewById(R.id.btnLogout)
        mainContentFrame = findViewById(R.id.main_content_frame)
    }

    private fun setupLogoutButton() {
        btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun setupNavigation() {
        // Botón de inicio ya está seleccionado por defecto
        selectButton(btnHome)

        btnHome.setOnClickListener {
            selectButton(btnHome)
            loadHomeContent()
        }

        btnCalendars.setOnClickListener {
            selectButton(btnCalendars)
            // 🔹 Cargar contenido dinámico de notificaciones
            displayContent(R.layout.content_calendars)
        }

        btnNotifications.setOnClickListener {
            selectButton(btnNotifications)
            // 🔹 Cargar contenido dinámico de notificaciones
            displayContent(R.layout.content_notifications)
        }

        btnPatients.setOnClickListener {
            selectButton(btnPatients)
            // 🔹 Cargar contenido dinámico de notificaciones
            displayContent(R.layout.content_patients)
        }
    }

    private fun selectButton(selectedButton: LinearLayout) {
        // Restablecer todos los botones
        val buttons = listOf(btnHome, btnCalendars, btnNotifications, btnPatients)
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

        // Recuperar datos de Firestore y mostrar el contenido de home
        loadHomeContent()
    }

    /**
     * Carga el contenido de home (content_home.xml) en el FrameLayout, recuperando los datos del usuario de Firestore.
     * Esto asegura que siempre se cargue con el nombre del doctor actualizado.
     */
    private fun loadHomeContent() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            goToLogin()
            return
        }

        val userId = currentUser.uid
        db.collection("medicos").document(userId).get()
            .addOnSuccessListener { document ->
                val name = document.getString("NOMBRE") ?: "Doctor"
                val lastName = document.getString("APELLIDO") ?: ""

                // Mostrar el contenido de home con los datos
                displayContent(R.layout.content_home, name, lastName)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar datos del médico.", Toast.LENGTH_SHORT).show()
                // Mostrar con valores por defecto en caso de error
                displayContent(R.layout.content_home, "Doctor", "")
            }
    }

    /**
     * Muestra el contenido del layout especificado en el FrameLayout principal.
     * @param layoutId El ID del layout a inflar.
     * @param name Nombre del doctor (solo para home).
     * @param lastName Apellido del doctor (solo para home).
     */
    private fun displayContent(layoutId: Int, name: String = "", lastName: String = "") {
        // Limpia cualquier vista anterior en el contenedor
        mainContentFrame.removeAllViews()

        // Infla el nuevo layout y lo agrega al FrameLayout
        val newLayout = LayoutInflater.from(this).inflate(layoutId, mainContentFrame, false)
        mainContentFrame.addView(newLayout)

        when (layoutId) {
            R.layout.content_home -> {
                // Configurar las vistas específicas de home
                val tvWelcome = newLayout.findViewById<TextView>(R.id.tvWelcome)
                val tvDoctorName = newLayout.findViewById<TextView>(R.id.tvDoctorName)
                val btnProfile = newLayout.findViewById<ImageButton>(R.id.btnProfile)
                val btnSettings = newLayout.findViewById<ImageButton>(R.id.btnSettings)

                tvWelcome?.text = "Hola Doctor/a,"
                tvDoctorName?.text = "$name $lastName"

                // Configurar listeners para btnProfile y btnSettings (solo disponibles en content_home)
                btnProfile?.setOnClickListener {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                }

                btnSettings?.setOnClickListener {
                    displayContent(R.layout.content_settings)
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
            R.layout.content_notifications -> {
                // 1. Obtener contenedores y datos
                val searchBar = newLayout.findViewById<EditText>(R.id.etSearchNotifications)
                val container = newLayout.findViewById<LinearLayout>(R.id.containerNotifications)
                val notificationsData = listOf(
                    ItemData("Brayan Calderon Quevedo", "Nuevo Paciente"),
                    ItemData("Maria Gomez Aguilar", "Cita Próxima"),
                    ItemData("Ricardo Moran Hurtado", "Seguimiento"),
                    ItemData("Carlos Alcantara Aguila", "Nuevo Paciente"),
                    ItemData("Jely Reategui Chongo", "Cita Próxima"),
                    ItemData("Mario Antizana Dumas", "Seguimiento")
                )

                // Lista para almacenar las vistas de los ítems
                val itemViews = mutableListOf<View>()

                // 2. Inflar ítems y guardar las vistas
                for (data in notificationsData) {
                    val itemView = LayoutInflater.from(this)
                        .inflate(R.layout.item_notification, container, false)
                    // Configurar el nombre y el tipo/estado
                    itemView.findViewById<TextView>(R.id.tvNotificationName).text = data.name
                    // 🔹 NOTIFICACIONES: Según item_notification.xml, este ID existe y se usa.
                    itemView.findViewById<TextView>(R.id.tvNotificationType).text = data.type

                    container.addView(itemView)
                    itemViews.add(itemView) // Guardamos la referencia a la vista
                }

                // 3. Configurar la búsqueda
                setupSearch(searchBar, itemViews, R.id.tvNotificationName)
            }

            // Lógica para Pacientes (content_patients)
            R.layout.content_patients -> {
                // 1. Obtener contenedores y datos
                val searchBar = newLayout.findViewById<EditText>(R.id.etSearchPatients)
                val container = newLayout.findViewById<LinearLayout>(R.id.containerPatients)
                val patientsData = listOf(
                    ItemData("Brayan Calderon Quevedo", "Nuevo Paciente"),
                    ItemData("Maria Gomez Aguilar", "Cita Próxima"),
                    ItemData("Ricardo Moran Hurtado", "Seguimiento"),
                    ItemData("Carlos Alcantara Aguila", "Nuevo Paciente"),
                    ItemData("Jely Reategui Chongo", "Cita Próxima"),
                    ItemData("Mario Antizana Dumas", "Seguimiento")
                )

                // Lista para almacenar las vistas de los ítems
                val itemViews = mutableListOf<View>()

                // 2. Inflar ítems y guardar las vistas
                for (data in patientsData) {
                    val itemView = LayoutInflater.from(this)
                        .inflate(R.layout.item_patient, container, false)
                    // Configurar el nombre
                    itemView.findViewById<TextView>(R.id.tvPatientName).text = data.name

                    // 🔹 CORRECCIÓN 2: Se ELIMINA la línea que intentaba asignar 'data.type' a un
                    // TextView de paciente, ya que el layout item_patient.xml proporcionado no lo tiene.
                    // Si deseas mostrar el tipo/estado, deberás modificar el XML de item_patient.xml

                    container.addView(itemView)
                    itemViews.add(itemView) // Guardamos la referencia a la vista
                }

                // 3. Configurar la búsqueda
                setupSearch(searchBar, itemViews, R.id.tvPatientName)
            }

            R.layout.content_calendars -> {
                val containerSchedule = newLayout.findViewById<LinearLayout>(R.id.containerSchedule)

                // Datos de ejemplo (pueden venir de Firestore luego)
                val appointments = listOf(
                    CalendarItemData("08:00", "Bryan Calderon", "#4CAF50"),
                    CalendarItemData("08:30", "Ricardo Moran", "#2196F3"),
                    CalendarItemData("08:45", "María Gomez", "#FF9800"),
                    CalendarItemData("09:00", "Carlos Alcántara", "#9C27B0"),
                    CalendarItemData("10:00", "Jely Reátegui", "#009688"),
                    CalendarItemData("10:30", "Mario Antizana", "#E91E63")
                )

                // Generar horas desde 8 a 22
                for (hour in 8..22) {
                    val hourLabel = String.format("%02d:00", hour)
                    val hourView = LayoutInflater.from(this)
                        .inflate(R.layout.item_calendar_hour, containerSchedule, false)

                    val tvHourLabel = hourView.findViewById<TextView>(R.id.tvHourLabel)
                    val containerAppointments = hourView.findViewById<LinearLayout>(R.id.containerAppointments)

                    tvHourLabel.text = hourLabel

                    // Filtrar citas de esa hora
                    val currentHourAppointments = appointments.filter {
                        it.hour.startsWith(String.format("%02d", hour))
                    }

                    // Inflar cada cita de esa hora
                    for (appt in currentHourAppointments) {
                        val itemView = LayoutInflater.from(this)
                            .inflate(R.layout.item_calendar_entry, containerAppointments, false)

                        val cardAppointment = itemView.findViewById<CardView>(R.id.cardAppointment)
                        val tvPatientName = itemView.findViewById<TextView>(R.id.tvPatientName)
                        val tvAvatarInitials = itemView.findViewById<TextView>(R.id.tvAvatarInitials)

                        tvPatientName.text = appt.patientName
                        val initials = appt.patientName.split(" ").map { it.first() }.take(2).joinToString("")
                        tvAvatarInitials.text = initials

                        val cardColor = Color.parseColor(appt.cardColor)
                        (cardAppointment.getChildAt(0) as LinearLayout).setBackgroundColor(cardColor)

                        // 🔹 Listener para mostrar ventana emergente (dialog)
                        cardAppointment.setOnClickListener {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("Detalles de la cita")
                            builder.setMessage(
                                "👤 Paciente: ${appt.patientName}\n🕓 Hora: ${appt.hour}"
                            )
                            builder.setPositiveButton("Cerrar", null)
                            builder.show()
                        }

                        containerAppointments.addView(itemView)
                    }

                    containerSchedule.addView(hourView)
                }

                newLayout.findViewById<ImageView>(R.id.btnBack)?.setOnClickListener {
                    loadHomeContent()
                }
            }
        }
    }
    private fun setupSearch(searchBar: EditText, itemViews: List<View>, textViewId: Int) {
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().toLowerCase()

                for (itemView in itemViews) {
                    val nameTextView = itemView.findViewById<TextView>(textViewId)
                    val name = nameTextView.text.toString().toLowerCase()

                    if (name.contains(query)) {
                        itemView.visibility = View.VISIBLE
                    } else {
                        itemView.visibility = View.GONE
                    }
                }
            }
        })
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
                db.collection("medicos").document(userId).update(updatedData.filterValues { it != null } as Map<String, Any>)
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

    private fun showLogoutDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Cerrar sesión")
            .setMessage("¿Está seguro que desea cerrar sesión?")
            .setPositiveButton("Sí") { dialog, _ ->
                // Cerrar sesión de Firebase
                auth.signOut()

                // Ir a la actividad MainApp (login o inicio)
                val intent = Intent(this, MainAppActivity::class.java) // Cambia MainAppActivity por el nombre real de tu actividad
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish() // Asegura que MainActivity se cierre
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
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