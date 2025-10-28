package com.example.app_capstone

import android.Manifest
import android.app.AlertDialog
import android.app.TimePickerDialog // ⬅️ NUEVO: Para seleccionar la hora
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
import android.view.inputmethod.EditorInfo
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.util.Calendar // ⬅️ NUEVO: Para obtener la hora/fecha actual
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

    // En la parte superior de la clase, después de las otras declaraciones
    private lateinit var actvUniversity: AutoCompleteTextView
    private lateinit var actvHospital: AutoCompleteTextView
    private lateinit var actvDistrito: AutoCompleteTextView
    private lateinit var actvSpecialty: AutoCompleteTextView

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

        // 🔹 NUEVAS REFERENCIAS para AutoCompleteTextView
        actvUniversity = findViewById(R.id.actvUniversity)
        actvHospital = findViewById(R.id.actvHospital)
        actvDistrito = findViewById(R.id.actvDistrito)
        actvSpecialty = findViewById(R.id.actvSpecialty)

        // 🔹 CAMPOS DE HORARIO/FECHAS
        llHorariosContainer = findViewById<LinearLayout>(R.id.llHorariosContainer)
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
        val ivAddHorario = findViewById<ImageView>(R.id.ivAddHorario)
        val ivAddFecha = findViewById<ImageView>(R.id.ivAddFecha)

        // 🔹 Referencia a la imagen de perfil
        ivProfilePicture = findViewById<ImageView>(R.id.ivProfilePicture)

        // 🔹 Cargar la foto desde Firestore/Storage
        loadProfileData() // ⬅️ Llamada principal para cargar datos y foto


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

// Dentro de fun onCreate(...)
// 🔹 Referencias de íconos de Navegación/Información (NUEVO)
        val ivBack = findViewById<ImageView>(R.id.ivBack)
        val ivInfo = findViewById<ImageView>(R.id.ivInfo)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

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
        // ⏰ NUEVO: Listener para el selector de RANGO DE HORA
        // -------------------------------------------------------------
        ivAddHorario.setOnClickListener {
            showTimeRangePickerDialog(llHorariosContainer) // Cambia el TextView por LinearLayout
        }

        ivAddFecha.setOnClickListener {
            showCalendarPicker(chipGroupFechas) // Cambia el TextView por ChipGroup
        }


        // -------------------------------------------------------------
        // 🔹 Funciones autoincrementales (sin cambios)
        // -------------------------------------------------------------

        ivEditUniversity.setOnClickListener {
            showUniversitySelectionDialog() // ✅ MANTIENE EL DIÁLOGO CON SUGERENCIAS
        }

        ivEditHospital.setOnClickListener {
            editHospitalField() // ✅ YA TIENES ESTE (ESTÁ BIEN)
        }

        ivEditSpecialty.setOnClickListener {
            showSpecialtySelectionDialog() // ✅ NUEVO DIÁLOGO CON SUGERENCIAS
        }
        ivEditDistrito.setOnClickListener {
            showDistritoSelectionDialog() // ✅ MANTIENE EL DIÁLOGO CON SUGERENCIAS
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


// -------------------------------------------------------------
// ⬅️ Lógica para el ícono de ATRÁS
// -------------------------------------------------------------
        ivBack.setOnClickListener {
            // Cierra esta actividad y regresa a la actividad anterior en la pila
            finish()
        }

// -------------------------------------------------------------
// ℹ️ Lógica para el ícono de INFORMACIÓN
// -------------------------------------------------------------
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
    }

    /**
     * Muestra dos TimePickerDialogs secuenciales para seleccionar el rango de hora de atención (INICIO y FIN).
     * Actualiza el campo HORARIO_ATENCION en Firestore.
     */

    private fun showTimeRangePickerDialog(container: LinearLayout) {
        val userId = auth.currentUser?.uid ?: return
        val calendar = Calendar.getInstance()
        val startPicker = TimePickerDialog(this, { _, startHour, startMinute ->
            val endPicker = TimePickerDialog(this, { _, endHour, endMinute ->
                if (endHour*60 + endMinute <= startHour*60 + startMinute) {
                    Toast.makeText(this, "La hora de fin debe ser posterior a la de inicio.", Toast.LENGTH_LONG).show()
                    return@TimePickerDialog
                }
                val rangeStr = String.format("%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute)
                db.collection("medicos").document(userId).get()
                    .addOnSuccessListener { doc ->
                        val existingRanges = doc.get("HORARIO_ATENCION") as? MutableList<String> ?: mutableListOf()
                        existingRanges.add(rangeStr)
                        db.collection("medicos").document(userId).update("HORARIO_ATENCION", existingRanges)
                            .addOnSuccessListener {
                                val tv = TextView(this)
                                tv.text = rangeStr
                                tv.setPadding(16,16,16,16)
                                tv.setBackgroundResource(R.drawable.bg_chip_style) // opcional, para que parezca un botón
                                tv.setOnClickListener {
                                    AlertDialog.Builder(this)
                                        .setTitle("Editar o eliminar horario")
                                        .setMessage("¿Deseas editar o eliminar este horario?")
                                        .setPositiveButton("Editar") { _, _ ->
                                            editHorario(rangeStr, tv, container) // ✅ usar 'container', no 'llHorariosContainer'
                                        }
                                        .setNegativeButton("Eliminar") { _, _ ->
                                            val updatedList = existingRanges.toMutableList()
                                            updatedList.remove(rangeStr)
                                            db.collection("medicos").document(userId)
                                                .update("HORARIO_ATENCION", updatedList)
                                            container.removeView(tv)
                                        }
                                        .show()
                                }

                                container.addView(tv)

                            }
                    }
            }, startHour+1, startMinute, true)
            endPicker.setTitle("Seleccionar hora de FIN")
            endPicker.show()
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
        startPicker.setTitle("Seleccionar hora de INICIO")
        startPicker.show()
    }

    private fun editHorario(oldRange: String, textView: TextView, container: LinearLayout) {
        val userId = auth.currentUser?.uid ?: return
        val calendar = Calendar.getInstance()

        val times = oldRange.split(" - ")
        val startParts = times[0].split(":")
        val endParts = times[1].split(":")

        val startPicker = TimePickerDialog(this, { _, startHour, startMinute ->
            val endPicker = TimePickerDialog(this, { _, endHour, endMinute ->
                val newRange = String.format("%02d:%02d - %02d:%02d", startHour, startMinute, endHour, endMinute)
                db.collection("medicos").document(userId).get()
                    .addOnSuccessListener { doc ->
                        val list = doc.get("HORARIO_ATENCION") as? MutableList<String> ?: mutableListOf()
                        val idx = list.indexOf(oldRange)
                        if (idx >= 0) list[idx] = newRange
                        db.collection("medicos").document(userId)
                            .update("HORARIO_ATENCION", list)
                        textView.text = newRange
                    }
            }, endParts[0].toInt(), endParts[1].toInt(), true)
            endPicker.setTitle("Hora de fin")
            endPicker.show()
        }, startParts[0].toInt(), startParts[1].toInt(), true)
        startPicker.setTitle("Hora de inicio")
        startPicker.show()
    }

    /**
     * Muestra un AlertDialog con checkboxes para seleccionar los días de atención.
     * Actualiza el campo DIAS_ATENCION en Firestore como un array de strings.
     */

    /**
     * Carga todos los datos del perfil, incluida la foto de perfil y los nuevos campos de horario/fechas.
     */
    private fun loadProfileData() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // 🔹 Cargar datos del médico
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
                val telefono = doctorData["CONTACTO"] as? String ?: "N/A"

                val idEspecialidad = doctorData["ID_ESPECIALIDAD"] as? Long
                val idUniversidad = doctorData["ID_UNIVERSIDAD"] as? Long
                // ❌ ELIMINADO: val idHospital = doctorData["ID_HOSPITAL"] as? Long
                val idDistrito = doctorData["ID_DISTRITO"] as? Long
                val idNacionalidad = doctorData["ID_NACIONALIDAD"] as? Long
                val expAnios = doctorData["EXP_ANIOS"]?.toString() ?: "N/A"
                val additionalInfo = doctorData["INFO_ADIC"] as? String ?: "N/A"
                val photoUrl = doctorData["FOTO_PERFIL"] as? String
                val idMedico = doctorData["ID_MEDICO"] as? Long // ✅ NECESITAMOS EL ID_NUMÉRICO

                // ⏰ NUEVOS CAMPOS DE HORARIO Y FECHAS
                val horarioList = doctorData["HORARIO_ATENCION"] as? List<String> ?: listOf("N/A")
                val diasList = doctorData["DIAS_ATENCION"] as? List<String> ?: listOf("N/A")

                // 🔹 Lógica para cargar la imagen de perfil
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

                // ✅ NUEVO: Cargar hospital desde doctor_hospital
                val hospitalTask = if (idMedico != null) {
                    db.collection("doctor_hospital")
                        .whereEqualTo("ID_MEDICO", idMedico)
                        .limit(1).get()
                } else {
                    Tasks.forResult(null as QuerySnapshot?)
                }

                val specialtyTask = idEspecialidad?.let {
                    db.collection("especialidad")
                        .whereEqualTo("ID_ESPECIALIDAD", it)
                        .limit(1).get()
                } ?: Tasks.forResult(null as QuerySnapshot?)

                val universityTask = idUniversidad?.let {
                    db.collection("universidad")
                        .whereEqualTo("ID_UNIVERSIDAD", it)
                        .limit(1).get()
                } ?: Tasks.forResult(null as QuerySnapshot?)

                val distritoTask = idDistrito?.let {
                    db.collection("distrito")
                        .whereEqualTo("ID_DISTRITO", it)
                        .limit(1).get()
                } ?: Tasks.forResult(null as QuerySnapshot?)

                val nacionalidadTask = idNacionalidad?.let {
                    db.collection("nacionalidad")
                        .whereEqualTo("ID_NACIONALIDAD", it)
                        .limit(1).get()
                } ?: Tasks.forResult(null as QuerySnapshot?)

                // ✅ AGREGAMOS hospitalTask a la lista de tareas
// ✅ AGREGAMOS hospitalTask a la lista de tareas
                Tasks.whenAllSuccess<Any>(
                    specialtyTask,
                    universityTask,
                    hospitalTask, // ✅ NUEVO: Tarea para hospital
                    distritoTask,
                    nacionalidadTask
                ).addOnSuccessListener { results ->
                    val specialtyName = (results.getOrNull(0) as? QuerySnapshot)
                        ?.documents?.firstOrNull()?.getString("ESPECIALIDAD") ?: "N/A"

                    val universityName = (results.getOrNull(1) as? QuerySnapshot)
                        ?.documents?.firstOrNull()?.getString("NOMBRE_UNIVERSIDAD") ?: "N/A"

                    val distritoName = (results.getOrNull(3) as? QuerySnapshot)
                        ?.documents?.firstOrNull()?.getString("NOMBRE_DISTRITO") ?: "N/A"

                    val nacionalidadName = (results.getOrNull(4) as? QuerySnapshot)
                        ?.documents?.firstOrNull()?.getString("NACIONALIDAD") ?: "N/A"

                    // ✅ NUEVO: Cargar y mostrar el hospital
                    val hospitalResult = results.getOrNull(2) as? QuerySnapshot
                    if (hospitalResult != null && !hospitalResult.isEmpty) {
                        val hospitalDoc = hospitalResult.documents.first()
                        val idHospital = hospitalDoc.getLong("ID_HOSPITAL")
                        if (idHospital != null) {
                            loadHospitalName(idHospital) { hospitalName ->
                                actvHospital.setText(hospitalName ?: "N/A")
                            }
                        } else {
                            actvHospital.setText("N/A")
                        }
                    } else {
                        actvHospital.setText("N/A")
                    }

                    // 🔹 Mostrar datos (USANDO AutoCompleteTextView ahora)
                    findViewById<TextView>(R.id.tvProfileName).text = "$name $lastName"
                    actvSpecialty.setText(specialtyName) // ✅ NUEVO - USAR AutoCompleteTextView                    findViewById<EditText>(R.id.etContactNumber).setText(telefono)
                    findViewById<TextView>(R.id.tvAge).text = "$edad años"
                    actvUniversity.setText(universityName) // ✅ NUEVO
                    findViewById<TextView>(R.id.tvExperienceYears).text = expAnios
                    findViewById<TextView>(R.id.tvAdditionalInfo).text = additionalInfo
                    actvDistrito.setText(distritoName) // ✅ NUEVO
                    findViewById<TextView>(R.id.tvNacionalidad).text = nacionalidadName

                    // ✅ CARGAR SUGERENCIAS para los AutoCompleteTextView
                    loadUniversitySuggestions()
                    loadHospitalSuggestions()
                    loadDistritoSuggestions()
                    loadSpecialtySuggestions()

// ⏰ Mostrar Horario y Fechas (listas)
                    val tvHorario = findViewById<TextView>(R.id.tvHorario)
                    val tvFechas = findViewById<TextView>(R.id.tvFechasAtencion)

// Concatenar horarios con separador " | "
                    llHorariosContainer.removeAllViews()
                    if (horarioList.isNotEmpty() && horarioList[0] != "N/A") {
                        horarioList.forEach { rangeStr ->
                            val tv = TextView(this)
                            tv.text = rangeStr
                            tv.setPadding(16,16,16,16)
                            tv.setBackgroundResource(R.drawable.bg_chip_style)
                            tv.setOnClickListener {
                                AlertDialog.Builder(this)
                                    .setTitle("Editar o eliminar horario")
                                    .setMessage("¿Deseas editar o eliminar este horario?")
                                    .setPositiveButton("Editar") { _, _ ->
                                        editHorario(rangeStr, tv, llHorariosContainer)
                                    }
                                    .setNegativeButton("Eliminar") { _, _ ->
                                        val updatedList = horarioList.toMutableList()
                                        updatedList.remove(rangeStr)
                                        db.collection("medicos").document(userId)
                                            .update("HORARIO_ATENCION", updatedList)
                                        llHorariosContainer.removeView(tv)
                                    }
                                    .show()
                            }
                            llHorariosContainer.addView(tv)
                        }
                    }


                    chipGroupFechas.removeAllViews()
                    if (diasList.isNotEmpty() && diasList[0] != "N/A") {
                        diasList.forEach { dateStr ->
                            val chip = Chip(this)
                            chip.text = dateStr
                            chip.isCloseIconVisible = true
                            chip.setOnClickListener {
                                AlertDialog.Builder(this)
                                    .setTitle("Editar o eliminar fecha")
                                    .setMessage("¿Deseas eliminar esta fecha?")
                                    .setPositiveButton("Eliminar") { _, _ ->
                                        val updatedDates = mutableListOf<String>()
                                        for (i in 0 until chipGroupFechas.childCount) {
                                            val c = chipGroupFechas.getChildAt(i) as Chip
                                            if (c != chip) updatedDates.add(c.text.toString())
                                        }
                                        db.collection("medicos").document(userId)
                                            .update("DIAS_ATENCION", updatedDates)
                                        chipGroupFechas.removeView(chip)

                                    }
                                    .show()
                            }
                            chipGroupFechas.addView(chip)
                        }
                    }



                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileActivity", "Error al cargar datos del perfil", e)
                Toast.makeText(this, "Error al cargar datos del perfil.", Toast.LENGTH_SHORT).show()
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

        // ---- Tabla de equivalencias (sin cambios) ----
        val synonyms = mapOf(
            // Perú
            "peru" to "peruana",
            "perú" to "peruana",
            "peruana" to "peruana",
            // México
            "mexico" to "mexicana",
            "méxico" to "mexicana",
            "mexicana" to "mexicana",
            // Argentina
            "argentina" to "argentina",
            "argentino" to "argentina",
            "argentina" to "argentina",
            // Chile
            "chile" to "chilena",
            "chilena" to "chilena",
            // Colombia
            "colombia" to "colombiana",
            "colombiana" to "colombiana",
            // España
            "espana" to "espanola",
            "españa" to "espanola",
            "española" to "espanola",
            // Estados Unidos
            "estados unidos" to "estadounidense",
            "eeuu" to "estadounidense",
            "estadounidense" to "estadounidense"
            // agrega más pares a medida que lo necesites
        )
        // -------------------------------------------------

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

                // Normalización básica (minúsculas, quitar acentos comunes, compactar espacios)
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
                val canonicalKey = synonyms[normalizedNew] ?: normalizedNew // si hay sinónimo, usamos la forma canónica

                // 🔹 Si estamos en la colección "nacionalidad", forzamos la forma femenina con mayúscula inicial
                val displayCanonical = if (collectionName == "nacionalidad") {
                    canonicalKey.replaceFirstChar { it.uppercase() } // Ej: "mexicana" → "Mexicana"
                } else {
                    newNameRaw.replaceFirstChar { it.uppercase() }    // otras colecciones (universidad, hospital, etc.)
                }

                val collectionRef = db.collection(collectionName)

                // 1️⃣ Obtener toda la colección (esperamos que las colecciones lookup sean pequeñas)
                collectionRef.get()
                    .addOnSuccessListener { snapshot ->
                        val existingDoc = snapshot.documents.firstOrNull { doc ->
                            val existingName = (doc.getString(nameFieldName) ?: "")
                            val normalizedExisting = normalize(existingName)
                            val existingCanonical = synonyms[normalizedExisting] ?: normalizedExisting
                            existingCanonical == canonicalKey
                        }

                        if (existingDoc != null) {
                            // Ya existe — usamos su ID
                            val existingId = existingDoc.getLong(idFieldName)
                            val displayName = existingDoc.getString(nameFieldName) ?: newNameRaw
                            updateMedicoField(userId, medicoFieldKey, existingId, textView, displayName)
                        } else {
                            // No existe — crear nuevo registro autoincremental
                            collectionRef.orderBy(idFieldName, Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .addOnSuccessListener { maxResult ->
                                    val lastId = maxResult.documents.firstOrNull()?.getLong(idFieldName) ?: 0L
                                    val newId = lastId + 1

                                    // Para el display guardamos la forma que ingresó el usuario (capitalizada)
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
        textView: AutoCompleteTextView,  // ✅ PARA AutoCompleteTextView
        newDisplayName: String
    ) {
        if (newId == null) return

        db.collection("medicos").document(userId)
            .update(fieldKey, newId)
            .addOnSuccessListener {
                textView.setText(newDisplayName)  // ✅ USAR setText()
                Toast.makeText(this, "Actualizado correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar médico", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Versión original para TextView (YA EXISTE EN TU CÓDIGO)
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

            // 1. Mostrar la imagen seleccionada usando Glide
            Glide.with(this).load(imageUri).circleCrop().into(ivProfilePicture)
            // 2. Guardar la imagen en Firebase Storage y actualizar Firestore
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

        // 1. Crear referencia en Storage: 'profile_images/UID_del_medico.jpg'
        val storageRef = storage.reference.child("profile_images/$userId.jpg")

        // 2. Subir el archivo
        storageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                // 3. Obtener la URL de descarga
                taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()

                    // 4. Actualizar el campo FOTO_PERFIL en Firestore
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

    private fun showCalendarPicker(chipGroup: ChipGroup) {
        val userId = auth.currentUser?.uid ?: return
        val builder = MaterialDatePicker.Builder.dateRangePicker().setTitleText("Seleccionar fechas de atención")
        val picker = builder.build()
        picker.show(supportFragmentManager, picker.toString())
        picker.addOnPositiveButtonClickListener { selection ->
            val startDate = selection.first
            val endDate = selection.second
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val datesList = mutableListOf<String>()
            val cal = Calendar.getInstance()
            cal.timeInMillis = startDate
            val endCal = Calendar.getInstance(); endCal.timeInMillis = endDate
            while (!cal.after(endCal)) { datesList.add(sdf.format(cal.time)); cal.add(Calendar.DAY_OF_MONTH,1) }
            db.collection("medicos").document(userId).get()
                .addOnSuccessListener { doc ->
                    val existingDates = doc.get("DIAS_ATENCION") as? MutableList<String> ?: mutableListOf()
                    existingDates.addAll(datesList)
                    existingDates.sort()
                    db.collection("medicos").document(userId).update("DIAS_ATENCION", existingDates)
                        .addOnSuccessListener {
                            datesList.forEach { dateStr ->
                                val chip = Chip(this)
                                chip.text = dateStr
                                chip.isCloseIconVisible = true
                                chip.setOnClickListener {
                                    val clickedChip = it as Chip  // 🔹 Creamos la referencia al chip clickeado
                                    AlertDialog.Builder(this)
                                        .setTitle("Editar o eliminar fecha")
                                        .setMessage("¿Deseas eliminar esta fecha?")
                                        .setPositiveButton("Eliminar") { _, _ ->
                                            val updatedDates = mutableListOf<String>()
                                            for (i in 0 until chipGroup.childCount) {
                                                val c = chipGroup.getChildAt(i) as Chip
                                                if (c != clickedChip) {  // ✔️ Ahora usamos la variable correcta
                                                    updatedDates.add(c.text.toString())
                                                }
                                            }
                                            db.collection("medicos").document(userId)
                                                .update("DIAS_ATENCION", updatedDates)
                                            chipGroup.removeView(clickedChip)
                                        }
                                        .show()
                                }
                                chipGroup.addView(chip)

                            }
                        }
                }
        }
    }


    private fun displayHorarioYFechas(tvHorario: TextView, tvFechas: TextView) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("medicos").document(userId).get()
            .addOnSuccessListener { doc ->
                val horarioList = doc.get("HORARIO_ATENCION") as? List<String> ?: listOf("N/A")
                val diasList = doc.get("DIAS_ATENCION") as? List<String> ?: listOf("N/A")

                val displayHorarios = if (horarioList.isNotEmpty() && horarioList[0] != "N/A") {
                    horarioList.joinToString(" | ") { "🕒 $it" }
                } else "🕒 N/A - N/A"

                val displayFechas = if (diasList.isNotEmpty() && diasList[0] != "N/A") {
                    diasList.joinToString(" ") { "📅 $it" }
                } else "📅 N/A"

                tvHorario.text = displayHorarios
                tvFechas.text = displayFechas
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

        // Primero obtener el ID_MEDICO numérico
        db.collection("medicos").document(userId).get()
            .addOnSuccessListener { medicoDoc ->
                val idMedico = medicoDoc.getLong("ID_MEDICO")

                if (idMedico == null) {
                    Toast.makeText(this, "Error: ID médico no encontrado", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Obtener el hospital actual
                db.collection("doctor_hospital")
                    .whereEqualTo("ID_MEDICO", idMedico)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { hospitalDocs ->
                        val currentHospitalId = hospitalDocs.documents.firstOrNull()?.getLong("ID_HOSPITAL")
                        var currentHospitalName = "N/A"

                        // Si hay hospital actual, cargar su nombre
                        if (currentHospitalId != null) {
                            loadHospitalName(currentHospitalId) { hospitalName ->
                                currentHospitalName = hospitalName ?: "N/A"
                                showHospitalSelectionDialog(idMedico, currentHospitalId, currentHospitalName, actvHospital)  // ✅ USAR actvHospital
                            }
                        } else {
                            showHospitalSelectionDialog(idMedico, currentHospitalId, currentHospitalName, actvHospital)  // ✅ USAR actvHospital
                        }
                    }
                    .addOnFailureListener {
                        showHospitalSelectionDialog(idMedico, null, "N/A", actvHospital)  // ✅ USAR actvHospital
                    }
            }
    }

    /**
     * Diálogo para seleccionar/crear hospital
     */
    /**
     * Diálogo para seleccionar/crear hospital (MEJORADA)
     */
    private fun showHospitalSelectionDialog(idMedico: Long, currentHospitalId: Long?, currentHospitalName: String, textView: AutoCompleteTextView) {
        // Crear un AutoCompleteTextView para el diálogo
        val actvDialog = AutoCompleteTextView(this)
        actvDialog.setPadding(50, 30, 50, 30)
        actvDialog.setText(currentHospitalName)
        actvDialog.hint = "Escriba o seleccione un hospital"

        // Cargar sugerencias para el diálogo
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

                // Usar la misma lógica de auto-creación pero para la relación doctor_hospital
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
        textView: AutoCompleteTextView  // ✅ CORREGIDO
    ) {
        val collectionName = "hospital"
        val idFieldName = "ID_HOSPITAL"
        val nameFieldName = "NOMBRE_HOSPITAL"

        val collectionRef = db.collection(collectionName)

        // Buscar si el hospital ya existe
        collectionRef.whereEqualTo(nameFieldName, newHospitalName).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    // Hospital existe - usar su ID
                    val existingHospital = snapshot.documents.first()
                    val existingId = existingHospital.getLong(idFieldName)
                    val displayName = existingHospital.getString(nameFieldName) ?: newHospitalName

                    updateDoctorHospitalRelation(idMedico, existingId, displayName, textView)
                } else {
                    // Hospital no existe - crear nuevo
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

        // Primero eliminar cualquier relación existente
        db.collection("doctor_hospital")
            .whereEqualTo("ID_MEDICO", idMedico)
            .get()
            .addOnSuccessListener { existingDocs ->
                val batch = db.batch()
                existingDocs.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }

                // Agregar nueva relación
                val newRelation = hashMapOf(
                    "ID_MEDICO" to idMedico,
                    "ID_HOSPITAL" to hospitalId
                )
                val newDocRef = db.collection("doctor_hospital").document()
                batch.set(newDocRef, newRelation)

                batch.commit()
                    .addOnSuccessListener {
                        textView.setText(hospitalName)  // ✅ USAR setText() en lugar de text
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
            textView = actvUniversity, // Ahora recibe AutoCompleteTextView
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
            textView = actvDistrito, // Ahora recibe AutoCompleteTextView
            medicoFieldKey = "ID_DISTRITO"
        )
    }

    /**
     * Muestra diálogo para seleccionar/crear universidad con AutoCompleteTextView
     */
    private fun showUniversitySelectionDialog() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // Crear un AutoCompleteTextView para el diálogo
        val actvDialog = AutoCompleteTextView(this)
        actvDialog.setPadding(50, 30, 50, 30)
        actvDialog.setText(actvUniversity.text.toString())
        actvDialog.hint = "Escriba o seleccione una universidad"

        // Cargar sugerencias para el diálogo
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

        // Crear un AutoCompleteTextView para el diálogo
        val actvDialog = AutoCompleteTextView(this)
        actvDialog.setPadding(50, 30, 50, 30)
        actvDialog.setText(actvDistrito.text.toString())
        actvDialog.hint = "Escriba o seleccione un distrito"

        // Cargar sugerencias para el diálogo
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

        // Crear un AutoCompleteTextView para el diálogo
        val actvDialog = AutoCompleteTextView(this)
        actvDialog.setPadding(50, 30, 50, 30)
        actvDialog.setText(actvSpecialty.text.toString())
        actvDialog.hint = "Escriba o seleccione una especialidad"

        // Cargar sugerencias para el diálogo
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
            textView = actvSpecialty, // Ahora recibe AutoCompleteTextView
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
