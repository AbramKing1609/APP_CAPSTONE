package com.example.app_capstone
import android.os.PowerManager
import android.provider.Settings

import android.os.Handler
import android.os.Looper
import android.app.DatePickerDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
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
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Switch
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.RecyclerView // Agregar esta importación
import com.example.app_capstone.fcm.MyFirebaseMessagingService
import com.example.app_capstone.fcm.NotificationDismissReceiver
import com.example.app_capstone.ui.ChatRoom.ChatRoomFragment
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.messaging.FirebaseMessaging
import java.lang.reflect.Array.set
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationSwitch: Switch

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var estadoFiltroActual = "Todos"
    private var mostrandoHistorial = false
    private var fechaFiltroHistorial: String? = null

    // 🔹 VARIABLE PARA CONTROLAR REDIRECCIONES
    private var isHandlingNotification = false
    private var notificationsEnabled = true // 🔹 Variable para controlar estado


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this)

        // 🔹 VERIFICACIÓN INICIAL FORZADA
        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        if (!sharedPreferences.contains("notifications_enabled")) {
            // Primera vez - establecer como true por defecto
            sharedPreferences.edit().putBoolean("notifications_enabled", true).apply()
            Log.d("MainActivity", "🔔 Estado inicial configurado: true (primera vez)")
        }

        // 🔹 CARGAR ESTADO DE NOTIFICACIONES
        loadNotificationState()

        initViews()
        setupNavigation()
        setupLogoutButton()

        setupNotificationSystem()
        checkNotificationPermission()

        // 🔹 VERIFICAR REDIRECCIÓN ANTES DE CARGAR HOME
        if (!handleDirectNotificationIntent(intent)) {
            // Solo cargar home si NO viene de notificación
            checkCurrentUserAndShowHome()
        }

        Log.d("MainActivity", "onCreate - Configurando listener de citas")
        configurarListenerCitas()
    }

    // 🔹 MANEJAR onNewIntent
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "📱 onNewIntent llamado")
        setIntent(intent)
        handleDirectNotificationIntent(intent)
    }

    // 🔹 FUNCIÓN MEJORADA - REDIRECCIÓN DIRECTA
    private fun handleDirectNotificationIntent(intent: Intent): Boolean {
        if (isHandlingNotification) {
            Log.d("MainActivity", "⚠️ Ya se está manejando una notificación, ignorando")
            return true
        }

        Log.d("MainActivity", "🔍 Analizando intent para redirección directa")

        val openDirectlyTo = intent.getStringExtra("OPEN_DIRECTLY_TO")
        val fromNotification = intent.getBooleanExtra("from_notification", false)

        Log.d("MainActivity", "🎯 OPEN_DIRECTLY_TO: $openDirectlyTo, from_notification: $fromNotification")

        // 🔹 REDIRECCIÓN DIRECTA A NOTIFICACIONES
        if (openDirectlyTo == "NOTIFICATIONS" || fromNotification) {
            Log.d("MainActivity", "🚀 REDIRECCIÓN DIRECTA A NOTIFICACIONES")
            isHandlingNotification = true

            // Pequeño delay para asegurar que la UI esté lista
            Handler(Looper.getMainLooper()).postDelayed({
                runOnUiThread {
                    try {
                        // 🔹 INICIALIZAR VISTAS SI NO LO ESTÁN
                        if (!::btnNotifications.isInitialized) {
                            initViews()
                        }

                        // 🔹 SELECCIONAR DIRECTAMENTE NOTIFICACIONES
                        selectButton(btnNotifications)
                        displayContent(R.layout.content_notifications)

                        // 🔹 MOSTRAR MENSAJE
                        val notificationTitle = intent.getStringExtra("notification_title")
                        val toastMessage = if (!notificationTitle.isNullOrEmpty()) {
                            "📩 $notificationTitle"
                        } else {
                            "📩 Tienes nuevas notificaciones"
                        }

                        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()
                        Log.d("MainActivity", "✅ REDIRECCIÓN DIRECTA EXITOSA")

                    } catch (e: Exception) {
                        Log.e("MainActivity", "❌ Error en redirección directa: ${e.message}")
                        // Fallback: cargar home normal
                        checkCurrentUserAndShowHome()
                    } finally {
                        isHandlingNotification = false
                    }
                }
            }, 800) // 🔹 Aumentar delay para mayor estabilidad

            return true // 🔹 INDICAR QUE SE MANEJÓ LA REDIRECCIÓN
        }

        return false // 🔹 NO HAY REDIRECCIÓN, CONTINUAR NORMAL
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

    // 🔹 MODIFICAR checkCurrentUserAndShowHome para no interferir
    private fun checkCurrentUserAndShowHome() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            goToLogin()
            return
        }

        // 🔹 VERIFICAR SI HAY REDIRECCIÓN PENDIENTE
        if (handleDirectNotificationIntent(intent)) {
            Log.d("MainActivity", "🔔 Redirección a notificaciones detectada, omitiendo home")
            return
        }

        // 🔹 SOLO CARGAR HOME SI NO HAY REDIRECCIÓN
        val userId = currentUser.uid
        db.collection("medicos").document(userId).get()
            .addOnSuccessListener { document ->
                val name = document.getString("NOMBRE") ?: "Doctor"
                val lastName = document.getString("APELLIDO") ?: ""

                // 🔹 VERIFICAR OTRA VEZ POR SI ACASO
                if (!handleDirectNotificationIntent(intent)) {
                    displayContent(R.layout.content_home, name, lastName)
                    Log.d("MainActivity", "🏠 Contenido home cargado")
                }
            }
            .addOnFailureListener {
                if (!handleDirectNotificationIntent(intent)) {
                    displayContent(R.layout.content_home, "Doctor", "")
                    Log.d("MainActivity", "🏠 Contenido home cargado (fallback)")
                }
            }
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

    // Agregar estas clases de datos al inicio del archivo
    data class PacienteReal(
        val ID_PACIENTE: Long = 0L,
        val ID_USUARIO: Long = 0L, // 🔹 NUEVO CAMPO
        val NOMBRE: String = "",
        val APELLIDO: String = "",
        val CORREO: String = "",
        val CELULAR: String = "",
        val DNI: String = "",
        val EDAD: Int = 0,
        val SEXO: String = ""
    )

    data class NotificacionReal(
        val ID_NOTIFICACIONES: Long = 0L,
        val TITULO: String = "",
        val MENSAJE: String = "",
        val FECHA_ENVIO: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
        val ID_PACIENTE: Long = 0L,
        val ID_MEDICO: Long = 0L
    )

    data class CitaReal(
        val ID_CITA: Long = 0L,
        val ID_PACIENTE: Long = 0L,
        val ID_MEDICO: Long = 0L,
        val FECHA: String = "",
        val HORA: String = "",
        val ESTADO: String = ""
    )


    private fun setupNotificationSystem() {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MyFirebaseMessagingService.CHANNEL_ID,
                MyFirebaseMessagingService.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // 🔹 HIGH para persistencia
            ).apply {
                description = "Notificaciones de citas y pacientes - PERSISTENTES"
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
                enableVibration(true)
                vibrationPattern = longArrayOf(1000, 800, 1000, 800)

                // 🔹 CONFIGURACIÓN PARA PERSISTENCIA
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
                setBypassDnd(true) // Ignorar "No molestar"

                // Para Android 8.0+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(true)
                }
            }
            notificationManager.createNotificationChannel(channel)
            Log.d("MainActivity", "✅ Canal PERSISTENTE creado en MainActivity")
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            } else {
                Log.d("MainActivity", "✅ Permiso de notificaciones concedido")
            }
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
                notificationSwitch = newLayout.findViewById<Switch>(R.id.swNotifications) // 🔹 Asegurar esta línea

                // 🔹 CONFIGURAR EL SWITCH INMEDIATAMENTE
                setupNotificationSwitch()

                btnChangePassword?.setOnClickListener {
                    showChangePasswordDialog()
                }

                btnAbout?.setOnClickListener {
                    showAboutDialog()
                }
            }

            R.layout.content_notifications -> {
                // 1. Obtener contenedores
                val searchBar = newLayout.findViewById<EditText>(R.id.etSearchNotifications)
                val container = newLayout.findViewById<LinearLayout>(R.id.containerNotifications)
                val btnHistory = newLayout.findViewById<ImageButton>(R.id.btnHistory)
                val btnBack = newLayout.findViewById<ImageView>(R.id.btnBack)

                // 2. Configurar botón de historial
                btnHistory?.setOnClickListener {
                    mostrarDialogoHistorialNotificaciones(container, searchBar)
                }

                // 3. Configurar botón de volver
                btnBack?.setOnClickListener {
                    loadHomeContent()
                }

                // 4. Cargar notificaciones del día actual por defecto
                cargarNotificacionesDelDia(container, searchBar)
            }

            R.layout.content_patients -> {
                // 1. Obtener contenedores
                val searchBar = newLayout.findViewById<EditText>(R.id.etSearchPatients)
                val container = newLayout.findViewById<LinearLayout>(R.id.containerPatients)
                val btnHistoryPatients = newLayout.findViewById<ImageButton>(R.id.btnHistoryPatients)
                val btnBack = newLayout.findViewById<ImageView>(R.id.btnBack)

                // 2. Configurar botón de historial de pacientes
                btnHistoryPatients?.setOnClickListener {
                    mostrarDialogoHistorialPacientesCompletados(container, searchBar)
                }

                // 3. Configurar botón de volver
                btnBack?.setOnClickListener {
                    loadHomeContent()
                }

                // 4. Cargar pacientes activos (no completados)
                cargarPacientesActivos(container, searchBar)
            }

            R.layout.content_calendars -> {
                val containerSchedule = newLayout.findViewById<LinearLayout>(R.id.containerSchedule)
                val spinnerEstadoFilter = newLayout.findViewById<Spinner>(R.id.spinnerEstadoFilter)

                // Configurar el Spinner de filtro
                configurarFiltroEstados(spinnerEstadoFilter, containerSchedule)

                // Cargar citas reales desde Firebase
                cargarCitasReales(containerSchedule, estadoFiltroActual)

                newLayout.findViewById<ImageView>(R.id.btnBack)?.setOnClickListener {
                    loadHomeContent()
                }
            }

            R.layout.content_settings -> {
                val btnChangePassword = newLayout.findViewById<Button>(R.id.btnChangePassword)
                val btnAbout = newLayout.findViewById<Button>(R.id.btnAbout)
                notificationSwitch = newLayout.findViewById<Switch>(R.id.swNotifications)

                // Configurar el switch de notificaciones
                setupNotificationSwitch()

                btnChangePassword?.setOnClickListener {
                    showChangePasswordDialog()
                }

                btnAbout?.setOnClickListener {
                    showAboutDialog()
                }
            }
        }
    }

    private fun setupNotificationSwitch() {
        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true)

        // 🔹 FORZAR LA SINCRONIZACIÓN VISUAL DEL SWITCH
        runOnUiThread {
            notificationSwitch.isChecked = notificationsEnabled
            Log.d("MainActivity", "🔔 Switch configurado a: $notificationsEnabled")
        }

        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            notificationsEnabled = isChecked

            // 🔹 GUARDAR ESTADO INMEDIATAMENTE
            sharedPreferences.edit().putBoolean("notifications_enabled", isChecked).apply()

            Log.d("MainActivity", "🔔 Switch cambiado a: $isChecked")

            if (isChecked) {
                enableNotifications()
            } else {
                disableNotifications()
            }
        }
    }

    private fun areNotificationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(MyFirebaseMessagingService.CHANNEL_ID)
            channel?.importance != NotificationManager.IMPORTANCE_NONE
        } else {
            NotificationManagerCompat.from(this).areNotificationsEnabled()
        }
    }

    private fun enableNotifications() {
        Log.d("MainActivity", "🔔 Activando notificaciones...")

        // Suscribirse a temas FCM
        FirebaseMessaging.getInstance().subscribeToTopic("medical_app")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("MainActivity", "✅ Suscripción a notificaciones exitosa")
                    Toast.makeText(this, "🔔 Notificaciones activadas", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("MainActivity", "❌ Error al suscribirse a notificaciones")
                    // Revertir el cambio visual
                    runOnUiThread {
                        notificationSwitch.isChecked = false
                        notificationsEnabled = false
                    }
                }
            }
    }

    private fun disableNotifications() {
        Log.d("MainActivity", "🔕 Desactivando notificaciones...")

        // Cancelar suscripción a temas FCM
        FirebaseMessaging.getInstance().unsubscribeFromTopic("medical_app")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("MainActivity", "🔕 Notificaciones desactivadas")
                    Toast.makeText(this, "🔕 Notificaciones desactivadas", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("MainActivity", "❌ Error al desactivar notificaciones")
                    // Revertir el cambio visual
                    runOnUiThread {
                        notificationSwitch.isChecked = true
                        notificationsEnabled = true
                    }
                }
            }
    }



    // 🔹 NUEVA FUNCIÓN: Configurar el Spinner de filtro
    private fun configurarFiltroEstados(spinner: Spinner, containerSchedule: LinearLayout) {
        // 🔹 CORRECCIÓN: Agregar "Completada" al filtro
        val estados = arrayOf("Todos", "Confirmada/Reservada", "Pendiente", "Completada", "Cancelada")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, estados)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Establecer el estado actual seleccionado
        val posicionActual = estados.indexOf(estadoFiltroActual)
        if (posicionActual >= 0) {
            spinner.setSelection(posicionActual)
        }

        // Listener para cambios en el filtro
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val nuevoEstado = estados[position]
                if (nuevoEstado != estadoFiltroActual) {
                    estadoFiltroActual = nuevoEstado
                    Log.d("MainActivity", "Filtro cambiado a: $estadoFiltroActual")
                    cargarCitasReales(containerSchedule, estadoFiltroActual)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    /**
     * Crea una notificación automáticamente cuando se agenda una cita
     */
    private fun crearNotificacionCita(cita: CitaReal, paciente: PacienteReal, tipo: String) {
        // 🔹 VERIFICACIÓN ESTRICTA AL INICIO
        if (!notificationsEnabled) {
            Log.d("MainActivity", "🔕 NOTIFICACIONES DESACTIVADAS - No se procesará cita de tipo: $tipo")
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("MainActivity", "Usuario no autenticado al crear notificación")
            return
        }

        // Obtener ID_MEDICO del usuario actual
        db.collection("medicos")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { medicoDoc ->
                val idMedico = medicoDoc.getLong("ID_MEDICO") ?: 0L
                if (idMedico == 0L) {
                    Log.e("MainActivity", "ID_MEDICO es 0 al crear notificación")
                    return@addOnSuccessListener
                }

                val fechaSolicitud = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

                // 🔹 CORRECCIÓN COMPLETA: Usar when exhaustivo
                val (titulo, mensaje) = when (tipo) {
                    "nueva_cita" -> Pair(
                        "📋 Nueva Cita Solicitada - $fechaSolicitud",
                        "El paciente ${paciente.NOMBRE} ${paciente.APELLIDO} ha solicitado una nueva cita para el ${formatearFecha(cita.FECHA)} a las ${cita.HORA.substring(0, 5)}\n\n📅 Solicitado el: $fechaSolicitud"
                    )
                    "cita_pendiente" -> Pair(
                        "⏳ Cita Pendiente - $fechaSolicitud",
                        "La cita con ${paciente.NOMBRE} ${paciente.APELLIDO} está pendiente de confirmación para el ${formatearFecha(cita.FECHA)}"
                    )
                    "cita_reservada" -> Pair(
                        "✅ Cita Confirmada - $fechaSolicitud",
                        "La cita con ${paciente.NOMBRE} ${paciente.APELLIDO} ha sido confirmada para el ${formatearFecha(cita.FECHA)} a las ${cita.HORA.substring(0, 5)}"
                    )
                    "cita_cancelada" -> Pair(
                        "❌ Cita Cancelada - $fechaSolicitud",
                        "La cita con ${paciente.NOMBRE} ${paciente.APELLIDO} programada para el ${formatearFecha(cita.FECHA)} ha sido cancelada"
                    )
                    "cita_completada" -> Pair(
                        "🏁 Cita Completada - $fechaSolicitud",
                        "La cita con ${paciente.NOMBRE} ${paciente.APELLIDO} ha sido completada exitosamente"
                    )
                    else -> Pair(
                        "📝 Actualización de Cita - $fechaSolicitud",
                        "Actualización en la cita del paciente ${paciente.NOMBRE} ${paciente.APELLIDO}"
                    )
                }

                val notificacionData = hashMapOf(
                    "ID_NOTIFICACIONES" to System.currentTimeMillis(),
                    "TITULO" to titulo,
                    "MENSAJE" to mensaje,
                    "FECHA_ENVIO" to com.google.firebase.Timestamp.now(),
                    "ID_PACIENTE" to cita.ID_PACIENTE,
                    "ID_MEDICO" to idMedico,
                    "FECHA_SOLICITUD" to fechaSolicitud
                )

                Log.d("MainActivity", "Creando notificación en Firestore: $titulo")

                // Guardar en Firebase
                db.collection("notificaciones")
                    .add(notificacionData)
                    .addOnSuccessListener {
                        Log.d("MainActivity", "✅ Notificación creada exitosamente en Firestore: $titulo")
                        // 🔹 VERIFICAR UNA VEZ MÁS ANTES DE ENVIAR NOTIFICACIÓN LOCAL
                        if (notificationsEnabled) {
                            enviarNotificacionLocal(cita, paciente, tipo)
                        } else {
                            Log.d("MainActivity", "🔕 Notificaciones desactivadas - No se enviará notificación local")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("MainActivity", "❌ Error al crear notificación en Firestore: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error al obtener médico: ${e.message}")
            }
    }

    private fun enviarNotificacionLocal(cita: CitaReal, paciente: PacienteReal, tipo: String) {
        // 🔹 VERIFICACIÓN ESTRICTA - SOLO ENVIAR SI ESTÁN ACTIVADAS
        if (!notificationsEnabled) {
            Log.d("MainActivity", "🔕 NOTIFICACIONES DESACTIVADAS - No se enviará notificación local")
            return
        }

        val (titulo, mensaje) = when (tipo) {
            "nueva_cita" -> Pair("📋 Nueva Cita", "Paciente: ${paciente.NOMBRE} ${paciente.APELLIDO}")
            "cita_reservada" -> Pair("✅ Cita Confirmada", "Paciente: ${paciente.NOMBRE} ${paciente.APELLIDO}")
            "cita_cancelada" -> Pair("❌ Cita Cancelada", "Paciente: ${paciente.NOMBRE} ${paciente.APELLIDO}")
            "cita_completada" -> Pair("🏁 Cita Completada", "Paciente: ${paciente.NOMBRE} ${paciente.APELLIDO}")
            else -> Pair("Notificación Médica", "Actualización de cita")
        }

        Log.d("MainActivity", "🔔 Enviando notificación local: $titulo")

        val notificationId = System.currentTimeMillis().toInt()

        // Intent directo a notificaciones
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("OPEN_DIRECTLY_TO", "NOTIFICATIONS")
            putExtra("from_notification", true)
            putExtra("notification_title", titulo)
            action = "OPEN_NOTIFICATIONS_${System.currentTimeMillis()}"
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para eliminar notificación
        val dismissIntent = Intent(this, NotificationDismissReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            notificationId,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // NOTIFICACIÓN
        val notification = NotificationCompat.Builder(this, MyFirebaseMessagingService.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .setTimeoutAfter(0)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .addAction(R.drawable.ic_notification, "Eliminar", dismissPendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)

        Log.d("MainActivity", "✅ Notificación local enviada - ID: $notificationId")
    }

    private fun loadNotificationState() {
        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true)
        Log.d("MainActivity", "🔔 Estado de notificaciones cargado: $notificationsEnabled")
    }

    // 🔹 NOTIFICACIÓN NORMAL COMO RESPALDO
    private fun crearNotificacionNormal(titulo: String, mensaje: String, intent: Intent) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt() + 1, // ID diferente
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val normalNotification = NotificationCompat.Builder(this, MyFirebaseMessagingService.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$titulo (Info)")
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(false) // No persistente
            .build()

        val normalNotificationId = System.currentTimeMillis().toInt() + 1
        notificationManager.notify(normalNotificationId, normalNotification)

        Log.d("MainActivity", "🔔 Notificación normal de respaldo enviada - ID: $normalNotificationId")
    }

    // 🔹 FUNCIÓN DE PRUEBA PARA NOTIFICACIONES PERSISTENTES
    private fun testPersistentNotification() {
        Log.d("MainActivity", "🧪 TEST: Probando notificación PERSISTENTE...")

        val testIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("OPEN_DIRECTLY_TO", "NOTIFICATIONS")
            putExtra("from_notification", true)
            putExtra("notification_title", "🧪 Notificación PERSISTENTE")
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            testIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notificación de prueba PERSISTENTE
        val notification = NotificationCompat.Builder(this, MyFirebaseMessagingService.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🧪 Notificación PERSISTENTE")
            .setContentText("Esta notificación debería permanecer en pantalla de bloqueo")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(false)
            .setTimeoutAfter(0) // 🔹 NO se auto-elimina
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .build()

        val notificationId = 9999 // ID fijo para pruebas
        notificationManager.notify(notificationId, notification)

        Log.d("MainActivity", "🧪 Notificación PERSISTENTE de prueba enviada - ID: $notificationId")
        Toast.makeText(this, "Notificación persistente enviada", Toast.LENGTH_SHORT).show()
    }

    /**
     * Valida si un paciente ya tiene una cita con el mismo médico en la misma fecha
     * RETORNA: true si ya existe una cita (y cuál es la cita existente), false si no existe
     */
    private fun validarCitaDuplicada(idPaciente: Long, fechaCita: String, callback: (Boolean, String?) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            callback(false, null)
            return
        }

        db.collection("medicos")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { medicoDoc ->
                val idMedico = medicoDoc.getLong("ID_MEDICO") ?: 0L
                if (idMedico == 0L) {
                    callback(false, null)
                    return@addOnSuccessListener
                }

                // Buscar citas del mismo paciente con el mismo médico en la misma fecha
                db.collection("cita")
                    .whereEqualTo("ID_PACIENTE", idPaciente)
                    .whereEqualTo("ID_MEDICO", idMedico)
                    .whereEqualTo("FECHA", fechaCita)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (documents.isEmpty) {
                            // No hay citas duplicadas
                            callback(false, null)
                        } else {
                            // Hay citas existentes - obtener la primera (debería haber solo una)
                            val citaExistente = documents.documents.first()
                            val idCitaExistente = citaExistente.id
                            Log.d("MainActivity", "Cita duplicada encontrada: $idCitaExistente")
                            callback(true, idCitaExistente)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("MainActivity", "Error al validar cita duplicada: ${e.message}")
                        callback(false, null)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error al obtener médico para validación: ${e.message}")
                callback(false, null)
            }
    }
    /**
     * Valida si un paciente ya tiene una cita solicitada HOY con el mismo médico
     */
    private fun validarCitaHoy(idPaciente: Long, callback: (Boolean) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            callback(false)
            return
        }

        val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        db.collection("medicos")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { medicoDoc ->
                val idMedico = medicoDoc.getLong("ID_MEDICO") ?: 0L
                if (idMedico == 0L) {
                    callback(false)
                    return@addOnSuccessListener
                }

                // 🔹 CORRECCIÓN: Buscar por parte del título que contiene la fecha
                db.collection("notificaciones")
                    .whereEqualTo("ID_PACIENTE", idPaciente)
                    .whereEqualTo("ID_MEDICO", idMedico)
                    .whereEqualTo("TITULO", "📋 Nueva Cita Solicitada - $fechaHoy")
                    .get()
                    .addOnSuccessListener { documents ->
                        val yaSolicitoHoy = !documents.isEmpty
                        Log.d("MainActivity", "Validación cita hoy - Paciente: $idPaciente, Ya solicitó hoy: $yaSolicitoHoy")
                        callback(yaSolicitoHoy)
                    }
                    .addOnFailureListener { e ->
                        Log.e("MainActivity", "Error al validar cita hoy: ${e.message}")
                        callback(false)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error al obtener médico para validación hoy: ${e.message}")
                callback(false)
            }
    }

    /**
     * Escucha cambios en las citas en tiempo real y genera notificaciones
     */
    private fun configurarListenerCitas() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("MainActivity", "Usuario no autenticado")
            return
        }

        db.collection("medicos")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { medicoDoc ->
                val idMedico = medicoDoc.getLong("ID_MEDICO") ?: 0L
                Log.d("MainActivity", "Configurando listener para médico ID: $idMedico")

                if (idMedico == 0L) {
                    Log.e("MainActivity", "ID_MEDICO es 0")
                    return@addOnSuccessListener
                }

                // 🔹 VARIABLE PARA RASTREAR ESTADOS ANTERIORES
                val estadosAnteriores = mutableMapOf<String, String>()

                // Listener en tiempo real para citas de este médico
                db.collection("cita")
                    .whereEqualTo("ID_MEDICO", idMedico)
                    .addSnapshotListener { snapshots, error ->
                        if (error != null) {
                            Log.e("MainActivity", "Error en listener de citas: ${error.message}")
                            return@addSnapshotListener
                        }

                        Log.d("MainActivity", "Listener activado - Cambios: ${snapshots?.documentChanges?.size}")

                        snapshots?.documentChanges?.forEach { change ->
                            val documentId = change.document.id
                            val estadoActual = change.document.getString("ESTADO") ?: ""

                            Log.d("MainActivity", "Tipo de cambio: ${change.type}, Estado actual: $estadoActual, Documento: $documentId")

                            when (change.type) {
                                DocumentChange.Type.ADDED -> {
                                    val cita = change.document.toObject(CitaReal::class.java)
                                    val documentId = change.document.id

                                    Log.d("MainActivity", "Nueva cita agregada: ${cita.ESTADO}, ID: $documentId, Paciente: ${cita.ID_PACIENTE}, Fecha: ${cita.FECHA}")

                                    // Validar cita duplicada en la misma fecha
                                    validarCitaDuplicada(cita.ID_PACIENTE, cita.FECHA) { tieneDuplicada, idCitaExistente ->
                                        if (tieneDuplicada && idCitaExistente != null) {
                                            Log.w("MainActivity", "❌ Paciente ya tiene cita en esta fecha - ID existente: $idCitaExistente")

                                            // 🔹 CORRECCIÓN IMPORTANTE: Solo eliminar la NUEVA cita si ya existe una
                                            if (idCitaExistente != documentId) {
                                                Log.w("MainActivity", "Eliminando cita duplicada: $documentId")

                                                // Eliminar la cita duplicada (la nueva)
                                                db.collection("cita").document(documentId).delete()
                                                    .addOnSuccessListener {
                                                        Log.d("MainActivity", "Cita duplicada eliminada: $documentId")
                                                        // Mostrar notificación de cita duplicada
                                                        obtenerPacienteYCrearNotificacion(cita, "cita_duplicada")
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.e("MainActivity", "Error al eliminar cita duplicada: ${e.message}")
                                                    }
                                            } else {
                                                Log.d("MainActivity", "La cita existente es la misma que la nueva, no se elimina")
                                            }
                                        } else {
                                            // Validar límite de 1 cita por día
                                            validarCitaHoy(cita.ID_PACIENTE) { yaSolicitoHoy ->
                                                if (yaSolicitoHoy) {
                                                    Log.w("MainActivity", "❌ Paciente ya solicitó cita hoy - Eliminando cita")

                                                    // Eliminar la cita que excede el límite
                                                    db.collection("cita").document(documentId).delete()
                                                        .addOnSuccessListener {
                                                            Log.d("MainActivity", "Cita excedente eliminada")
                                                            // Obtener datos del paciente para mostrar alerta
                                                            db.collection("pacientes")
                                                                .whereEqualTo("ID_PACIENTE", cita.ID_PACIENTE)
                                                                .get()
                                                                .addOnSuccessListener { pacientes ->
                                                                    if (!pacientes.isEmpty) {
                                                                        val pacienteDoc = pacientes.documents.first()
                                                                        val paciente = PacienteReal(
                                                                            ID_PACIENTE = pacienteDoc.getLong("ID_PACIENTE") ?: 0L,
                                                                            //ID_USUARIO = getSafeLong(pacienteDoc, "ID_USUARIO"), // ← AGREGAR ESTO
                                                                            NOMBRE = pacienteDoc.getString("NOMBRE") ?: "",
                                                                            APELLIDO = pacienteDoc.getString("APELLIDO") ?: "",
                                                                            CORREO = pacienteDoc.getString("CORREO") ?: "",
                                                                            CELULAR = getSafeString(pacienteDoc, "CELULAR"),
                                                                            DNI = pacienteDoc.getString("DNI") ?: "",
                                                                            EDAD = 0,
                                                                            SEXO = pacienteDoc.getString("SEXO") ?: ""
                                                                        )
                                                                        mostrarAlertaLimiteCitas(paciente)
                                                                    }
                                                                }
                                                        }
                                                } else {
                                                    // ✅ Validaciones pasadas - Crear notificación normal
                                                    obtenerPacienteYCrearNotificacion(cita, "nueva_cita")
                                                }
                                            }
                                        }
                                    }
                                }

                                DocumentChange.Type.MODIFIED -> {
                                    // Cita modificada (cambio de estado) - NOTIFICAR SIEMPRE
                                    val cita = change.document.toObject(CitaReal::class.java)
                                    val estadoNuevo = cita.ESTADO.toLowerCase()

                                    Log.d("MainActivity", "Cita modificada - Nuevo estado: $estadoNuevo")

                                    when (estadoNuevo) {
                                        "pendiente" -> {
                                            Log.d("MainActivity", "Creando notificación de cita pendiente")
                                            obtenerPacienteYCrearNotificacion(cita, "cita_pendiente")
                                        }
                                        "reservada", "confirmada" -> {
                                            Log.d("MainActivity", "Creando notificación de cita reservada/confirmada")
                                            obtenerPacienteYCrearNotificacion(cita, "cita_reservada")
                                        }
                                        "cancelada" -> {
                                            Log.d("MainActivity", "Creando notificación de cita cancelada")
                                            obtenerPacienteYCrearNotificacion(cita, "cita_cancelada")
                                        }
                                        "completada" -> {
                                            Log.d("MainActivity", "Creando notificación de cita completada")
                                            obtenerPacienteYCrearNotificacion(cita, "cita_completada")
                                        }
                                        else -> {
                                            Log.d("MainActivity", "Cambio de estado no manejado: $estadoNuevo")
                                        }
                                    }
                                }

                                DocumentChange.Type.REMOVED -> {
                                    Log.d("MainActivity", "Cita eliminada: $documentId")
                                    estadosAnteriores.remove(documentId)
                                }
                            }
                        }
                    }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error al obtener médico para listener: ${e.message}")
            }
    }
    /**
     * 🔹 FUNCIÓN TEMPORAL: Verificar el estado de las citas en Firebase
     */
    private fun verificarEstadoCitas() {
        val currentUser = auth.currentUser
        if (currentUser == null) return

        db.collection("medicos")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { medicoDoc ->
                val idMedico = medicoDoc.getLong("ID_MEDICO") ?: 0L

                db.collection("cita")
                    .whereEqualTo("ID_MEDICO", idMedico)
                    .get()
                    .addOnSuccessListener { documents ->
                        Log.d("MainActivity", "=== VERIFICACIÓN DE CITAS ===")
                        Log.d("MainActivity", "Total citas encontradas: ${documents.size()}")

                        for (document in documents) {
                            val idCita = document.id
                            val idPaciente = document.getLong("ID_PACIENTE") ?: 0L
                            val fecha = document.getString("FECHA") ?: ""
                            val estado = document.getString("ESTADO") ?: ""

                            Log.d("MainActivity", "Cita ID: $idCita, Paciente: $idPaciente, Fecha: $fecha, Estado: $estado")
                        }
                        Log.d("MainActivity", "=== FIN VERIFICACIÓN ===")

                        if (documents.isEmpty) {
                            Toast.makeText(this, "⚠️ No hay citas en la base de datos", Toast.LENGTH_LONG).show()
                        }
                    }
            }
    }
    /**
     * Muestra una alerta cuando un paciente intenta agendar más de una cita por día
     */
    private fun mostrarAlertaLimiteCitas(paciente: PacienteReal) {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Límite de Citas Alcanzado")
            builder.setMessage("El paciente ${paciente.NOMBRE} ${paciente.APELLIDO} ya tiene una cita solicitada para hoy.\n\nSolo se permite 1 solicitud de cita por paciente por día.")
            builder.setPositiveButton("Entendido", null)
            builder.setIcon(android.R.drawable.ic_dialog_info)
            builder.show()
        }
    }
    /**
     * Obtiene datos del paciente y crea la notificación correspondiente
     */
    private fun obtenerPacienteYCrearNotificacion(cita: CitaReal, tipoNotificacion: String) {
        db.collection("pacientes")
            .whereEqualTo("ID_PACIENTE", cita.ID_PACIENTE)
            .get()
            .addOnSuccessListener { pacientes ->
                if (!pacientes.isEmpty) {
                    val pacienteDoc = pacientes.documents.first()

                    // 🔹 CORRECCIÓN: Manejar EDAD como String ya que en Firebase es "25" (string)
                    val edadString = getSafeString(pacienteDoc, "EDAD")
                    val edad = try {
                        edadString.toInt()
                    } catch (e: NumberFormatException) {
                        0
                    }

                    // 🔹 CORRECCIÓN: Manejar CELULAR que puede ser String o Number
                    val celular = getSafeString(pacienteDoc, "CELULAR")

                    val paciente = PacienteReal(
                        ID_PACIENTE = getSafeLong(pacienteDoc, "ID_PACIENTE"),
                        //ID_USUARIO = getSafeLong(pacienteDoc, "ID_USUARIO"), // ← AGREGAR ESTO
                        NOMBRE = getSafeString(pacienteDoc, "NOMBRE"),
                        APELLIDO = getSafeString(pacienteDoc, "APELLIDO"),
                        CORREO = getSafeString(pacienteDoc, "CORREO"),
                        CELULAR = celular, // 🔹 Usar la función segura
                        DNI = getSafeString(pacienteDoc, "DNI"),
                        EDAD = edad,
                        SEXO = getSafeString(pacienteDoc, "SEXO")
                    )
                    crearNotificacionCita(cita, paciente, tipoNotificacion)
                }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error al obtener paciente: ${e.message}")
            }
    }

    /**
     * Carga las notificaciones reales del médico desde Firebase Firestore
     */
    private fun cargarNotificacionesReales(container: LinearLayout, searchBar: EditText) {
        mostrarLoadingIndicator(container, true)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            mostrarLoadingIndicator(container, false)
            return
        }

        // Primero obtener el ID_MEDICO del usuario actual
        db.collection("medicos")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { medicoDoc ->
                val idMedico = medicoDoc.getLong("ID_MEDICO") ?: 0L

                if (idMedico == 0L) {
                    mostrarLoadingIndicator(container, false)
                    Toast.makeText(this, "No se encontró información del médico", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Buscar notificaciones para este médico - ordenadas por fecha (más recientes primero)
                db.collection("notificaciones")
                    .whereEqualTo("ID_MEDICO", idMedico)
                    .orderBy("FECHA_ENVIO", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .addOnSuccessListener { documents ->
                        mostrarLoadingIndicator(container, false)

                        if (documents.isEmpty) {
                            mostrarEstadoVacio(container, "No hay notificaciones")
                            return@addOnSuccessListener
                        }

                        val notificacionesList = mutableListOf<NotificacionReal>()
                        val itemViews = mutableListOf<View>()

                        for (document in documents) {
                            val notificacion = NotificacionReal(
                                ID_NOTIFICACIONES = document.getLong("ID_NOTIFICACIONES") ?: 0L,
                                TITULO = document.getString("TITULO") ?: "",
                                MENSAJE = document.getString("MENSAJE") ?: "",
                                FECHA_ENVIO = document.getTimestamp("FECHA_ENVIO") ?: com.google.firebase.Timestamp.now(),
                                ID_PACIENTE = document.getLong("ID_PACIENTE") ?: 0L,
                                ID_MEDICO = document.getLong("ID_MEDICO") ?: 0L
                            )
                            notificacionesList.add(notificacion)
                        }

                        // Inflar y mostrar notificaciones
                        for (notificacion in notificacionesList) {
                            val itemView = LayoutInflater.from(this)
                                .inflate(R.layout.item_notification, container, false)

                            // Configurar nombre del paciente
                            configurarNombrePacienteEnNotificacion(itemView, notificacion.ID_PACIENTE)

                            // 🔹 CORRECCIÓN: Mostrar el TÍTULO como tipo/estado (igual que antes)
                            itemView.findViewById<TextView>(R.id.tvNotificationType).text = notificacion.TITULO

                            // Listener para mostrar detalles
                            itemView.setOnClickListener {
                                mostrarDetallesNotificacion(notificacion)
                            }

                            container.addView(itemView)
                            itemViews.add(itemView)
                        }

                        // Configurar búsqueda
                        setupSearch(searchBar, itemViews, R.id.tvNotificationName)
                    }
                    .addOnFailureListener { e ->
                        mostrarLoadingIndicator(container, false)
                        Toast.makeText(this, "Error al cargar notificaciones: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                mostrarLoadingIndicator(container, false)
                Toast.makeText(this, "Error al obtener datos del médico: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Formatea la fecha de la notificación para mostrar de manera relativa
     */
    private fun formatearFechaNotificacion(timestamp: com.google.firebase.Timestamp): String {
        return try {
            val date = timestamp.toDate()
            val now = Date()
            val diff = now.time - date.time
            val minutes = diff / (60 * 1000)
            val hours = diff / (60 * 60 * 1000)
            val days = diff / (24 * 60 * 60 * 1000)

            when {
                minutes < 1 -> "Hace unos segundos"
                minutes < 60 -> "Hace $minutes min"
                hours < 24 -> "Hace $hours h"
                days < 7 -> "Hace $days días"
                else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
            }
        } catch (e: Exception) {
            "Fecha no disponible"
        }
    }

    // 🔹 TEMPORAL: Forzar recarga de notificaciones
    private fun recargarNotificaciones() {
        // Si estás en la pantalla de notificaciones, recargar
        val currentLayout = mainContentFrame.getChildAt(0)
        if (currentLayout != null) {
            val container = currentLayout.findViewById<LinearLayout>(R.id.containerNotifications)
            val searchBar = currentLayout.findViewById<EditText>(R.id.etSearchNotifications)
            if (container != null && searchBar != null) {
                cargarNotificacionesReales(container, searchBar)
                Toast.makeText(this, "Notificaciones recargadas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Carga los pacientes reales del médico desde Firebase Firestore
     */
    private fun cargarPacientesReales(container: LinearLayout, searchBar: EditText) {
        mostrarLoadingIndicator(container, true)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            mostrarLoadingIndicator(container, false)
            return
        }

        // Primero obtener el ID_MEDICO del usuario actual
        db.collection("medicos")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { medicoDoc ->
                val idMedico = medicoDoc.getLong("ID_MEDICO") ?: 0L

                if (idMedico == 0L) {
                    mostrarLoadingIndicator(container, false)
                    Toast.makeText(this, "No se encontró información del médico", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Buscar citas de este médico para obtener los pacientes
                db.collection("cita")
                    .whereEqualTo("ID_MEDICO", idMedico)
                    .get()
                    .addOnSuccessListener { citasDocuments ->
                        if (citasDocuments.isEmpty) {
                            mostrarLoadingIndicator(container, false)
                            mostrarEstadoVacio(container, "No hay pacientes")
                            return@addOnSuccessListener
                        }

                        val pacientesIds = mutableSetOf<Long>()
                        for (document in citasDocuments) {
                            val idPaciente = document.getLong("ID_PACIENTE") ?: 0L
                            if (idPaciente != 0L) {
                                pacientesIds.add(idPaciente)
                            }
                        }

                        if (pacientesIds.isEmpty()) {
                            mostrarLoadingIndicator(container, false)
                            mostrarEstadoVacio(container, "No hay pacientes")
                            return@addOnSuccessListener
                        }

                        // Buscar información de los pacientes
                        db.collection("pacientes")
                            .whereIn("ID_PACIENTE", pacientesIds.toList())
                            .get()
                            .addOnSuccessListener { pacientesDocuments ->
                                mostrarLoadingIndicator(container, false)

                                // ✅ VERIFICAR SI HAY DOCUMENTOS
                                if (pacientesDocuments.isEmpty) {
                                    mostrarEstadoVacio(container, "No se encontraron datos de pacientes")
                                    return@addOnSuccessListener
                                }

                                val pacientesList = mutableListOf<PacienteReal>()
                                val itemViews = mutableListOf<View>()

                                for (document in pacientesDocuments) {
                                    try {
                                        // 🔹 CORRECCIÓN: Usar funciones seguras para todos los campos
                                        val edadString = getSafeString(document, "EDAD")
                                        val edad = try {
                                            edadString.toInt()
                                        } catch (e: NumberFormatException) {
                                            0
                                        }

                                        val paciente = PacienteReal(
                                            ID_PACIENTE = getSafeLong(document, "ID_PACIENTE"),
                                            ID_USUARIO = getSafeLong(document, "ID_USUARIO"), // ← Esto es importante
                                            NOMBRE = getSafeString(document, "NOMBRE"),
                                            APELLIDO = getSafeString(document, "APELLIDO"),
                                            CORREO = getSafeString(document, "CORREO"),
                                            CELULAR = getSafeString(document, "CELULAR"), // 🔹 Usar función segura
                                            DNI = getSafeString(document, "DNI"),
                                            EDAD = edad,
                                            SEXO = getSafeString(document, "SEXO")
                                        )
                                        pacientesList.add(paciente)
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Error al procesar paciente: ${e.message}")
                                    }
                                }

                                // ✅ VERIFICAR SI HAY PACIENTES DESPUÉS DEL PROCESAMIENTO
                                if (pacientesList.isEmpty()) {
                                    mostrarEstadoVacio(container, "No se pudieron cargar los datos de pacientes")
                                    return@addOnSuccessListener
                                }

                                // Inflar y mostrar pacientes
                                for (paciente in pacientesList) {
                                    val itemView = LayoutInflater.from(this)
                                        .inflate(R.layout.item_patient, container, false)

                                    itemView.findViewById<TextView>(R.id.tvPatientName).text =
                                        "${paciente.NOMBRE} ${paciente.APELLIDO}"

                                    // Listener para mostrar detalles del paciente
                                    itemView.setOnClickListener {
                                        mostrarDetallesPaciente(paciente)
                                    }

                                    container.addView(itemView)
                                    itemViews.add(itemView)
                                }

                                // Configurar búsqueda
                                setupSearch(searchBar, itemViews, R.id.tvPatientName)
                            }
                            .addOnFailureListener { e ->
                                mostrarLoadingIndicator(container, false)
                                Log.e("MainActivity", "Error al cargar pacientes: ${e.message}")
                                mostrarEstadoVacio(container, "Error al cargar pacientes")
                                Toast.makeText(this, "Error al cargar pacientes", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        mostrarLoadingIndicator(container, false)
                        Log.e("MainActivity", "Error al buscar citas: ${e.message}")
                        mostrarEstadoVacio(container, "Error al buscar citas")
                        Toast.makeText(this, "Error al buscar citas", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                mostrarLoadingIndicator(container, false)
                Log.e("MainActivity", "Error al obtener datos del médico: ${e.message}")
                mostrarEstadoVacio(container, "Error del médico")
                Toast.makeText(this, "Error al obtener datos del médico", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Carga las citas reales para el calendario - VERSIÓN CORREGIDA
     */
    private fun cargarCitasReales(containerSchedule: LinearLayout, estadoFiltro: String = "Todos") {
        val currentUser = auth.currentUser
        if (currentUser == null) return

        // Limpiar solo las citas existentes
        limpiarCitasExistentes(containerSchedule)

        // Solo generar estructura si no existe
        if (!existeEstructuraCronograma(containerSchedule)) {
            generarEstructuraCronograma(containerSchedule)
        }

        // SEGUNDO: Cargar las citas reales desde Firebase
        db.collection("medicos")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { medicoDoc ->
                if (!medicoDoc.exists()) {
                    Log.e("MainActivity", "No se encontró documento del médico")
                    return@addOnSuccessListener
                }

                val idMedico = medicoDoc.getLong("ID_MEDICO") ?: 0L
                Log.d("MainActivity", "ID_MEDICO encontrado: $idMedico, Filtro: $estadoFiltro")

                if (idMedico == 0L) {
                    Log.e("MainActivity", "ID_MEDICO es 0")
                    return@addOnSuccessListener
                }

                // Consulta SIMPLIFICADA - solo por ID_MEDICO
                db.collection("cita")
                    .whereEqualTo("ID_MEDICO", idMedico)
                    .get()
                    .addOnSuccessListener { documents ->
                        Log.d("MainActivity", "Citas encontradas (todas): ${documents.size()}")

                        val citasList = mutableListOf<CitaReal>()
                        for (document in documents) {
                            val estado = document.getString("ESTADO") ?: ""
                            val idCita = document.getLong("ID_CITA") ?: 0L

                            // 🔹 CORRECCIÓN: AGREGAR FILTRO PARA "COMPLETADA"
                            val cumpleFiltro = when (estadoFiltro) {
                                "Todos" -> true
                                "Confirmada/Reservada" -> estado.equals("confirmada", ignoreCase = true) ||
                                        estado.equals("reservada", ignoreCase = true)
                                "Completada" -> estado.equals("completada", ignoreCase = true)
                                else -> estado.equals(estadoFiltro, ignoreCase = true)
                            }

                            if (cumpleFiltro) {
                                Log.d("MainActivity", "Cita válida: ID=$idCita, Estado=$estado, Hora=${document.getString("HORA")}")

                                val cita = CitaReal(
                                    ID_CITA = idCita,
                                    ID_PACIENTE = document.getLong("ID_PACIENTE") ?: 0L,
                                    ID_MEDICO = document.getLong("ID_MEDICO") ?: 0L,
                                    FECHA = document.getString("FECHA") ?: "",
                                    HORA = document.getString("HORA") ?: "",
                                    ESTADO = estado
                                )
                                citasList.add(cita)
                            } else {
                                Log.d("MainActivity", "Cita descartada por filtro: ID=$idCita, Estado=$estado")
                            }
                        }

                        Log.d("MainActivity", "Citas procesadas (filtradas): ${citasList.size}")

                        // TERCERO: Mostrar las citas en el cronograma
                        mostrarCitasEnCronograma(containerSchedule, citasList)
                    }
                    .addOnFailureListener { e ->
                        Log.e("MainActivity", "Error al cargar citas: ${e.message}")
                        Toast.makeText(this, "Error al cargar citas", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error al obtener médico: ${e.message}")
                Toast.makeText(this, "Error al obtener datos del médico", Toast.LENGTH_SHORT).show()
            }
    }

    // 🔹 DEBUG: Verificar TODAS las citas en la base de datos
    private fun debugTodasLasCitas() {
        db.collection("cita")
            .get()
            .addOnSuccessListener { documents ->
                Log.d("MainActivity", "=== DEBUG TODAS LAS CITAS ===")
                for (document in documents) {
                    val idMedico = document.getLong("ID_MEDICO") ?: 0L
                    val estado = document.getString("ESTADO") ?: ""
                    val hora = document.getString("HORA") ?: ""
                    val idCita = document.getLong("ID_CITA") ?: 0L

                    Log.d("MainActivity", "Cita ID: $idCita, Médico: $idMedico, Estado: $estado, Hora: $hora")
                }
                Log.d("MainActivity", "=== FIN DEBUG ===")
            }
    }
    // 🔹 REEMPLAZAR la función mostrarEstadoVacio con una versión específica para calendario
    private fun mostrarEstadoVacioCalendario(containerSchedule: LinearLayout, mensaje: String) {
        // NO eliminar todas las vistas, solo buscar y actualizar un mensaje de estado vacío

        // Buscar si ya existe un TextView de estado vacío
        var emptyView: TextView? = null
        for (i in 0 until containerSchedule.childCount) {
            val child = containerSchedule.getChildAt(i)
            if (child is TextView && child.id == View.generateViewId()) {
                emptyView = child
                break
            }
        }

        if (emptyView == null) {
            // Crear una vista simple sin layout
            emptyView = TextView(this).apply {
                id = View.generateViewId()
                text = mensaje
                setTextColor(Color.parseColor("#9E9E9E"))
                textSize = 16f
                gravity = android.view.Gravity.CENTER
                setPadding(0, 32, 0, 32)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            // Agregar después del título "Cronograma"
            containerSchedule.addView(emptyView, 2) // Posición 2: después del CalendarView y título
        } else {
            emptyView.text = mensaje
        }

        // Asegurarse de que esté visible
        emptyView.visibility = View.VISIBLE
    }

    // 🔹 ACTUALIZAR mostrarCitasEnCronograma para usar la nueva función
    private fun mostrarCitasEnCronograma(containerSchedule: LinearLayout, citasList: List<CitaReal>) {
        Log.d("MainActivity", "MostrarCitasEnCronograma - Total citas: ${citasList.size}")

        // PRIMERO: Limpiar solo las citas existentes (no todo el cronograma)
        limpiarCitasExistentes(containerSchedule)

        if (citasList.isEmpty()) {
            Log.d("MainActivity", "No hay citas para mostrar")
            // Mensaje específico según el filtro
            val mensaje = when (estadoFiltroActual) {
                "Confirmada/Reservada" -> "No hay citas confirmadas o reservadas"
                else -> "No hay citas $estadoFiltroActual"
            }
            mostrarEstadoVacioCalendario(containerSchedule, mensaje)
            return
        } else {
            // OCULTAR mensaje de estado vacío si existe
            ocultarEstadoVacioCalendario(containerSchedule)
        }

        // Para cada cita, buscar su hora correspondiente y agregarla
        for (cita in citasList) {
            Log.d("MainActivity", "Procesando cita: Hora=${cita.HORA}, PacienteID=${cita.ID_PACIENTE}, Estado=${cita.ESTADO}")

            // Convertir a hora completa "15:30:00" -> "15:00"
            val horaCita = convertirAHoraCompleta(cita.HORA)
            Log.d("MainActivity", "Hora convertida a completa: $horaCita")

            var horaEncontrada = false

            // Buscar el contenedor de la hora correspondiente en el cronograma
            for (i in 0 until containerSchedule.childCount) {
                val child = containerSchedule.getChildAt(i)
                val tvHourLabel = child.findViewById<TextView>(R.id.tvHourLabel)

                if (tvHourLabel != null) {
                    val horaViewLabel = tvHourLabel.text.toString() // "15:00"
                    Log.d("MainActivity", "Comparando con hora del cronograma: $horaViewLabel")

                    // Comparar horas exactas
                    if (horaCita == horaViewLabel) {
                        Log.d("MainActivity", "¡Hora encontrada! Agregando cita...")
                        horaEncontrada = true
                        val containerAppointments = child.findViewById<LinearLayout>(R.id.containerAppointments)

                        // Buscar información del paciente para esta cita
                        db.collection("pacientes")
                            .whereEqualTo("ID_PACIENTE", cita.ID_PACIENTE)
                            .get()
                            .addOnSuccessListener { pacientes ->
                                if (!pacientes.isEmpty) {
                                    val paciente = pacientes.documents.first()
                                    val nombrePaciente = "${paciente.getString("NOMBRE") ?: ""} ${paciente.getString("APELLIDO") ?: ""}"
                                    Log.d("MainActivity", "Paciente encontrado: $nombrePaciente")

                                    val itemView = LayoutInflater.from(this)
                                        .inflate(R.layout.item_calendar_entry, containerAppointments, false)

                                    val cardAppointment = itemView.findViewById<CardView>(R.id.cardAppointment)
                                    val tvPatientName = itemView.findViewById<TextView>(R.id.tvPatientName)
                                    val tvAvatarInitials = itemView.findViewById<TextView>(R.id.tvAvatarInitials)

                                    tvPatientName.text = nombrePaciente
                                    val initials = nombrePaciente.split(" ")
                                        .mapNotNull { it.firstOrNull() }
                                        .take(2)
                                        .joinToString("")
                                    tvAvatarInitials.text = initials

                                    // 🔹 CORRECCIÓN: AGREGAR COLOR PARA "COMPLETADA"
                                    val cardColor = when (cita.ESTADO.toLowerCase()) {
                                        "confirmada", "reservada" -> Color.parseColor("#4CAF50") // Verde
                                        "pendiente" -> Color.parseColor("#FF9800")  // Naranja
                                        "completada" -> Color.parseColor("#4AB9FF") // Verde azulado (teal)
                                        "cancelada" -> Color.parseColor("#F44336")  // Rojo
                                        else -> Color.parseColor("#9E9E9E")         // Gris por defecto
                                    }

                                    (cardAppointment.getChildAt(0) as LinearLayout).setBackgroundColor(cardColor)

                                    // Listener para mostrar ventana emergente (dialog)
                                    cardAppointment.setOnClickListener {
                                        val builder = AlertDialog.Builder(this)
                                        builder.setTitle("Detalles de la cita")
                                        builder.setMessage(
                                            "👤 Paciente: $nombrePaciente\n" +
                                                    "🕓 Hora: ${cita.HORA}\n" +
                                                    "📅 Fecha: ${formatearFecha(cita.FECHA)}\n" +
                                                    "📋 Estado: ${cita.ESTADO}"
                                        )
                                        builder.setPositiveButton("Cerrar", null)
                                        builder.show()
                                    }

                                    containerAppointments.addView(itemView)
                                    Log.d("MainActivity", "Cita agregada exitosamente")
                                } else {
                                    Log.e("MainActivity", "No se encontró paciente con ID: ${cita.ID_PACIENTE}")
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("MainActivity", "Error al buscar paciente: ${e.message}")
                            }

                        break // Salir del bucle una vez encontrada la hora
                    }
                }
            }

            if (!horaEncontrada) {
                Log.e("MainActivity", "No se encontró la hora $horaCita en el cronograma")
            }
        }
    }

    // 🔹 NUEVA FUNCIÓN: Limpiar solo las citas existentes (mantener estructura del cronograma)
    private fun limpiarCitasExistentes(containerSchedule: LinearLayout) {
        for (i in 0 until containerSchedule.childCount) {
            val child = containerSchedule.getChildAt(i)
            val containerAppointments = child.findViewById<LinearLayout>(R.id.containerAppointments)
            if (containerAppointments != null) {
                containerAppointments.removeAllViews()
            }
        }
        Log.d("MainActivity", "Citas existentes limpiadas")
    }

    // 🔹 NUEVA FUNCIÓN: Ocultar mensaje de estado vacío
    private fun ocultarEstadoVacioCalendario(containerSchedule: LinearLayout) {
        for (i in 0 until containerSchedule.childCount) {
            val child = containerSchedule.getChildAt(i)
            if (child is TextView && child.id != R.id.tvTitleSchedule) {
                child.visibility = View.GONE
            }
        }
    }

    // 🔹 NUEVA FUNCIÓN: Verificar si ya existe la estructura del cronograma
    private fun existeEstructuraCronograma(containerSchedule: LinearLayout): Boolean {
        for (i in 0 until containerSchedule.childCount) {
            val child = containerSchedule.getChildAt(i)
            if (child.findViewById<TextView>(R.id.tvHourLabel) != null) {
                return true
            }
        }
        return false
    }

    /**
     * 🔹 FUNCIÓN MEJORADA: Convierte diferentes formatos de hora
     */
    private fun convertirFormatoHora(horaOriginal: String): String {
        return try {
            when {
                horaOriginal.contains(":") -> {
                    val partes = horaOriginal.split(":")
                    when (partes.size) {
                        3 -> { // Formato "HH:MM:SS"
                            val hora = partes[0].padStart(2, '0')
                            val minuto = partes[1].padStart(2, '0')
                            "$hora:$minuto"
                        }
                        2 -> { // Formato "HH:MM"
                            val hora = partes[0].padStart(2, '0')
                            val minuto = partes[1].padStart(2, '0')
                            "$hora:$minuto"
                        }
                        else -> horaOriginal
                    }
                }
                horaOriginal.length == 4 -> { // Formato "HHMM"
                    "${horaOriginal.substring(0, 2)}:${horaOriginal.substring(2, 4)}"
                }
                else -> horaOriginal
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al convertir hora: $horaOriginal")
            horaOriginal
        }
    }

    /**
     * 🔹 FUNCIÓN: Formatea la fecha para mejor presentación
     */
    private fun formatearFecha(fecha: String): String {
        return try {
            // Convierte "2025-11-20" a "20/11/2025" o similar
            if (fecha.length == 10 && fecha.contains("-")) {
                val partes = fecha.split("-")
                if (partes.size == 3) {
                    "${partes[2]}/${partes[1]}/${partes[0]}"
                } else {
                    fecha
                }
            } else {
                fecha
            }
        } catch (e: Exception) {
            fecha
        }
    }

    /**
     * Elimina solo las horas del cronograma, manteniendo CalendarView y títulos
     */
    private fun eliminarSoloHorasDelCronograma(containerSchedule: LinearLayout) {
        // Encontrar y eliminar solo las vistas de horas (item_calendar_hour)
        val viewsToRemove = mutableListOf<View>()
        for (i in 0 until containerSchedule.childCount) {
            val child = containerSchedule.getChildAt(i)
            // Buscar vistas que tengan tvHourLabel (son las horas del cronograma)
            if (child.findViewById<TextView>(R.id.tvHourLabel) != null) {
                viewsToRemove.add(child)
            }
        }

        for (view in viewsToRemove) {
            containerSchedule.removeView(view)
        }
        Log.d("MainActivity", "Horas eliminadas: ${viewsToRemove.size}")
    }

    /**
     * Genera la estructura base del cronograma con horas de 8:00 a 22:00 (solo horas completas)
     */
    private fun generarEstructuraCronograma(containerSchedule: LinearLayout) {
        // Generar horas desde 8 a 22 (solo horas en punto)
        for (hour in 8..22) {
            val hourLabel = String.format("%02d:00", hour)
            val hourView = LayoutInflater.from(this)
                .inflate(R.layout.item_calendar_hour, containerSchedule, false)

            val tvHourLabel = hourView.findViewById<TextView>(R.id.tvHourLabel)
            tvHourLabel.text = hourLabel

            containerSchedule.addView(hourView)
        }
        Log.d("MainActivity", "Estructura del cronograma generada: 8:00 - 22:00 (horas completas)")
    }

    /**
     * 🔹 FUNCIÓN MEJORADA: Convierte cualquier hora a formato de hora completa
     * Ej: "15:30:00" -> "15:00", "08:45:00" -> "08:00"
     */
    private fun convertirAHoraCompleta(horaOriginal: String): String {
        return try {
            if (horaOriginal.contains(":")) {
                val partes = horaOriginal.split(":")
                if (partes.size >= 1) {
                    val hora = partes[0].padStart(2, '0')
                    "$hora:00" // Siempre devuelve hora completa
                } else {
                    horaOriginal
                }
            } else {
                horaOriginal
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al convertir hora: $horaOriginal")
            horaOriginal
        }
    }

    /**
     * Muestra un indicador de carga
     */
    private fun mostrarLoadingIndicator(container: LinearLayout, mostrar: Boolean) {
        if (mostrar) {
            container.removeAllViews()
            val loadingView = TextView(this).apply {
                text = "Cargando notificaciones..."
                setTextColor(Color.parseColor("#9E9E9E"))
                textSize = 16f
                gravity = android.view.Gravity.CENTER
                setPadding(0, 32, 0, 32)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            container.addView(loadingView)
        }
    }

    /**
     * Muestra estado vacío
     */
    private fun mostrarEstadoVacio(container: LinearLayout, mensaje: String) {
        container.removeAllViews()
        val emptyView = TextView(this).apply {
            text = mensaje
            setTextColor(Color.parseColor("#9E9E9E"))
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 32, 0, 32)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(emptyView)
    }

    /**
     * Configura el nombre del paciente en una notificación
     */
    private fun configurarNombrePacienteEnNotificacion(itemView: View, idPaciente: Long) {
        if (idPaciente == 0L) {
            itemView.findViewById<TextView>(R.id.tvNotificationName).text = "Paciente"
            return
        }

        db.collection("pacientes")
            .whereEqualTo("ID_PACIENTE", idPaciente)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val paciente = documents.documents.first()
                    val nombre = paciente.getString("NOMBRE") ?: ""
                    val apellido = paciente.getString("APELLIDO") ?: ""
                    itemView.findViewById<TextView>(R.id.tvNotificationName).text = "$nombre $apellido"
                } else {
                    itemView.findViewById<TextView>(R.id.tvNotificationName).text = "Paciente no encontrado"
                }
            }
            .addOnFailureListener {
                itemView.findViewById<TextView>(R.id.tvNotificationName).text = "Paciente"
            }
    }

    /**
     * Muestra detalles de una notificación
     */
    private fun mostrarDetallesNotificacion(notificacion: NotificacionReal) {
        // Buscar información del paciente para mostrar detalles completos
        if (notificacion.ID_PACIENTE != 0L) {
            db.collection("pacientes")
                .whereEqualTo("ID_PACIENTE", notificacion.ID_PACIENTE)
                .get()
                .addOnSuccessListener { pacientes ->
                    val nombrePaciente = if (!pacientes.isEmpty) {
                        val paciente = pacientes.documents.first()
                        "${paciente.getString("NOMBRE") ?: ""} ${paciente.getString("APELLIDO") ?: ""}"
                    } else {
                        "Paciente no encontrado"
                    }

                    val fechaFormateada = try {
                        val date = notificacion.FECHA_ENVIO.toDate()
                        SimpleDateFormat("dd/MM/yyyy 'a las' HH:mm", Locale.getDefault()).format(date)
                    } catch (e: Exception) {
                        "Fecha no disponible"
                    }

                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Detalles de la Notificación")
                    builder.setMessage(
                        "👤 Paciente: $nombrePaciente\n" +
                                "📋 Tipo: ${notificacion.TITULO}\n" +
                                "💬 Mensaje: ${notificacion.MENSAJE}\n" +
                                "📅 Fecha: $fechaFormateada"
                    )
                    builder.setPositiveButton("Cerrar", null)
                    builder.show()
                }
                .addOnFailureListener {
                    // Si falla la búsqueda del paciente, mostrar solo la notificación básica
                    AlertDialog.Builder(this)
                        .setTitle("Detalles de la Notificación")
                        .setMessage(
                            "📋 Tipo: ${notificacion.TITULO}\n" +
                                    "💬 Mensaje: ${notificacion.MENSAJE}"
                        )
                        .setPositiveButton("Cerrar", null)
                        .show()
                }
        } else {
            AlertDialog.Builder(this)
                .setTitle("Detalles de la Notificación")
                .setMessage(
                    "📋 Tipo: ${notificacion.TITULO}\n" +
                            "💬 Mensaje: ${notificacion.MENSAJE}"
                )
                .setPositiveButton("Cerrar", null)
                .show()
        }
    }

    /**
     * Muestra detalles de un paciente
     */
    private fun mostrarDetallesPaciente(paciente: PacienteReal) {
        val mensaje = """
        Nombre: ${paciente.NOMBRE} ${paciente.APELLIDO}
        DNI: ${if (paciente.DNI.isNotEmpty()) paciente.DNI else "No especificado"}
        Edad: ${if (paciente.EDAD > 0) "${paciente.EDAD} años" else "No especificada"}
        Sexo: ${if (paciente.SEXO.isNotEmpty()) paciente.SEXO else "No especificado"}
        Celular: ${if (paciente.CELULAR.isNotEmpty()) paciente.CELULAR else "No especificado"}
        Correo: ${if (paciente.CORREO.isNotEmpty()) paciente.CORREO else "No especificado"}
    """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Información del Paciente")
            .setMessage(mensaje)
            .setPositiveButton("Cerrar", null)
            .show()
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

    /**
     * Función auxiliar para obtener valores de Firestore de forma segura
     */
    private fun getSafeString(document: com.google.firebase.firestore.DocumentSnapshot, field: String): String {
        return try {
            when (val value = document.get(field)) {
                is String -> value
                is Long -> value.toString()
                is Double -> value.toLong().toString()
                is Int -> value.toString()
                is Float -> value.toInt().toString()
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun getSafeLong(document: com.google.firebase.firestore.DocumentSnapshot, field: String): Long {
        return try {
            when (val value = document.get(field)) {
                is Long -> value
                is Double -> value.toLong()
                is Int -> value.toLong()
                is String -> value.toLongOrNull() ?: 0L
                else -> 0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Carga solo las notificaciones del día actual
     */
    private fun cargarNotificacionesDelDia(container: LinearLayout, searchBar: EditText) {
        mostrarLoadingIndicator(container, true)
        mostrandoHistorial = false
        fechaFiltroHistorial = null

        val currentUser = auth.currentUser
        if (currentUser == null) {
            mostrarLoadingIndicator(container, false)
            return
        }

        val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        db.collection("medicos")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { medicoDoc ->
                val idMedico = medicoDoc.getLong("ID_MEDICO") ?: 0L

                if (idMedico == 0L) {
                    mostrarLoadingIndicator(container, false)
                    return@addOnSuccessListener
                }

                // Buscar notificaciones de HOY
                val startOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                val endOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.time

                db.collection("notificaciones")
                    .whereEqualTo("ID_MEDICO", idMedico)
                    .whereGreaterThanOrEqualTo("FECHA_ENVIO", com.google.firebase.Timestamp(startOfDay))
                    .whereLessThanOrEqualTo("FECHA_ENVIO", com.google.firebase.Timestamp(endOfDay))
                    .orderBy("FECHA_ENVIO", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { documents ->
                        mostrarLoadingIndicator(container, false)

                        if (documents.isEmpty) {
                            mostrarEstadoVacio(container, "No hay notificaciones para hoy")
                            return@addOnSuccessListener
                        }

                        mostrarNotificacionesEnContenedor(documents, container, searchBar, "Hoy: $fechaHoy")
                    }
                    .addOnFailureListener { e ->
                        mostrarLoadingIndicator(container, false)
                        Toast.makeText(this, "Error al cargar notificaciones: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    /**
     * Muestra el diálogo de historial de notificaciones
     */
    private fun mostrarDialogoHistorialNotificaciones(container: LinearLayout, searchBar: EditText) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_notifications_history, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)

        val dialog = builder.create()
        dialog.show()

        // Referencias a las vistas del diálogo
        val etFilterDate = dialogView.findViewById<EditText>(R.id.etFilterDate)
        val btnToday = dialogView.findViewById<Button>(R.id.btnToday)
        val btnYesterday = dialogView.findViewById<Button>(R.id.btnYesterday)
        val btnClearFilter = dialogView.findViewById<Button>(R.id.btnClearFilter)
        val btnCloseHistory = dialogView.findViewById<Button>(R.id.btnCloseHistory)
        val containerHistory = dialogView.findViewById<LinearLayout>(R.id.containerHistory)
        val tvHistoryTitle = dialogView.findViewById<TextView>(R.id.tvHistoryTitle)

        // Configurar selector de fecha
        etFilterDate.setOnClickListener {
            mostrarSelectorFecha(etFilterDate)
        }

        // Botón "Hoy"
        btnToday.setOnClickListener {
            val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            etFilterDate.setText(fechaHoy)
            cargarHistorialNotificaciones(fechaHoy, containerHistory, tvHistoryTitle)
        }

        // Botón "Ayer"
        btnYesterday.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val fechaAyer = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)
            etFilterDate.setText(fechaAyer)
            cargarHistorialNotificaciones(fechaAyer, containerHistory, tvHistoryTitle)
        }

        // Botón "Limpiar Filtro"
        btnClearFilter.setOnClickListener {
            etFilterDate.setText("")
            containerHistory.removeAllViews()
            tvHistoryTitle.text = "Seleccione una fecha para ver el historial"
        }

        // Botón "Cerrar"
        btnCloseHistory.setOnClickListener {
            dialog.dismiss()
            // Recargar notificaciones del día actual al cerrar
            cargarNotificacionesDelDia(container, searchBar)
        }

        // Cargar historial si ya hay una fecha filtrada
        fechaFiltroHistorial?.let { fecha ->
            etFilterDate.setText(fecha)
            cargarHistorialNotificaciones(fecha, containerHistory, tvHistoryTitle)
        }
    }

    /**
     * Muestra el selector de fecha
     */
    private fun mostrarSelectorFecha(etFilterDate: EditText) {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, day ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, day)
                }
                val fechaFormateada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate.time)
                etFilterDate.setText(fechaFormateada)

                // Cargar historial automáticamente al seleccionar fecha
                val containerHistory = (etFilterDate.parent?.parent as? View)?.findViewById<LinearLayout>(R.id.containerHistory)
                val tvHistoryTitle = (etFilterDate.parent?.parent as? View)?.findViewById<TextView>(R.id.tvHistoryTitle)
                if (containerHistory != null && tvHistoryTitle != null) {
                    cargarHistorialNotificaciones(fechaFormateada, containerHistory, tvHistoryTitle)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    /**
     * Carga el historial de notificaciones para una fecha específica
     */
    private fun cargarHistorialNotificaciones(fecha: String, container: LinearLayout, tvTitle: TextView) {
        container.removeAllViews()
        mostrarLoadingIndicatorHistorial(container, true)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            mostrarLoadingIndicatorHistorial(container, false)
            return
        }

        // Convertir fecha a formato para consulta
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = dateFormat.parse(fecha)

        val calendar = Calendar.getInstance().apply {
            time = date!!
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.time

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.time

        db.collection("medicos")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { medicoDoc ->
                val idMedico = medicoDoc.getLong("ID_MEDICO") ?: 0L

                if (idMedico == 0L) {
                    mostrarLoadingIndicatorHistorial(container, false)
                    return@addOnSuccessListener
                }

                db.collection("notificaciones")
                    .whereEqualTo("ID_MEDICO", idMedico)
                    .whereGreaterThanOrEqualTo("FECHA_ENVIO", com.google.firebase.Timestamp(startOfDay))
                    .whereLessThanOrEqualTo("FECHA_ENVIO", com.google.firebase.Timestamp(endOfDay))
                    .orderBy("FECHA_ENVIO", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { documents ->
                        mostrarLoadingIndicatorHistorial(container, false)

                        tvTitle.text = "Notificaciones del $fecha (${documents.size()} encontradas)"

                        if (documents.isEmpty) {
                            mostrarEstadoVacioHistorial(container, "No hay notificaciones para esta fecha")
                            return@addOnSuccessListener
                        }

                        // Guardar fecha filtrada para persistencia
                        fechaFiltroHistorial = fecha

                        // Mostrar notificaciones en el historial
                        for (document in documents) {
                            val notificacion = document.toObject(NotificacionReal::class.java)
                            val itemView = LayoutInflater.from(this)
                                .inflate(R.layout.item_notification, container, false)

                            configurarNombrePacienteEnNotificacion(itemView, notificacion.ID_PACIENTE)
                            itemView.findViewById<TextView>(R.id.tvNotificationType).text = notificacion.TITULO

                            // Mostrar hora exacta en el historial
                            val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(notificacion.FECHA_ENVIO.toDate())
                            itemView.findViewById<TextView>(R.id.tvNotificationType).text = "${notificacion.TITULO} - $hora"

                            itemView.setOnClickListener {
                                mostrarDetallesNotificacion(notificacion)
                            }

                            container.addView(itemView)
                        }
                    }
                    .addOnFailureListener { e ->
                        mostrarLoadingIndicatorHistorial(container, false)
                        Toast.makeText(this, "Error al cargar historial: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    /**
     * Funciones auxiliares para el historial
     */
    private fun mostrarLoadingIndicatorHistorial(container: LinearLayout, mostrar: Boolean) {
        if (mostrar) {
            container.removeAllViews()
            val loadingView = TextView(this).apply {
                text = "Cargando historial..."
                setTextColor(Color.parseColor("#9E9E9E"))
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                setPadding(0, 16, 0, 16)
            }
            container.addView(loadingView)
        }
    }

    private fun mostrarEstadoVacioHistorial(container: LinearLayout, mensaje: String) {
        container.removeAllViews()
        val emptyView = TextView(this).apply {
            text = mensaje
            setTextColor(Color.parseColor("#9E9E9E"))
            textSize = 14f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 16, 0, 16)
        }
        container.addView(emptyView)
    }


    /**
     * Función genérica para mostrar notificaciones en cualquier contenedor
     */
    private fun mostrarNotificacionesEnContenedor(
        documents: QuerySnapshot,
        container: LinearLayout,
        searchBar: EditText?,
        titulo: String = "Notificaciones"
    ) {
        container.removeAllViews()

        val notificacionesList = mutableListOf<NotificacionReal>()
        val itemViews = mutableListOf<View>()

        for (document in documents) {
            val notificacion = NotificacionReal(
                ID_NOTIFICACIONES = document.getLong("ID_NOTIFICACIONES") ?: 0L,
                TITULO = document.getString("TITULO") ?: "",
                MENSAJE = document.getString("MENSAJE") ?: "",
                FECHA_ENVIO = document.getTimestamp("FECHA_ENVIO") ?: com.google.firebase.Timestamp.now(),
                ID_PACIENTE = document.getLong("ID_PACIENTE") ?: 0L,
                ID_MEDICO = document.getLong("ID_MEDICO") ?: 0L
            )
            notificacionesList.add(notificacion)
        }

        // Mostrar título si es necesario
        if (titulo.isNotEmpty()) {
            val titleView = TextView(this).apply {
                text = titulo
                setTextColor(Color.parseColor("#000000"))
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setPadding(0, 0, 0, 16)
            }
            container.addView(titleView)
        }

        // Inflar y mostrar notificaciones
        for (notificacion in notificacionesList) {
            val itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_notification, container, false)

            configurarNombrePacienteEnNotificacion(itemView, notificacion.ID_PACIENTE)
            itemView.findViewById<TextView>(R.id.tvNotificationType).text = notificacion.TITULO

            itemView.setOnClickListener {
                mostrarDetallesNotificacion(notificacion)
            }

            container.addView(itemView)
            itemViews.add(itemView)
        }

        // Configurar búsqueda si se proporcionó searchBar
        searchBar?.let {
            setupSearch(it, itemViews, R.id.tvNotificationName)
        }
    }

    /**
     * Carga solo los pacientes con citas activas (no completadas) - VERSIÓN SIN ÍNDICE
     */
    private fun cargarPacientesActivos(container: LinearLayout, searchBar: EditText) {
        mostrarLoadingIndicator(container, true)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            mostrarLoadingIndicator(container, false)
            return
        }

        // Primero obtener el ID_MEDICO del usuario actual
        db.collection("medicos")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { medicoDoc ->
                val idMedico = medicoDoc.getLong("ID_MEDICO") ?: 0L

                if (idMedico == 0L) {
                    mostrarLoadingIndicator(container, false)
                    return@addOnSuccessListener
                }

                // 🔹 CONSULTA SIMPLIFICADA: Solo por ID_MEDICO (sin filtro por estado)
                db.collection("cita")
                    .whereEqualTo("ID_MEDICO", idMedico)
                    .get()
                    .addOnSuccessListener { citasDocuments ->
                        mostrarLoadingIndicator(container, false)

                        if (citasDocuments.isEmpty) {
                            mostrarEstadoVacio(container, "No hay pacientes activos")
                            return@addOnSuccessListener
                        }

                        val pacientesIds = mutableSetOf<Long>()
                        val citasActivas = mutableListOf<com.google.firebase.firestore.QueryDocumentSnapshot>()

                        // 🔹 FILTRAR LOCALMENTE las citas no completadas
                        for (document in citasDocuments) {
                            val idPaciente = document.getLong("ID_PACIENTE") ?: 0L
                            val estado = document.getString("ESTADO") ?: ""

                            if (idPaciente != 0L && estado != "completada") {
                                pacientesIds.add(idPaciente)
                                citasActivas.add(document)
                            }
                        }

                        if (pacientesIds.isEmpty()) {
                            mostrarEstadoVacio(container, "No hay pacientes activos")
                            return@addOnSuccessListener
                        }

                        // Buscar información de los pacientes activos
                        db.collection("pacientes")
                            .whereIn("ID_PACIENTE", pacientesIds.toList())
                            .get()
                            .addOnSuccessListener { pacientesDocuments ->
                                mostrarLoadingIndicator(container, false)

                                if (pacientesDocuments.isEmpty) {
                                    mostrarEstadoVacio(container, "No se encontraron datos de pacientes")
                                    return@addOnSuccessListener
                                }

                                val pacientesList = mutableListOf<PacienteReal>()
                                val itemViews = mutableListOf<View>()

                                for (document in pacientesDocuments) {
                                    try {
                                        val paciente = PacienteReal(
                                            ID_PACIENTE = getSafeLong(document, "ID_PACIENTE"),
                                            ID_USUARIO = getSafeLong(document, "ID_USUARIO"), // ← Esto es importante
                                            NOMBRE = getSafeString(document, "NOMBRE"),
                                            APELLIDO = getSafeString(document, "APELLIDO"),
                                            CORREO = getSafeString(document, "CORREO"),
                                            CELULAR = getSafeString(document, "CELULAR"),
                                            DNI = getSafeString(document, "DNI"),
                                            EDAD = 0,
                                            SEXO = getSafeString(document, "SEXO")
                                        )
                                        pacientesList.add(paciente)
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Error al procesar paciente: ${e.message}")
                                    }
                                }

                                // Inflar y mostrar pacientes con botones funcionales
                                for (paciente in pacientesList) {
                                    val itemView = LayoutInflater.from(this)
                                        .inflate(R.layout.item_patient, container, false)

                                    itemView.findViewById<TextView>(R.id.tvPatientName).text =
                                        "${paciente.NOMBRE} ${paciente.APELLIDO}"

                                    // 🔹 CONFIGURAR BOTÓN CHECK PARA MARCAR COMO COMPLETADO
                                    val btnCheckComplete = itemView.findViewById<ImageButton>(R.id.btnCheckComplete)
                                    btnCheckComplete.setOnClickListener {
                                        mostrarDialogoCompletarCita(paciente)
                                    }

                                    // Configurar otros botones (opcional)
                                    configurarBotonesPaciente(itemView, paciente)

                                    container.addView(itemView)
                                    itemViews.add(itemView)
                                }

                                // Configurar búsqueda
                                setupSearch(searchBar, itemViews, R.id.tvPatientName)
                            }
                            .addOnFailureListener { e ->
                                mostrarLoadingIndicator(container, false)
                                Log.e("MainActivity", "Error al cargar pacientes: ${e.message}")
                                mostrarEstadoVacio(container, "Error al cargar pacientes")
                            }
                    }
                    .addOnFailureListener { e ->
                        mostrarLoadingIndicator(container, false)
                        Log.e("MainActivity", "Error al buscar citas: ${e.message}")
                        mostrarEstadoVacio(container, "Error al buscar citas")
                    }
            }
            .addOnFailureListener { e ->
                mostrarLoadingIndicator(container, false)
                Log.e("MainActivity", "Error al obtener datos del médico: ${e.message}")
                mostrarEstadoVacio(container, "Error del médico")
            }
    }

    /**
     * Muestra diálogo para confirmar completar la cita de un paciente
     */
    private fun mostrarDialogoCompletarCita(paciente: PacienteReal) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Completar Cita")
        builder.setMessage("¿Desea marcar como COMPLETADA la cita del paciente ${paciente.NOMBRE} ${paciente.APELLIDO}?")

        builder.setPositiveButton("Sí, Completar") { dialog, _ ->
            completarCitaPaciente(paciente)
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    /**
     * Completa la cita de un paciente y actualiza la base de datos
     */
    private fun completarCitaPaciente(paciente: PacienteReal) {
        val currentUser = auth.currentUser
        if (currentUser == null) return

        db.collection("medicos")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { medicoDoc ->
                val idMedico = medicoDoc.getLong("ID_MEDICO") ?: 0L
                if (idMedico == 0L) return@addOnSuccessListener

                // Buscar la cita activa del paciente
                db.collection("cita")
                    .whereEqualTo("ID_PACIENTE", paciente.ID_PACIENTE)
                    .whereEqualTo("ID_MEDICO", idMedico)
                    .whereNotEqualTo("ESTADO", "completada")
                    .get()
                    .addOnSuccessListener { citas ->
                        if (!citas.isEmpty) {
                            val citaDoc = citas.documents.first()
                            val citaId = citaDoc.id

                            // Actualizar estado a "completada"
                            db.collection("cita").document(citaId)
                                .update("ESTADO", "completada")
                                .addOnSuccessListener {
                                    Log.d("MainActivity", "✅ Cita marcada como completada")

                                    // Crear notificación de cita completada
                                    val cita = citaDoc.toObject(CitaReal::class.java)
                                    if (cita != null) {
                                        crearNotificacionCitaCompletada(cita, paciente)
                                    }

                                    // Mostrar mensaje de éxito
                                    Toast.makeText(this, "Cita completada exitosamente", Toast.LENGTH_SHORT).show()

                                    // Recargar la lista de pacientes
                                    val currentLayout = mainContentFrame.getChildAt(0)
                                    if (currentLayout != null) {
                                        val container = currentLayout.findViewById<LinearLayout>(R.id.containerPatients)
                                        val searchBar = currentLayout.findViewById<EditText>(R.id.etSearchPatients)
                                        if (container != null && searchBar != null) {
                                            cargarPacientesActivos(container, searchBar)
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("MainActivity", "❌ Error al completar cita: ${e.message}")
                                    Toast.makeText(this, "Error al completar cita", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
            }
    }

    /**
     * Crea notificación especial para cita completada
     */
    private fun crearNotificacionCitaCompletada(cita: CitaReal, paciente: PacienteReal) {
        // 🔹 VERIFICACIÓN ESTRICTA AL INICIO
        if (!notificationsEnabled) {
            Log.d("MainActivity", "🔕 NOTIFICACIONES DESACTIVADAS - No se procesará cita completada")
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("MainActivity", "Usuario no autenticado al crear notificación de cita completada")
            return
        }

        // Obtener ID_MEDICO del usuario actual
        db.collection("medicos")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { medicoDoc ->
                val idMedico = medicoDoc.getLong("ID_MEDICO") ?: 0L
                if (idMedico == 0L) {
                    Log.e("MainActivity", "ID_MEDICO es 0 al crear notificación de cita completada")
                    return@addOnSuccessListener
                }

                val fechaSolicitud = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

                // 🔹 CORRECCIÓN: Definir título y mensaje específicos para cita completada
                val titulo = "🏁 Cita Completada - $fechaSolicitud"
                val mensaje = "La cita con ${paciente.NOMBRE} ${paciente.APELLIDO} ha sido completada exitosamente el ${formatearFecha(cita.FECHA)}"

                val notificacionData = hashMapOf(
                    "ID_NOTIFICACIONES" to System.currentTimeMillis(),
                    "TITULO" to titulo,
                    "MENSAJE" to mensaje,
                    "FECHA_ENVIO" to com.google.firebase.Timestamp.now(),
                    "ID_PACIENTE" to cita.ID_PACIENTE,
                    "ID_MEDICO" to idMedico,
                    "FECHA_SOLICITUD" to fechaSolicitud
                )

                Log.d("MainActivity", "Creando notificación de cita completada en Firestore: $titulo")

                // Guardar en Firebase
                db.collection("notificaciones")
                    .add(notificacionData)
                    .addOnSuccessListener {
                        Log.d("MainActivity", "✅ Notificación de cita completada creada exitosamente en Firestore: $titulo")
                        // 🔹 VERIFICAR UNA VEZ MÁS ANTES DE ENVIAR NOTIFICACIÓN LOCAL
                        if (notificationsEnabled) {
                            enviarNotificacionLocal(cita, paciente, "cita_completada")
                        } else {
                            Log.d("MainActivity", "🔕 Notificaciones desactivadas - No se enviará notificación local de cita completada")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("MainActivity", "❌ Error al crear notificación de cita completada en Firestore: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error al obtener médico para notificación de cita completada: ${e.message}")
            }
    }

    /**
     * Muestra el historial de pacientes completados - VERSIÓN ACTUALIZADA CON FILTRO
     */
    private fun mostrarDialogoHistorialPacientesCompletados(container: LinearLayout, searchBar: EditText) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_patients_history, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)

        val dialog = builder.create()
        dialog.show()

        // Referencias a las vistas del diálogo
        val etFilterDate = dialogView.findViewById<EditText>(R.id.etFilterDate)
        val btnToday = dialogView.findViewById<Button>(R.id.btnToday)
        val btnYesterday = dialogView.findViewById<Button>(R.id.btnYesterday)
        val btnAll = dialogView.findViewById<Button>(R.id.btnAll)
        val btnClearFilter = dialogView.findViewById<Button>(R.id.btnClearFilter)
        val btnCloseHistory = dialogView.findViewById<Button>(R.id.btnCloseHistory)
        val containerHistory = dialogView.findViewById<LinearLayout>(R.id.containerHistory)
        val tvHistoryTitle = dialogView.findViewById<TextView>(R.id.tvHistoryTitle)

        // Cambiar título
        tvHistoryTitle.text = "Seleccione una fecha para filtrar"

        // Configurar selector de fecha
        etFilterDate.setOnClickListener {
            mostrarSelectorFechaPacientes(etFilterDate, containerHistory, tvHistoryTitle)
        }

        // Botón "Hoy"
        btnToday.setOnClickListener {
            val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            etFilterDate.setText(fechaHoy)
            cargarPacientesPorFecha(fechaHoy, containerHistory, tvHistoryTitle)
        }

        // Botón "Ayer"
        btnYesterday.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val fechaAyer = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)
            etFilterDate.setText(fechaAyer)
            cargarPacientesPorFecha(fechaAyer, containerHistory, tvHistoryTitle)
        }

        // Botón "Todos"
        btnAll.setOnClickListener {
            etFilterDate.setText("")
            cargarTodosLosPacientes(containerHistory, tvHistoryTitle)
        }

        // Botón "Limpiar Filtro"
        btnClearFilter.setOnClickListener {
            etFilterDate.setText("")
            containerHistory.removeAllViews()
            tvHistoryTitle.text = "Seleccione una fecha para filtrar"
        }

        // Botón "Cerrar"
        btnCloseHistory.setOnClickListener {
            dialog.dismiss()
        }

        // Cargar todos los pacientes por defecto
        cargarTodosLosPacientes(containerHistory, tvHistoryTitle)
    }

    /**
     * Selector de fecha para pacientes
     */
    private fun mostrarSelectorFechaPacientes(etFilterDate: EditText, containerHistory: LinearLayout, tvHistoryTitle: TextView) {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, day ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, day)
                }
                val fechaFormateada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate.time)
                etFilterDate.setText(fechaFormateada)
                cargarPacientesPorFecha(fechaFormateada, containerHistory, tvHistoryTitle)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    /**
     * Carga pacientes por fecha específica - VERSIÓN CORREGIDA (filtra por FECHA_FIN_CONSULTA)
     */
    private fun cargarPacientesPorFecha(fecha: String, container: LinearLayout, titleView: TextView) {
        container.removeAllViews()
        mostrarLoadingIndicatorHistorial(container, true)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            mostrarLoadingIndicatorHistorial(container, false)
            return
        }

        Log.d("MainActivity", "🔍 Buscando pacientes completados para fecha: $fecha")

        db.collection("medicos")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { medicoDoc ->
                val idMedico = medicoDoc.getLong("ID_MEDICO") ?: 0L
                if (idMedico == 0L) {
                    mostrarLoadingIndicatorHistorial(container, false)
                    return@addOnSuccessListener
                }

                Log.d("MainActivity", "👨‍⚕️ ID Médico: $idMedico")

                // 🔹 BUSCAR CITAS COMPLETADAS DEL MÉDICO
                db.collection("cita")
                    .whereEqualTo("ID_MEDICO", idMedico)
                    .whereEqualTo("ESTADO", "completada")
                    .get()
                    .addOnSuccessListener { citasCompletadas ->
                        mostrarLoadingIndicatorHistorial(container, false)

                        Log.d("MainActivity", "📄 Total de citas completadas: ${citasCompletadas.size()}")

                        val citasFiltradas = mutableListOf<com.google.firebase.firestore.QueryDocumentSnapshot>()

                        // 🔹 FILTRAR POR FECHA_FIN_CONSULTA
                        for (cita in citasCompletadas) {
                            val fechaFinConsulta = cita.getTimestamp("FECHA_FIN_CONSULTA")
                            val idPaciente = cita.getLong("ID_PACIENTE") ?: 0L
                            val idCita = cita.getLong("ID_CITA") ?: 0L

                            if (fechaFinConsulta != null && coincideFechaFinConsulta(fecha, fechaFinConsulta)) {
                                citasFiltradas.add(cita)
                                Log.d("MainActivity", "✅ Cita completada COINCIDE con filtro - ID: $idCita, Paciente: $idPaciente")
                            }
                        }

                        Log.d("MainActivity", "🎯 Citas completadas filtradas: ${citasFiltradas.size}")

                        if (citasFiltradas.isEmpty()) {
                            mostrarEstadoVacioHistorial(container, "No hay pacientes completados para la fecha seleccionada")
                            titleView.text = "Pacientes completados del $fecha - 0 resultados"
                            return@addOnSuccessListener
                        }

                        titleView.text = "Pacientes completados del $fecha - ${citasFiltradas.size} resultados"

                        // Obtener detalles de los pacientes
                        for (cita in citasFiltradas) {
                            val idPaciente = cita.getLong("ID_PACIENTE") ?: continue
                            obtenerDetallesPacienteParaHistorial(idPaciente, cita, container)
                        }
                    }
                    .addOnFailureListener { e ->
                        mostrarLoadingIndicatorHistorial(container, false)
                        Log.e("MainActivity", "❌ Error al cargar citas completadas: ${e.message}")
                        Toast.makeText(this, "Error al cargar pacientes completados", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                mostrarLoadingIndicatorHistorial(container, false)
                Log.e("MainActivity", "❌ Error al obtener datos del médico: ${e.message}")
                Toast.makeText(this, "Error al obtener datos del médico", Toast.LENGTH_SHORT).show()
            }
    }
    /**
     * 🔹 NUEVA FUNCIÓN: Compara si una fecha en formato DD/MM/YYYY coincide con FECHA_FIN_CONSULTA
     */
    private fun coincideFechaFinConsulta(fechaFiltro: String, fechaFinConsulta: com.google.firebase.Timestamp): Boolean {
        return try {
            // Formato del filtro: DD/MM/YYYY
            val dateFormatFiltro = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechaFiltroDate = dateFormatFiltro.parse(fechaFiltro)

            if (fechaFiltroDate == null) return false

            // Convertir Timestamp a Date
            val fechaFinDate = fechaFinConsulta.toDate()

            // Comparar solo día, mes y año (ignorar hora)
            val calendarFiltro = Calendar.getInstance().apply { time = fechaFiltroDate }
            val calendarFin = Calendar.getInstance().apply { time = fechaFinDate }

            val diaFiltro = calendarFiltro.get(Calendar.DAY_OF_MONTH)
            val mesFiltro = calendarFiltro.get(Calendar.MONTH)
            val añoFiltro = calendarFiltro.get(Calendar.YEAR)

            val diaFin = calendarFin.get(Calendar.DAY_OF_MONTH)
            val mesFin = calendarFin.get(Calendar.MONTH)
            val añoFin = calendarFin.get(Calendar.YEAR)

            val coincide = (diaFiltro == diaFin && mesFiltro == mesFin && añoFiltro == añoFin)

            if (coincide) {
                Log.d("MainActivity", "🎯 Fecha fin consulta coincide: $fechaFiltro == ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fechaFinDate)}")
            }

            coincide
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Error al comparar fecha fin consulta: $fechaFiltro - ${e.message}")
            false
        }
    }
    /**
     * 🔹 NUEVA FUNCIÓN: Compara si una fecha en formato DD/MM/YYYY coincide con una fecha de la base de datos
     */
    private fun coincideFecha(fechaFiltro: String, fechaBaseDatos: String): Boolean {
        return try {
            // Formato del filtro: DD/MM/YYYY
            val dateFormatFiltro = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechaFiltroDate = dateFormatFiltro.parse(fechaFiltro)

            if (fechaFiltroDate == null) return false

            // Intentar diferentes formatos de la base de datos
            val formatosBaseDatos = listOf(
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()),
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
                SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            )

            for (formato in formatosBaseDatos) {
                try {
                    val fechaBaseDate = formato.parse(fechaBaseDatos)
                    if (fechaBaseDate != null && fechaFiltroDate == fechaBaseDate) {
                        Log.d("MainActivity", "🎯 Fechas coinciden: $fechaFiltro == $fechaBaseDatos (formato: ${formato.toPattern()})")
                        return true
                    }
                } catch (e: Exception) {
                    // Continuar con el siguiente formato
                    continue
                }
            }

            // 🔹 COMPARACIÓN DIRECTA POR DÍA, MES Y AÑO (más robusta)
            val calendarFiltro = Calendar.getInstance().apply { time = fechaFiltroDate }
            val diaFiltro = calendarFiltro.get(Calendar.DAY_OF_MONTH)
            val mesFiltro = calendarFiltro.get(Calendar.MONTH)
            val añoFiltro = calendarFiltro.get(Calendar.YEAR)

            // Intentar extraer día, mes y año de la fecha de la base de datos
            val patrones = listOf(
                Regex("""(\d{4})[-/](\d{1,2})[-/](\d{1,2})"""), // YYYY-MM-DD o YYYY/MM/DD
                Regex("""(\d{1,2})[-/](\d{1,2})[-/](\d{4})""")  // DD-MM-YYYY o DD/MM/YYYY
            )

            for (patron in patrones) {
                val match = patron.find(fechaBaseDatos)
                if (match != null) {
                    val grupos = match.groupValues
                    if (grupos.size == 4) {
                        val (_, p1, p2, p3) = grupos

                        val (diaBD, mesBD, añoBD) = try {
                            if (patron.pattern.contains("""(\d{4})[-/]""")) {
                                // Formato: YYYY-MM-DD
                                Triple(p3.toInt(), p2.toInt() - 1, p1.toInt()) // Mes en Calendar es 0-based
                            } else {
                                // Formato: DD-MM-YYYY
                                Triple(p1.toInt(), p2.toInt() - 1, p3.toInt()) // Mes en Calendar es 0-based
                            }
                        } catch (e: NumberFormatException) {
                            continue
                        }

                        if (diaBD == diaFiltro && mesBD == mesFiltro && añoBD == añoFiltro) {
                            Log.d("MainActivity", "🎯 Fechas coinciden (comparación directa): $fechaFiltro == $fechaBaseDatos")
                            return true
                        }
                    }
                }
            }

            false
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Error al comparar fechas: $fechaFiltro vs $fechaBaseDatos - ${e.message}")
            false
        }
    }



    /**
     * Carga todos los pacientes completados (sin filtro de fecha) - VERSIÓN CORREGIDA
     */
    private fun cargarTodosLosPacientes(container: LinearLayout, titleView: TextView) {
        container.removeAllViews()
        mostrarLoadingIndicatorHistorial(container, true)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            mostrarLoadingIndicatorHistorial(container, false)
            return
        }

        db.collection("medicos")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { medicoDoc ->
                val idMedico = medicoDoc.getLong("ID_MEDICO") ?: 0L
                if (idMedico == 0L) {
                    mostrarLoadingIndicatorHistorial(container, false)
                    return@addOnSuccessListener
                }

                // 🔹 BUSCAR SOLO CITAS COMPLETADAS
                db.collection("cita")
                    .whereEqualTo("ID_MEDICO", idMedico)
                    .whereEqualTo("ESTADO", "completada")
                    .get()
                    .addOnSuccessListener { citasCompletadas ->
                        mostrarLoadingIndicatorHistorial(container, false)

                        Log.d("MainActivity", "📄 Total de citas completadas: ${citasCompletadas.size()}")

                        if (citasCompletadas.isEmpty()) {
                            mostrarEstadoVacioHistorial(container, "No hay pacientes completados en el historial")
                            titleView.text = "Todos los pacientes completados - 0 resultados"
                            return@addOnSuccessListener
                        }

                        titleView.text = "Todos los pacientes completados - ${citasCompletadas.size()} resultados"

                        // 🔹 ORDENAR POR FECHA_FIN_CONSULTA (más recientes primero)
                        val citasOrdenadas = citasCompletadas.sortedByDescending { cita ->
                            cita.getTimestamp("FECHA_FIN_CONSULTA")?.toDate() ?: Date(0)
                        }

                        // Obtener detalles de todos los pacientes completados
                        for (cita in citasOrdenadas) {
                            val idPaciente = cita.getLong("ID_PACIENTE") ?: continue
                            obtenerDetallesPacienteParaHistorial(idPaciente, cita, container)
                        }
                    }
                    .addOnFailureListener { e ->
                        mostrarLoadingIndicatorHistorial(container, false)
                        Log.e("MainActivity", "❌ Error al cargar citas completadas: ${e.message}")
                        Toast.makeText(this, "Error al cargar pacientes completados", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                mostrarLoadingIndicatorHistorial(container, false)
                Log.e("MainActivity", "❌ Error al obtener datos del médico: ${e.message}")
                Toast.makeText(this, "Error al obtener datos del médico", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Método alternativo si falla el whereIn - busca en ambos formatos de fecha por separado
     */
    private fun cargarPacientesPorFechaAlternativo(
        idMedico: Long,
        fecha1: String,
        fecha2: String,
        fechaOriginal: String,
        container: LinearLayout,
        titleView: TextView
    ) {
        val todasLasCitas = mutableListOf<com.google.firebase.firestore.QueryDocumentSnapshot>()

        mostrarLoadingIndicatorHistorial(container, true)

        Log.d("MainActivity", "Buscando citas en formato 1: $fecha1")
        Log.d("MainActivity", "Buscando citas en formato 2: $fecha2")

        // Primera consulta - formato YYYY-MM-DD
        db.collection("cita")
            .whereEqualTo("ID_MEDICO", idMedico)
            .whereEqualTo("FECHA", fecha1)
            .get()
            .addOnSuccessListener { citas1 ->
                Log.d("MainActivity", "Citas encontradas en formato 1: ${citas1.size()}")
                todasLasCitas.addAll(citas1)

                // Segunda consulta - formato YYYY/MM/DD
                db.collection("cita")
                    .whereEqualTo("ID_MEDICO", idMedico)
                    .whereEqualTo("FECHA", fecha2)
                    .get()
                    .addOnSuccessListener { citas2 ->
                        Log.d("MainActivity", "Citas encontradas en formato 2: ${citas2.size()}")
                        todasLasCitas.addAll(citas2)

                        mostrarLoadingIndicatorHistorial(container, false)

                        // 🔹 CORRECCIÓN: Usar isEmpty() con paréntesis
                        if (todasLasCitas.isEmpty()) {
                            mostrarEstadoVacioHistorial(container, "No hay pacientes para la fecha seleccionada")
                            titleView.text = "Pacientes del $fechaOriginal - 0 resultados"
                            return@addOnSuccessListener
                        }

                        // Eliminar duplicados por ID_CITA
                        val citasUnicas = todasLasCitas.distinctBy { it.getLong("ID_CITA") ?: 0L }

                        titleView.text = "Pacientes del $fechaOriginal - ${citasUnicas.size} resultados"

                        // Obtener detalles de los pacientes
                        for (cita in citasUnicas) {
                            val idPaciente = cita.getLong("ID_PACIENTE") ?: continue
                            obtenerDetallesPacienteParaHistorial(idPaciente, cita, container)
                        }
                    }
                    .addOnFailureListener { e2 ->
                        mostrarLoadingIndicatorHistorial(container, false)
                        Log.e("MainActivity", "Error en segunda consulta de fecha: ${e2.message}")
                        // Mostrar lo que se encontró en la primera consulta
                        // 🔹 CORRECCIÓN: Usar isEmpty() con paréntesis
                        if (todasLasCitas.isNotEmpty()) {
                            mostrarLoadingIndicatorHistorial(container, false)
                            val citasUnicas = todasLasCitas.distinctBy { it.getLong("ID_CITA") ?: 0L }
                            titleView.text = "Pacientes del $fechaOriginal - ${citasUnicas.size} resultados"
                            for (cita in citasUnicas) {
                                val idPaciente = cita.getLong("ID_PACIENTE") ?: continue
                                obtenerDetallesPacienteParaHistorial(idPaciente, cita, container)
                            }
                        } else {
                            mostrarEstadoVacioHistorial(container, "No hay pacientes para la fecha seleccionada")
                            titleView.text = "Pacientes del $fechaOriginal - 0 resultados"
                        }
                    }
            }
            .addOnFailureListener { e1 ->
                mostrarLoadingIndicatorHistorial(container, false)
                Log.e("MainActivity", "Error en primera consulta de fecha: ${e1.message}")
                mostrarEstadoVacioHistorial(container, "Error al cargar pacientes: ${e1.message}")
            }
    }
    /**
     * Obtiene detalles del paciente para mostrar en el historial - VERSIÓN MEJORADA
     */
    private fun obtenerDetallesPacienteParaHistorial(idPaciente: Long, cita: com.google.firebase.firestore.QueryDocumentSnapshot, container: LinearLayout) {
        db.collection("pacientes")
            .whereEqualTo("ID_PACIENTE", idPaciente)
            .get()
            .addOnSuccessListener { pacientes ->
                if (!pacientes.isEmpty()) {
                    val paciente = pacientes.documents.first()
                    val nombre = paciente.getString("NOMBRE") ?: "Nombre no disponible"
                    val apellido = paciente.getString("APELLIDO") ?: ""
                    val fechaFinConsulta = cita.getTimestamp("FECHA_FIN_CONSULTA")
                    val estado = cita.getString("ESTADO") ?: "Estado no disponible"
                    val hora = cita.getString("HORA") ?: ""

                    // Formatear fecha de finalización
                    val fechaFinFormateada = if (fechaFinConsulta != null) {
                        try {
                            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            dateFormat.format(fechaFinConsulta.toDate())
                        } catch (e: Exception) {
                            "Fecha no disponible"
                        }
                    } else {
                        "Fecha no disponible"
                    }

                    agregarPacienteAListaHistorial("$nombre $apellido", fechaFinFormateada, hora, estado, container)
                }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error al obtener paciente: ${e.message}")
            }
    }

    /**
     * Agrega un paciente a la lista del historial
     */
    private fun agregarPacienteAListaHistorial(nombre: String, fecha: String, hora: String, estado: String, container: LinearLayout) {
        val itemView = LayoutInflater.from(this).inflate(R.layout.item_notification, container, false)

        val tvPatientName = itemView.findViewById<TextView>(R.id.tvNotificationName)
        val tvNotificationType = itemView.findViewById<TextView>(R.id.tvNotificationType)

        tvPatientName.text = nombre

        // Formatear hora si está disponible
        val horaFormateada = if (hora.isNotEmpty() && hora.length >= 5) {
            hora.substring(0, 5) // Tomar solo HH:MM
        } else {
            hora
        }

        // Mostrar información de la cita
        val infoCita = if (horaFormateada.isNotEmpty()) {
            "📅 $fecha - 🕒 $horaFormateada - $estado"
        } else {
            "📅 $fecha - $estado"
        }

        tvNotificationType.text = infoCita

        // Color según el estado
        when (estado.toLowerCase(Locale.getDefault())) {
            "completada" -> tvNotificationType.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            "cancelada" -> tvNotificationType.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            "pendiente" -> tvNotificationType.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
            "confirmada", "reservada" -> tvNotificationType.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark))
            else -> tvNotificationType.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }

        container.addView(itemView)
    }

    /**
     * Carga la lista de pacientes con citas completadas
     */
    private fun cargarPacientesCompletados(container: LinearLayout) {
        container.removeAllViews()
        mostrarLoadingIndicatorHistorial(container, true)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            mostrarLoadingIndicatorHistorial(container, false)
            return
        }

        db.collection("medicos")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { medicoDoc ->
                val idMedico = medicoDoc.getLong("ID_MEDICO") ?: 0L
                if (idMedico == 0L) {
                    mostrarLoadingIndicatorHistorial(container, false)
                    return@addOnSuccessListener
                }

                // Buscar citas completadas
                db.collection("cita")
                    .whereEqualTo("ID_MEDICO", idMedico)
                    .whereEqualTo("ESTADO", "completada")
                    .orderBy("FECHA", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { citasDocuments ->
                        if (citasDocuments.isEmpty) {
                            mostrarLoadingIndicatorHistorial(container, false)
                            mostrarEstadoVacioHistorial(container, "No hay pacientes completados")
                            return@addOnSuccessListener
                        }

                        val pacientesIds = mutableSetOf<Long>()
                        val citasCompletadas = mutableListOf<CitaReal>()

                        for (document in citasDocuments) {
                            val idPaciente = document.getLong("ID_PACIENTE") ?: 0L
                            if (idPaciente != 0L) {
                                pacientesIds.add(idPaciente)

                                val cita = CitaReal(
                                    ID_CITA = document.getLong("ID_CITA") ?: 0L,
                                    ID_PACIENTE = idPaciente,
                                    ID_MEDICO = document.getLong("ID_MEDICO") ?: 0L,
                                    FECHA = document.getString("FECHA") ?: "",
                                    HORA = document.getString("HORA") ?: "",
                                    ESTADO = document.getString("ESTADO") ?: ""
                                )
                                citasCompletadas.add(cita)
                            }
                        }

                        if (pacientesIds.isEmpty()) {
                            mostrarLoadingIndicatorHistorial(container, false)
                            mostrarEstadoVacioHistorial(container, "No hay pacientes completados")
                            return@addOnSuccessListener
                        }

                        // Buscar información de los pacientes completados
                        db.collection("pacientes")
                            .whereIn("ID_PACIENTE", pacientesIds.toList())
                            .get()
                            .addOnSuccessListener { pacientesDocuments ->
                                mostrarLoadingIndicatorHistorial(container, false)

                                val pacientesMap = mutableMapOf<Long, PacienteReal>()
                                for (document in pacientesDocuments) {
                                    val paciente = PacienteReal(
                                        ID_PACIENTE = getSafeLong(document, "ID_PACIENTE"),
                                        //ID_USUARIO = getSafeLong(document, "ID_USUARIO"), // ← Esto es importante
                                        NOMBRE = getSafeString(document, "NOMBRE"),
                                        APELLIDO = getSafeString(document, "APELLIDO"),
                                        CORREO = getSafeString(document, "CORREO"),
                                        CELULAR = getSafeString(document, "CELULAR"),
                                        DNI = getSafeString(document, "DNI"),
                                        EDAD = 0,
                                        SEXO = getSafeString(document, "SEXO")
                                    )
                                    pacientesMap[paciente.ID_PACIENTE] = paciente
                                }

                                // Mostrar pacientes completados
                                for (cita in citasCompletadas) {
                                    val paciente = pacientesMap[cita.ID_PACIENTE]
                                    if (paciente != null) {
                                        val itemView = LayoutInflater.from(this)
                                            .inflate(R.layout.item_notification, container, false)

                                        itemView.findViewById<TextView>(R.id.tvNotificationName).text =
                                            "${paciente.NOMBRE} ${paciente.APELLIDO}"

                                        itemView.findViewById<TextView>(R.id.tvNotificationType).text =
                                            "✅ Completado - ${formatearFecha(cita.FECHA)} ${cita.HORA.substring(0, 5)}"

                                        container.addView(itemView)
                                    }
                                }
                            }
                    }
            }
    }
    /**
     * Configura los botones de acción para cada paciente (WhatsApp, Gmail, Check)
     */
    private fun configurarBotonesPaciente(itemView: View, paciente: PacienteReal) {
        // 🔹 NUEVO: Botón de Información
        val btnInfo = itemView.findViewById<ImageButton>(R.id.btnNotification)
        btnInfo?.setOnClickListener {
            mostrarInformacionCompletaPaciente(paciente)
        }
        // Botón de ChatRoom
        val btnChatRoom = itemView.findViewById<ImageButton>(R.id.btnChatRoom)
        btnChatRoom?.setOnClickListener {
            abrirChatInterno(paciente)
        }

        // Botón de Gmail
        val btnGmail = itemView.findViewById<ImageButton>(R.id.btnGmail)
        btnGmail?.setOnClickListener {
            abrirGmail(paciente)
        }

        // El botón Check ya está configurado por separado en cargarPacientesActivos
    }
    /**
     * Abre el fragmento de chat interno de la app - VERSIÓN CORREGIDA CON DATOS REALES
     */
    private fun abrirChatInterno(paciente: PacienteReal) {
        try {
            Log.d("MainActivity", "🔹 Abriendo chat con: ${paciente.NOMBRE} ${paciente.APELLIDO}, ID: ${paciente.ID_PACIENTE}")

            // 🔹 BUSCAR LA CITA REAL EN FIREBASE
            obtenerCitaRealDelPaciente(paciente) { citaReal ->
                if (citaReal != null) {
                    Log.d("MainActivity", "✅ Cita real encontrada - ID: ${citaReal.ID_CITA}, Estado: ${citaReal.ESTADO}")

                    val fragment = ChatRoomFragment().apply {
                        arguments = Bundle().apply {
                            putLong("id_cita", citaReal.ID_CITA) // 🔹 USAR ID REAL
                            putLong("id_paciente", paciente.ID_PACIENTE)
                            putString("nombre_paciente", "${paciente.NOMBRE} ${paciente.APELLIDO}")
                            putString("especialidad", "Consulta General")
                        }
                    }

                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_content_frame, fragment)
                        .addToBackStack("chat_room")
                        .commit()

                    Log.d("MainActivity", "✅ Fragmento de chat abierto con cita REAL ID: ${citaReal.ID_CITA}")
                    Toast.makeText(this, "Chat abierto con ${paciente.NOMBRE}", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("MainActivity", "❌ No se encontró cita real para el paciente")
                    mostrarDialogoSinCitaReal(paciente)
                }
            }

        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Error al abrir chat: ${e.message}", e)
            Toast.makeText(this, "Error al abrir chat", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Obtiene la cita REAL del paciente desde Firebase
     */
    private fun obtenerCitaRealDelPaciente(paciente: PacienteReal, callback: (CitaReal?) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            callback(null)
            return
        }

        Log.d("MainActivity", "🔍 Buscando cita REAL para paciente: ${paciente.ID_PACIENTE}")

        db.collection("medicos")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { medicoDoc ->
                val idMedico = medicoDoc.getLong("ID_MEDICO") ?: 0L
                if (idMedico == 0L) {
                    Log.e("MainActivity", "ID_MEDICO es 0")
                    callback(null)
                    return@addOnSuccessListener
                }

                // Buscar citas REALES del paciente
                db.collection("cita")
                    .whereEqualTo("ID_PACIENTE", paciente.ID_PACIENTE)
                    .whereEqualTo("ID_MEDICO", idMedico)
                    .get()
                    .addOnSuccessListener { documents ->
                        Log.d("MainActivity", "📄 Citas reales encontradas: ${documents.size()}")

                        if (documents.isEmpty) {
                            Log.e("MainActivity", "❌ No hay citas reales para el paciente ${paciente.ID_PACIENTE}")
                            callback(null)
                            return@addOnSuccessListener
                        }

                        // Mostrar todas las citas para debug
                        for (document in documents) {
                            val idCita = document.getLong("ID_CITA") ?: 0L
                            val estado = document.getString("ESTADO") ?: "sin estado"
                            val fecha = document.getString("FECHA") ?: "sin fecha"
                            val hora = document.getString("HORA") ?: "sin hora"

                            Log.d("MainActivity", "📋 Cita - ID: $idCita, Estado: $estado, Fecha: $fecha, Hora: $hora")
                        }

                        // 🔹 BUSCAR PRIMERO CITA ACTIVA (no completada)
                        var citaSeleccionada: CitaReal? = null
                        for (document in documents) {
                            val estado = document.getString("ESTADO") ?: ""
                            val idCita = document.getLong("ID_CITA") ?: 0L

                            if (estado != "completada" && idCita != 0L) {
                                citaSeleccionada = CitaReal(
                                    ID_CITA = idCita,
                                    ID_PACIENTE = document.getLong("ID_PACIENTE") ?: 0L,
                                    ID_MEDICO = document.getLong("ID_MEDICO") ?: 0L,
                                    FECHA = document.getString("FECHA") ?: "",
                                    HORA = document.getString("HORA") ?: "",
                                    ESTADO = estado
                                )
                                Log.d("MainActivity", "✅ Cita activa seleccionada: ID=$idCita, Estado=$estado")
                                break
                            }
                        }

                        // 🔹 SI NO HAY CITA ACTIVA, USAR LA PRIMERA CITA ENCONTRADA
                        if (citaSeleccionada == null && !documents.isEmpty) {
                            val primeraCitaDoc = documents.documents.first()
                            citaSeleccionada = CitaReal(
                                ID_CITA = primeraCitaDoc.getLong("ID_CITA") ?: 0L,
                                ID_PACIENTE = primeraCitaDoc.getLong("ID_PACIENTE") ?: 0L,
                                ID_MEDICO = primeraCitaDoc.getLong("ID_MEDICO") ?: 0L,
                                FECHA = primeraCitaDoc.getString("FECHA") ?: "",
                                HORA = primeraCitaDoc.getString("HORA") ?: "",
                                ESTADO = primeraCitaDoc.getString("ESTADO") ?: ""
                            )
                            Log.d("MainActivity", "⚠️ Usando primera cita encontrada: ID=${citaSeleccionada.ID_CITA}")
                        }

                        callback(citaSeleccionada)
                    }
                    .addOnFailureListener { e ->
                        Log.e("MainActivity", "❌ Error al buscar citas reales: ${e.message}")
                        callback(null)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "❌ Error al obtener médico: ${e.message}")
                callback(null)
            }
    }

    /**
     * Muestra diálogo cuando no se encuentra cita real
     */
    private fun mostrarDialogoSinCitaReal(paciente: PacienteReal) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Cita No Encontrada")
        builder.setMessage("No se encontró una cita en la base de datos para el paciente ${paciente.NOMBRE} ${paciente.APELLIDO}.\n\nPuedes crear una cita nueva desde el calendario.")

        builder.setPositiveButton("Crear Cita") { dialog, _ ->
            // Navegar a calendario para crear nueva cita
            displayContent(R.layout.content_calendars)
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }
    /**
     * Abre Gmail para enviar correo al paciente - VERSIÓN CORREGIDA
     */
    private fun abrirGmail(paciente: PacienteReal) {
        try {
            Log.d("MainActivity", "📧 Intentando abrir Gmail para: ${paciente.NOMBRE} ${paciente.APELLIDO}")

            // 🔹 BUSCAR EL CORREO REAL EN LA TABLA USUARIO
            buscarCorreoUsuario(paciente.ID_USUARIO) { correo ->
                if (correo.isNotEmpty() && correo != "No especificado") {
                    Log.d("MainActivity", "✅ Correo encontrado: $correo")

                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$correo")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(correo))
                        putExtra(Intent.EXTRA_SUBJECT, "Consulta Médica - Dr. ${getDoctorName()}")
                        putExtra(Intent.EXTRA_TEXT,
                            "Estimado/a ${paciente.NOMBRE} ${paciente.APELLIDO},\n\n" +
                                    "Espero que se encuentre bien.\n\n" +
                                    "Saludos cordiales,\n" +
                                    "Dr. ${getDoctorName()}"
                        )
                    }

                    // Verificar si hay alguna app que pueda manejar el intent
                    val packageManager = packageManager
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                        Log.d("MainActivity", "✅ Gmail abierto exitosamente")
                    } else {
                        // Si no hay Gmail, intentar con cualquier app de correo
                        val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "message/rfc822"
                            putExtra(Intent.EXTRA_EMAIL, arrayOf(correo))
                            putExtra(Intent.EXTRA_SUBJECT, "Consulta Médica - Dr. ${getDoctorName()}")
                            putExtra(Intent.EXTRA_TEXT,
                                "Estimado/a ${paciente.NOMBRE} ${paciente.APELLIDO},\n\n" +
                                        "Espero que se encuentre bien.\n\n" +
                                        "Saludos cordiales,\n" +
                                        "Dr. ${getDoctorName()}"
                            )
                        }

                        if (fallbackIntent.resolveActivity(packageManager) != null) {
                            startActivity(Intent.createChooser(fallbackIntent, "Enviar correo"))
                            Log.d("MainActivity", "✅ App de correo alternativa abierta")
                        } else {
                            runOnUiThread {
                                Toast.makeText(this, "No hay aplicación de correo instalada", Toast.LENGTH_LONG).show()
                            }
                            Log.e("MainActivity", "❌ No hay app de correo instalada")
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Correo electrónico no disponible para este paciente", Toast.LENGTH_LONG).show()
                    }
                    Log.e("MainActivity", "❌ Correo no disponible para el paciente")
                }
            }

        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Error al abrir Gmail: ${e.message}", e)
            runOnUiThread {
                Toast.makeText(this, "Error al abrir correo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Obtiene el nombre del doctor actual
     */
    private fun getDoctorName(): String {
        val currentUser = auth.currentUser
        if (currentUser == null) return ""

        // Podrías cachear este valor para mejor performance
        return "Médico" // Por ahora retornamos un valor por defecto
        // En una implementación real, buscarías esto de Firestore
    }

    /**
     * Muestra toda la información del paciente en un diálogo emergente (SIN ÍNDICE)
     */
    private fun mostrarInformacionCompletaPaciente(paciente: PacienteReal) {
        try {
            // Buscar información adicional de la cita del paciente
            val currentUser = auth.currentUser
            if (currentUser != null) {
                db.collection("medicos")
                    .document(currentUser.uid)
                    .get()
                    .addOnSuccessListener { medicoDoc ->
                        val idMedico = medicoDoc.getLong("ID_MEDICO") ?: 0L
                        if (idMedico != 0L) {
                            // 🔹 CONSULTA SIMPLIFICADA - Solo por ID_MEDICO para evitar índice compuesto
                            db.collection("cita")
                                .whereEqualTo("ID_MEDICO", idMedico)
                                .get()
                                .addOnSuccessListener { citas ->
                                    // Filtrar localmente por ID_PACIENTE y ordenar por fecha
                                    val citasDelPaciente = citas.documents
                                        .filter { doc ->
                                            doc.getLong("ID_PACIENTE") == paciente.ID_PACIENTE
                                        }
                                        .sortedByDescending { doc ->
                                            doc.getString("FECHA") ?: ""
                                        }

                                    // 🔹 BUSCAR CORREO EN TABLA USUARIO
                                    buscarCorreoUsuario(paciente.ID_USUARIO) { correo ->
                                        if (citasDelPaciente.isNotEmpty()) {
                                            val citaDoc = citasDelPaciente.first()
                                            val estadoCita = citaDoc.getString("ESTADO") ?: "No disponible"
                                            val fechaCita = citaDoc.getString("FECHA") ?: "No disponible"
                                            val horaCita = citaDoc.getString("HORA") ?: "No disponible"
                                            val fechaFinConsulta = citaDoc.getTimestamp("FECHA_FIN_CONSULTA")

                                            // Formatear la fecha si es necesario
                                            val fechaFormateada = formatearFecha(fechaCita)

                                            // Formatear la hora si es necesaria
                                            val horaFormateada = if (horaCita.length >= 5) {
                                                horaCita.substring(0, 5)
                                            } else {
                                                horaCita
                                            }

                                            // Formatear fecha de fin de consulta si existe
                                            val fechaFinFormateada = if (fechaFinConsulta != null) {
                                                try {
                                                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                                    dateFormat.format(fechaFinConsulta.toDate())
                                                } catch (e: Exception) {
                                                    "No disponible"
                                                }
                                            } else {
                                                "No disponible"
                                            }

                                            // Mostrar el diálogo con toda la información
                                            mostrarDialogoInformacionCompleta(
                                                paciente,
                                                estadoCita,
                                                fechaFormateada,
                                                horaFormateada,
                                                fechaFinFormateada,
                                                correo
                                            )
                                        } else {
                                            // No se encontraron citas para este paciente
                                            mostrarDialogoInformacionCompleta(
                                                paciente,
                                                "Sin cita",
                                                "No disponible",
                                                "No disponible",
                                                "No disponible",
                                                correo
                                            )
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("MainActivity", "Error al buscar citas: ${e.message}")
                                    // Buscar correo incluso si hay error en citas
                                    buscarCorreoUsuario(paciente.ID_USUARIO) { correo ->
                                        mostrarDialogoInformacionCompleta(
                                            paciente,
                                            "Error al cargar",
                                            "No disponible",
                                            "No disponible",
                                            "No disponible",
                                            correo
                                        )
                                    }
                                }
                        } else {
                            buscarCorreoUsuario(paciente.ID_USUARIO) { correo ->
                                mostrarDialogoInformacionCompleta(
                                    paciente,
                                    "ID Médico no disponible",
                                    "No disponible",
                                    "No disponible",
                                    "No disponible",
                                    correo
                                )
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("MainActivity", "Error al obtener médico: ${e.message}")
                        buscarCorreoUsuario(paciente.ID_USUARIO) { correo ->
                            mostrarDialogoInformacionCompleta(
                                paciente,
                                "Error médico",
                                "No disponible",
                                "No disponible",
                                "No disponible",
                                correo
                            )
                        }
                    }
            } else {
                buscarCorreoUsuario(paciente.ID_USUARIO) { correo ->
                    mostrarDialogoInformacionCompleta(
                        paciente,
                        "Usuario no autenticado",
                        "No disponible",
                        "No disponible",
                        "No disponible",
                        correo
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al mostrar información del paciente: ${e.message}")
            buscarCorreoUsuario(paciente.ID_USUARIO) { correo ->
                mostrarDialogoInformacionCompleta(
                    paciente,
                    "Error general",
                    "No disponible",
                    "No disponible",
                    "No disponible",
                    correo
                )
            }
        }
    }

    /**
     * Busca el correo del paciente en la tabla usuario usando ID_USUARIO - VERSIÓN MEJORADA
     */
    private fun buscarCorreoUsuario(idUsuario: Long, callback: (String) -> Unit) {
        if (idUsuario == 0L) {
            Log.e("MainActivity", "❌ ID_USUARIO es 0, no se puede buscar correo")
            callback("No especificado")
            return
        }

        Log.d("MainActivity", "🔍 Buscando correo para ID_USUARIO: $idUsuario")

        db.collection("usuario")
            .whereEqualTo("ID_USUARIO", idUsuario)
            .get()
            .addOnSuccessListener { documentos ->
                if (!documentos.isEmpty) {
                    val usuarioDoc = documentos.documents.first()
                    val correo = usuarioDoc.getString("CORREO") ?: "No especificado"
                    Log.d("MainActivity", "✅ Correo encontrado en BD: $correo")
                    callback(correo)
                } else {
                    Log.e("MainActivity", "❌ No se encontró usuario con ID: $idUsuario")
                    callback("No especificado")
                }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "❌ Error al buscar correo: ${e.message}")
                callback("No especificado")
            }
    }
    /**
     * Muestra un diálogo con toda la información del paciente (VERSIÓN MEJORADA CON LAYOUT)
     */
    private fun mostrarDialogoInformacionCompleta(
        paciente: PacienteReal,
        estadoCita: String,
        fechaCita: String,
        horaCita: String,
        fechaFinConsulta: String,
        correoUsuario: String
    ) {
        // Inflar el layout personalizado
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_patient_info, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)

        // Referencias a las vistas
        val tvNombre = dialogView.findViewById<TextView>(R.id.tvNombre)
        val tvDni = dialogView.findViewById<TextView>(R.id.tvDni)
        val tvEdad = dialogView.findViewById<TextView>(R.id.tvEdad)
        val tvSexo = dialogView.findViewById<TextView>(R.id.tvSexo)
        val tvCelular = dialogView.findViewById<TextView>(R.id.tvCelular)
        val tvCorreo = dialogView.findViewById<TextView>(R.id.tvCorreo)
        val tvEstadoCita = dialogView.findViewById<TextView>(R.id.tvEstadoCita)
        val tvFechaCita = dialogView.findViewById<TextView>(R.id.tvFechaCita)
        val tvHoraCita = dialogView.findViewById<TextView>(R.id.tvHoraCita)
        val tvFinConsulta = dialogView.findViewById<TextView>(R.id.tvFinConsulta)

        // Configurar los datos
        tvNombre.text = "Nombre: ${paciente.NOMBRE} ${paciente.APELLIDO}"
        tvDni.text = "DNI: ${if (paciente.DNI.isNotEmpty()) paciente.DNI else "No especificado"}"
        tvEdad.text = "Edad: ${if (paciente.EDAD > 0) "${paciente.EDAD} años" else "No especificada"}"
        tvSexo.text = "Sexo: ${if (paciente.SEXO.isNotEmpty()) paciente.SEXO else "No especificado"}"
        tvCelular.text = "Celular: ${if (paciente.CELULAR.isNotEmpty()) paciente.CELULAR else "No especificado"}"
        tvCorreo.text = "Correo: $correoUsuario"

        // Determinar color del estado
        val (colorEstado, emojiEstado) = when (estadoCita.toLowerCase(Locale.getDefault())) {
            "confirmada", "reservada" -> Pair("#4CAF50", "✅") // Verde
            "pendiente" -> Pair("#FF9800", "⏳")  // Naranja
            "completada" -> Pair("#2196F3", "🏁") // Azul
            "cancelada" -> Pair("#F44336", "❌")  // Rojo
            else -> Pair("#9E9E9E", "📝")         // Gris
        }

        tvEstadoCita.text = "Estado: $emojiEstado $estadoCita"
        tvEstadoCita.setTextColor(Color.parseColor(colorEstado))

        tvFechaCita.text = "Fecha: $fechaCita"
        tvHoraCita.text = "Hora: $horaCita"

        // Mostrar fin de consulta solo si está disponible
        if (fechaFinConsulta != "No disponible") {
            tvFinConsulta.text = "Fin Consulta: $fechaFinConsulta"
            tvFinConsulta.visibility = View.VISIBLE
        } else {
            tvFinConsulta.visibility = View.GONE
        }

        // Solo botón de cerrar
        builder.setPositiveButton("Cerrar") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()

        // Personalizar el botón
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
    }

    private fun debugNotificationState() {
        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val estadoGuardado = sharedPreferences.getBoolean("notifications_enabled", true)
        Log.d("MainActivity", "🔔 DEBUG - Estado guardado: $estadoGuardado")
        Log.d("MainActivity", "🔔 DEBUG - Variable en memoria: $notificationsEnabled")

        if (::notificationSwitch.isInitialized) {
            Log.d("MainActivity", "🔔 DEBUG - Estado del switch UI: ${notificationSwitch.isChecked}")
        }
    }
}