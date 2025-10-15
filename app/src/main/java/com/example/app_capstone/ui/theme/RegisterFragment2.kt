package com.example.app_capstone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

// Corresponde a los campos: ID_HOSPITAL, ID_ESPECIALIDAD, COLEGIATURA, ID_UNIVERSIDAD, AÑIO_GRADUACION, EXP_ANIOS, PRECIO, DISPONIBILIDAD, JORNADA_ATENCION
class RegisterFragment2 : Fragment(), RegisterActivity.RegisterFragmentInterface {

    // 1. Instancia de Firestore
    private val db = FirebaseFirestore.getInstance()

    // TextInputLayouts (Dropdowns)
    private lateinit var tilHospital: TextInputLayout
    private lateinit var tilEspecialidad: TextInputLayout
    private lateinit var tilUniversity: TextInputLayout

    // TextInputLayouts (Campos de texto normales)
    private lateinit var tilColegiatura: TextInputLayout
    private lateinit var tilGraduationYear: TextInputLayout
    private lateinit var tilExperienceYears: TextInputLayout
    private lateinit var tilPrice: TextInputLayout
    private lateinit var tilAvailability: TextInputLayout
    private lateinit var tilWorkingHours: TextInputLayout

    // AutoCompleteTextViews
    private lateinit var actvHospital: AutoCompleteTextView
    private lateinit var actvEspecialidad: AutoCompleteTextView
    private lateinit var actvUniversity: AutoCompleteTextView

    // EditTexts
    private lateinit var etColegiatura: EditText
    private lateinit var etGraduationYear: EditText
    private lateinit var etExperienceYears: EditText
    private lateinit var etPrice: EditText
    private lateinit var etAvailability: EditText
    private lateinit var etWorkingHours: EditText


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register2, container, false)
        initViews(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 2. Llamada a la función de carga de datos para los 3 campos.
        // La función carga los datos en un ArrayAdapter, permitiendo el autocompletado y filtrado
        // al escribir en el AutoCompleteTextView.

        // Carga de Hospitales
        loadAutocompleteData("hospital", "NOMBRE_HOSPITAL", actvHospital, tilHospital)

        // Carga de Especialidades
        loadAutocompleteData("especialidad", "ESPECIALIDAD", actvEspecialidad, tilEspecialidad)

        // Carga de Universidades
        loadAutocompleteData("universidad", "NOMBRE_UNIVERSIDAD", actvUniversity, tilUniversity)
    }

    private fun initViews(view: View) {
        // Inicialización de TextInputLayouts (Dropdowns)
        tilHospital = view.findViewById(R.id.tilHospital)
        tilEspecialidad = view.findViewById(R.id.tilEspecialidad)
        tilUniversity = view.findViewById(R.id.tilUniversity)

        // Inicialización de TextInputLayouts (Campos de texto normales)
        tilColegiatura = view.findViewById(R.id.tilColegiatura)
        tilGraduationYear = view.findViewById(R.id.tilGraduationYear)
        tilExperienceYears = view.findViewById(R.id.tilExperienceYears)
        tilPrice = view.findViewById(R.id.tilPrice)
        tilAvailability = view.findViewById(R.id.tilAvailability)
        tilWorkingHours = view.findViewById(R.id.tilWorkingHours)

        // Inicialización de AutoCompleteTextViews
        actvHospital = view.findViewById(R.id.actvHospital)
        actvEspecialidad = view.findViewById(R.id.actvEspecialidad)
        actvUniversity = view.findViewById(R.id.actvUniversity)

        // Inicialización de EditTexts
        etColegiatura = view.findViewById(R.id.etColegiatura)
        etGraduationYear = view.findViewById(R.id.etGraduationYear)
        etExperienceYears = view.findViewById(R.id.etExperienceYears)
        etPrice = view.findViewById(R.id.etPrice)
        etAvailability = view.findViewById(R.id.etAvailability)
        etWorkingHours = view.findViewById(R.id.etWorkingHours)
    }

    /**
     * Función para cargar datos de una colección de Firestore y poblar un AutoCompleteTextView.
     * Esta función usa un ArrayAdapter, el cual habilita automáticamente la funcionalidad
     * de auto-sugerencia y filtrado mientras el usuario escribe.
     */
    private fun loadAutocompleteData(
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
                // Usamos el layout simple_dropdown_item_1line, que permite el filtrado.
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

        // 3. Validación de AutoCompleteTextViews
        if (actvHospital.text.isNullOrEmpty()) {
            tilHospital.error = "Campo obligatorio (Hospital)"
            isValid = false
        } else tilHospital.error = null

        if (actvEspecialidad.text.isNullOrEmpty()) {
            tilEspecialidad.error = "Campo obligatorio (Especialidad)"
            isValid = false
        } else tilEspecialidad.error = null

        if (actvUniversity.text.isNullOrEmpty()) {
            tilUniversity.error = "Campo obligatorio (Universidad)"
            isValid = false
        } else tilUniversity.error = null

        // ... [Lógica de validación de campos de texto normales (sin cambios significativos)] ...

        if (etColegiatura.text.isNullOrEmpty()) {
            tilColegiatura.error = "Campo obligatorio"
            isValid = false
        } else tilColegiatura.error = null

        val graduationYear = etGraduationYear.text.toString().toIntOrNull()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        if (graduationYear == null || graduationYear > currentYear || graduationYear < 1950) {
            tilGraduationYear.error = "Año de graduación inválido"
            isValid = false
        } else tilGraduationYear.error = null

        val expYears = etExperienceYears.text.toString().toIntOrNull()
        if (expYears == null || expYears < 0 || (graduationYear != null && expYears > (currentYear - graduationYear))) {
            tilExperienceYears.error = "Años de experiencia inválidos o inconsistentes"
            isValid = false
        } else tilExperienceYears.error = null

        val price = etPrice.text.toString().toDoubleOrNull()
        if (price == null || price <= 0) {
            tilPrice.error = "Precio inválido"
            isValid = false
        } else tilPrice.error = null

        if (etAvailability.text.isNullOrEmpty()) {
            tilAvailability.error = "Campo obligatorio (DISPONIBILIDAD)"
            isValid = false
        } else tilAvailability.error = null

        if (etWorkingHours.text.isNullOrEmpty()) {
            tilWorkingHours.error = "Campo obligatorio (JORNADA_ATENCION)"
            isValid = false
        } else tilWorkingHours.error = null


        if (isValid) {
            // Se guardan los datos: ahora con los valores seleccionados del AutoCompleteTextView
            (activity as RegisterActivity).saveFormData("ID_HOSPITAL", actvHospital.text.toString())
            (activity as RegisterActivity).saveFormData("ID_ESPECIALIDAD", actvEspecialidad.text.toString())
            (activity as RegisterActivity).saveFormData("COLEGIATURA", etColegiatura.text.toString())
            (activity as RegisterActivity).saveFormData("ID_UNIVERSIDAD", actvUniversity.text.toString())
            (activity as RegisterActivity).saveFormData("AÑIO_GRADUACION", graduationYear!!)
            (activity as RegisterActivity).saveFormData("EXP_ANIOS", expYears!!)
            (activity as RegisterActivity).saveFormData("PRECIO", price!!)
            (activity as RegisterActivity).saveFormData("DISPONIBILIDAD", etAvailability.text.toString())
            (activity as RegisterActivity).saveFormData("JORNADA_ATENCION", etWorkingHours.text.toString())
        }

        return isValid
    }
}
