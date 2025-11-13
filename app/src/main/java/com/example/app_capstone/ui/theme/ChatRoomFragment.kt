package com.example.app_capstone.ui.ChatRoom

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app_capstone.MainActivity
import com.example.app_capstone.R
import com.example.app_capstone.databinding.FragmentChatRoomBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore

class ChatRoomFragment : Fragment() {

    private var _binding: FragmentChatRoomBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var realtimeDb: DatabaseReference

    private lateinit var messagesAdapter: MessagesAdapter
    private val messagesList = mutableListOf<Message>()

    private var chatRoomId: String = ""
    private var currentUserId: String = ""
    private var medicoId: Long = 0L

    // Datos que vendrán del Intent/Arguments
    private var idCita: Long = 0L
    private var idPaciente: Long = 0L
    private var nombrePaciente: String = ""
    private var especialidad: String = ""

    // Listeners de Firebase
    private var messagesListener: ChildEventListener? = null
    private var typingListener: ValueEventListener? = null
    private var chatRoomListener: ValueEventListener? = null

    // Control de escritura
    private var isTyping = false
    private val typingHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val stopTypingRunnable = Runnable { setTypingStatus(false) }

    companion object {
        private const val ARG_ID_CITA = "id_cita"
        private const val ARG_ID_PACIENTE = "id_paciente"
        private const val ARG_NOMBRE_PACIENTE = "nombre_paciente"
        private const val ARG_ESPECIALIDAD = "especialidad"

        fun newInstance(
            idCita: Long,
            idPaciente: Long,
            nombrePaciente: String,
            especialidad: String
        ): ChatRoomFragment {
            val fragment = ChatRoomFragment()
            val args = Bundle().apply {
                putLong(ARG_ID_CITA, idCita)
                putLong(ARG_ID_PACIENTE, idPaciente)
                putString(ARG_NOMBRE_PACIENTE, nombrePaciente)
                putString(ARG_ESPECIALIDAD, especialidad)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtener datos de los arguments
        obtenerDatosArguments()
    }

    private fun obtenerDatosArguments() {
        try {
            Log.d("ChatRoom", "🔹 Obteniendo datos desde arguments...")

            // 🔹 USAR ESTE MÉTODO QUE SÍ FUNCIONA
            idCita = arguments?.getLong("id_cita") ?: 0L
            idPaciente = arguments?.getLong("id_paciente") ?: 0L
            nombrePaciente = arguments?.getString("nombre_paciente") ?: ""
            especialidad = arguments?.getString("especialidad") ?: "Consulta General"

            Log.d("ChatRoom", "📊 Datos cargados:")
            Log.d("ChatRoom", "   • ID Cita: $idCita")
            Log.d("ChatRoom", "   • ID Paciente: $idPaciente")
            Log.d("ChatRoom", "   • Nombre: '$nombrePaciente'")
            Log.d("ChatRoom", "   • Especialidad: '$especialidad'")

            if (idCita != 0L && idPaciente != 0L && nombrePaciente.isNotEmpty()) {
                Log.d("ChatRoom", "✅ Todos los datos están completos")
                // Proceder con la inicialización
                initFirebase()
                setupBackPressHandler()
                obtenerDatosMedico()
            } else {
                Log.e("ChatRoom", "❌ Datos incompletos")
                mostrarErrorDatosIncompletos()
            }

        } catch (e: Exception) {
            Log.e("ChatRoom", "💥 Error al obtener argumentos: ${e.message}", e)
            mostrarErrorDatosIncompletos()
        }
    }

    private fun initFirebase() {
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        realtimeDb = FirebaseDatabase.getInstance().reference
        currentUserId = auth.currentUser?.uid ?: ""
    }

    private fun setupBackPressHandler() {
        // Interceptar botón atrás del sistema
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    mostrarDialogoSalir()
                }
            }
        )

        // Botón atrás de la UI
        binding.btnBack.setOnClickListener {
            mostrarDialogoSalir()
        }
    }

    private fun obtenerDatosMedico() {
        mostrarLoading(true)

        val userEmail = auth.currentUser?.email
        if (userEmail == null) {
            Toast.makeText(requireContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        // Buscar ID de usuario por email
        firestore.collection("usuario")
            .whereEqualTo("CORREO", userEmail)
            .get()
            .addOnSuccessListener { usuarios ->
                if (!usuarios.isEmpty) {
                    val idUsuario = usuarios.documents.first().getLong("ID_USUARIO")

                    // Buscar ID de médico
                    firestore.collection("medicos")
                        .whereEqualTo("ID_USUARIO", idUsuario)
                        .get()
                        .addOnSuccessListener { medicos ->
                            if (!medicos.isEmpty) {
                                medicoId = medicos.documents.first().getLong("ID_MEDICO") ?: 0L
                                val nombreMedico = medicos.documents.first().getString("NOMBRE") ?: ""
                                val apellidoMedico = medicos.documents.first().getString("APELLIDO") ?: ""

                                inicializarChat(nombreMedico, apellidoMedico)
                            } else {
                                mostrarError("No se encontró información del médico")
                            }
                        }
                        .addOnFailureListener { e ->
                            mostrarError("Error al buscar médico: ${e.message}")
                        }
                } else {
                    mostrarError("Usuario no encontrado")
                }
            }
            .addOnFailureListener { e ->
                mostrarError("Error al buscar usuario: ${e.message}")
            }
    }

    private fun inicializarChat(nombreMedico: String, apellidoMedico: String) {
        // Crear ID único para el chat room basado en la cita
        chatRoomId = "chat_cita_${idCita}_paciente_${idPaciente}_medico_${medicoId}"

        Log.d("ChatRoom", "Inicializando chat DOCTOR: $chatRoomId")

        // Configurar UI
        setupUI(nombreMedico, apellidoMedico)
        setupRecyclerView()
        setupInputListeners()

        // Verificar/Crear chat room en Realtime Database
        verificarOCrearChatRoom(nombreMedico, apellidoMedico)

        // Escuchar mensajes
        listenToMessages()

        // Escuchar estado de escritura
        listenToTypingStatus()

        // Escuchar estado del chat room
        listenToChatRoomStatus()

        mostrarLoading(false)
    }

    private fun setupUI(nombreMedico: String, apellidoMedico: String) {
        // ✅ DOCTOR: Mostrar nombre del paciente en la cabecera
        binding.tvUserName.text = nombrePaciente
        binding.tvUserRole.text = "Paciente"
        binding.tvStatus.text = "En línea"

        binding.btnEndConsultation.setOnClickListener {
            mostrarDialogoFinalizarConsulta()
        }
    }

    private fun setupRecyclerView() {
        messagesAdapter = MessagesAdapter(messagesList, currentUserId)

        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true // Mostrar desde el final
            }
            adapter = messagesAdapter
        }
    }

    private fun setupInputListeners() {
        // Botón enviar
        binding.btnSend.setOnClickListener {
            enviarMensaje()
        }

        // Listener de escritura
        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty() && !isTyping) {
                    setTypingStatus(true)
                }

                // Resetear el timer de "dejó de escribir"
                typingHandler.removeCallbacks(stopTypingRunnable)
                typingHandler.postDelayed(stopTypingRunnable, 2000)
            }

            override fun afterTextChanged(s: Editable?) {
                binding.btnSend.isEnabled = !s.isNullOrBlank()
            }
        })

        // Botón adjuntar (opcional - puedes implementarlo después)
        binding.btnAttach.setOnClickListener {
            Toast.makeText(requireContext(), "Función próximamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun verificarOCrearChatRoom(nombreMedico: String, apellidoMedico: String) {
        val chatRoomRef = realtimeDb.child("chatRooms").child(chatRoomId)

        chatRoomRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                // Crear nuevo chat room
                val chatRoom = ChatRoom(
                    chatRoomId = chatRoomId,
                    citaId = idCita,
                    pacienteId = idPaciente,
                    medicoId = medicoId,
                    pacienteName = nombrePaciente,
                    medicoName = "$nombreMedico $apellidoMedico",
                    especialidad = especialidad,
                    status = "active",
                    createdAt = System.currentTimeMillis(),
                    lastMessageTime = System.currentTimeMillis(),
                    lastMessage = "Chat iniciado"
                )

                chatRoomRef.setValue(chatRoom)
                    .addOnSuccessListener {
                        Log.d("ChatRoom", "Chat room creado exitosamente")
                    }
                    .addOnFailureListener { e ->
                        Log.e("ChatRoom", "Error al crear chat room: ${e.message}")
                    }
            } else {
                // Verificar si el chat está activo
                val status = snapshot.child("status").getValue(String::class.java)
                if (status == "completed" || status == "cancelled") {
                    mostrarChatFinalizado()
                }
            }
        }
    }

    private fun listenToMessages() {
        // 🔹 PREVENIR MÚLTIPLES LISTENERS
        if (messagesListener != null) {
            Log.w("ChatRoom", "⚠️ Ya existe un listener activo, removiendo...")
            val messagesRef = realtimeDb.child("chatRooms").child(chatRoomId).child("messages")
            messagesRef.removeEventListener(messagesListener!!)
        }

        Log.d("ChatRoom", "🎯 Creando NUEVO listener de mensajes")

        val messagesRef = realtimeDb.child("chatRooms").child(chatRoomId).child("messages")

        messagesListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)
                if (message != null) {
                    Log.d("ChatRoom", "Mensaje recibido: ${message.message}")

                    // ✅ AGREGAR directamente a la lista
                    messagesList.add(message)

                    // ✅ NOTIFICAR al adapter que se agregó un item
                    messagesAdapter.notifyItemInserted(messagesList.size - 1)

                    // ✅ Log para debug
                    Log.d("ChatRoom", "Lista ahora tiene ${messagesList.size} mensajes")

                    // ✅ Scroll después de agregar
                    scrollToBottom()

                    // Marcar como leído (si el mensaje es del paciente)
                    if (message.senderId != currentUserId && message.status != "read") {
                        marcarMensajeComoLeido(message.messageId)
                    }

                    // Ocultar empty state
                    binding.emptyStateLayout.visibility = View.GONE
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)
                if (message != null) {
                    val index = messagesList.indexOfFirst { it.messageId == message.messageId }
                    if (index != -1) {
                        messagesList[index] = message
                        messagesAdapter.notifyItemChanged(index)
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRoom", "Error al escuchar mensajes: ${error.message}")
            }
        }

        messagesRef.addChildEventListener(messagesListener!!)
    }

    // ✅ NUEVA FUNCIÓN: Scroll seguro al final
    private fun scrollToBottom() {
        try {
            val size = messagesList.size
            Log.d("ChatRoom", "scrollToBottom - Lista size: $size, Adapter count: ${messagesAdapter.itemCount}")

            if (size == 0) {
                Log.w("ChatRoom", "Lista de mensajes vacía, no se puede hacer scroll")
                return
            }

            if (messagesAdapter.itemCount == 0) {
                Log.w("ChatRoom", "Adapter vacío, esperando...")
                // Reintentar después de un delay
                binding.rvMessages.postDelayed({
                    if (messagesAdapter.itemCount > 0) {
                        binding.rvMessages.scrollToPosition(messagesAdapter.itemCount - 1)
                    }
                }, 100)
                return
            }

            val lastPosition = size - 1
            Log.d("ChatRoom", "Haciendo scroll a posición: $lastPosition")

            binding.rvMessages.post {
                try {
                    if (binding.rvMessages.isAttachedToWindow && lastPosition >= 0) {
                        binding.rvMessages.scrollToPosition(lastPosition)
                        Log.d("ChatRoom", "✅ Scroll completado")
                    }
                } catch (e: Exception) {
                    Log.e("ChatRoom", "Error en post scroll: ${e.message}")
                }
            }

        } catch (e: Exception) {
            Log.e("ChatRoom", "Error al hacer scroll: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun enviarMensaje() {
        val messageText = binding.etMessage.text.toString().trim()
        if (messageText.isEmpty()) return

        val messageId = realtimeDb.child("chatRooms").child(chatRoomId)
            .child("messages").push().key ?: return

        // ✅ DOCTOR: El senderType ahora es "medico"
        val message = Message(
            messageId = messageId,
            senderId = currentUserId,
            senderType = "medico",
            senderName = "Doctor",
            message = messageText,
            timestamp = System.currentTimeMillis(),
            status = "sent",
            type = "text"
        )

        realtimeDb.child("chatRooms").child(chatRoomId)
            .child("messages").child(messageId)
            .setValue(message)
            .addOnSuccessListener {
                Log.d("ChatRoom", "Mensaje enviado exitosamente")

                // Actualizar último mensaje
                val updates = hashMapOf<String, Any>(
                    "lastMessage" to messageText,
                    "lastMessageTime" to System.currentTimeMillis()
                )

                realtimeDb.child("chatRooms").child(chatRoomId)
                    .updateChildren(updates)

                // Limpiar campo de texto
                binding.etMessage.text?.clear()

                // Detener indicador de escritura
                setTypingStatus(false)

                // ✅ Scroll al final después de enviar
                scrollToBottom()
            }
            .addOnFailureListener { e ->
                Log.e("ChatRoom", "Error al enviar mensaje: ${e.message}")
                Toast.makeText(
                    requireContext(),
                    "Error al enviar mensaje",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun listenToTypingStatus() {
        // ✅ DOCTOR: Escuchar cuando el PACIENTE está escribiendo
        val typingRef = realtimeDb.child("chatRooms").child(chatRoomId).child("pacienteTyping")

        typingListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isTyping = snapshot.getValue(Boolean::class.java) ?: false
                binding.typingIndicator.visibility = if (isTyping) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRoom", "Error al escuchar typing: ${error.message}")
            }
        }

        typingRef.addValueEventListener(typingListener!!)
    }

    private fun listenToChatRoomStatus() {
        val statusRef = realtimeDb.child("chatRooms").child(chatRoomId).child("status")

        chatRoomListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java)
                if (status == "completed" || status == "cancelled") {
                    mostrarChatFinalizado()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRoom", "Error al escuchar status: ${error.message}")
            }
        }

        statusRef.addValueEventListener(chatRoomListener!!)
    }

    private fun setTypingStatus(typing: Boolean) {
        isTyping = typing
        // ✅ DOCTOR: Indicar que el MÉDICO está escribiendo
        realtimeDb.child("chatRooms").child(chatRoomId)
            .child("medicoTyping")
            .setValue(typing)
    }

    private fun marcarMensajeComoLeido(messageId: String) {
        realtimeDb.child("chatRooms").child(chatRoomId)
            .child("messages").child(messageId)
            .child("status")
            .setValue("read")
    }

    private fun mostrarDialogoSalir() {
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ ¿Salir de la consulta?")
            .setMessage("Si sales de la consulta, se marcará como finalizada y no podrás volver a ingresar.\n\n¿Estás seguro de que deseas salir?")
            .setPositiveButton("Sí, salir") { dialog, _ ->
                finalizarConsulta()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun mostrarDialogoFinalizarConsulta() {
        AlertDialog.Builder(requireContext())
            .setTitle("Finalizar Consulta")
            .setMessage("¿Estás seguro de que deseas finalizar la consulta con $nombrePaciente?")
            .setPositiveButton("Finalizar") { dialog, _ ->
                finalizarConsulta()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun finalizarConsulta() {
        mostrarLoading(true)

        // 1. Marcar chat room como completado en Realtime Database
        val updates = hashMapOf<String, Any>(
            "status" to "completed",
            "completedAt" to System.currentTimeMillis()
        )

        realtimeDb.child("chatRooms").child(chatRoomId)
            .updateChildren(updates)
            .addOnSuccessListener {
                Log.d("ChatRoom", "Chat room marcado como completado")

                // 2. Actualizar estado de la cita en Firestore
                actualizarEstadoCita()
            }
            .addOnFailureListener { e ->
                Log.e("ChatRoom", "Error al finalizar chat: ${e.message}")
                mostrarLoading(false)
                Toast.makeText(
                    requireContext(),
                    "Error al finalizar consulta",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun actualizarEstadoCita() {
        firestore.collection("cita")
            .whereEqualTo("ID_CITA", idCita)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents.first()
                    document.reference.update(
                        mapOf(
                            "ESTADO" to "completada",
                            "FECHA_FIN_CONSULTA" to com.google.firebase.Timestamp.now()
                        )
                    ).addOnSuccessListener {
                        Log.d("ChatRoom", "Cita marcada como completada")
                        mostrarLoading(false)

                        Toast.makeText(
                            requireContext(),
                            "Consulta finalizada exitosamente",
                            Toast.LENGTH_SHORT
                        ).show()

                        // ✅ REGRESAR A MainActivity (pantalla principal del médico)
                        val intent = Intent(requireContext(), MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        activity?.finish()
                    }.addOnFailureListener { e ->
                        Log.e("ChatRoom", "Error al actualizar cita: ${e.message}")
                        mostrarLoading(false)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatRoom", "Error al buscar cita: ${e.message}")
                mostrarLoading(false)
            }
    }

    private fun mostrarChatFinalizado() {
        if (!isAdded || activity == null) {
            Log.w("ChatRoom", "Fragment no attached")
            return
        }

        try {
            AlertDialog.Builder(requireContext())
                .setTitle("Consulta Finalizada")
                .setMessage("Esta consulta ha sido finalizada. No puedes enviar más mensajes.")
                .setPositiveButton("Entendido") { dialog, _ ->
                    dialog.dismiss()
                    navegarAtras()
                }
                .setCancelable(false)
                .show()
        } catch (e: Exception) {
            Log.e("ChatRoom", "Error: ${e.message}")
            navegarAtras()
        }
    }

    private fun navegarAtras() {
        try {
            if (!isAdded || view == null || activity == null) {
                return
            }

            // ✅ USAR MainActivity (pantalla principal del médico)
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            activity?.finish()
        } catch (e: Exception) {
            Log.e("ChatRoom", "Error navegando: ${e.message}")
        }
    }

    private fun mostrarLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun mostrarError(mensaje: String) {
        mostrarLoading(false)
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
        Log.e("ChatRoom", mensaje)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Remover listeners
        messagesListener?.let {
            realtimeDb.child("chatRooms").child(chatRoomId)
                .child("messages").removeEventListener(it)
        }

        typingListener?.let {
            realtimeDb.child("chatRooms").child(chatRoomId)
                .child("pacienteTyping").removeEventListener(it)
        }

        chatRoomListener?.let {
            realtimeDb.child("chatRooms").child(chatRoomId)
                .child("status").removeEventListener(it)
        }

        // Detener indicador de escritura
        setTypingStatus(false)
        typingHandler.removeCallbacks(stopTypingRunnable)

        _binding = null
    }

    private fun diagnosticarDatosRecibidos() {
        Log.d("ChatRoom", "=== DIAGNÓSTICO DATOS RECIBIDOS ===")
        Log.d("ChatRoom", "Arguments: ${arguments}")

        arguments?.keySet()?.forEach { key ->
            val value = when (val obj = arguments?.get(key)) {
                is Long -> obj.toString()
                is String -> obj
                else -> obj?.toString() ?: "null"
            }
            Log.d("ChatRoom", "   $key: $value")
        }

        Log.d("ChatRoom", "idCita: $idCita")
        Log.d("ChatRoom", "idPaciente: $idPaciente")
        Log.d("ChatRoom", "nombrePaciente: $nombrePaciente")
        Log.d("ChatRoom", "especialidad: $especialidad")
        Log.d("ChatRoom", "=== FIN DIAGNÓSTICO ===")

        if (idCita == 0L || idPaciente == 0L) {
            Log.e("ChatRoom", "❌ ERROR: Datos incompletos detectados")
            // Mostrar diálogo de error pero NO cerrar la app
            mostrarErrorDatosIncompletos()
        } else {
            // Inicializar normalmente
            initFirebase()
            setupBackPressHandler()
            obtenerDatosMedico()
        }
    }

    private fun mostrarErrorDatosIncompletos() {
        try {
            val mensaje = """
            No se pudieron cargar los datos del chat.
            
            Valores actuales:
            • ID Cita: $idCita
            • ID Paciente: $idPaciente
            • Nombre: ${if (nombrePaciente.isEmpty()) "VACÍO" else nombrePaciente}
            
            Por favor, vuelve atrás e intenta nuevamente.
        """.trimIndent()

            AlertDialog.Builder(requireContext())
                .setTitle("Error de Datos")
                .setMessage(mensaje)
                .setPositiveButton("Volver") { dialog, _ ->
                    parentFragmentManager.popBackStack()
                    dialog.dismiss()
                }
                .setCancelable(false)
                .show()
        } catch (e: Exception) {
            Log.e("ChatRoom", "Error al mostrar diálogo, forzando regreso: ${e.message}")
            parentFragmentManager.popBackStack()
        }
    }

}