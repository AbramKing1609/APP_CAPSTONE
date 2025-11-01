package com.example.app_capstone

import android.Manifest
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.util.Calendar
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {
    private lateinit var llHorariosContainer: LinearLayout
    private lateinit var chipGroupFechas: ChipGroup
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val PICK_IMAGE_REQUEST = 100
    private val PERMISSION_REQUEST_CODE = 200
    private lateinit var ivProfilePicture: ImageView

    private lateinit var actvUniversity: AutoCompleteTextView
    private lateinit var actvHospital: AutoCompleteTextView
    private lateinit var actvDistrito: AutoCompleteTextView
    private lateinit var actvSpecialty: AutoCompleteTextView
    private lateinit var chipGroupDiasFiltro: ChipGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.content_profile)

        // 🔹 Referencias de vistas EXISTENTES...
        val tvProfileName = findViewById<TextView>(R.id.tvProfileName)
        val tvExperienceYears = findViewById<TextView>(R.id.tvExperienceYears)
        val tvAdditionalInfo = findViewById<TextView>(R.id.tvAdditionalInfo)
        val tvAge = findViewById<TextView>(R.id.tvAge)
        val etContactNumber = findViewById<EditText>(R.id.etContactNumber)
        val tvNacionalidad = findViewById<TextView>(R.id.tvNacionalidad)
        // 🔹 NUEVAS REFERENCIAS para el filtro por día
        chipGroupDiasFiltro = findViewById(R.id.chipGroupDiasFiltro)
        // 🔹 NUEVAS REFERENCIAS para AutoCompleteTextView
        actvUniversity = findViewById(R.id.actvUniversity)
        actvHospital = findViewById(R.id.actvHospital)
        actvDistrito = findViewById(R.id.actvDistrito)
        actvSpecialty = findViewById(R.id.actvSpecialty)

        // 🔹 CAMPOS DE HORARIO/FECHAS
        //llHorariosContainer = findViewById<LinearLayout>(R.id.llHorariosContainer)
        chipGroupFechas = findViewById<ChipGroup>(R.id.chipGroupFechas)

        // 🔹 Iconos de edición
        val ivEditName = findViewById<ImageView>(R.id.ivEditName)
        val ivEditAge = findViewById<ImageView>(R.id.ivEditAge)
        val ivEditExperience = findViewById<ImageView>(R.id.ivEditExperience)
        val ivEditUniversity = findViewById<ImageView>(R.id.ivEditUniversity)
        val ivEditHospital = findViewById<ImageView>(R.id.ivEditHospital)
        val ivEditAdditionalInfo = findViewById<ImageView>(R.id.ivEditAdditionalInfo)
        val ivEditPhone = findViewById<ImageView>(R.id.ivEditPhone)
        val ivEditSpecialty = findViewById<ImageView>(R.id.ivEditDescription)

        // 🔹 NUEVOS ICONOS agregados
        val ivEditDistrito = findViewById<ImageView>(R.id.ivEditDistrito)
        val ivEditNacionalidad = findViewById<ImageView>(R.id.ivEditNacionalidad)

        // 🔹 ICONOS DE HORARIO/FECHAS
        //val ivAddHorario = findViewById<ImageView>(R.id.ivAddHorario)
        val ivAddFecha = findViewById<ImageView>(R.id.ivAddFecha)

        // 🔹 Referencia a la imagen de perfil
        ivProfilePicture = findViewById<ImageView>(R.id.ivProfilePicture)

        // 🔹 Cargar la foto desde Firestore/Storage
        loadProfileData()

        // 🔹 Al hacer clic, abrir galería para cambiar foto
        ivProfilePicture.setOnClickListener {
            checkAndOpenGallery()
        }

        val btnChangePassword = findViewById<Button>(R.id.btnChangePassword)

        btnChangePassword.setOnClickListener {
            // Crear un LinearLayout vertical para el diálogo
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            layout.setPadding(50, 40, 50, 10)

            val etCurrent = EditText(this)
            etCurrent.hint = "Contraseña actual"
            etCurrent.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

            val etNew = EditText(this)
            etNew.hint = "Nueva contraseña"
            etNew.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

            val etRepeat = EditText(this)
            etRepeat.hint = "Repetir nueva contraseña"
            etRepeat.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

            layout.addView(etCurrent)
            layout.addView(etNew)
            layout.addView(etRepeat)

            val dialog = AlertDialog.Builder(this)
                .setTitle("Cambiar contraseña")
                .setView(layout)
                .setPositiveButton("Guardar", null) // 🔹 manejaremos el click manualmente
                .setNegativeButton("Cancelar", null)
                .create()

            dialog.show()

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val currentPass = etCurrent.text.toString()
                val newPass = etNew.text.toString()
                val repeatPass = etRepeat.text.toString()

                var valid = true

                if (currentPass.isEmpty()) {
                    etCurrent.error = "Campo obligatorio"
                    valid = false
                } else {
                    etCurrent.error = null
                }

                if (newPass.isEmpty()) {
                    etNew.error = "Campo obligatorio"
                    valid = false
                } else {
                    etNew.error = null
                }

                if (repeatPass.isEmpty()) {
                    etRepeat.error = "Campo obligatorio"
                    valid = false
                } else {
                    etRepeat.error = null
                }

                if (!valid) return@setOnClickListener

                if (newPass != repeatPass) {
                    etRepeat.error = "Las contraseñas no coinciden"
                    return@setOnClickListener
                } else {
                    etRepeat.error = null
                }

                // Reautenticamos
                val user = auth.currentUser
                if (user == null || user.email.isNullOrEmpty()) {
                    Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(user.email!!, currentPass)
                user.reauthenticate(credential)
                    .addOnSuccessListener {
                        user.updatePassword(newPass)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Contraseña actualizada correctamente", Toast.LENGTH_LONG).show()
                                dialog.dismiss() // 🔹 Cerramos solo si todo salió bien
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error al actualizar la contraseña: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener {
                        etCurrent.error = "Contraseña actual incorrecta" // 🔹 Mostramos el error en el mismo campo
                    }
            }
        }

        val ivBack = findViewById<ImageView>(R.id.ivBack)
        val ivInfo = findViewById<ImageView>(R.id.ivInfo)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        // 🔹 MIGRAR DOCUMENTOS EXISTENTES (si es necesario)
        migrarIDsDisponibilidad()
        val userId = currentUser.uid

        // 📝 Funciones de edición de campos de texto/número
        ivEditName.setOnClickListener {
            editField("NOMBRE", tvProfileName.text.toString(), tvProfileName)
        }

        ivEditAge.setOnClickListener {
            editField("EDAD", tvAge.text.toString().replace(" años", ""), tvAge, " años")
        }

        ivEditExperience.setOnClickListener {
            editField("EXP_ANIOS", tvExperienceYears.text.toString(), tvExperienceYears)
        }

        ivEditAdditionalInfo.setOnClickListener {
            editField("INFO_ADIC", tvAdditionalInfo.text.toString(), tvAdditionalInfo)
        }

        ivEditPhone.setOnClickListener {
            editField("CONTACTO", etContactNumber.text.toString(), etContactNumber)
        }

        // -------------------------------------------------------------
        // ⏰ MODIFICADO: Ahora el horario se selecciona DESPUÉS de la fecha
        // -------------------------------------------------------------
        ivAddFecha.setOnClickListener {
            showCalendarWithTimePicker()
        }

        // Eliminamos el listener de ivAddHorario ya que ahora se selecciona todo junto
        //ivAddHorario.visibility = View.GONE

        // -------------------------------------------------------------
        // 🔹 Funciones autoincrementales (sin cambios)
        // -------------------------------------------------------------

        ivEditUniversity.setOnClickListener {
            showUniversitySelectionDialog()
        }

        ivEditHospital.setOnClickListener {
            editHospitalField()
        }

        ivEditSpecialty.setOnClickListener {
            showSpecialtySelectionDialog()
        }
        ivEditDistrito.setOnClickListener {
            showDistritoSelectionDialog()
        }

        ivEditNacionalidad.setOnClickListener {
            editFieldWithAutoCreate(
                collectionName = "nacionalidad",
                idFieldName = "ID_NACIONALIDAD",
                nameFieldName = "NACIONALIDAD",
                currentName = tvNacionalidad.text.toString(),
                textView = tvNacionalidad,
                medicoFieldKey = "ID_NACIONALIDAD"
            )
        }

        ivBack.setOnClickListener {
            finish()
        }

        ivInfo.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Información del Perfil Médico")
                .setMessage(
                    "Esta pantalla contiene toda la información de tu perfil profesional. " +
                            "Puedes editar cualquier campo tocando el ícono de lápiz (✏️) junto a él. " +
                            "La información de horarios y fechas de atención es vital para que los pacientes puedan agendar citas."
                )
                .setPositiveButton("Entendido", null)
                .show()
        }

        // 🔹 INICIALIZAR FILTRO POR DÍA
        setupDiaFiltro()
    }

    /**
     * NUEVO: Configura el filtro por día de la semana
     */
    private fun setupDiaFiltro() {
        val diasSemana = listOf(
            "Lunes", "Martes", "Miércoles", "Jueves",
            "Viernes", "Sábado", "Domingo", "Todos"
        )

        diasSemana.forEach { dia ->
            val chip = Chip(this).apply {
                text = dia
                isCheckable = true
                isClickable = true
                chipBackgroundColor = getColorStateList(R.color.chip_background_color)
                setTextColor(getColorStateList(R.color.chip_text_color))
            }

            chip.setOnClickListener {
                filtrarPorDia(dia)
            }

            chipGroupDiasFiltro.addView(chip)
        }

        // Seleccionar "Todos" por defecto
        (chipGroupDiasFiltro.getChildAt(diasSemana.size - 1) as? Chip)?.isChecked = true
    }

    /**
     * NUEVO: Filtra las disponibilidades por día de la semana - VERSIÓN ACTUALIZADA
     */
    private fun filtrarPorDia(diaSeleccionado: String) {
        val currentUser = auth.currentUser ?: return

        db.collection("medicos").document(currentUser.uid).get()
            .addOnSuccessListener { medicoDoc ->
                val idMedico = medicoDoc.getLong("ID_MEDICO")
                if (idMedico == null) {
                    Toast.makeText(this, "Error: ID médico no encontrado", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Limpiar la vista actual
                chipGroupFechas.removeAllViews()

                if (diaSeleccionado == "Todos") {
                    // Mostrar todas las disponibilidades
                    loadDisponibilidadesFromFirestore(idMedico)
                } else {
                    // Filtrar por día específico
                    db.collection("disponibilidad")
                        .whereEqualTo("ID_MEDICO", idMedico)
                        .whereEqualTo("DIA_SEMANA", diaSeleccionado)
                        .get()
                        .addOnSuccessListener { documents ->
                            if (documents.isEmpty) {
                                Toast.makeText(this, "No hay disponibilidades para $diaSeleccionado", Toast.LENGTH_SHORT).show()
                            } else {
                                for (document in documents) {
                                    val fecha = document.getString("FECHA")
                                    val horarios = document.get("HORA")
                                    val docId = document.id

                                    if (fecha != null && horarios != null) {
                                        when (horarios) {
                                            is List<*> -> {
                                                // Nueva estructura: array de horarios
                                                val listaHorarios = horarios.filterIsInstance<String>()
                                                agregarDisponibilidadUI(fecha, listaHorarios, docId)
                                            }
                                            is String -> {
                                                // Estructura antigua: horario único
                                                val listaHorarios = listOf(horarios)
                                                agregarDisponibilidadUI(fecha, listaHorarios, docId)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("ProfileActivity", "Error al filtrar por día: ${e.message}")
                            Toast.makeText(this, "Error al filtrar disponibilidades", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }
    /**
     * NUEVO: Muestra primero el calendario para seleccionar fecha y luego el selector de múltiples horarios
     */
    private fun showCalendarWithTimePicker() {
        val userId = auth.currentUser?.uid ?: return

        // Obtener el ID_MEDICO numérico
        db.collection("medicos").document(userId).get()
            .addOnSuccessListener { medicoDoc ->
                val idMedico = medicoDoc.getLong("ID_MEDICO")
                if (idMedico == null) {
                    Toast.makeText(this, "Error: ID médico no encontrado", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val builder = MaterialDatePicker.Builder.dateRangePicker().setTitleText("Seleccionar fechas de atención").setTheme(R.style.CustomMaterialDatePickerTheme)
                val picker = builder.build()
                picker.show(supportFragmentManager, picker.toString())
                picker.addOnPositiveButtonClickListener { selection ->
                    val startDate = selection.first
                    val endDate = selection.second

                    // 🔹 NUEVO FORMATO: YYYY/MM/DD
                    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                    val datesList = mutableListOf<String>()
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = startDate
                    val endCal = Calendar.getInstance(); endCal.timeInMillis = endDate

                    while (!cal.after(endCal)) {
                        datesList.add(sdf.format(cal.time))
                        cal.add(Calendar.DAY_OF_MONTH,1)
                    }

                    // Después de seleccionar las fechas, mostrar el selector de múltiples horarios
                    showMultipleTimePickerForDates(idMedico, datesList)
                }
            }
    }
    /**
     * NUEVO: Muestra el selector para agregar múltiples horarios para las fechas seleccionadas
     */
    private fun showMultipleTimePickerForDates(idMedico: Long, datesList: List<String>) {
        val horariosList = mutableListOf<String>()

        // Crear el diálogo programáticamente
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }

        val titleTextView = TextView(this).apply {
            text = "Agregue los horarios de atención:"
            textSize = 16f
            setPadding(0, 0, 0, 30)
        }
        dialogView.addView(titleTextView)

        // Contenedor para los horarios (con ScrollView)
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f // 🔹 CORRECCIÓN: Usar 1.0f
            )
        }

        val containerHorarios = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollView.addView(containerHorarios)
        dialogView.addView(scrollView)

        // Botón para agregar más horarios
        val btnAddMore = Button(this).apply {
            text = "+ Agregar otro horario"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER_HORIZONTAL }
            setOnClickListener {
                agregarCampoHorarioProgramatico(containerHorarios)
            }
        }
        dialogView.addView(btnAddMore)

        // Agregar primer horario por defecto
        agregarCampoHorarioProgramatico(containerHorarios)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Agregar Horarios para las Fechas Seleccionadas")
            .setView(dialogView)
            .setPositiveButton("Guardar Todos") { _, _ ->
                // Recopilar todos los horarios ingresados
                horariosList.clear()
                for (i in 0 until containerHorarios.childCount) {
                    val horarioLayout = containerHorarios.getChildAt(i) as? LinearLayout
                    val timePicker = horarioLayout?.getChildAt(0) as? TimePicker
                    timePicker?.let {
                        val hora = String.format("%02d:%02d:00", it.hour, it.minute)
                        horariosList.add(hora)
                    }
                }

                if (horariosList.isNotEmpty()) {
                    // Guardar cada fecha con todos los horarios
                    saveDisponibilidadCompleta(idMedico, datesList, horariosList)
                } else {
                    Toast.makeText(this, "Debe agregar al menos un horario", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
    }

    /**
     * NUEVO: Agrega un campo de selección de horario al contenedor (programáticamente)
     */
    private fun agregarCampoHorarioProgramatico(container: LinearLayout) {
        val horarioLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 8)
        }

        val timePicker = TimePicker(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f // 🔹 CORRECCIÓN: Usar 1.0f en lugar de 1f
            )
            setIs24HourView(true)
        }
        horarioLayout.addView(timePicker)

        val btnRemove = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                48,
                48
            ).apply { setMargins(16, 0, 0, 0) }
            setImageResource(android.R.drawable.ic_delete)
            setOnClickListener {
                if (container.childCount > 1) {
                    container.removeView(horarioLayout)
                } else {
                    Toast.makeText(this@ProfileActivity, "Debe haber al menos un horario", Toast.LENGTH_SHORT).show()
                }
            }
        }
        horarioLayout.addView(btnRemove)

        container.addView(horarioLayout)
    }


    /**
     * CORREGIDO: Guarda la disponibilidad con horarios ordenados
     */
    private fun saveDisponibilidadCompleta(idMedico: Long, datesList: List<String>, horariosList: List<String>) {
        var totalGuardados = 0
        val totalAguardar = datesList.size

        // 🔹 ORDENAR LOS HORARIOS DE FORMA ASCENDENTE
        val horariosOrdenados = ordenarHorarios(horariosList)

        // 🔹 OBTENER EL ÚLTIMO ID_DISPONIBILIDAD PARA AUTOINCREMENTAR
        db.collection("disponibilidad")
            .orderBy("ID_DISPONIBILIDAD", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { lastDocSnapshot ->
                var ultimoId = 0L

                if (!lastDocSnapshot.isEmpty) {
                    val lastDoc = lastDocSnapshot.documents[0]
                    ultimoId = lastDoc.getLong("ID_DISPONIBILIDAD") ?: 0L
                }

                datesList.forEach { fechaStr ->
                    // 🔹 INCREMENTAR EL ID PARA CADA NUEVA DISPONIBILIDAD
                    val nuevoId = ultimoId + 1
                    ultimoId = nuevoId

                    // 🔹 PRIMERO VERIFICAR SI YA EXISTE UNA DISPONIBILIDAD PARA ESTA FECHA
                    db.collection("disponibilidad")
                        .whereEqualTo("ID_MEDICO", idMedico)
                        .whereEqualTo("FECHA", fechaStr)
                        .get()
                        .addOnSuccessListener { existingDocs ->
                            if (existingDocs.isEmpty) {
                                // 🔹 NO EXISTE: Crear nuevo documento con ID_DISPONIBILIDAD
                                val disponibilidadData = hashMapOf(
                                    "ID_DISPONIBILIDAD" to nuevoId,
                                    "ID_MEDICO" to idMedico,
                                    "FECHA" to fechaStr,
                                    "HORA" to horariosOrdenados, // 🔹 USAR HORARIOS ORDENADOS
                                    "DIA_SEMANA" to getDiaSemanaFromDate(fechaStr),
                                    "DISPONIBLE" to true,
                                    "RESERVADO_AT" to null,
                                    "RESERVADO_POR" to null
                                )

                                db.collection("disponibilidad").add(disponibilidadData)
                                    .addOnSuccessListener { documentReference ->
                                        totalGuardados++
                                        agregarDisponibilidadUI(fechaStr, horariosOrdenados, documentReference.id)

                                        if (totalGuardados == totalAguardar) {
                                            Toast.makeText(this, "$totalGuardados disponibilidades guardadas correctamente", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Error al guardar disponibilidad: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                // 🔹 YA EXISTE: Actualizar el documento existente
                                val existingDoc = existingDocs.documents[0]
                                val existingHorarios = existingDoc.get("HORA") as? List<String> ?: emptyList()

                                // Combinar horarios existentes con los nuevos y ORDENAR
                                val horariosCombinados = (existingHorarios + horariosList).distinct()
                                val horariosCombinadosOrdenados = ordenarHorarios(horariosCombinados)

                                existingDoc.reference.update("HORA", horariosCombinadosOrdenados)
                                    .addOnSuccessListener {
                                        totalGuardados++
                                        // Actualizar la vista
                                        actualizarDisponibilidadUI(fechaStr, horariosCombinadosOrdenados, existingDoc.id)

                                        if (totalGuardados == totalAguardar) {
                                            Toast.makeText(this, "$totalGuardados disponibilidades actualizadas correctamente", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Error al actualizar disponibilidad: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error al verificar disponibilidad existente: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al obtener último ID: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    /**
     * NUEVO: Función para ordenar horarios de forma ascendente
     */
    private fun ordenarHorarios(horarios: List<String>): List<String> {
        return horarios.sortedWith(compareBy { horario ->
            // Convertir "HH:mm:ss" a minutos totales para ordenar
            val partes = horario.split(":")
            val horas = partes[0].toInt()
            val minutos = partes[1].toInt()
            horas * 60 + minutos
        })
    }
    /**
     * MODIFICADO: Función para migrar documentos existentes - AHORA ORDENA HORARIOS
     */
    private fun migrarIDsDisponibilidad() {
        db.collection("disponibilidad")
            .whereEqualTo("ID_DISPONIBILIDAD", null)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("Migration", "No hay documentos para migrar")
                    return@addOnSuccessListener
                }

                // Obtener el último ID_DISPONIBILIDAD
                db.collection("disponibilidad")
                    .orderBy("ID_DISPONIBILIDAD", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { lastDocSnapshot ->
                        var ultimoId = 0L

                        if (!lastDocSnapshot.isEmpty) {
                            val lastDoc = lastDocSnapshot.documents[0]
                            ultimoId = lastDoc.getLong("ID_DISPONIBILIDAD") ?: 0L
                        }

                        val batch = db.batch()
                        documents.documents.forEachIndexed { index, doc ->
                            val nuevoId = ultimoId + index + 1
                            val docRef = db.collection("disponibilidad").document(doc.id)

                            // 🔹 ORDENAR HORARIOS EXISTENTES DURANTE LA MIGRACIÓN
                            val horarios = doc.get("HORA")
                            if (horarios is List<*>) {
                                val listaHorarios = horarios.filterIsInstance<String>()
                                val horariosOrdenados = ordenarHorarios(listaHorarios)
                                batch.update(docRef,
                                    "ID_DISPONIBILIDAD", nuevoId,
                                    "HORA", horariosOrdenados
                                )
                            } else {
                                batch.update(docRef, "ID_DISPONIBILIDAD", nuevoId)
                            }
                        }

                        batch.commit()
                            .addOnSuccessListener {
                                Log.d("Migration", "${documents.size()} documentos migrados y horarios ordenados")
                            }
                            .addOnFailureListener { e ->
                                Log.e("Migration", "Error en migración: ${e.message}")
                            }
                    }
            }
            .addOnFailureListener { e ->
                Log.e("Migration", "Error al obtener documentos para migrar: ${e.message}")
            }
    }


    /**
     * MODIFICADO: Actualizar disponibilidad en la UI - SIN MOSTRAR ID
     */
    private fun actualizarDisponibilidadUI(fecha: String, horarios: List<String>, docId: String) {
        // Buscar y actualizar el chip existente
        for (i in 0 until chipGroupFechas.childCount) {
            val chip = chipGroupFechas.getChildAt(i) as? Chip
            if (chip?.text?.contains(fecha) == true) {
                val horariosTexto = if (horarios.size == 1) {
                    horarios[0]
                } else {
                    "${horarios.size} horarios: ${horarios.joinToString(", ")}"
                }
                chip.text = "$fecha - $horariosTexto" // 🔹 SIN ID
                break
            }
        }
    }

    /**
     * SOBRECARGA: Para mantener compatibilidad con código existente
     */
    private fun saveDisponibilidadCompleta(idMedico: Long, datesList: List<String>, horarioUnico: String) {
        saveDisponibilidadCompleta(idMedico, datesList, listOf(horarioUnico))
    }

    /**
     * MODIFICADO: Agrega la disponibilidad a la interfaz de usuario - SIN MOSTRAR ID
     */
    private fun agregarDisponibilidadUI(fecha: String, horarios: List<String>, docId: String, idDisponibilidad: Long? = null) {
        val horariosTexto = if (horarios.size == 1) {
            horarios[0]
        } else {
            "${horarios.size} horarios: ${horarios.joinToString(", ")}"
        }

        val chip = Chip(this).apply {
            text = "$fecha - $horariosTexto" // 🔹 QUITAMOS EL ID DEL TEXTO VISUAL
            isCloseIconVisible = true
            chipBackgroundColor = getColorStateList(R.color.disponibilidad_chip_color)
            setTextColor(getColorStateList(R.color.disponibilidad_text_color))
        }

        chip.setOnClickListener {
            val mensaje = StringBuilder()
            mensaje.append("Fecha: $fecha\n")
            mensaje.append("Horarios:\n${horarios.joinToString("\n") { "• $it" }}")

            AlertDialog.Builder(this)
                .setTitle("Disponibilidad")
                .setMessage(mensaje.toString())
                .setPositiveButton("Editar") { _, _ ->
                    editarDisponibilidad(fecha, horarios, docId, chip)
                }
                .setNeutralButton("Eliminar") { _, _ ->
                    eliminarDisponibilidad(docId, chip)
                }
                .setNegativeButton("Cerrar", null)
                .show()
        }

        chipGroupFechas.addView(chip)
    }


    /**
     * SOBRECARGA: Para compatibilidad con código existente
     */
    private fun agregarDisponibilidadUI(fecha: String, horarios: List<String>, docId: String) {
        agregarDisponibilidadUI(fecha, horarios, docId, null)
    }

    /**
     * SOBRECARGA: Para compatibilidad con código que usa horario único
     */
    private fun agregarDisponibilidadUI(fecha: String, horario: String, docId: String) {
        agregarDisponibilidadUI(fecha, listOf(horario), docId, null)
    }

    /**
     * NUEVO: Función para verificar la estructura actual de los datos en Firestore
     */
    private fun debugFirestoreStructure() {
        val currentUser = auth.currentUser ?: return

        db.collection("medicos").document(currentUser.uid).get()
            .addOnSuccessListener { medicoDoc ->
                val idMedico = medicoDoc.getLong("ID_MEDICO")
                if (idMedico != null) {
                    db.collection("disponibilidad")
                        .whereEqualTo("ID_MEDICO", idMedico)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { documents ->
                            if (!documents.isEmpty) {
                                val doc = documents.documents[0]
                                val horarios = doc.get("HORA")
                                Log.d("FirestoreDebug", "Estructura actual:")
                                Log.d("FirestoreDebug", "HORA field: $horarios")
                                Log.d("FirestoreDebug", "HORA type: ${horarios?.let { it::class.java.simpleName }}")

                                if (horarios is List<*>) {
                                    Log.d("FirestoreDebug", "HORA es un array con ${horarios.size} elementos:")
                                    horarios.forEachIndexed { index, hora ->
                                        Log.d("FirestoreDebug", "  [$index] = $hora")
                                    }
                                }
                            }
                        }
                }
            }
    }
    /**
     * MODIFICADO: Editar disponibilidad existente - ahora maneja lista de horarios
     */
    private fun editarDisponibilidad(fechaActual: String, horariosActuales: List<String>, docId: String, chip: Chip) {
        // Primero preguntar si quiere cambiar la fecha o los horarios
        val options = arrayOf("Cambiar Fecha", "Cambiar Horarios", "Cambiar Ambos")

        AlertDialog.Builder(this)
            .setTitle("¿Qué deseas editar?")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> cambiarSoloFecha(docId, chip, fechaActual, horariosActuales)
                    1 -> cambiarSoloHorarios(docId, chip, fechaActual, horariosActuales)
                    2 -> cambiarAmbos(docId, chip, fechaActual, horariosActuales)
                }
            }
            .show()
    }
    /**
     * SOBRECARGA: Para compatibilidad con código que usa horario único
     */
    private fun editarDisponibilidad(fechaActual: String, horarioActual: String, docId: String, chip: Chip) {
        editarDisponibilidad(fechaActual, listOf(horarioActual), docId, chip)
    }
    /**
     * MODIFICADO: Cambiar solo la fecha (mantener horarios)
     */
    private fun cambiarSoloFecha(docId: String, chip: Chip, fechaActual: String, horariosActuales: List<String>) {
        val builder = MaterialDatePicker.Builder.datePicker().setTitleText("Seleccionar nueva fecha")
        val picker = builder.build()
        picker.show(supportFragmentManager, picker.toString())
        picker.addOnPositiveButtonClickListener { selectedDate ->
            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            val nuevaFecha = sdf.format(Date(selectedDate))

            db.collection("disponibilidad").document(docId)
                .update(
                    "FECHA", nuevaFecha,
                    "DIA_SEMANA", getDiaSemanaFromDate(nuevaFecha)
                )
                .addOnSuccessListener {
                    val horariosTexto = if (horariosActuales.size == 1) {
                        horariosActuales[0]
                    } else {
                        "${horariosActuales.size} horarios: ${horariosActuales.joinToString(", ")}"
                    }
                    chip.text = "$nuevaFecha - $horariosTexto"
                    Toast.makeText(this, "Fecha actualizada", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al actualizar fecha: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
    /**
     * SOBRECARGA: Para compatibilidad con código que usa horario único
     */
    private fun cambiarSoloFecha(docId: String, chip: Chip, fechaActual: String, horarioActual: String) {
        cambiarSoloFecha(docId, chip, fechaActual, listOf(horarioActual))
    }

    /**
     * MODIFICADO: Cambiar solo los horarios de una disponibilidad existente - AHORA ORDENA
     */
    private fun cambiarSoloHorarios(docId: String, chip: Chip, fechaActual: String, horariosActuales: List<String>) {
        val horariosList = mutableListOf<String>()

        // Crear el diálogo programáticamente
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }

        val titleTextView = TextView(this).apply {
            text = "Editar horarios de atención:"
            textSize = 16f
            setPadding(0, 0, 0, 30)
        }
        dialogView.addView(titleTextView)

        // Contenedor para los horarios (con ScrollView)
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
        }

        val containerHorarios = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollView.addView(containerHorarios)
        dialogView.addView(scrollView)

        // Botón para agregar más horarios
        val btnAddMore = Button(this).apply {
            text = "+ Agregar otro horario"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER_HORIZONTAL }
            setOnClickListener {
                agregarCampoHorarioProgramatico(containerHorarios)
            }
        }
        dialogView.addView(btnAddMore)

        // 🔹 AGREGAR HORARIOS ACTUALES ORDENADOS
        val horariosOrdenados = ordenarHorarios(horariosActuales)
        horariosOrdenados.forEach { horario ->
            agregarCampoHorarioConValor(containerHorarios, horario)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Editar Horarios")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                // Recopilar todos los horarios ingresados
                horariosList.clear()
                for (i in 0 until containerHorarios.childCount) {
                    val horarioLayout = containerHorarios.getChildAt(i) as? LinearLayout
                    val timePicker = horarioLayout?.getChildAt(0) as? TimePicker
                    timePicker?.let {
                        val hora = String.format("%02d:%02d:00", it.hour, it.minute)
                        horariosList.add(hora)
                    }
                }

                if (horariosList.isNotEmpty()) {
                    // 🔹 ORDENAR LOS HORARIOS ANTES DE GUARDAR
                    val horariosOrdenados = ordenarHorarios(horariosList)

                    // Actualizar solo los horarios
                    db.collection("disponibilidad").document(docId)
                        .update("HORA", horariosOrdenados)
                        .addOnSuccessListener {
                            val horariosTexto = if (horariosOrdenados.size == 1) {
                                horariosOrdenados[0]
                            } else {
                                "${horariosOrdenados.size} horarios: ${horariosOrdenados.joinToString(", ")}"
                            }
                            chip.text = "$fechaActual - $horariosTexto"
                            Toast.makeText(this, "Horarios actualizados y ordenados", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error al actualizar horarios: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Debe agregar al menos un horario", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
    }

    /**
     * MODIFICADO: Cambiar ambos - fecha y horarios
     */
    private fun cambiarAmbos(docId: String, chip: Chip, fechaActual: String, horariosActuales: List<String>) {
        // Primero cambiar fecha
        val builder = MaterialDatePicker.Builder.datePicker().setTitleText("Seleccionar nueva fecha")
        val picker = builder.build()
        picker.show(supportFragmentManager, picker.toString())
        picker.addOnPositiveButtonClickListener { selectedDate ->
            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            val nuevaFecha = sdf.format(Date(selectedDate))

            // Luego cambiar horarios
            cambiarSoloHorarios(docId, chip, nuevaFecha, horariosActuales)
        }
    }

    /**
     * SOBRECARGA: Para compatibilidad con código que usa horario único
     */
    private fun cambiarAmbos(docId: String, chip: Chip, fechaActual: String, horarioActual: String) {
        cambiarAmbos(docId, chip, fechaActual, listOf(horarioActual))
    }
    /**
     * NUEVO: Agrega un campo de horario con valor predefinido
     */
    private fun agregarCampoHorarioConValor(container: LinearLayout, horario: String) {
        val horarioLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 8)
        }

        val timeParts = horario.split(":")
        val hourValue = timeParts[0].toInt() // 🔹 CAMBIAR nombre para evitar conflicto
        val minuteValue = timeParts[1].toInt() // 🔹 CAMBIAR nombre para evitar conflicto

        val timePicker = TimePicker(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            )
            setIs24HourView(true)
            // 🔹 CORRECCIÓN: Usar currentHour y currentMinute en lugar de hour y minute
            currentHour = hourValue
            currentMinute = minuteValue
        }
        horarioLayout.addView(timePicker)

        val btnRemove = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                48,
                48
            ).apply { setMargins(16, 0, 0, 0) }
            setImageResource(android.R.drawable.ic_delete)
            setOnClickListener {
                if (container.childCount > 1) {
                    container.removeView(horarioLayout)
                } else {
                    Toast.makeText(this@ProfileActivity, "Debe haber al menos un horario", Toast.LENGTH_SHORT).show()
                }
            }
        }
        horarioLayout.addView(btnRemove)

        container.addView(horarioLayout)
    }

    /**
     * SOBRECARGA: Para compatibilidad con código que usa horario único
     * 🔹 CAMBIAR NOMBRE para evitar conflicto
     */
    private fun cambiarSoloHorarioUnico(docId: String, chip: Chip, fechaActual: String, horarioActual: String) {
        cambiarSoloHorarios(docId, chip, fechaActual, listOf(horarioActual))
    }

    /**
     * SOBRECARGA: Para compatibilidad con código que usa horario único
     * 🔹 CAMBIAR NOMBRE para evitar conflicto
     */
    private fun cambiarAmbosUnico(docId: String, chip: Chip, fechaActual: String, horarioActual: String) {
        cambiarAmbos(docId, chip, fechaActual, listOf(horarioActual))
    }
    /**
     * FUNCIÓN AUXILIAR: Convierte fecha de DD/MM/YYYY a YYYY/MM/DD
     */
    private fun convertirFechaFormato(fechaVieja: String): String {
        return try {
            val sdfViejo = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val sdfNuevo = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            val date = sdfViejo.parse(fechaVieja)
            sdfNuevo.format(date)
        } catch (e: Exception) {
            fechaVieja // Si falla, devolver la fecha original
        }
    }

    /**
     * FUNCIÓN AUXILIAR: Convierte horario de rango a horario único
     */
    private fun convertirHorarioFormato(horarioViejo: String): String {
        return try {
            // Si es un rango "HH:MM - HH:MM", tomar solo la hora de inicio
            if (horarioViejo.contains(" - ")) {
                val horaInicio = horarioViejo.split(" - ")[0]
                "$horaInicio:00" // Agregar segundos
            } else {
                // Si ya es un horario único, asegurar que tenga segundos
                if (horarioViejo.count { it == ':' } == 1) {
                    "$horarioViejo:00"
                } else {
                    horarioViejo
                }
            }
        } catch (e: Exception) {
            "00:00:00" // Valor por defecto si falla
        }
    }
    /**
     * MODIFICADO: Cambiar solo el horario - versión simplificada para horario único
     */
    private fun cambiarSoloHorario(docId: String, chip: Chip, fechaActual: String, horarioActual: String) {
        val calendar = Calendar.getInstance()

        // Parsear el horario actual para pre-seleccionar en el TimePicker
        val timeParts = horarioActual.split(":")
        val currentHour = timeParts[0].toInt()
        val currentMinute = timeParts[1].toInt()

        // 🔹 MODIFICADO: Solo un TimePicker para horario único
        val timePicker = TimePickerDialog(this, { _, hour, minute ->
            // 🔹 NUEVO FORMATO: HH:mm:ss
            val nuevoHorario = String.format("%02d:%02d:00", hour, minute)

            // Actualizar solo el horario
            db.collection("disponibilidad").document(docId)
                .update("HORA", nuevoHorario)
                .addOnSuccessListener {
                    chip.text = "$fechaActual - $nuevoHorario"
                    Toast.makeText(this, "Horario actualizado", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al actualizar horario: ${e.message}", Toast.LENGTH_SHORT).show()
                }

        }, currentHour, currentMinute, true)

        timePicker.setTitle("Seleccionar nuevo horario")
        timePicker.show()
    }

    /**
     * NUEVO: Eliminar disponibilidad
     */
    private fun eliminarDisponibilidad(docId: String, chip: Chip) {
        db.collection("disponibilidad").document(docId)
            .delete()
            .addOnSuccessListener {
                chipGroupFechas.removeView(chip)
                Toast.makeText(this, "Disponibilidad eliminada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar disponibilidad: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * MODIFICADO: Carga todas las disponibilidades desde Firestore
     */
    private fun loadProfileData() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        db.collection("medicos").document(userId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(this, "Datos no encontrados.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val doctorData = doc.data ?: return@addOnSuccessListener
                val name = doctorData["NOMBRE"] as? String ?: "N/A"
                val lastName = doctorData["APELLIDO"] as? String ?: ""
                val edad = doctorData["EDAD"]?.toString() ?: "N/A"
                val telefono = doctorData["CELULAR"] as? String ?: "N/A"

                val idEspecialidad = doctorData["ID_ESPECIALIDAD"] as? Long
                val idUniversidad = doctorData["ID_UNIVERSIDAD"] as? Long
                val idDistrito = doctorData["ID_DISTRITO"] as? Long
                val idNacionalidad = doctorData["ID_NACIONALIDAD"] as? Long
                val expAnios = doctorData["EXP_ANIOS"]?.toString() ?: "N/A"
                val additionalInfo = doctorData["INFO_ADIC"] as? String ?: "N/A"
                val photoUrl = doctorData["FOTO_PERFIL"] as? String
                val idMedico = doctorData["ID_MEDICO"] as? Long

                // 🔹 Mostrar datos básicos
                findViewById<TextView>(R.id.tvProfileName).text = "$name $lastName"
                findViewById<TextView>(R.id.tvAge).text = "$edad años"
                findViewById<EditText>(R.id.etContactNumber).setText(telefono)
                findViewById<TextView>(R.id.tvExperienceYears).text = expAnios
                findViewById<TextView>(R.id.tvAdditionalInfo).text = additionalInfo

                // 🔹 Cargar disponibilidades completas
                if (idMedico != null) {
                    loadDisponibilidadesFromFirestore(idMedico)
                }

                // 🔹 Cargar imagen de perfil
                if (!photoUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(photoUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(ivProfilePicture)
                } else {
                    ivProfilePicture.setImageResource(R.drawable.ic_profile)
                }

                // 🔹 Cargar nombres asociados a los IDs ------------------------

                // Universidad
                if (idUniversidad != null) {
                    db.collection("universidad")
                        .whereEqualTo("ID_UNIVERSIDAD", idUniversidad)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { docs ->
                            val nombreUniversidad = docs.firstOrNull()?.getString("NOMBRE_UNIVERSIDAD") ?: "N/A"
                            actvUniversity.setText(nombreUniversidad)
                        }
                }

                // Distrito
                if (idDistrito != null) {
                    db.collection("distrito")
                        .whereEqualTo("ID_DISTRITO", idDistrito)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { docs ->
                            val nombreDistrito = docs.firstOrNull()?.getString("NOMBRE_DISTRITO") ?: "N/A"
                            actvDistrito.setText(nombreDistrito)
                        }
                }

                // Especialidad
                if (idEspecialidad != null) {
                    db.collection("especialidad")
                        .whereEqualTo("ID_ESPECIALIDAD", idEspecialidad)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { docs ->
                            val nombreEspecialidad = docs.firstOrNull()?.getString("ESPECIALIDAD") ?: "N/A"
                            actvSpecialty.setText(nombreEspecialidad)
                        }
                }

                // Nacionalidad
                if (idNacionalidad != null) {
                    db.collection("nacionalidad")
                        .whereEqualTo("ID_NACIONALIDAD", idNacionalidad)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { docs ->
                            val nombreNacionalidad = docs.firstOrNull()?.getString("NACIONALIDAD") ?: "N/A"
                            findViewById<TextView>(R.id.tvNacionalidad).text = nombreNacionalidad
                        }
                }

                // Hospital (usa tabla intermedia doctor_hospital)
                if (idMedico != null) {
                    db.collection("doctor_hospital")
                        .whereEqualTo("ID_MEDICO", idMedico)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { doctorHospDocs ->
                            val idHospital = doctorHospDocs.firstOrNull()?.getLong("ID_HOSPITAL")
                            if (idHospital != null) {
                                db.collection("hospital")
                                    .whereEqualTo("ID_HOSPITAL", idHospital)
                                    .limit(1)
                                    .get()
                                    .addOnSuccessListener { hospDocs ->
                                        val nombreHospital = hospDocs.firstOrNull()?.getString("NOMBRE_HOSPITAL") ?: "N/A"
                                        actvHospital.setText(nombreHospital)
                                    }
                            }
                        }
                }
                // -------------------------------------------------------------

            }
            .addOnFailureListener { e ->
                Log.e("ProfileActivity", "Error al cargar datos del perfil", e)
                Toast.makeText(this, "Error al cargar datos del perfil.", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * MODIFICADO: Carga las disponibilidades desde Firestore - ORDENA HORARIOS AL CARGAR
     */
    private fun loadDisponibilidadesFromFirestore(idMedico: Long) {
        chipGroupFechas.removeAllViews()

        db.collection("disponibilidad")
            .whereEqualTo("ID_MEDICO", idMedico)
            .get()
            .addOnSuccessListener { documents ->
                // 🔹 VERIFICAR SI HAY DOCUMENTOS SIN ID_DISPONIBILIDAD Y MIGRARLOS
                val documentosSinID = documents.documents.filter {
                    it.getLong("ID_DISPONIBILIDAD") == null
                }

                if (documentosSinID.isNotEmpty()) {
                    migrarIDsDisponibilidad()
                }

                for (document in documents) {
                    val fecha = document.getString("FECHA")
                    val horarios = document.get("HORA")
                    val idDisponibilidad = document.getLong("ID_DISPONIBILIDAD")
                    val docId = document.id

                    if (fecha != null && horarios != null) {
                        when (horarios) {
                            is List<*> -> {
                                // Nueva estructura: array de horarios - ORDENAR AL CARGAR
                                val listaHorarios = horarios.filterIsInstance<String>()
                                val horariosOrdenados = ordenarHorarios(listaHorarios)
                                agregarDisponibilidadUI(fecha, horariosOrdenados, docId, idDisponibilidad)
                            }
                            is String -> {
                                // Estructura antigua: horario único
                                val listaHorarios = listOf(horarios)
                                agregarDisponibilidadUI(fecha, listaHorarios, docId, idDisponibilidad)
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileActivity", "Error al cargar disponibilidades", e)
            }
    }

    /**
     * MODIFICADO: Obtiene el día de la semana a partir de una fecha en formato YYYY/MM/DD
     */
    private fun getDiaSemanaFromDate(dateStr: String): String {
        return try {
            // 🔹 CAMBIADO: Ahora espera formato YYYY/MM/DD
            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            val date = sdf.parse(dateStr)
            val cal = Calendar.getInstance()
            cal.time = date
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Lunes"
                Calendar.TUESDAY -> "Martes"
                Calendar.WEDNESDAY -> "Miércoles"
                Calendar.THURSDAY -> "Jueves"
                Calendar.FRIDAY -> "Viernes"
                Calendar.SATURDAY -> "Sábado"
                Calendar.SUNDAY -> "Domingo"
                else -> "Desconocido"
            }
        } catch (e: Exception) {
            "Desconocido"
        }
    }

    /**
     * Función auxiliar para cargar el nombre del hospital desde su ID
     */
    private fun loadHospitalName(idHospital: Long, callback: (String?) -> Unit) {
        db.collection("hospital")
            .whereEqualTo("ID_HOSPITAL", idHospital)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                val hospitalName = documents.documents.firstOrNull()?.getString("NOMBRE_HOSPITAL")
                callback(hospitalName)
            }
            .addOnFailureListener { e ->
                Log.e("ProfileActivity", "Error al cargar nombre del hospital", e)
                callback(null)
            }
    }

    /**
     * Verifica los permisos de lectura de almacenamiento antes de abrir la galería.
     */
    private fun checkAndOpenGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    PERMISSION_REQUEST_CODE
                )
            }
        } else {
            openGallery()
        }
    }

    /**
     * Abre la actividad de la galería para seleccionar una imagen.
     */
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    /**
     * Maneja el resultado de la solicitud de permisos.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                Toast.makeText(this, "Permiso de almacenamiento denegado. No se puede seleccionar la foto.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 🔧 Funciones de edición (sin cambios)
    private fun editField(fieldKey: String, currentValue: String, textView: TextView, suffix: String = "") {
        val editText = EditText(this)
        editText.setText(currentValue)

        AlertDialog.Builder(this)
            .setTitle("Editar ${fieldKey.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}")
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                val newValue = editText.text.toString()
                val userId = auth.currentUser?.uid ?: return@setPositiveButton

                db.collection("medicos").document(userId)
                    .update(fieldKey, newValue)
                    .addOnSuccessListener {
                        textView.text = "$newValue$suffix"
                        Toast.makeText(this, "Actualizado correctamente", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun editField(fieldKey: String, currentValue: String, editTextField: EditText, suffix: String = "") {
        val editText = EditText(this)
        editText.setText(currentValue)

        AlertDialog.Builder(this)
            .setTitle("Editar ${fieldKey.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}")
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                val newValue = editText.text.toString()
                val userId = auth.currentUser?.uid ?: return@setPositiveButton

                db.collection("medicos").document(userId)
                    .update(fieldKey, newValue)
                    .addOnSuccessListener {
                        editTextField.setText("$newValue$suffix")
                        Toast.makeText(this, "Actualizado correctamente", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun editFieldWithAutoCreate(
        collectionName: String,
        idFieldName: String,
        nameFieldName: String,
        currentName: String,
        textView: TextView,
        medicoFieldKey: String
    ) {
        val editText = EditText(this)
        editText.setText(currentName)

        val synonyms = mapOf(
            "peru" to "peruana",
            "perú" to "peruana",
            "peruana" to "peruana",
            "mexico" to "mexicana",
            "méxico" to "mexicana",
            "mexicana" to "mexicana",
            "argentina" to "argentina",
            "argentino" to "argentina",
            "chile" to "chilena",
            "chilena" to "chilena",
            "colombia" to "colombiana",
            "colombiana" to "colombiana",
            "espana" to "espanola",
            "españa" to "espanola",
            "española" to "espanola",
            "estados unidos" to "estadounidense",
            "eeuu" to "estadounidense",
            "estadounidense" to "estadounidense"
        )

        AlertDialog.Builder(this)
            .setTitle("Editar ${nameFieldName.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}")
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                val newNameRaw = editText.text.toString().trim()
                val userId = auth.currentUser?.uid ?: return@setPositiveButton

                if (newNameRaw.isEmpty()) {
                    Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                fun normalize(s: String): String {
                    return s.lowercase()
                        .replace("á", "a")
                        .replace("é", "e")
                        .replace("í", "i")
                        .replace("ó", "o")
                        .replace("ú", "u")
                        .replace("ñ", "n")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                }

                val normalizedNew = normalize(newNameRaw)
                val canonicalKey = synonyms[normalizedNew] ?: normalizedNew

                val displayCanonical = if (collectionName == "nacionalidad") {
                    canonicalKey.replaceFirstChar { it.uppercase() }
                } else {
                    newNameRaw.replaceFirstChar { it.uppercase() }
                }

                val collectionRef = db.collection(collectionName)

                collectionRef.get()
                    .addOnSuccessListener { snapshot ->
                        val existingDoc = snapshot.documents.firstOrNull { doc ->
                            val existingName = (doc.getString(nameFieldName) ?: "")
                            val normalizedExisting = normalize(existingName)
                            val existingCanonical = synonyms[normalizedExisting] ?: normalizedExisting
                            existingCanonical == canonicalKey
                        }

                        if (existingDoc != null) {
                            val existingId = existingDoc.getLong(idFieldName)
                            val displayName = existingDoc.getString(nameFieldName) ?: newNameRaw
                            updateMedicoField(userId, medicoFieldKey, existingId, textView, displayName)
                        } else {
                            collectionRef.orderBy(idFieldName, Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .addOnSuccessListener { maxResult ->
                                    val lastId = maxResult.documents.firstOrNull()?.getLong(idFieldName) ?: 0L
                                    val newId = lastId + 1

                                    val displayForSave = displayCanonical

                                    val newDoc = hashMapOf(
                                        idFieldName to newId,
                                        nameFieldName to displayForSave
                                    )

                                    collectionRef.add(newDoc)
                                        .addOnSuccessListener {
                                            updateMedicoField(userId, medicoFieldKey, newId, textView, displayForSave)
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(this, "Error al crear nuevo registro", Toast.LENGTH_SHORT).show()
                                        }
                                }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al verificar existencia", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Versión sobrecargada de updateMedicoField para AutoCompleteTextView
     */
    private fun updateMedicoField(
        userId: String,
        fieldKey: String,
        newId: Long?,
        textView: AutoCompleteTextView,
        newDisplayName: String
    ) {
        if (newId == null) return

        db.collection("medicos").document(userId)
            .update(fieldKey, newId)
            .addOnSuccessListener {
                textView.setText(newDisplayName)
                Toast.makeText(this, "Actualizado correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar médico", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Versión original para TextView
     */
    private fun updateMedicoField(
        userId: String,
        fieldKey: String,
        newId: Long?,
        textView: TextView,
        newDisplayName: String
    ) {
        if (newId == null) return

        db.collection("medicos").document(userId)
            .update(fieldKey, newId)
            .addOnSuccessListener {
                textView.text = newDisplayName
                Toast.makeText(this, "Actualizado correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar médico", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            val imageUri: Uri = data.data!!

            Glide.with(this).load(imageUri).circleCrop().into(ivProfilePicture)
            uploadImageToFirebase(imageUri)
        }
    }

    /**
     * Sube la imagen a Firebase Storage y guarda la URL en Firestore.
     */
    private fun uploadImageToFirebase(imageUri: Uri) {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Error: No hay usuario autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        val storageRef = storage.reference.child("profile_images/$userId.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()

                    db.collection("medicos").document(userId)
                        .update("FOTO_PERFIL", downloadUrl)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Foto de perfil actualizada en la nube.", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error al guardar URL en Firestore.", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileActivity", "Error al subir la imagen a Storage", e)
                Toast.makeText(this, "Fallo al subir la foto.", Toast.LENGTH_LONG).show()
            }
    }

    private fun updatePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser
        if (user == null || user.email.isNullOrEmpty()) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(user.email!!, currentPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Contraseña actualizada correctamente", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al actualizar la contraseña: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Contraseña actual incorrecta", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Nueva función para editar la relación hospital-médico
     */
    private fun editHospitalField() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        db.collection("medicos").document(userId).get()
            .addOnSuccessListener { medicoDoc ->
                val idMedico = medicoDoc.getLong("ID_MEDICO")

                if (idMedico == null) {
                    Toast.makeText(this, "Error: ID médico no encontrado", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                db.collection("doctor_hospital")
                    .whereEqualTo("ID_MEDICO", idMedico)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { hospitalDocs ->
                        val currentHospitalId = hospitalDocs.documents.firstOrNull()?.getLong("ID_HOSPITAL")
                        var currentHospitalName = "N/A"

                        if (currentHospitalId != null) {
                            loadHospitalName(currentHospitalId) { hospitalName ->
                                currentHospitalName = hospitalName ?: "N/A"
                                showHospitalSelectionDialog(idMedico, currentHospitalId, currentHospitalName, actvHospital)
                            }
                        } else {
                            showHospitalSelectionDialog(idMedico, currentHospitalId, currentHospitalName, actvHospital)
                        }
                    }
                    .addOnFailureListener {
                        showHospitalSelectionDialog(idMedico, null, "N/A", actvHospital)
                    }
            }
    }

    /**
     * Diálogo para seleccionar/crear hospital
     */
    private fun showHospitalSelectionDialog(idMedico: Long, currentHospitalId: Long?, currentHospitalName: String, textView: AutoCompleteTextView) {
        val actvDialog = AutoCompleteTextView(this)
        actvDialog.setPadding(50, 30, 50, 30)
        actvDialog.setText(currentHospitalName)
        actvDialog.hint = "Escriba o seleccione un hospital"

        db.collection("hospital")
            .get()
            .addOnSuccessListener { documents ->
                val hospitalNames = documents.mapNotNull { it.getString("NOMBRE_HOSPITAL") }
                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, hospitalNames)
                actvDialog.setAdapter(adapter)
                actvDialog.threshold = 1
            }

        AlertDialog.Builder(this)
            .setTitle("Editar Hospital")
            .setView(actvDialog)
            .setPositiveButton("Guardar") { _, _ ->
                val newHospitalName = actvDialog.text.toString().trim()

                if (newHospitalName.isEmpty()) {
                    Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                editFieldWithAutoCreateForHospital(
                    idMedico = idMedico,
                    currentHospitalId = currentHospitalId,
                    newHospitalName = newHospitalName,
                    textView = textView
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Versión modificada de editFieldWithAutoCreate para hospitales
     */
    private fun editFieldWithAutoCreateForHospital(
        idMedico: Long,
        currentHospitalId: Long?,
        newHospitalName: String,
        textView: AutoCompleteTextView
    ) {
        val collectionName = "hospital"
        val idFieldName = "ID_HOSPITAL"
        val nameFieldName = "NOMBRE_HOSPITAL"

        val collectionRef = db.collection(collectionName)

        collectionRef.whereEqualTo(nameFieldName, newHospitalName).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val existingHospital = snapshot.documents.first()
                    val existingId = existingHospital.getLong(idFieldName)
                    val displayName = existingHospital.getString(nameFieldName) ?: newHospitalName

                    updateDoctorHospitalRelation(idMedico, existingId, displayName, textView)
                } else {
                    collectionRef.orderBy(idFieldName, Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { maxResult ->
                            val lastId = maxResult.documents.firstOrNull()?.getLong(idFieldName) ?: 0L
                            val newId = lastId + 1

                            val newDoc = hashMapOf(
                                idFieldName to newId,
                                nameFieldName to newHospitalName
                            )

                            collectionRef.document().set(newDoc)
                                .addOnSuccessListener {
                                    updateDoctorHospitalRelation(idMedico, newId, newHospitalName, textView)
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Error al crear nuevo hospital", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error al obtener último ID", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al buscar hospital", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Actualiza la relación médico-hospital en doctor_hospital
     */
    private fun updateDoctorHospitalRelation(idMedico: Long, hospitalId: Long?, hospitalName: String, textView: AutoCompleteTextView) {
        if (hospitalId == null) return

        db.collection("doctor_hospital")
            .whereEqualTo("ID_MEDICO", idMedico)
            .get()
            .addOnSuccessListener { existingDocs ->
                val batch = db.batch()
                existingDocs.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }

                val newRelation = hashMapOf(
                    "ID_MEDICO" to idMedico,
                    "ID_HOSPITAL" to hospitalId
                )
                val newDocRef = db.collection("doctor_hospital").document()
                batch.set(newDocRef, newRelation)

                batch.commit()
                    .addOnSuccessListener {
                        textView.setText(hospitalName)
                        Toast.makeText(this, "Hospital actualizado correctamente", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al actualizar relación hospital", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al eliminar relaciones anteriores", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Carga sugerencias de universidades desde Firestore
     */
    private fun loadUniversitySuggestions() {
        db.collection("universidad")
            .get()
            .addOnSuccessListener { documents ->
                val universityNames = documents.mapNotNull { it.getString("NOMBRE_UNIVERSIDAD") }
                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, universityNames)
                actvUniversity.setAdapter(adapter)
            }
            .addOnFailureListener { e ->
                Log.e("ProfileActivity", "Error al cargar universidades: ${e.message}")
            }
    }

    /**
     * Carga sugerencias de hospitales desde Firestore
     */
    private fun loadHospitalSuggestions() {
        db.collection("hospital")
            .get()
            .addOnSuccessListener { documents ->
                val hospitalNames = documents.mapNotNull { it.getString("NOMBRE_HOSPITAL") }
                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, hospitalNames)
                actvHospital.setAdapter(adapter)
            }
            .addOnFailureListener { e ->
                Log.e("ProfileActivity", "Error al cargar hospitales: ${e.message}")
            }
    }

    /**
     * Carga sugerencias de distritos desde Firestore
     */
    private fun loadDistritoSuggestions() {
        db.collection("distrito")
            .get()
            .addOnSuccessListener { documents ->
                val distritoNames = documents.mapNotNull { it.getString("NOMBRE_DISTRITO") }
                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, distritoNames)
                actvDistrito.setAdapter(adapter)
            }
            .addOnFailureListener { e ->
                Log.e("ProfileActivity", "Error al cargar distritos: ${e.message}")
            }
    }

    /**
     * Guarda la universidad seleccionada
     */
    private fun saveUniversitySelection(universityName: String) {
        val userId = auth.currentUser?.uid ?: return
        editFieldWithAutoCreate(
            collectionName = "universidad",
            idFieldName = "ID_UNIVERSIDAD",
            nameFieldName = "NOMBRE_UNIVERSIDAD",
            currentName = universityName,
            textView = actvUniversity,
            medicoFieldKey = "ID_UNIVERSIDAD"
        )
    }

    /**
     * Guarda el hospital seleccionado
     */
    private fun saveHospitalSelection(hospitalName: String) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        db.collection("medicos").document(userId).get()
            .addOnSuccessListener { medicoDoc ->
                val idMedico = medicoDoc.getLong("ID_MEDICO")
                if (idMedico != null) {
                    editFieldWithAutoCreateForHospital(
                        idMedico = idMedico,
                        currentHospitalId = null,
                        newHospitalName = hospitalName,
                        textView = actvHospital
                    )
                }
            }
    }

    /**
     * Guarda el distrito seleccionado
     */
    private fun saveDistritoSelection(distritoName: String) {
        val userId = auth.currentUser?.uid ?: return
        editFieldWithAutoCreate(
            collectionName = "distrito",
            idFieldName = "ID_DISTRITO",
            nameFieldName = "NOMBRE_DISTRITO",
            currentName = distritoName,
            textView = actvDistrito,
            medicoFieldKey = "ID_DISTRITO"
        )
    }

    /**
     * Muestra diálogo para seleccionar/crear universidad con AutoCompleteTextView
     */
    private fun showUniversitySelectionDialog() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val actvDialog = AutoCompleteTextView(this)
        actvDialog.setPadding(50, 30, 50, 30)
        actvDialog.setText(actvUniversity.text.toString())
        actvDialog.hint = "Escriba o seleccione una universidad"

        db.collection("universidad")
            .get()
            .addOnSuccessListener { documents ->
                val universityNames = documents.mapNotNull { it.getString("NOMBRE_UNIVERSIDAD") }
                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, universityNames)
                actvDialog.setAdapter(adapter)
                actvDialog.threshold = 1
            }

        AlertDialog.Builder(this)
            .setTitle("Editar Universidad")
            .setView(actvDialog)
            .setPositiveButton("Guardar") { _, _ ->
                val selectedUniversity = actvDialog.text.toString().trim()

                if (selectedUniversity.isEmpty()) {
                    Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                saveUniversitySelection(selectedUniversity)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Muestra diálogo para seleccionar/crear distrito con AutoCompleteTextView
     */
    private fun showDistritoSelectionDialog() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val actvDialog = AutoCompleteTextView(this)
        actvDialog.setPadding(50, 30, 50, 30)
        actvDialog.setText(actvDistrito.text.toString())
        actvDialog.hint = "Escriba o seleccione un distrito"

        db.collection("distrito")
            .get()
            .addOnSuccessListener { documents ->
                val distritoNames = documents.mapNotNull { it.getString("NOMBRE_DISTRITO") }
                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, distritoNames)
                actvDialog.setAdapter(adapter)
                actvDialog.threshold = 1
            }

        AlertDialog.Builder(this)
            .setTitle("Editar Distrito")
            .setView(actvDialog)
            .setPositiveButton("Guardar") { _, _ ->
                val selectedDistrito = actvDialog.text.toString().trim()

                if (selectedDistrito.isEmpty()) {
                    Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                saveDistritoSelection(selectedDistrito)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Muestra diálogo para seleccionar/crear especialidad con AutoCompleteTextView
     */
    private fun showSpecialtySelectionDialog() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val actvDialog = AutoCompleteTextView(this)
        actvDialog.setPadding(50, 30, 50, 30)
        actvDialog.setText(actvSpecialty.text.toString())
        actvDialog.hint = "Escriba o seleccione una especialidad"

        db.collection("especialidad")
            .get()
            .addOnSuccessListener { documents ->
                val specialtyNames = documents.mapNotNull { it.getString("ESPECIALIDAD") }
                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, specialtyNames)
                actvDialog.setAdapter(adapter)
                actvDialog.threshold = 1
            }

        AlertDialog.Builder(this)
            .setTitle("Editar Especialidad")
            .setView(actvDialog)
            .setPositiveButton("Guardar") { _, _ ->
                val selectedSpecialty = actvDialog.text.toString().trim()

                if (selectedSpecialty.isEmpty()) {
                    Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                saveSpecialtySelection(selectedSpecialty)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Guarda la especialidad seleccionada
     */
    private fun saveSpecialtySelection(specialtyName: String) {
        val userId = auth.currentUser?.uid ?: return
        editFieldWithAutoCreate(
            collectionName = "especialidad",
            idFieldName = "ID_ESPECIALIDAD",
            nameFieldName = "ESPECIALIDAD",
            currentName = specialtyName,
            textView = actvSpecialty,
            medicoFieldKey = "ID_ESPECIALIDAD"
        )
    }

    /**
     * Carga sugerencias de especialidades desde Firestore
     */
    private fun loadSpecialtySuggestions() {
        db.collection("especialidad")
            .get()
            .addOnSuccessListener { documents ->
                val specialtyNames = documents.mapNotNull { it.getString("ESPECIALIDAD") }
                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, specialtyNames)
                actvSpecialty.setAdapter(adapter)
            }
            .addOnFailureListener { e ->
                Log.e("ProfileActivity", "Error al cargar especialidades: ${e.message}")
            }
    }
}