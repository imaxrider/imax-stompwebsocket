/*
 * Copyright (C) 2016 Naver Corp. (naik-software)
 * Copyright (C) 2017 Forrest Hopkins III (forresthopkinsa)
 * Copyright (C) 2024-2026 IMAX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.imax.stompwebsocket

import android.util.Log
import com.imax.stompwebsocket.dto.LifecycleEvent
import com.imax.stompwebsocket.dto.StompCommand
import com.imax.stompwebsocket.dto.StompHeader
import com.imax.stompwebsocket.dto.StompMessage
import com.imax.stompwebsocket.pathmatcher.PathMatcher
import com.imax.stompwebsocket.pathmatcher.SimplePathMatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.TreeMap
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class StompClient(
    private val mUri: String,
    private val mConnectHttpHeaders: Map<String, String>?,
    private val mOkHttpClient: OkHttpClient
) {

    private var topics: ConcurrentHashMap<String, String>? = null
    private var legacyWhitespace = false

    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _lifecycleFlow = MutableSharedFlow<LifecycleEvent>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val lifecycleFlow: SharedFlow<LifecycleEvent> = _lifecycleFlow.asSharedFlow()

    private val _messageFlow = MutableSharedFlow<StompMessage>(
        extraBufferCapacity = 64
    )
    val messageFlow: SharedFlow<StompMessage> = _messageFlow.asSharedFlow()

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()

    private val streamMap = ConcurrentHashMap<String, Flow<StompMessage>>()
    private var pathMatcher: PathMatcher = SimplePathMatcher()
    private var headers: List<StompHeader>? = null
    private var webSocket: WebSocket? = null

    private lateinit var heartBeatTask: HeartBeatTask

    init {
        // Initialize simple heartBeatTask with empty scope initially
        initHeartBeatTask()
    }

    private fun initHeartBeatTask() {
        heartBeatTask = HeartBeatTask(
            object : HeartBeatTask.SendCallback {
                override fun sendClientHeartBeat(pingMessage: String) {
                    sendHeartBeat(pingMessage)
                }
            },
            object : HeartBeatTask.FailedListener {
                override fun onServerHeartBeatFailed() {
                    scope.launch {
                        _lifecycleFlow.emit(LifecycleEvent(LifecycleEvent.Type.FAILED_SERVER_HEARTBEAT))
                    }
                }
            },
            scope
        )
    }

    fun withServerHeartbeat(ms: Int): StompClient {
        heartBeatTask.setServerHeartbeat(ms)
        return this
    }

    fun withClientHeartbeat(ms: Int): StompClient {
        heartBeatTask.setClientHeartbeat(ms)
        return this
    }

    fun connect() {
        connect(null)
    }

    fun connect(_headers: List<StompHeader>?) {
        Log.d(TAG, "Connect")
        this.headers = _headers

        if (isConnected) {
            Log.d(TAG, "Already connected, ignore")
            return
        }

        // Fresh scope for each connection session
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        
        // Re-init heartbeat task bound to the new scope
        val serverHb = heartBeatTask.getServerHeartbeat()
        val clientHb = heartBeatTask.getClientHeartbeat()
        initHeartBeatTask()
        heartBeatTask.setServerHeartbeat(serverHb)
        heartBeatTask.setClientHeartbeat(clientHb)

        val requestBuilder = Request.Builder().url(mUri)
        mConnectHttpHeaders?.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }

        webSocket = mOkHttpClient.newWebSocket(requestBuilder.build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                scope.launch {
                    val connectHeaders = ArrayList<StompHeader>()
                    connectHeaders.add(StompHeader(StompHeader.VERSION, SUPPORTED_VERSIONS))
                    connectHeaders.add(
                        StompHeader(
                            StompHeader.HEART_BEAT,
                            "${heartBeatTask.getClientHeartbeat()},${heartBeatTask.getServerHeartbeat()}"
                        )
                    )
                    _headers?.let { connectHeaders.addAll(it) }

                    val connectFrame = StompMessage(StompCommand.CONNECT, connectHeaders, null).compile(legacyWhitespace)
                    webSocket.send(connectFrame)

                    val openedEvent = LifecycleEvent(LifecycleEvent.Type.OPENED)
                    val headersMap = TreeMap<String, String>()
                    for (name in response.headers.names()) {
                        headersMap[name] = response.headers[name] ?: ""
                    }
                    openedEvent.handshakeResponseHeaders = headersMap
                    _lifecycleFlow.emit(openedEvent)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val message = StompMessage.from(text)
                if (heartBeatTask.consumeHeartBeat(message)) {
                    scope.launch {
                        _messageFlow.emit(message)
                    }
                }
                if (message.command == StompCommand.CONNECTED) {
                    _connectionState.value = true
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val text = bytes.utf8()
                val message = StompMessage.from(text)
                if (heartBeatTask.consumeHeartBeat(message)) {
                    scope.launch {
                        _messageFlow.emit(message)
                    }
                }
                if (message.command == StompCommand.CONNECTED) {
                    _connectionState.value = true
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = false
                scope.launch {
                    _lifecycleFlow.emit(LifecycleEvent(LifecycleEvent.Type.CLOSED))
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = false
                scope.launch {
                    _lifecycleFlow.emit(LifecycleEvent(LifecycleEvent.Type.ERROR, Exception(t)))
                    _lifecycleFlow.emit(LifecycleEvent(LifecycleEvent.Type.CLOSED))
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }
        })
    }

    suspend fun send(destination: String, data: String? = null) {
        send(
            StompMessage(
                StompCommand.SEND,
                listOf(StompHeader(StompHeader.DESTINATION, destination)),
                data
            )
        )
    }

    suspend fun send(destination: String, data: String?, username: String) {
        val stompHeaders = ArrayList<StompHeader>()
        stompHeaders.add(StompHeader(StompHeader.DESTINATION, destination))
        stompHeaders.add(StompHeader("user", username))
        send(StompMessage(StompCommand.SEND, stompHeaders, data))
    }

    suspend fun send(stompMessage: StompMessage) {
        // Wait until connected
        connectionState.first { isConnected -> isConnected }
        val compileMessage = stompMessage.compile(legacyWhitespace)
        val success = webSocket?.send(compileMessage) ?: false
        if (!success) {
            throw IllegalStateException("Failed to send message: WebSocket connection is closed or not ready")
        }
    }

    private fun sendHeartBeat(pingMessage: String) {
        scope.launch {
            connectionState.first { isConnected -> isConnected }
            webSocket?.send(pingMessage)
        }
    }

    fun reConnect() {
        disconnect()
        connect(headers)
    }

    fun disconnect() {
        heartBeatTask.shutdown()
        webSocket?.close(1000, "Disconnect called")
        webSocket = null
        _connectionState.value = false
        scope.launch {
            _lifecycleFlow.emit(LifecycleEvent(LifecycleEvent.Type.CLOSED))
        }
        scope.cancel("Disconnect called")
        topics?.clear()
        streamMap.clear()
    }

    fun topic(destinationPath: String): Flow<StompMessage> {
        return topic(destinationPath, null)
    }

    fun topic(dest: String, headerList: List<StompHeader>?): Flow<StompMessage> {
        if (dest.isEmpty()) {
            return flow { throw IllegalArgumentException("Invalid topic") }
        }

        return streamMap.getOrPut(dest) {
            flow {
                _messageFlow
                    .filter { msg -> pathMatcher.matches(dest, msg) }
                    .collect { emit(it) }
            }
            .onStart {
                subscribePath(dest, headerList)
            }
            .onCompletion {
                unsubscribePath(dest)
                streamMap.remove(dest)
            }
            .shareIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 0, replayExpirationMillis = 0),
                replay = 0
            )
        }
    }

    private suspend fun subscribePath(destinationPath: String, headerList: List<StompHeader>?) {
        val topicId = UUID.randomUUID().toString()
        val currentTopics = topics ?: ConcurrentHashMap<String, String>().also { topics = it }

        if (currentTopics.containsKey(destinationPath)) {
            return
        }

        currentTopics[destinationPath] = topicId
        val headers = ArrayList<StompHeader>()
        headers.add(StompHeader(StompHeader.ID, topicId))
        headers.add(StompHeader(StompHeader.DESTINATION, destinationPath))
        headers.add(StompHeader(StompHeader.ACK, DEFAULT_ACK))

        Log.d(TAG, "subscribed path! $topicId")

        if (headerList != null) {
            headers.addAll(headerList)
        }
        send(StompMessage(StompCommand.SUBSCRIBE, headers, null))
    }

    private suspend fun unsubscribePath(dest: String) {
        val currentTopics = topics ?: return
        val topicId = currentTopics.remove(dest) ?: return

        Log.i(TAG, "Unsubscribed: $dest")
        try {
            send(
                StompMessage(
                    StompCommand.UNSUBSCRIBE,
                    listOf(StompHeader(StompHeader.ID, topicId)),
                    null
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error unsubscribing path", e)
        }
    }

    fun setPathMatcher(pathMatcher: PathMatcher) {
        this.pathMatcher = pathMatcher
    }

    val isConnected: Boolean
        get() = connectionState.value

    fun setLegacyWhitespace(legacyWhitespace: Boolean) {
        this.legacyWhitespace = legacyWhitespace
    }

    fun getTopicId(dest: String): String? {
        return topics?.get(dest)
    }

    companion object {
        private val TAG = StompClient::class.java.simpleName
        const val SUPPORTED_VERSIONS = "1.1,1.2"
        const val DEFAULT_ACK = "auto"
    }
}
