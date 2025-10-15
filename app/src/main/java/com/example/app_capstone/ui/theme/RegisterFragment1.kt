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
import com.google.firebase.firestore.FirebaseFirestore // Importación necesaria

// Corresponde a los campos: NOMBRE, APELLIDO, DNI, CORREO, CELULAR, EDAD, ID_DISTRITO, ID_NACIONALIDAD
class RegisterFragment1 : Fragment(), RegisterActivity.RegisterFragmentInterface {

    // 1. Instancia de Firestore
    private val db = FirebaseFirestore.getInstance()

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

        // 2. Llamada a la función de carga de datos
        // Carga de Distritos (Colección: 'distrito', Campo de visualización: 'NOMBRE_DISTRITO')
        loadDropdownData("distrito", "NOMBRE_DISTRITO", actvDistrito, tilDistrito)

        // Carga de Nacionalidades (Colección: 'nacionalidad', Campo de visualización: 'NACIONALIDAD')
        loadDropdownData("nacionalidad", "NACIONALIDAD", actvNacionalidad, tilNacionalidad)
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
     * Función genérica para cargar datos de una colección de Firestore y poblar un AutoCompleteTextView.
     */
    private fun loadDropdownData(
        collectionName: String,
        fieldName: String,
        actv: AutoCompleteTextView,
        til: TextInputLayout
    ) {
        db.collection(collectionName).get()
            .addOnSuccessListener { result ->
                // Mapea los documentos a una lista de Strings usando el campo especificado
                val items = result.documents.mapNotNull { it.getString(fieldName) }

                // Crea el adaptador
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, items)
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

        // ... [Lógica de validación de campos de texto normales (sin cambios significativos)] ...

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

        // 3. Validación de AutoCompleteTextViews
        if (actvDistrito.text.isNullOrEmpty()) {
            tilDistrito.error = "Campo obligatorio (Distrito)"
            isValid = false
        } else tilDistrito.error = null

        if (actvNacionalidad.text.isNullOrEmpty()) {
            tilNacionalidad.error = "Campo obligatorio (Nacionalidad)"
            isValid = false
        } else tilNacionalidad.error = null

        if (isValid) {
            // Se guardan los datos: ahora con los valores seleccionados del AutoCompleteTextView
            (activity as RegisterActivity).saveFormData("NOMBRE", etName.text.toString())
            (activity as RegisterActivity).saveFormData("APELLIDO", etLastName.text.toString())
            (activity as RegisterActivity).saveFormData("DNI", dni)
            (activity as RegisterActivity).saveFormData("CORREO", email)
            (activity as RegisterActivity).saveFormData("CELULAR", celular)
            (activity as RegisterActivity).saveFormData("EDAD", age!!)
            // Guarda el valor de texto, que temporalmente sirve como ID
            (activity as RegisterActivity).saveFormData("ID_DISTRITO", actvDistrito.text.toString())
            (activity as RegisterActivity).saveFormData("ID_NACIONALIDAD", actvNacionalidad.text.toString())
        }

        return isValid
    }
}
