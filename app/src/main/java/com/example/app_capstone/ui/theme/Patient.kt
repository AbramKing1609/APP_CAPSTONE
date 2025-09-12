package com.example.app_capstone

data class Patient(
    var id: String = "",
    val doctorId: String = "",
    val nombre: String = "",
    val apellido: String = "",
    val edad: Int = 0,
    val telefono: String = "",
    val email: String = "",
    val historial: String = ""
)