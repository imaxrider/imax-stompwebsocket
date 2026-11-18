package com.imax.stompwebsocket

import com.imax.stompwebsocket.dto.StompCommand
import com.imax.stompwebsocket.dto.StompHeader
import com.imax.stompwebsocket.dto.StompMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HeartBeatTaskTest {

    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `consumeHeartBeat filters out server pong newline and passes other frames`() {
        val task = HeartBeatTask(
            sendCallback = object : HeartBeatTask.SendCallback {
                override fun sendClientHeartBeat(pingMessage: String) {}
            },
            failedListener = null,
            scope = scope
        )

        val pong = StompMessage(StompCommand.UNKNOWN, emptyList(), "\n")
        val regularMessage = StompMessage(StompCommand.MESSAGE, emptyList(), "hello")
        val sendFrame = StompMessage(StompCommand.SEND, emptyList(), "hi")

        assertFalse(task.consumeHeartBeat(pong))
        assertTrue(task.consumeHeartBeat(regularMessage))
        assertTrue(task.consumeHeartBeat(sendFrame))
    }

    @Test
    fun `setters and getters store configured heartbeat intervals`() {
        val task = HeartBeatTask(
            sendCallback = object : HeartBeatTask.SendCallback {
                override fun sendClientHeartBeat(pingMessage: String) {}
            },
            failedListener = null,
            scope = scope
        )

        task.setClientHeartbeat(5000)
        task.setServerHeartbeat(10000)

        assertEquals(5000, task.getClientHeartbeat())
        assertEquals(10000, task.getServerHeartbeat())
    }

    @Test
    fun `client heartbeat sends CRLF ping after negotiated interval`() = runBlocking {
        val pingSent = CompletableDeferred<String>()
        val task = HeartBeatTask(
            sendCallback = object : HeartBeatTask.SendCallback {
                override fun sendClientHeartBeat(pingMessage: String) {
                    pingSent.complete(pingMessage)
                }
            },
            failedListener = null,
            scope = scope
        )

        task.setClientHeartbeat(30)
        val connectedFrame = StompMessage(
            StompCommand.CONNECTED,
            listOf(StompHeader(StompHeader.HEART_BEAT, "0,30")),
            null
        )
        task.consumeHeartBeat(connectedFrame)

        val sent = withTimeout(1000) { pingSent.await() }
        assertEquals("\r\n", sent)
        task.shutdown()
    }

    @Test
    fun `server heartbeat failure invokes listener when no server activity arrives`() = runBlocking {
        val failed = CompletableDeferred<Unit>()
        val task = HeartBeatTask(
            sendCallback = object : HeartBeatTask.SendCallback {
                override fun sendClientHeartBeat(pingMessage: String) {}
            },
            failedListener = object : HeartBeatTask.FailedListener {
                override fun onServerHeartBeatFailed() {
                    failed.complete(Unit)
                }
            },
            scope = scope
        )

        task.setServerHeartbeat(25)
        val connectedFrame = StompMessage(
            StompCommand.CONNECTED,
            listOf(StompHeader(StompHeader.HEART_BEAT, "25,0")),
            null
        )
        task.consumeHeartBeat(connectedFrame)

        withTimeout(1500) { failed.await() }
        task.shutdown()
    }
}
