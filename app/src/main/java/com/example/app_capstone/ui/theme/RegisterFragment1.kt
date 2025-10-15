package com.example.app_capstone

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore

// Corresponde a los campos: NOMBRE, APELLIDO, DNI, CORREO, CELULAR, EDAD, ID_DISTRITO, ID_NACIONALIDAD
class RegisterFragment1 : Fragment(), RegisterActivity.RegisterFragmentInterface {

    // 1. Instancia de Firestore
    private val db = FirebaseFirestore.getInstance()

    // Mapas OBLIGATORIOS para almacenar la relación NOMBRE_VISIBLE -> ID_REAL para cada dropdown
    private val distritoMap = mutableMapOf<String, String>()
    private val nacionalidadMap = mutableMapOf<String, String>()

    // TextInputLayouts y EditTexts/AutoCompleteTextViews
    private lateinit var tilName: TextInputLayout
    private lateinit var tilLastName: TextInputLayout
    private lateinit var tilDni: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilCelular: TextInputLayout
    private lateinit var tilAge: TextInputLayout
    private lateinit var tilDistrito: TextInputLayout
    private lateinit var tilNacionalidad: TextInputLayout

    private lateinit var etName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etDni: EditText
    private lateinit var etEmail: EditText
    private lateinit var etCelular: EditText
    private lateinit var etAge: EditText

    private lateinit var actvDistrito: AutoCompleteTextView
    private lateinit var actvNacionalidad: AutoCompleteTextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register1, container, false)
        initViews(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Carga de Distritos (Collection: 'distrito', Display: 'NOMBRE_DISTRITO', ID: 'ID_DISTRITO')
        loadDropdownData(
            collectionName = "distrito",
            fieldName = "NOMBRE_DISTRITO",
            idFieldName = "ID_DISTRITO",
            actv = actvDistrito,
            til = tilDistrito,
            map = distritoMap
        )

        // Carga de Nacionalidades (Collection: 'nacionalidad', Display: 'NACIONALIDAD', ID: 'ID_NACIONALIDAD')
        loadDropdownData(
            collectionName = "nacionalidad",
            fieldName = "NACIONALIDAD",
            idFieldName = "ID_NACIONALIDAD",
            actv = actvNacionalidad,
            til = tilNacionalidad,
            map = nacionalidadMap
        )
    }

    private fun initViews(view: View) {
        // Inicialización de TextInputLayouts
        tilName = view.findViewById(R.id.tilName)
        tilLastName = view.findViewById(R.id.tilLastName)
        tilDni = view.findViewById(R.id.tilDni)
        tilEmail = view.findViewById(R.id.tilEmail)
        tilCelular = view.findViewById(R.id.tilCelular)
        tilAge = view.findViewById(R.id.tilAge)
        tilDistrito = view.findViewById(R.id.tilDistrito)
        tilNacionalidad = view.findViewById(R.id.tilNacionalidad)

        // Inicialización de EditTexts/AutoCompleteTextViews
        etName = view.findViewById(R.id.etName)
        etLastName = view.findViewById(R.id.etLastName)
        etDni = view.findViewById(R.id.etDni)
        etEmail = view.findViewById(R.id.etEmail)
        etCelular = view.findViewById(R.id.etCelular)
        etAge = view.findViewById(R.id.etAge)
        actvDistrito = view.findViewById(R.id.actvDistrito)
        actvNacionalidad = view.findViewById(R.id.actvNacionalidad)
    }

    /**
     * Función genérica para cargar datos de una colección de Firestore, poblar un AutoCompleteTextView
     * y almacenar el mapeo de Nombre Visible -> ID Real.
     */
    private fun loadDropdownData(
        collectionName: String,
        fieldName: String, // Campo que se muestra al usuario (e.g., NOMBRE_DISTRITO)
        idFieldName: String, // Campo que contiene el ID real (e.g., ID_DISTRITO)
        actv: AutoCompleteTextView,
        til: TextInputLayout,
        map: MutableMap<String, String> // El mapa para almacenar Nombre Visible -> ID Real
    ) {
        db.collection(collectionName).get()
            .addOnSuccessListener { result ->
                map.clear() // Limpiar el mapa antes de cargar nuevos datos
                val displayNames = mutableListOf<String>()

                result.documents.forEach { document ->
                    val name = document.getString(fieldName)
                    // CORRECCIÓN CLAVE: document.get() seguido de toString() maneja IDs de tipo Number o String
                    val id = document.get(idFieldName)?.toString()

                    if (name != null && id != null) {
                        displayNames.add(name)
                        map[name] = id // Mapear el nombre (lo que se ve) al ID (lo que se guarda)
                    }
                }

                // Crea el adaptador con los nombres de visualización
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayNames)
                actv.setAdapter(adapter)

                // Limpia el error cuando se selecciona un ítem
                actv.setOnItemClickListener { _, _, _, _ ->
                    til.error = null
                }
            }
            .addOnFailureListener { e ->
                til.error = "Error al cargar $collectionName: ${e.message}"
            }
    }


    override fun validateFields(): Boolean {
        var isValid = true

        // --- Validación de campos de texto normales ---
        if (etName.text.isNullOrEmpty()) {
            tilName.error = "Campo obligatorio"
            isValid = false
        } else tilName.error = null

        if (etLastName.text.isNullOrEmpty()) {
            tilLastName.error = "Campo obligatorio"
            isValid = false
        } else tilLastName.error = null

        val dni = etDni.text.toString()
        if (dni.isEmpty() || dni.length != 8) {
            tilDni.error = "DNI debe tener 8 dígitos"
            isValid = false
        } else tilDni.error = null

        val email = etEmail.text.toString()
        if (email.isEmpty()) {
            tilEmail.error = "Campo obligatorio"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Email inválido"
            isValid = false
        } else tilEmail.error = null

        val celular = etCelular.text.toString()
        if (celular.isEmpty()) {
            tilCelular.error = "Campo obligatorio"
            isValid = false
        } else tilCelular.error = null

        val age = etAge.text.toString().toIntOrNull()
        if (age == null || age <= 0 || age >= 100) {
            tilAge.error = "Edad inválida (0-99)"
            isValid = false
        } else tilAge.error = null

        // --- Validación y obtención de IDs de AutoCompleteTextViews ---

        val selectedDistritoName = actvDistrito.text.toString()
        val selectedNacionalidadName = actvNacionalidad.text.toString()

        // 1. Obtener el ID como String del mapa
        val distritoIdString = distritoMap[selectedDistritoName]
        val nacionalidadIdString = nacionalidadMap[selectedNacionalidadName]

        // 2. Convertir a Int (esta línea resuelve el conflicto de declaración)
        val distritoId = distritoIdString?.toIntOrNull()
        val nacionalidadId = nacionalidadIdString?.toIntOrNull()

        if (distritoId == null || selectedDistritoName.isEmpty() || !distritoMap.containsKey(selectedDistritoName)) {
            tilDistrito.error = "Seleccione un Distrito válido de la lista"
            isValid = false
        } else tilDistrito.error = null

        if (nacionalidadId == null || selectedNacionalidadName.isEmpty() || !nacionalidadMap.containsKey(selectedNacionalidadName)) {
            tilNacionalidad.error = "Seleccione una Nacionalidad válida de la lista"
            isValid = false
        } else tilNacionalidad.error = null


        if (isValid) {
            // Se guardan los datos de texto normales
            (activity as RegisterActivity).saveFormData("NOMBRE", etName.text.toString())
            (activity as RegisterActivity).saveFormData("APELLIDO", etLastName.text.toString())
            (activity as RegisterActivity).saveFormData("DNI", dni)
            (activity as RegisterActivity).saveFormData("CORREO", email)
            (activity as RegisterActivity).saveFormData("CELULAR", celular)
            (activity as RegisterActivity).saveFormData("EDAD", age!!)

            // AHORA SE GUARDAN LOS IDs REALES OBTENIDOS DEL MAPA
            (activity as RegisterActivity).saveFormData("ID_DISTRITO", distritoId!!)
            (activity as RegisterActivity).saveFormData("ID_NACIONALIDAD", nacionalidadId!!)
        }

        return isValid
    }
}
