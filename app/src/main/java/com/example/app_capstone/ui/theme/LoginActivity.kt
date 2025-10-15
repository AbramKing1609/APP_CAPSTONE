package com.example.app_capstone

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.app_capstone.RegisterActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot

class LoginActivity : AppCompatActivity() {

    private lateinit var tilUsername: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvGoRegister: TextView

    // Instancia de FirebaseAuth para la autenticación
    private lateinit var auth: FirebaseAuth
    // Instancia de FirebaseFirestore para la base de datos
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        // Inicializar vistas
        tilUsername = findViewById(R.id.tilUsername)
        tilPassword = findViewById(R.id.tilPassword)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvGoRegister = findViewById(R.id.tvGoRegister)

        // Inicializar la instancia de Firebase Auth y Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        btnLogin.setOnClickListener {
            checkCredentials()
        }

        tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun checkCredentials() {
        val userInput = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // 1. Validar campos de entrada
        if (userInput.isEmpty()) {
            tilUsername.error = "Correo o Colegiatura es obligatorio"
            return
        } else {
            tilUsername.error = null
        }

        if (password.isEmpty()) {
            tilPassword.error = "La contraseña es obligatoria"
            return
        } else {
            tilPassword.error = null
        }

        // 2. Determinar el tipo de entrada
        if (Patterns.EMAIL_ADDRESS.matcher(userInput).matches()) {
            // La entrada parece un correo electrónico, intentar inicio de sesión directo
            signInWithEmail(userInput, password)
        } else {
            // La entrada se trata como número de colegiatura, se busca el email en Firestore
            searchByColegiaturaAndSignIn(userInput, password)
        }
    }

    private fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Inicio de sesión exitoso con email, ahora obtener datos de Firestore usando el UID
                    fetchDoctorDataAndNavigate(auth.currentUser!!.uid)
                } else {
                    handleAuthFailure(task.exception)
                }
            }
    }

    private fun searchByColegiaturaAndSignIn(colegiatura: String, password: String) {
        Log.d("LoginActivity", "Buscando doctor con colegiatura: $colegiatura") // LOG ADDED

        // Primero, se debe buscar el documento del doctor por su número de colegiatura.
        db.collection("medicos")
            .whereEqualTo("COLEGIATURA", colegiatura)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Log.d("LoginActivity", "No se encontró ningún doctor con la colegiatura: $colegiatura") // LOG ADDED
                    // No se encontró un doctor con esa colegiatura
                    Toast.makeText(this, "Número de colegiatura no encontrado", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("LoginActivity", "Doctor encontrado en Firestore. Intentando autenticación.") // LOG ADDED
                    // Se encontró un doctor, se obtiene el primer documento (asumiendo que es único)
                    val document = querySnapshot.documents[0]
                    // La clave del email en su registro era "CORREO"
                    val email = document.getString("CORREO")

                    if (email != null) {
                        Log.d("LoginActivity", "Correo recuperado: $email") // LOG ADDED
                        // Se utiliza el correo electrónico obtenido para la autenticación
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    // Inicio de sesión exitoso, navega con los datos del doctor
                                    fetchDoctorDataAndNavigate(auth.currentUser!!.uid)
                                } else {
                                    handleAuthFailure(task.exception)
                                }
                            }
                    } else {
                        Log.e("LoginActivity", "El campo 'CORREO' es nulo o no existe en el documento del médico.") // LOG ADDED
                        Toast.makeText(this, "Error: el correo electrónico del doctor no se encontró en Firestore.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                // Error al buscar en Firestore
                Log.e("LoginActivity", "Error al buscar el documento en Firestore", e)
                Toast.makeText(this, "Error al iniciar sesión: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchDoctorDataAndNavigate(uid: String) {
        Log.d("LoginActivity", "Fetch: Intentando obtener datos del doctor con UID: $uid") // LOG DETALLADO
        db.collection("medicos").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val doctorName = document.getString("NOMBRE")
                    val doctorLastName = document.getString("APELLIDO") // Recuperamos el apellido
                    val fullName = if (doctorName != null && doctorLastName != null) {
                        "$doctorName $doctorLastName"
                    } else {
                        doctorName ?: "Doctor" // Usar solo nombre o un valor por defecto
                    }

                    Log.d("LoginActivity", "Fetch: Documento encontrado. Nombre completo recuperado: $fullName") // LOG DETALLADO

                    saveDoctorDataToSharedPreferences(document)
                    Toast.makeText(
                        this@LoginActivity,
                        "Bienvenido ${fullName}", // Usar NOMBRE COMPLETO para el Toast
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Log.e("LoginActivity", "Fetch: Documento no encontrado en 'medicos' para UID: $uid") // LOG DETALLADO
                    Toast.makeText(this, "Error: Datos del doctor no encontrados en Firestore.", Toast.LENGTH_LONG).show()
                    auth.signOut() // Cerrar sesión si no se encuentran los datos
                }
            }
            .addOnFailureListener { e ->
                Log.e("LoginActivity", "Error al obtener datos del doctor por UID", e)
                Toast.makeText(this, "Error al obtener datos de usuario: ${e.message}", Toast.LENGTH_LONG).show()
                auth.signOut()
            }
    }

    private fun saveDoctorDataToSharedPreferences(document: DocumentSnapshot) {
        // ¡IMPORTANTE! Se usan los nombres de las claves que definió en RegisterFragment1/2 para guardar en Firestore
        val sharedPref = getSharedPreferences("ConsultasPeru", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("name", document.getString("NOMBRE"))
            putString("lastName", document.getString("APELLIDO"))
            putString("email", document.getString("CORREO"))
            putString("dni", document.getString("DNI"))
            putString("celular", document.getString("CELULAR"))
            putString("age", document.get("EDAD")?.toString())
            putString("city", document.get("ID_DISTRITO")?.toString()) // Asegurar que los IDs se recuperen como String
            putString("nationality", document.get("ID_NACIONALIDAD")?.toString()) // Asegurar que los IDs se recuperen como String
            putString("especialidad", document.get("ID_ESPECIALIDAD")?.toString()) // Asegurar que los IDs se recuperen como String
            putString("colegiatura", document.getString("COLEGIATURA"))
            putString("university", document.get("ID_UNIVERSIDAD")?.toString()) // Asegurar que los IDs se recuperen como String
            putString("graduationYear", document.get("AÑIO_GRADUACION")?.toString())
            putString("experienceYears", document.get("EXP_ANIOS")?.toString())
            putString("price", document.get("PRECIO")?.toString())
            putString("availability", document.getString("DISPONIBILIDAD"))
            putString("workingHours", document.getString("JORNADA_ATENCION"))
            putString("additionalInfo", document.getString("INFORMACION_ADICIONAL")) // Asumiendo que existe
            putString("profileImageUrl", document.getString("profileImageUrl")) // Si lo va a usar

            apply()
        }
    }

    private fun handleAuthFailure(exception: Exception?) {
        Log.w("LoginActivity", "signIn:failure", exception)
        val errorMessage = when (exception) {
            is FirebaseAuthInvalidCredentialsException -> "Credenciales incorrectas (Contraseña o Correo)"
            is FirebaseAuthInvalidUserException -> "Usuario no registrado"
            else -> "Error de autenticación: ${exception?.message}"
        }
        Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_SHORT).show()
    }
}
