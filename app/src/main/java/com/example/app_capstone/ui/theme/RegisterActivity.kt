package com.example.app_capstone

import android.content.Intent
import android.os.Bundle
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*


class RegisterActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var tvPageIndicator: TextView
    private lateinit var btnRegister: Button
    private lateinit var tvLoginLink: TextView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Datos del formulario que se guardarán temporalmente, usando los nombres de los campos de Firestore
    private val formData = mutableMapOf<String, Any>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_activity)

        // Nota: Las importaciones de Firebase AppCheck y Functions han sido eliminadas ya que no se usan en este código.

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
            // Obtener el fragmento actual por el tag para forzar la validación
            val currentFragment = supportFragmentManager.findFragmentByTag("f" + viewPager.adapter!!.getItemId(currentItem)) as? RegisterFragmentInterface
            if (currentFragment != null && currentFragment.validateFields()) {
                viewPager.currentItem = currentItem + 1
            }
        }

        btnBack.setOnClickListener {
            val currentItem = viewPager.currentItem
            viewPager.currentItem = currentItem - 1
        }

        btnRegister.setOnClickListener {
            registerDoctor()
        }

        tvLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun updateUIForPage(position: Int) {
        tvPageIndicator.text = "${position + 1}/3"
        btnBack.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
        btnNext.visibility = if (position == 2) View.INVISIBLE else View.VISIBLE
        btnRegister.visibility = if (position == 2) View.VISIBLE else View.GONE
        tvLoginLink.visibility = if (position == 2) View.GONE else View.VISIBLE
    }

    private fun registerDoctor() {
        // 1. Validación final en el último fragmento
        val lastFragment = supportFragmentManager.findFragmentByTag("f" + viewPager.adapter!!.getItemId(2)) as? RegisterFragment3
        if (lastFragment?.validateFields() == true) {
            val email = formData["CORREO"] as String
            val password = lastFragment.getPassword()
            val aceptoTerminos = formData["TERMINOSACEPTADOS"] as Boolean

            // 2. Crear usuario en Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        val uid = authTask.result?.user?.uid ?: return@addOnCompleteListener
                        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                        // Campos que se inicializan en el proceso de registro
                        formData["ID_MEDICO"] = uid
                        formData["ID_USUARIO"] = uid
                        formData["FECHAACEPTACION"] = currentDate
                        formData["PACIENTES_ATENDIDOS"] = 0
                        formData["FOTO_PERFIL"] = "" // Placeholder para la URL de la foto
                        formData["TERMINOSACEPTADOS"] = aceptoTerminos // Se asegura de guardar el booleano final

                        // 3. Crear registro en la colección 'usuario' (Auth data)
                        val userData = hashMapOf(
                            "ID_USUARIO" to uid,
                            "CORREO" to email,
                            "CONTRASEÑA" to password, // Nota: Esto es solo para fines de demo. En producción, solo se guardaría un hash.
                            "FECHA_REGISTRO" to currentDate,
                            "TIPO_USUARIO" to "medico"
                        )

                        db.collection("usuario").document(uid).set(userData)
                            .addOnSuccessListener {
                                // 4. Crear registro en la colección 'medicos' (Profile data)
                                db.collection("medicos").document(uid).set(formData)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, LoginActivity::class.java))
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        // Si falla el guardado en 'medicos', se debe considerar eliminar el usuario de Auth
                                        auth.currentUser?.delete()
                                        Toast.makeText(this, "Error guardando datos del perfil: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                            .addOnFailureListener { e ->
                                // Si falla el guardado en 'usuario', se debe considerar eliminar el usuario de Auth
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

    // Interfaz para los fragmentos de registro
    interface RegisterFragmentInterface {
        fun validateFields(): Boolean
    }

    // Adaptador para el ViewPager
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

        // Se sobrescribe este método para que findFragmentByTag funcione correctamente
        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun containsItem(itemId: Long): Boolean {
            return itemId >= 0 && itemId < itemCount
        }
    }
}
