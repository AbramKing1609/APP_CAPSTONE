package com.example.app_capstone

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout

class RegisterFragment1 : Fragment(), RegisterActivity.RegisterFragmentInterface {

    private lateinit var tilName: TextInputLayout
    private lateinit var tilLastName: TextInputLayout
    private lateinit var tilDni: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilCelular: TextInputLayout
    private lateinit var tilAge: TextInputLayout
    private lateinit var tilCity: TextInputLayout
    private lateinit var tilNationality: TextInputLayout

    private lateinit var etName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etDni: EditText
    private lateinit var etEmail: EditText
    private lateinit var etCelular: EditText
    private lateinit var etAge: EditText
    private lateinit var etCity: EditText
    private lateinit var etNationality: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register1, container, false)
        initViews(view)
        return view
    }

    private fun initViews(view: View) {
        tilName = view.findViewById(R.id.tilName)
        tilLastName = view.findViewById(R.id.tilLastName)
        tilDni = view.findViewById(R.id.tilDni)
        tilEmail = view.findViewById(R.id.tilEmail)
        tilCelular = view.findViewById(R.id.tilCelular)
        tilAge = view.findViewById(R.id.tilAge)
        tilCity = view.findViewById(R.id.tilCity)
        tilNationality = view.findViewById(R.id.tilNationality)

        etName = view.findViewById(R.id.etName)
        etLastName = view.findViewById(R.id.etLastName)
        etDni = view.findViewById(R.id.etDni)
        etEmail = view.findViewById(R.id.etEmail)
        etCelular = view.findViewById(R.id.etCelular)
        etAge = view.findViewById(R.id.etAge)
        etCity = view.findViewById(R.id.etCity)
        etNationality = view.findViewById(R.id.etNationality)
    }

    override fun validateFields(): Boolean {
        var isValid = true

        if (etName.text.isNullOrEmpty()) {
            tilName.error = "Campo obligatorio"
            isValid = false
        } else tilName.error = null

        if (etLastName.text.isNullOrEmpty()) {
            tilLastName.error = "Campo obligatorio"
            isValid = false
        } else tilLastName.error = null

        if (etDni.text.isNullOrEmpty()) {
            tilDni.error = "Campo obligatorio"
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

        if (etCelular.text.isNullOrEmpty()) {
            tilCelular.error = "Campo obligatorio"
            isValid = false
        } else tilCelular.error = null

        if (etAge.text.isNullOrEmpty() || etAge.text.toString().toIntOrNull() == null) {
            tilAge.error = "Edad inválida"
            isValid = false
        } else tilAge.error = null

        if (etCity.text.isNullOrEmpty()) {
            tilCity.error = "Campo obligatorio"
            isValid = false
        } else tilCity.error = null

        if (etNationality.text.isNullOrEmpty()) {
            tilNationality.error = "Campo obligatorio"
            isValid = false
        } else tilNationality.error = null

        if (isValid) {
            (activity as RegisterActivity).saveFormData("name", etName.text.toString())
            (activity as RegisterActivity).saveFormData("lastName", etLastName.text.toString())
            (activity as RegisterActivity).saveFormData("dni", etDni.text.toString())
            (activity as RegisterActivity).saveFormData("email", etEmail.text.toString())
            (activity as RegisterActivity).saveFormData("celular", etCelular.text.toString())
            (activity as RegisterActivity).saveFormData("age", etAge.text.toString().toInt())
            (activity as RegisterActivity).saveFormData("city", etCity.text.toString())
            (activity as RegisterActivity).saveFormData("nationality", etNationality.text.toString())
        }

        return isValid
    }
}
