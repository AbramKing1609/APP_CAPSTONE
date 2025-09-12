package com.example.app_capstone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout

class RegisterFragment3 : Fragment(), RegisterActivity.RegisterFragmentInterface {

    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var tilAdditionalInfo: TextInputLayout

    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var etAdditionalInfo: EditText

    private lateinit var chkAcceptTerms: CheckBox

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register3, container, false)
        initViews(view)
        return view
    }

    private fun initViews(view: View) {
        tilPassword = view.findViewById(R.id.tilPassword)
        tilConfirmPassword = view.findViewById(R.id.tilConfirmPassword)
        tilAdditionalInfo = view.findViewById(R.id.tilAdditionalInfo)

        etPassword = view.findViewById(R.id.etPassword)
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword)
        etAdditionalInfo = view.findViewById(R.id.etAdditionalInfo)

        chkAcceptTerms = view.findViewById(R.id.chkAcceptTerms)
    }

    override fun validateFields(): Boolean {
        var isValid = true

        val password = etPassword.text.toString()
        if (password.isEmpty()) {
            tilPassword.error = "La contraseña es obligatoria"
            isValid = false
        } else if (password.length < 6) {
            tilPassword.error = "Debe tener al menos 6 caracteres"
            isValid = false
        } else tilPassword.error = null

        val confirmPassword = etConfirmPassword.text.toString()
        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.error = "Confirme su contraseña"
            isValid = false
        } else if (password != confirmPassword) {
            tilConfirmPassword.error = "Las contraseñas no coinciden"
            isValid = false
        } else tilConfirmPassword.error = null

        if (!chkAcceptTerms.isChecked) {
            chkAcceptTerms.error = "Debe aceptar los términos y condiciones"
            isValid = false
        } else chkAcceptTerms.error = null

        if (isValid) {
            (activity as RegisterActivity).saveFormData("additional_info", etAdditionalInfo.text.toString())
        }

        return isValid
    }

    fun getPassword(): String {
        return etPassword.text.toString()
    }
}
