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
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView // Agregar esta importación

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
    private var estadoFiltroActual = "Todos" // Estado por defecto

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

    // Agregar estas clases de datos al inicio del archivo
    data class PacienteReal(
        val ID_PACIENTE: Long = 0L,
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
                // 1. Obtener contenedores
                val searchBar = newLayout.findViewById<EditText>(R.id.etSearchNotifications)
                val container = newLayout.findViewById<LinearLayout>(R.id.containerNotifications)

                // 2. Cargar notificaciones reales desde Firebase
                cargarNotificacionesReales(container, searchBar)
            }

            R.layout.content_patients -> {
                // 1. Obtener contenedores
                val searchBar = newLayout.findViewById<EditText>(R.id.etSearchPatients)
                val container = newLayout.findViewById<LinearLayout>(R.id.containerPatients)

                // 2. Cargar pacientes reales desde Firebase
                cargarPacientesReales(container, searchBar)
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

                // Buscar notificaciones para este médico
                db.collection("notificaciones")
                    .whereEqualTo("ID_MEDICO", idMedico)
                    .orderBy("FECHA_ENVIO", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { documents ->
                        mostrarLoadingIndicator(container, false)

                        if (documents.isEmpty) { // ✅ Esto está correcto para Firestore
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

                            // Configurar nombre (buscar nombre del paciente)
                            configurarNombrePacienteEnNotificacion(itemView, notificacion.ID_PACIENTE)

                            // ✅ CORREGIDO: Solo usar tvNotificationType para mostrar título
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
                                        // ✅ USAR FUNCIONES SEGURAS PARA TODOS LOS CAMPOS
                                        val paciente = PacienteReal(
                                            ID_PACIENTE = getSafeLong(document, "ID_PACIENTE"),
                                            NOMBRE = getSafeString(document, "NOMBRE"),
                                            APELLIDO = getSafeString(document, "APELLIDO"),
                                            CORREO = getSafeString(document, "CORREO"),
                                            CELULAR = getSafeString(document, "CELULAR"),
                                            DNI = getSafeString(document, "DNI"), // ✅ MANEJADO SEGURO
                                            EDAD = getSafeLong(document, "EDAD").toInt(),
                                            SEXO = getSafeString(document, "SEXO")
                                        )
                                        pacientesList.add(paciente)
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Error al procesar paciente: ${e.message}")
                                        // Continuar con el siguiente paciente
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
     * Muestra un indicador de carga (versión simplificada sin layout)
     */
    private fun mostrarLoadingIndicator(container: LinearLayout, mostrar: Boolean) {
        container.removeAllViews()

        if (mostrar) {
            // Crear loading view simple sin layout
            val loadingView = TextView(this).apply {
                text = "Cargando..."
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
     * Muestra estado vacío (versión simplificada sin layout)
     */
    private fun mostrarEstadoVacio(container: LinearLayout, mensaje: String) {
        container.removeAllViews()

        // Crear una vista simple sin layout
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
                if (!documents.isEmpty) { // ✅ Esto está correcto para Firestore
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
        AlertDialog.Builder(this)
            .setTitle(notificacion.TITULO)
            .setMessage(notificacion.MENSAJE)
            .setPositiveButton("Cerrar", null)
            .show()
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
}