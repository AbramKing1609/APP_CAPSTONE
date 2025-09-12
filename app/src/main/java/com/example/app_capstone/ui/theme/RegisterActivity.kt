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

class RegisterActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var tvPageIndicator: TextView
    private lateinit var btnRegister: Button
    private lateinit var tvLoginLink: TextView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Datos del formulario que se guardarán temporalmente
    private val formData = mutableMapOf<String, Any>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_activity)

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
            val currentFragment = supportFragmentManager.findFragmentByTag("f" + currentItem) as? RegisterFragmentInterface
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
        // Validación final en el último fragmento
        val lastFragment = supportFragmentManager.findFragmentByTag("f2") as? RegisterFragment3
        if (lastFragment?.validateFields() == true) {
            val email = formData["email"] as String
            val password = lastFragment.getPassword()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = task.result?.user?.uid ?: return@addOnCompleteListener
                        formData["uid"] = uid

                        db.collection("doctores").document(uid).set(formData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error guardando datos: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        Toast.makeText(this, "Error de autenticación: ${task.exception?.message}", Toast.LENGTH_LONG).show()
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
    }
}
