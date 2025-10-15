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

    // Mapas OBLIGATORIOS para almacenar la relación NOMBRE_VISIBLE -> ID_REAL
    private val hospitalMap = mutableMapOf<String, String>()
    private val especialidadMap = mutableMapOf<String, String>()
    private val universityMap = mutableMapOf<String, String>()

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

        // Carga de Hospitales (Collection: 'hospital', Display: 'NOMBRE_HOSPITAL', ID: 'ID_HOSPITAL')
        loadAutocompleteData(
            collectionName = "hospital",
            fieldName = "NOMBRE_HOSPITAL",
            idFieldName = "ID_HOSPITAL",
            actv = actvHospital,
            til = tilHospital,
            map = hospitalMap
        )

        // Carga de Especialidades (Collection: 'especialidad', Display: 'ESPECIALIDAD', ID: 'ID_ESPECIALIDAD')
        loadAutocompleteData(
            collectionName = "especialidad",
            fieldName = "ESPECIALIDAD",
            idFieldName = "ID_ESPECIALIDAD",
            actv = actvEspecialidad,
            til = tilEspecialidad,
            map = especialidadMap
        )

        // Carga de Universidades (Collection: 'universidad', Display: 'NOMBRE_UNIVERSIDAD', ID: 'ID_UNIVERSIDAD')
        loadAutocompleteData(
            collectionName = "universidad",
            fieldName = "NOMBRE_UNIVERSIDAD",
            idFieldName = "ID_UNIVERSIDAD",
            actv = actvUniversity,
            til = tilUniversity,
            map = universityMap
        )
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
     * Función para cargar datos de una colección de Firestore, poblar un AutoCompleteTextView
     * y almacenar el mapeo de Nombre Visible -> ID Real. Soporta IDs almacenados como String o Number.
     */
    private fun loadAutocompleteData(
        collectionName: String,
        fieldName: String, // Campo que se muestra al usuario (e.g., NOMBRE_HOSPITAL)
        idFieldName: String, // Campo que contiene el ID real (e.g., ID_HOSPITAL)
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

        // --- Validación y obtención de IDs de AutoCompleteTextViews ---

        val selectedHospitalName = actvHospital.text.toString()
        val selectedEspecialidadName = actvEspecialidad.text.toString()
        val selectedUniversityName = actvUniversity.text.toString()

        // 1. Obtener el ID como String del mapa
        val hospitalIdString = hospitalMap[selectedHospitalName]
        val especialidadIdString = especialidadMap[selectedEspecialidadName]
        val universityIdString = universityMap[selectedUniversityName]

        // 2. Convertir a Int (utilizando .toIntOrNull())
        val hospitalId = hospitalIdString?.toIntOrNull()
        val especialidadId = especialidadIdString?.toIntOrNull()
        val universityId = universityIdString?.toIntOrNull()

        if (hospitalId == null || selectedHospitalName.isEmpty() || !hospitalMap.containsKey(selectedHospitalName)) {
            tilHospital.error = "Seleccione un Hospital válido de la lista"
            isValid = false
        } else tilHospital.error = null

        if (especialidadId == null || selectedEspecialidadName.isEmpty() || !especialidadMap.containsKey(selectedEspecialidadName)) {
            tilEspecialidad.error = "Seleccione una Especialidad válida de la lista"
            isValid = false
        } else tilEspecialidad.error = null

        if (universityId == null || selectedUniversityName.isEmpty() || !universityMap.containsKey(selectedUniversityName)) {
            tilUniversity.error = "Seleccione una Universidad válida de la lista"
            isValid = false
        } else tilUniversity.error = null

        // --- Validación de campos de texto normales ---

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
            // AHORA SE GUARDAN LOS IDS REALES OBTENIDOS DEL MAPA
            (activity as RegisterActivity).saveFormData("ID_HOSPITAL", hospitalId!!)
            (activity as RegisterActivity).saveFormData("ID_ESPECIALIDAD", especialidadId!!)
            (activity as RegisterActivity).saveFormData("COLEGIATURA", etColegiatura.text.toString())
            (activity as RegisterActivity).saveFormData("ID_UNIVERSIDAD", universityId!!)
            (activity as RegisterActivity).saveFormData("AÑIO_GRADUACION", graduationYear!!)
            (activity as RegisterActivity).saveFormData("EXP_ANIOS", expYears!!)
            (activity as RegisterActivity).saveFormData("PRECIO", price!!)
            (activity as RegisterActivity).saveFormData("DISPONIBILIDAD", etAvailability.text.toString())
            (activity as RegisterActivity).saveFormData("JORNADA_ATENCION", etWorkingHours.text.toString())
        }

        return isValid
    }
}
