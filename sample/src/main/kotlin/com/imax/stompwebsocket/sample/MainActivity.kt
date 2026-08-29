package com.imax.stompwebsocket.sample

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.imax.stompwebsocket.Stomp
import com.imax.stompwebsocket.StompClient
import com.imax.stompwebsocket.dto.LifecycleEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var stompClient: StompClient? = null
    private var subscriptionJob: Job? = null

    private lateinit var etUri: EditText
    private lateinit var btnConnect: Button
    private lateinit var btnDisconnect: Button
    private lateinit var tvStatus: TextView
    private lateinit var etTopic: EditText
    private lateinit var btnSubscribe: Button
    private lateinit var etSendDest: EditText
    private lateinit var etPayload: EditText
    private lateinit var btnSend: Button
    private lateinit var tvLog: TextView
    private lateinit var svLog: ScrollView
    private lateinit var btnClearLog: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etUri = findViewById(R.id.etUri)
        btnConnect = findViewById(R.id.btnConnect)
        btnDisconnect = findViewById(R.id.btnDisconnect)
        tvStatus = findViewById(R.id.tvStatus)
        etTopic = findViewById(R.id.etTopic)
        btnSubscribe = findViewById(R.id.btnSubscribe)
        etSendDest = findViewById(R.id.etSendDest)
        etPayload = findViewById(R.id.etPayload)
        btnSend = findViewById(R.id.btnSend)
        tvLog = findViewById(R.id.tvLog)
        svLog = findViewById(R.id.svLog)
        btnClearLog = findViewById(R.id.btnClearLog)
    }

    private fun setupListeners() {
        btnConnect.setOnClickListener {
            val uri = etUri.text.toString().trim()
            if (uri.isEmpty()) {
                Toast.makeText(this, "Please enter WebSocket URI", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            connectStomp(uri)
        }

        btnDisconnect.setOnClickListener {
            disconnectStomp()
        }

        btnSubscribe.setOnClickListener {
            val topic = etTopic.text.toString().trim()
            if (topic.isEmpty()) {
                Toast.makeText(this, "Please enter topic destination", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            toggleSubscribe(topic)
        }

        btnSend.setOnClickListener {
            val dest = etSendDest.text.toString().trim()
            val payload = etPayload.text.toString().trim()
            if (dest.isEmpty()) {
                Toast.makeText(this, "Please enter send destination", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendMessage(dest, payload)
        }

        btnClearLog.setOnClickListener {
            tvLog.text = "[System Log Output]\n"
        }
    }

    private fun connectStomp(uri: String) {
        log("Connecting to $uri ...")

        stompClient = Stomp.over(uri)

        stompClient?.let { client ->
            // Collect connection state
            lifecycleScope.launch {
                client.connectionState.collect { isConnected ->
                    updateConnectionStateUi(isConnected)
                }
            }

            // Collect lifecycle events
            lifecycleScope.launch {
                client.lifecycleFlow.collect { event ->
                    when (event.type) {
                        LifecycleEvent.Type.OPENED -> log("Lifecycle: Connection OPENED")
                        LifecycleEvent.Type.CLOSED -> log("Lifecycle: Connection CLOSED")
                        LifecycleEvent.Type.ERROR -> log("Lifecycle: ERROR - ${event.exception?.message}")
                        LifecycleEvent.Type.FAILED_SERVER_HEARTBEAT -> log("Lifecycle: FAILED_SERVER_HEARTBEAT")
                    }
                }
            }

            client.connect()
        }
    }

    private fun disconnectStomp() {
        log("Disconnecting...")
        subscriptionJob?.cancel()
        subscriptionJob = null
        btnSubscribe.text = "Subscribe"
        stompClient?.disconnect()
        stompClient = null
    }

    private fun toggleSubscribe(topic: String) {
        val client = stompClient
        if (client == null || !client.isConnected) {
            Toast.makeText(this, "Please connect first", Toast.LENGTH_SHORT).show()
            return
        }

        if (subscriptionJob?.isActive == true) {
            subscriptionJob?.cancel()
            subscriptionJob = null
            btnSubscribe.text = "Subscribe"
            log("Unsubscribed from $topic")
        } else {
            subscriptionJob = lifecycleScope.launch {
                log("Subscribing to $topic ...")
                client.topic(topic).collect { message ->
                    log("Received [$topic]: ${message.payload}")
                }
            }
            btnSubscribe.text = "Unsubscribe"
        }
    }

    private fun sendMessage(destination: String, payload: String) {
        val client = stompClient
        if (client == null || !client.isConnected) {
            Toast.makeText(this, "Please connect first", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                client.send(destination, payload)
                log("Sent to [$destination]: $payload")
            } catch (e: Exception) {
                log("Send Error: ${e.message}")
            }
        }
    }

    private fun updateConnectionStateUi(isConnected: Boolean) {
        if (isConnected) {
            tvStatus.text = "Status: CONNECTED"
            tvStatus.setTextColor(Color.parseColor("#4CAF50"))
            btnConnect.isEnabled = false
            btnDisconnect.isEnabled = true
        } else {
            tvStatus.text = "Status: DISCONNECTED"
            tvStatus.setTextColor(Color.parseColor("#F44336"))
            btnConnect.isEnabled = true
            btnDisconnect.isEnabled = false
        }
    }

    private fun log(message: String) {
        tvLog.append("$message\n")
        svLog.post {
            svLog.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stompClient?.disconnect()
    }
}
