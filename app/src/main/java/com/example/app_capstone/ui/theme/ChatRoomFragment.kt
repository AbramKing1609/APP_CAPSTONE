package com.example.app_capstone.ui.ChatRoom

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
        obtenerDatosArguments()
    }

    private fun obtenerDatosArguments() {
        try {
            Log.d("ChatRoom", "🔹 Obteniendo datos desde arguments...")

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
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    mostrarDialogoSalir()
                }
            }
        )

        binding.btnBack.setOnClickListener {
            mostrarDialogoSalir()
        }
    }

    private fun obtenerDatosMedico() {
        mostrarLoading(true)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            mostrarLoading(false)
            return
        }

        Log.d("ChatRoom", "🔍 Buscando médico para usuario UID: ${currentUser.uid}")

        firestore.collection("medicos")
            .whereEqualTo("ID_FIREBASE", currentUser.uid)
            .get()
            .addOnSuccessListener { medicos ->
                if (!medicos.isEmpty) {
                    val medicoDoc = medicos.documents.first()
                    medicoId = medicoDoc.getLong("ID_MEDICO") ?: 0L
                    val nombreMedico = medicoDoc.getString("NOMBRE") ?: ""
                    val apellidoMedico = medicoDoc.getString("APELLIDO") ?: ""

                    Log.d("ChatRoom", "✅ Médico encontrado por ID_FIREBASE: $medicoId - $nombreMedico $apellidoMedico")
                    inicializarChat(nombreMedico, apellidoMedico)
                } else {
                    Log.d("ChatRoom", "⚠️ No se encontró por ID_FIREBASE, buscando por CORREO...")
                    buscarMedicoPorCorreo(currentUser.email)
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatRoom", "❌ Error al buscar médico por ID_FIREBASE: ${e.message}")
                buscarMedicoPorCorreo(currentUser.email)
            }
    }

    private fun buscarMedicoPorCorreo(userEmail: String?) {
        if (userEmail == null) {
            mostrarError("Error: Email de usuario no disponible")
            return
        }

        Log.d("ChatRoom", "🔍 Buscando médico por correo: $userEmail")

        firestore.collection("medicos")
            .whereEqualTo("CORREO", userEmail)
            .get()
            .addOnSuccessListener { medicos ->
                if (!medicos.isEmpty) {
                    val medicoDoc = medicos.documents.first()
                    medicoId = medicoDoc.getLong("ID_MEDICO") ?: 0L
                    val nombreMedico = medicoDoc.getString("NOMBRE") ?: ""
                    val apellidoMedico = medicoDoc.getString("APELLIDO") ?: ""

                    Log.d("ChatRoom", "✅ Médico encontrado por CORREO: $medicoId - $nombreMedico $apellidoMedico")
                    inicializarChat(nombreMedico, apellidoMedico)
                } else {
                    Log.e("ChatRoom", "❌ No se encontró médico con correo: $userEmail")
                    mostrarError("No se encontró información del médico. Verifica que estés registrado como médico.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatRoom", "❌ Error al buscar médico por correo: ${e.message}")
                mostrarError("Error al buscar médico: ${e.message}")
            }
    }

    private fun inicializarChat(nombreMedico: String, apellidoMedico: String) {
        chatRoomId = "chat_cita_${idCita}_paciente_${idPaciente}_medico_${medicoId}"

        Log.d("ChatRoom", "Inicializando chat DOCTOR: $chatRoomId")

        setupUI(nombreMedico, apellidoMedico)
        setupRecyclerView()
        setupInputListeners()

        // ✅ CORREGIDO: Solo verificar/crear chat room - Los listeners se inicializan DENTRO
        verificarOCrearChatRoom(nombreMedico, apellidoMedico)

        mostrarLoading(false)
    }

    private fun setupUI(nombreMedico: String, apellidoMedico: String) {
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
                stackFromEnd = true
            }
            adapter = messagesAdapter
        }
    }

    private fun setupInputListeners() {
        binding.btnSend.setOnClickListener {
            enviarMensaje()
        }

        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty() && !isTyping) {
                    setTypingStatus(true)
                }
                typingHandler.removeCallbacks(stopTypingRunnable)
                typingHandler.postDelayed(stopTypingRunnable, 2000)
            }
            override fun afterTextChanged(s: Editable?) {
                binding.btnSend.isEnabled = !s.isNullOrBlank()
            }
        })

        binding.btnAttach.setOnClickListener {
            Toast.makeText(requireContext(), "Función próximamente", Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ CORREGIDO: Secuencia mejorada para prevenir activación temprana
    private fun verificarOCrearChatRoom(nombreMedico: String, apellidoMedico: String) {
        val chatRoomRef = realtimeDb.child("chatRooms").child(chatRoomId)

        chatRoomRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
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
                        Log.d("ChatRoom", "✅ Chat room creado exitosamente con status: active")

                        // ✅ ESCUCHAR SOLO DESPUÉS de crear el chat room
                        listenToMessages()
                        listenToTypingStatus()
                        listenToChatRoomStatus()
                    }
                    .addOnFailureListener { e ->
                        Log.e("ChatRoom", "❌ Error al crear chat room: ${e.message}")
                    }
            } else {
                val status = snapshot.child("status").getValue(String::class.java)
                Log.d("ChatRoom", "📊 Chat room existente - Status: $status")

                if (status == "completed" || status == "cancelled") {
                    mostrarChatFinalizado()
                } else {
                    // ✅ Si está activo, inicializar listeners
                    listenToMessages()
                    listenToTypingStatus()
                    listenToChatRoomStatus()
                }
            }
        }
    }

    private fun listenToMessages() {
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
                    messagesList.add(message)
                    messagesAdapter.notifyItemInserted(messagesList.size - 1)
                    Log.d("ChatRoom", "Lista ahora tiene ${messagesList.size} mensajes")
                    scrollToBottom()

                    if (message.senderId != currentUserId && message.status != "read") {
                        marcarMensajeComoLeido(message.messageId)
                    }
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

                val updates = hashMapOf<String, Any>(
                    "lastMessage" to messageText,
                    "lastMessageTime" to System.currentTimeMillis()
                )

                realtimeDb.child("chatRooms").child(chatRoomId)
                    .updateChildren(updates)

                binding.etMessage.text?.clear()
                setTypingStatus(false)
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

    // ✅ CORREGIDO: Listener mejorado para prevenir activación temprana
    private fun listenToChatRoomStatus() {
        val statusRef = realtimeDb.child("chatRooms").child(chatRoomId).child("status")

        // ✅ REMOVER LISTENER ANTERIOR si existe
        chatRoomListener?.let {
            statusRef.removeEventListener(it)
            chatRoomListener = null
        }

        chatRoomListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java)
                Log.d("ChatRoom", "📊 Estado del chat room cambiado: $status")

                // ✅ SOLO actuar si el estado es "completed" o "cancelled"
                if (status == "completed" || status == "cancelled") {
                    Log.d("ChatRoom", "🔴 Chat finalizado - Mostrando diálogo")
                    mostrarChatFinalizado()
                }
                // ✅ IGNORAR otros estados ("active", etc.)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRoom", "❌ Error al escuchar status: ${error.message}")
            }
        }

        statusRef.addValueEventListener(chatRoomListener!!)
    }

    private fun setTypingStatus(typing: Boolean) {
        isTyping = typing
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

    // ✅ ACTUALIZADO: Diálogo de salir mejorado
    private fun mostrarDialogoSalir() {
        if (!isAdded || activity?.isFinishing == true) return

        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ ¿Salir de la consulta?")
            .setMessage("Si sales de la consulta, se marcará como finalizada y no podrás volver a ingresar.\n\n¿Estás seguro de que deseas salir?")
            .setPositiveButton("Sí, salir") { dialog, _ ->
                // ✅ Usar navegación segura
                navegarAtrasSeguro()
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

        val updates = hashMapOf<String, Any>(
            "status" to "completed",
            "completedAt" to System.currentTimeMillis()
        )

        realtimeDb.child("chatRooms").child(chatRoomId)
            .updateChildren(updates)
            .addOnSuccessListener {
                Log.d("ChatRoom", "Chat room marcado como completado")
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

    // ✅ CORREGIDO: Navegación segura sin cerrar la app
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

                        // ✅ CORREGIDO: Navegación segura
                        navegarAtrasSeguro()
                    }.addOnFailureListener { e ->
                        Log.e("ChatRoom", "Error al actualizar cita: ${e.message}")
                        mostrarLoading(false)
                        // ✅ Navegar incluso si hay error
                        navegarAtrasSeguro()
                    }
                } else {
                    Log.e("ChatRoom", "No se encontró la cita")
                    mostrarLoading(false)
                    navegarAtrasSeguro()
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatRoom", "Error al buscar cita: ${e.message}")
                mostrarLoading(false)
                // ✅ Navegar incluso si hay error
                navegarAtrasSeguro()
            }
    }

    // ✅ NUEVA FUNCIÓN: Chat finalizado mejorado
    private fun mostrarChatFinalizado() {
        // ✅ VERIFICACIONES MÚLTIPLES para prevenir diálogos fantasmas
        if (!isAdded || isRemoving || activity?.isFinishing == true || activity?.isDestroyed == true) {
            Log.w("ChatRoom", "❌ No mostrar diálogo - Fragment/Activity no disponible")
            return
        }

        try {
            // ✅ VERIFICAR que realmente estamos en un chat finalizado
            if (chatRoomId.isNotEmpty()) {
                val chatRoomRef = realtimeDb.child("chatRooms").child(chatRoomId).child("status")
                chatRoomRef.get().addOnSuccessListener { snapshot ->
                    val currentStatus = snapshot.getValue(String::class.java)
                    if (currentStatus == "completed" || currentStatus == "cancelled") {
                        Log.d("ChatRoom", "✅ Confirmado - Chat realmente finalizado, mostrando diálogo")
                        mostrarDialogoChatFinalizado()
                    } else {
                        Log.w("ChatRoom", "⚠️ Estado actual: $currentStatus - No mostrar diálogo")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChatRoom", "💥 Error en mostrarChatFinalizado: ${e.message}")
        }
    }

    // ✅ NUEVA FUNCIÓN: Diálogo de chat finalizado separado
    private fun mostrarDialogoChatFinalizado() {
        try {
            AlertDialog.Builder(requireContext())
                .setTitle("Consulta Finalizada")
                .setMessage("Esta consulta ha sido finalizada. No puedes enviar más mensajes.")
                .setPositiveButton("Entendido") { dialog, _ ->
                    dialog.dismiss()
                    navegarAtrasSeguro()
                }
                .setCancelable(false)
                .show()
        } catch (e: Exception) {
            Log.e("ChatRoom", "💥 Error mostrando diálogo: ${e.message}")
            navegarAtrasSeguro()
        }
    }

    // ✅ NUEVA FUNCIÓN: Navegación segura sin cerrar la app
    private fun navegarAtrasSeguro() {
        try {
            if (!isAdded || activity == null) {
                Log.w("ChatRoom", "No se puede navegar - fragmento no adjunto")
                return
            }

            // ✅ VERIFICACIÓN DOBLE antes de navegar
            if (isVisible && !isRemoving) {
                Log.d("ChatRoom", "✅ Navegando seguro al home")

                // Usar popBackStack para regresar al fragmento anterior
                parentFragmentManager.popBackStack()
            } else {
                Log.w("ChatRoom", "Fragmento no visible o removiéndose, no navegar")
            }
        } catch (e: Exception) {
            Log.e("ChatRoom", "Error en navegación segura: ${e.message}")
        }
    }

    // ✅ FUNCIÓN ORIGINAL (mantener por compatibilidad)
    private fun navegarAtras() {
        navegarAtrasSeguro() // ✅ Redirigir a la versión segura
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

        Log.d("ChatRoom", "🔴 onDestroyView - Removiendo todos los listeners")

        // Remover listeners de manera más agresiva
        try {
            messagesListener?.let {
                realtimeDb.child("chatRooms").child(chatRoomId)
                    .child("messages").removeEventListener(it)
                messagesListener = null
            }

            typingListener?.let {
                realtimeDb.child("chatRooms").child(chatRoomId)
                    .child("pacienteTyping").removeEventListener(it)
                typingListener = null
            }

            chatRoomListener?.let {
                realtimeDb.child("chatRooms").child(chatRoomId)
                    .child("status").removeEventListener(it)
                chatRoomListener = null
            }
        } catch (e: Exception) {
            Log.e("ChatRoom", "Error removiendo listeners: ${e.message}")
        }

        // Detener indicador de escritura
        setTypingStatus(false)
        typingHandler.removeCallbacks(stopTypingRunnable)

        _binding = null
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