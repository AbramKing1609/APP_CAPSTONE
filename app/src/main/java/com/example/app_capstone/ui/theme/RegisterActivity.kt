package com.example.app_capstone

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class RegisterActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var tvPageIndicator: TextView
    private lateinit var btnRegister: Button
    private lateinit var tvLoginLink: TextView
    private lateinit var functions: FirebaseFunctions
    // INSTANCIA DE CLIENTE OKHTTP
    private val client = OkHttpClient.Builder()
        // AUMENTAMOS EL TIEMPO DE ESPERA (TIMEOUT) A 60 SEGUNDOS
        // PARA DARLE TIEMPO A SELENIUM DE COMPLETAR EL SCRAPING
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Datos del formulario que se guardarán temporalmente, usando los nombres de los campos de Firestore
    private val formData = mutableMapOf<String, Any>()

    /**
     * Función auxiliar para normalizar cadenas:
     * 1. Elimina acentos (diacríticos).
     * 2. Elimina cualquier carácter que no sea letra o espacio (ej: guiones, apóstrofes).
     * 3. Convierte a minúsculas.
     * 4. Convierte múltiples espacios en uno solo y recorta los bordes.
     */
    private fun normalizeString(input: String): String {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "") // Elimina diacríticos (acentos)
            .replace(Regex("[^a-zA-Z\\s]"), "") // ELIMINA CUALQUIER CARACTER QUE NO SEA LETRA O ESPACIO (CRÍTICO)
            .lowercase(Locale.ROOT) // Convierte a minúsculas
            .replace(Regex("\\s+"), " ") // Reemplaza múltiples espacios con uno solo
            .trim()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_activity)

        FirebaseApp.initializeApp(this)
        // Inicialización de App Check (necesario para validarDNI)
        Firebase.appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance(),
        )
        functions = Firebase.functions("us-central1")
        initViews()
        setupViewPager()
        setupListeners()
    }

    private fun initViews() {
        viewPager = findViewById(R.id.viewPager)
        btnNext = findViewById(R.id.btnNext)
        btnBack = findViewById(R.id.btnBack)
        tvPageIndicator = findViewById(R.id.tvPageIndicator)
        btnRegister = findViewById(R.id.btnRegister)
        tvLoginLink = findViewById(R.id.tvLoginLink)
    }

    private fun setupViewPager() {
        val pagerAdapter = RegisterPagerAdapter(this)
        viewPager.adapter = pagerAdapter
        viewPager.isUserInputEnabled = false // Deshabilita el deslizamiento manual

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateUIForPage(position)
            }
        })
    }

    private fun setupListeners() {
        btnNext.setOnClickListener {
            val currentItem = viewPager.currentItem
            val currentFragment = supportFragmentManager.findFragmentByTag("f${viewPager.adapter!!.getItemId(currentItem)}") as? RegisterFragmentInterface
            if (currentFragment != null && currentFragment.validateFields()) {
                viewPager.currentItem = currentItem + 1
            }
        }

        btnBack.setOnClickListener {
            val currentItem = viewPager.currentItem
            viewPager.currentItem = currentItem - 1
        }

        btnRegister.setOnClickListener {
            val dni = formData["DNI"]?.toString() ?: ""
            val nombre = formData["NOMBRE"]?.toString() ?: ""
            val apellido = formData["APELLIDO"]?.toString() ?: ""
            val colegiatura = formData["COLEGIATURA"]?.toString() ?: ""

            if (dni.isEmpty() || nombre.isEmpty() || apellido.isEmpty() || colegiatura.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (colegiatura.length < 4 || !colegiatura.matches(Regex("\\d+"))) {
                Toast.makeText(this, "El número de colegiatura debe ser numérico y tener al menos 4 dígitos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("RegisterActivity", "Enviando DNI: $dni, Nombre: $nombre, Apellido: $apellido, Colegiatura: $colegiatura")
            controlDNi(dni, nombre, apellido)
        }

        tvLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun controlDNi(dni: String, nombre: String, apellido: String) {
        val dato = hashMapOf("dni" to dni, "nombre" to nombre, "apellido" to apellido)
        Log.d("controlDNi", "Datos enviados a validarDNI (Firebase Function): $dato")

        functions.getHttpsCallable("validarDNI")
            .call(dato)
            .addOnSuccessListener { result ->
                try {
                    val dnis = result.data as? Map<*, *>
                    if (dnis != null) {
                        val valido = dnis["esValido"] as? Boolean ?: false
                        val coincide = dnis["coincide"] as? Boolean ?: false
                        val mensaje = dnis["mensaje"] as? String ?: ""

                        Log.d("controlDNi", "Respuesta de validarDNI: esValido=$valido, coincide=$coincide, mensaje=$mensaje")

                        if (valido && coincide) {
                            Toast.makeText(this, "DNI verificado", Toast.LENGTH_SHORT).show()
                            val colegiatura = formData["COLEGIATURA"]?.toString() ?: ""
                            if (colegiatura.isEmpty()) {
                                Log.e("controlDNi", "COLEGIATURA está vacía en formData")
                                Toast.makeText(this, "Error: Número de colegiatura no disponible", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }
                            Log.d("controlDNi", "DNI válido, validando colegiatura (API Local): $colegiatura")
                            controlColegiatura(colegiatura, nombre, apellido)
                        } else if (valido && !coincide) {
                            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
                        } else {
                            Log.e("controlDNi", "DNI no válido")
                            Toast.makeText(this, "Error: DNI no válido", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("controlDNi", "Respuesta nula de validarDNI")
                        Toast.makeText(this, "Error: Respuesta inválida del servidor", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("controlDNi", "Error procesando respuesta: ${e.message}", e)
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("controlDNi", "Error al validar DNI: ${e.message}", e)
                Toast.makeText(this, "Error al validar DNI: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun controlColegiatura(collegiateNumber: String, nombre: String, apellido: String) {
        // Usamos 10.0.2.2 para acceder al localhost de la máquina host desde el emulador.
        val url = "http://10.0.2.2:5000/api/v1/medico/$collegiateNumber"
        val request = Request.Builder()
            .url(url)
            .build()

        Log.d("controlColegiatura", "Llamando a API local: $url")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Log.e("controlColegiatura", "Fallo de conexión OkHttp: ${e.message}", e)
                    // Mostrar error de timeout/conexión
                    Toast.makeText(this@RegisterActivity, "Error de conexión con la API: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = it.body?.string()
                    runOnUiThread {
                        if (!it.isSuccessful) {
                            // Errores HTTP 4xx o 5xx
                            Log.e("controlColegiatura", "Error HTTP: ${it.code}, Cuerpo: $responseBody")
                            Toast.makeText(this@RegisterActivity, "Error en la API: Código ${it.code}", Toast.LENGTH_LONG).show()
                            return@runOnUiThread
                        }

                        try {
                            // Procesamiento de la respuesta JSON (Ej: 200 OK)
                            val json = JSONObject(responseBody ?: "{}")
                            val status = json.optString("status")

                            if (status == "Encontrado") {
                                // 1. NORMALIZAR DATOS DE LA API (Eliminar acentos, caracteres especiales y minúsculas)
                                val nombresAPIString = normalizeString(json.optString("nombres"))
                                val apellidoPaternoAPI = normalizeString(json.optString("apellido_paterno"))
                                val apellidoMaternoAPI = normalizeString(json.optString("apellido_materno"))

                                // 2. NORMALIZAR DATOS INGRESADOS POR EL USUARIO
                                val nombreInput = normalizeString(nombre)
                                val apellidoInput = normalizeString(apellido)

                                // Concatenar el apellido completo normalizado de la API para la comparación
                                val apellidoCompletoAPI = "$apellidoPaternoAPI $apellidoMaternoAPI".trim()

                                // Coincidencia de Nombre: Comprueba que CADA palabra ingresada por el usuario
                                // esté contenida en la cadena completa de nombres de la API.
                                val nombreInputWords = nombreInput.split(Regex("\\s+")).filter { it.isNotEmpty() }

                                // Si el usuario ingresa una o más palabras, todas ellas deben estar en los nombres de la API.
                                val nombreCoincide = nombreInputWords.all { word ->
                                    nombresAPIString.contains(word)
                                }

                                // Coincidencia de Apellido: Comprueba que el Input coincida con el Paterno, el Materno, o el Completo.
                                val apellidoCoincide = apellidoPaternoAPI == apellidoInput ||
                                        apellidoMaternoAPI == apellidoInput ||
                                        apellidoCompletoAPI == apellidoInput // <- NUEVA VERIFICACIÓN PARA AP. COMPLETOS

                                Log.d("controlColegiatura", "API Nombres: '$nombresAPIString', ApellidoP: '$apellidoPaternoAPI', ApellidoM: '$apellidoMaternoAPI' (NORMALIZADOS)")
                                Log.d("controlColegiatura", "Input Nombre: '$nombreInput' (Words: $nombreInputWords), Apellido: '$apellidoInput' (NORMALIZADOS)")
                                Log.d("controlColegiatura", "Coincide Nombre: $nombreCoincide, Coincide Apellido: $apellidoCoincide")


                                if (nombreCoincide && apellidoCoincide) {
                                    Toast.makeText(this@RegisterActivity, "Colegiatura verificada", Toast.LENGTH_SHORT).show()
                                    registerDoctor()
                                } else {
                                    Log.w("controlColegiatura", "Datos no coinciden. Nombres API: $nombresAPIString, Apellidos API: $apellidoPaternoAPI/$apellidoMaternoAPI, Input: $nombreInput $apellidoInput")
                                    // Se mantiene el mensaje de error con la sugerencia, aunque las posibilidades de fallo se reducen.
                                    Toast.makeText(this@RegisterActivity, "El nombre o apellido ingresado no coincide con los datos del CMP. Verifique mayúsculas/minúsculas o nombres completos.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                // Status: No encontrado, Error, etc.
                                val mensaje = json.optString("message", "Colegiatura no válida o no encontrada.")
                                Log.w("controlColegiatura", "Estado de API: $status, Mensaje: $mensaje")
                                Toast.makeText(this@RegisterActivity, mensaje, Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Log.e("controlColegiatura", "Error al parsear JSON: ${e.message}", e)
                            Toast.makeText(this@RegisterActivity, "Error procesando respuesta de API: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        })
    }

    private fun updateUIForPage(position: Int) {
        tvPageIndicator.text = "${position + 1}/3"
        btnBack.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
        btnNext.visibility = if (position == 2) View.INVISIBLE else View.VISIBLE
        btnRegister.visibility = if (position == 2) View.VISIBLE else View.GONE
        tvLoginLink.visibility = if (position == 2) View.GONE else View.VISIBLE
    }

    private fun registerDoctor() {
        val lastFragment = supportFragmentManager.findFragmentByTag("f${viewPager.adapter!!.getItemId(2)}") as? RegisterFragment3
        if (lastFragment?.validateFields() == true) {
            val email = formData["CORREO"] as String
            val password = lastFragment.getPassword()
            val aceptoTerminos = formData["TERMINOSACEPTADO"] as? Boolean ?: false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        val uid = authTask.result?.user?.uid ?: return@addOnCompleteListener
                        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                        formData["ID_MEDICO"] = uid
                        formData["ID_USUARIO"] = uid
                        formData["FECHAACEPTACION"] = currentDate
                        formData["PACIENTES_ATENDIDOS"] = 0
                        formData["FOTO_PERFIL"] = ""
                        formData["TERMINOSACEPTADO"] = aceptoTerminos

                        val userData = hashMapOf(
                            "ID_USUARIO" to uid,
                            "CORREO" to email,
                            "CONTRASEÑA" to password, // Nota: No guardar en producción
                            "FECHA_REGISTRO" to currentDate,
                            "TIPO_USUARIO" to "medico"
                        )

                        db.collection("usuario").document(uid).set(userData)
                            .addOnSuccessListener {
                                db.collection("medicos").document(uid).set(formData)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, LoginActivity::class.java))
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        auth.currentUser?.delete()
                                        Toast.makeText(this, "Error guardando datos del perfil: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                            .addOnFailureListener { e ->
                                auth.currentUser?.delete()
                                Toast.makeText(this, "Error guardando datos de usuario: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        Toast.makeText(this, "Error de autenticación: ${authTask.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    fun saveFormData(key: String, value: Any) {
        formData[key] = value
    }

    interface RegisterFragmentInterface {
        fun validateFields(): Boolean
    }

    private inner class RegisterPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> RegisterFragment1()
                1 -> RegisterFragment2()
                2 -> RegisterFragment3()
                else -> throw IllegalStateException("Invalid position: $position")
            }
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun containsItem(itemId: Long): Boolean {
            return itemId >= 0 && itemId < itemCount
        }
    }
}
