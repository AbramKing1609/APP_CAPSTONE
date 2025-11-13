package com.example.app_capstone.ui.ChatRoom

import com.google.firebase.database.PropertyName

data class Message(
    @PropertyName("messageId")
    var messageId: String = "",

    @PropertyName("senderId")
    var senderId: String = "", // ID del usuario (paciente o médico)

    @PropertyName("senderType")
    var senderType: String = "", // "paciente" o "medico"

    @PropertyName("senderName")
    var senderName: String = "",

    @PropertyName("message")
    var message: String = "",

    @PropertyName("timestamp")
    var timestamp: Long = 0L,

    @PropertyName("status")
    var status: String = "sent", // "sent", "delivered", "read"

    @PropertyName("type")
    var type: String = "text" // "text", "image", "file"
) {
    // Constructor vacío requerido por Firebase
    constructor() : this("", "", "", "", "", 0L, "sent", "text")

    fun getFormattedTime(): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        return String.format("%02d:%02d", hour, minute)
    }
}

data class ChatRoom(
    @PropertyName("chatRoomId")
    var chatRoomId: String = "",

    @PropertyName("citaId")
    var citaId: Long = 0L,

    @PropertyName("pacienteId")
    var pacienteId: Long = 0L,

    @PropertyName("medicoId")
    var medicoId: Long = 0L,

    @PropertyName("pacienteName")
    var pacienteName: String = "",

    @PropertyName("medicoName")
    var medicoName: String = "",

    @PropertyName("especialidad")
    var especialidad: String = "",

    @PropertyName("status")
    var status: String = "active", // "active", "completed", "cancelled"

    @PropertyName("createdAt")
    var createdAt: Long = 0L,

    @PropertyName("lastMessageTime")
    var lastMessageTime: Long = 0L,

    @PropertyName("lastMessage")
    var lastMessage: String = "",

    @PropertyName("medicoTyping")
    var medicoTyping: Boolean = false,

    @PropertyName("pacienteTyping")
    var pacienteTyping: Boolean = false
) {
    constructor() : this("", 0L, 0L, 0L, "", "", "", "active", 0L, 0L, "", false, false)
}