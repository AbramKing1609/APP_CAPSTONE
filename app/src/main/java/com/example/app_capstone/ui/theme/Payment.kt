package com.example.app_capstone

data class Payment(
    var id: String = "",
    val doctorId: String = "",
    val monto: Double = 0.0,
    val fecha: String = "",
    val estado: String = "",
    val descripcion: String = ""
)