package com.example.app_capstone

data class Notification(
    var id: String = "",
    val doctorId: String = "",
    val titulo: String = "",
    val mensaje: String = "",
    val timestamp: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
    val leida: Boolean = false
)