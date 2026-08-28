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
import com.imax.stompwebsocket.dto.StompCommand
import com.imax.stompwebsocket.dto.StompHeader
import com.imax.stompwebsocket.dto.StompMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds

class HeartBeatTask(
    private val sendCallback: SendCallback,
    private val failedListener: FailedListener?,
    private val scope: CoroutineScope
) {

    private var serverHeartbeat = 0
    private var clientHeartbeat = 0

    private var serverHeartbeatNew = 0
    private var clientHeartbeatNew = 0

    @Volatile
    private var lastServerHeartBeat: Long = 0

    private var clientSendJob: Job? = null
    private var serverCheckJob: Job? = null

    fun setServerHeartbeat(serverHeartbeat: Int) {
        this.serverHeartbeatNew = serverHeartbeat
    }

    fun setClientHeartbeat(clientHeartbeat: Int) {
        this.clientHeartbeatNew = clientHeartbeat
    }

    fun getServerHeartbeat(): Int = serverHeartbeatNew

    fun getClientHeartbeat(): Int = clientHeartbeatNew

    fun consumeHeartBeat(message: StompMessage): Boolean {
        when (message.command) {
            StompCommand.CONNECTED -> {
                heartBeatHandshake(message.findHeader(StompHeader.HEART_BEAT))
            }
            StompCommand.SEND -> {
                abortClientHeartBeatSend()
            }
            StompCommand.MESSAGE -> {
                abortServerHeartBeatCheck()
            }
            StompCommand.UNKNOWN -> {
                if ("\n" == message.payload) {
                    Log.d(TAG, "<<< PONG")
                    abortServerHeartBeatCheck()
                    return false
                }
            }
            else -> {}
        }
        return true
    }

    fun shutdown() {
        clientSendJob?.cancel()
        serverCheckJob?.cancel()
        lastServerHeartBeat = 0
    }

    private fun heartBeatHandshake(heartBeatHeader: String?) {
        if (heartBeatHeader != null) {
            val heartbeats = heartBeatHeader.split(",")
            if (clientHeartbeatNew > 0 && heartbeats.size > 1) {
                clientHeartbeat = max(clientHeartbeatNew, heartbeats[1].toInt())
            }
            if (serverHeartbeatNew > 0 && heartbeats.isNotEmpty()) {
                serverHeartbeat = max(serverHeartbeatNew, heartbeats[0].toInt())
            }
        }
        if (clientHeartbeat > 0 || serverHeartbeat > 0) {
            if (clientHeartbeat > 0) {
                Log.d(TAG, "Client will send heart-beat every $clientHeartbeat ms")
                scheduleClientHeartBeat()
            }
            if (serverHeartbeat > 0) {
                Log.d(TAG, "Client will listen to server heart-beat every $serverHeartbeat ms")
                lastServerHeartBeat = System.currentTimeMillis()
                scheduleServerHeartBeatCheck()
            }
        }
    }

    private fun scheduleServerHeartBeatCheck() {
        if (serverHeartbeat > 0) {
            serverCheckJob?.cancel()
            serverCheckJob = scope.launch(Dispatchers.Default) {
                delay(serverHeartbeat.toLong().milliseconds)
                checkServerHeartBeat()
            }
        }
    }

    private fun checkServerHeartBeat() {
        if (serverHeartbeat > 0) {
            val now = System.currentTimeMillis()
            val boundary = now - (3 * serverHeartbeat)
            if (lastServerHeartBeat < boundary) {
                Log.d(TAG, "It's a sad day ;( Server didn't send heart-beat on time. Last received at '$lastServerHeartBeat' and now is '$now'")
                failedListener?.onServerHeartBeatFailed()
            } else {
                Log.d(TAG, "We were checking and server sent heart-beat on time. So well-behaved :)")
                lastServerHeartBeat = System.currentTimeMillis()
                scheduleServerHeartBeatCheck()
            }
        }
    }

    private fun abortServerHeartBeatCheck() {
        lastServerHeartBeat = System.currentTimeMillis()
        scheduleServerHeartBeatCheck()
    }

    private fun scheduleClientHeartBeat() {
        if (clientHeartbeat > 0) {
            clientSendJob?.cancel()
            clientSendJob = scope.launch(Dispatchers.Default) {
                delay(clientHeartbeat.toLong().milliseconds)
                sendClientHeartBeat()
            }
        }
    }

    private fun sendClientHeartBeat() {
        sendCallback.sendClientHeartBeat("\r\n")
        Log.d(TAG, "PING >>>")
        scheduleClientHeartBeat()
    }

    private fun abortClientHeartBeatSend() {
        scheduleClientHeartBeat()
    }

    interface FailedListener {
        fun onServerHeartBeatFailed()
    }

    interface SendCallback {
        fun sendClientHeartBeat(pingMessage: String)
    }

    companion object {
        private val TAG = HeartBeatTask::class.java.simpleName
    }
}
