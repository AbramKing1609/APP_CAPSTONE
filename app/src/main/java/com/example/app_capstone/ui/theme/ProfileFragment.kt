package com.example.app_capstone

import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class ProfileActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.content_profile)

        // 🔹 Referencias de vistas
        val tvProfileName = findViewById<TextView>(R.id.tvProfileName)
        val tvProfileSpecialty = findViewById<TextView>(R.id.tvProfileSpecialty)
        val tvUniversity = findViewById<TextView>(R.id.tvUniversity)
        val tvExperienceYears = findViewById<TextView>(R.id.tvExperienceYears)
        val tvHospital = findViewById<TextView>(R.id.tvHospital)
        val tvAdditionalInfo = findViewById<TextView>(R.id.tvAdditionalInfo)
        val tvAge = findViewById<TextView>(R.id.tvAge)
        val etContactNumber = findViewById<EditText>(R.id.etContactNumber)

        // 🔹 NUEVOS CAMPOS agregados
        val tvDistrito = findViewById<TextView>(R.id.tvDistrito)
        val tvNacionalidad = findViewById<TextView>(R.id.tvNacionalidad)

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

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

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
                val idHospital = doctorData["ID_HOSPITAL"] as? Long
                val idDistrito = doctorData["ID_DISTRITO"] as? Long
                val idNacionalidad = doctorData["ID_NACIONALIDAD"] as? Long
                val expAnios = doctorData["EXP_ANIOS"]?.toString() ?: "N/A"
                val additionalInfo = doctorData["INFO_ADIC"] as? String ?: "N/A"

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

                val hospitalTask = idHospital?.let {
                    db.collection("hospital")
                        .whereEqualTo("ID_HOSPITAL", it)
                        .limit(1).get()
                } ?: Tasks.forResult(null as QuerySnapshot?)

                // 🔹 NUEVAS CONSULTAS
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


                Tasks.whenAllSuccess<Any>(
                    specialtyTask,
                    universityTask,
                    hospitalTask,
                    distritoTask,
                    nacionalidadTask
                ).addOnSuccessListener { results ->
                    val specialtyName = (results.getOrNull(0) as? QuerySnapshot)
                        ?.documents?.firstOrNull()?.getString("ESPECIALIDAD") ?: "N/A"

                    val universityName = (results.getOrNull(1) as? QuerySnapshot)
                        ?.documents?.firstOrNull()?.getString("NOMBRE_UNIVERSIDAD") ?: "N/A"

                    val hospitalName = (results.getOrNull(2) as? QuerySnapshot)
                        ?.documents?.firstOrNull()?.getString("NOMBRE_HOSPITAL") ?: "N/A"

                    val distritoName = (results.getOrNull(3) as? QuerySnapshot)
                        ?.documents?.firstOrNull()?.getString("NOMBRE_DISTRITO") ?: "N/A"

                    val nacionalidadName = (results.getOrNull(4) as? QuerySnapshot)
                        ?.documents?.firstOrNull()?.getString("NACIONALIDAD") ?: "N/A"

                    // 🔹 Mostrar datos
                    tvProfileName.text = "$name $lastName"
                    tvProfileSpecialty.text = specialtyName
                    etContactNumber.setText(telefono)
                    tvAge.text = "$edad años"
                    tvUniversity.text = universityName
                    tvExperienceYears.text = expAnios
                    tvHospital.text = hospitalName
                    tvAdditionalInfo.text = additionalInfo
                    tvDistrito.text = distritoName
                    tvNacionalidad.text = nacionalidadName
                }
            }

        // 📝 Funciones de edición
        ivEditName.setOnClickListener {
            editField("NOMBRE", tvProfileName.text.toString(), tvProfileName)
        }

        ivEditAge.setOnClickListener {
            editField("EDAD", tvAge.text.toString().replace(" años", ""), tvAge, " años")
        }

        ivEditExperience.setOnClickListener {
            editField("EXP_ANIOS", tvExperienceYears.text.toString(), tvExperienceYears)
        }

        // 🔹 Actualizado: universidad, hospital, especialidad con creación automática
        ivEditUniversity.setOnClickListener {
            editFieldWithAutoCreate(
                collectionName = "universidad",
                idFieldName = "ID_UNIVERSIDAD",
                nameFieldName = "NOMBRE_UNIVERSIDAD",
                currentName = tvUniversity.text.toString(),
                textView = tvUniversity,
                medicoFieldKey = "ID_UNIVERSIDAD"
            )
        }

        ivEditHospital.setOnClickListener {
            editFieldWithAutoCreate(
                collectionName = "hospital",
                idFieldName = "ID_HOSPITAL",
                nameFieldName = "NOMBRE_HOSPITAL",
                currentName = tvHospital.text.toString(),
                textView = tvHospital,
                medicoFieldKey = "ID_HOSPITAL"
            )
        }

        ivEditSpecialty.setOnClickListener {
            editFieldWithAutoCreate(
                collectionName = "especialidad",
                idFieldName = "ID_ESPECIALIDAD",
                nameFieldName = "ESPECIALIDAD",
                currentName = tvProfileSpecialty.text.toString(),
                textView = tvProfileSpecialty,
                medicoFieldKey = "ID_ESPECIALIDAD"
            )
        }

        // 🔹 Nuevas funciones autoincrementales
        ivEditDistrito.setOnClickListener {
            editFieldWithAutoCreate(
                collectionName = "distrito",
                idFieldName = "ID_DISTRITO",
                nameFieldName = "NOMBRE_DISTRITO",
                currentName = tvDistrito.text.toString(),
                textView = tvDistrito,
                medicoFieldKey = "ID_DISTRITO"
            )
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

        ivEditAdditionalInfo.setOnClickListener {
            editField("INFO_ADIC", tvAdditionalInfo.text.toString(), tvAdditionalInfo)
        }

        ivEditPhone.setOnClickListener {
            editField("CONTACTO", etContactNumber.text.toString(), etContactNumber)
        }
    }

    // 🔧 Genérico para campos simples
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

    // 🔧 Sobrecarga para EditText (como el número de contacto)
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

    // 🧩 Nueva función: crear registro si no existe (autoincremental)
// 🧩 Nueva versión mejorada: evita duplicados por mayúsculas/minúsculas
// y mapea países/variantes a un nombre canónico (ej: "peru", "perú", "peruana" -> "peruana")
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

        // ---- Tabla de equivalencias (puedes ampliar) ----
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
                            collectionRef.orderBy(idFieldName, com.google.firebase.firestore.Query.Direction.DESCENDING)
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
}
