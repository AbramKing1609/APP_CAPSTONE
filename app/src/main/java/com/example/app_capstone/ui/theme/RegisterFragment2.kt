package com.example.app_capstone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout

class RegisterFragment2 : Fragment(), RegisterActivity.RegisterFragmentInterface {

    private lateinit var tilEspecialidad: TextInputLayout
    private lateinit var tilColegiatura: TextInputLayout
    private lateinit var tilUniversity: TextInputLayout
    private lateinit var tilExperienceYears: TextInputLayout
    private lateinit var tilHospital: TextInputLayout

    private lateinit var etEspecialidad: EditText
    private lateinit var etColegiatura: EditText
    private lateinit var etUniversity: EditText
    private lateinit var etExperienceYears: EditText
    private lateinit var etHospital: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register2, container, false)
        initViews(view)
        return view
    }

    private fun initViews(view: View) {
        tilEspecialidad = view.findViewById(R.id.tilEspecialidad)
        tilColegiatura = view.findViewById(R.id.tilColegiatura)
        tilUniversity = view.findViewById(R.id.tilUniversity)
        tilExperienceYears = view.findViewById(R.id.tilExperienceYears)
        tilHospital = view.findViewById(R.id.tilHospital)

        etEspecialidad = view.findViewById(R.id.etEspecialidad)
        etColegiatura = view.findViewById(R.id.etColegiatura)
        etUniversity = view.findViewById(R.id.etUniversity)
        etExperienceYears = view.findViewById(R.id.etExperienceYears)
        etHospital = view.findViewById(R.id.etHospital)
    }

    override fun validateFields(): Boolean {
        var isValid = true

        if (etEspecialidad.text.isNullOrEmpty()) {
            tilEspecialidad.error = "Campo obligatorio"
            isValid = false
        } else tilEspecialidad.error = null

        if (etColegiatura.text.isNullOrEmpty()) {
            tilColegiatura.error = "Campo obligatorio"
            isValid = false
        } else tilColegiatura.error = null

        if (etUniversity.text.isNullOrEmpty()) {
            tilUniversity.error = "Campo obligatorio"
            isValid = false
        } else tilUniversity.error = null

        if (etExperienceYears.text.isNullOrEmpty() || etExperienceYears.text.toString().toIntOrNull() == null) {
            tilExperienceYears.error = "Años de experiencia inválidos"
            isValid = false
        } else tilExperienceYears.error = null

        if (etHospital.text.isNullOrEmpty()) {
            tilHospital.error = "Campo obligatorio"
            isValid = false
        } else tilHospital.error = null

        if (isValid) {
            (activity as RegisterActivity).saveFormData("especialidad", etEspecialidad.text.toString())
            (activity as RegisterActivity).saveFormData("colegiatura", etColegiatura.text.toString())
            (activity as RegisterActivity).saveFormData("university", etUniversity.text.toString())
            (activity as RegisterActivity).saveFormData("experience_years", etExperienceYears.text.toString().toInt())
            (activity as RegisterActivity).saveFormData("hospital", etHospital.text.toString())
        }

        return isValid
    }
}
