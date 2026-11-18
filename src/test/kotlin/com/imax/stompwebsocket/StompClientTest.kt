package com.imax.stompwebsocket

import com.imax.stompwebsocket.dto.LifecycleEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class StompClientTest {

    @Test
    fun `initial connection state is disconnected`() {
        val client = Stomp.over("ws://localhost:8080/ws")

        assertFalse(client.isConnected)
        assertFalse(client.connectionState.value)
        assertNull(client.getTopicId("/topic/chat"))
    }

    @Test
    fun `fluent heartbeat configuration returns same client instance`() {
        val client = Stomp.over("ws://localhost:8080/ws")

        assertSame(client, client.withClientHeartbeat(5000))
        assertSame(client, client.withServerHeartbeat(10000))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `topic with empty destination throws IllegalArgumentException on collect`() = runBlocking {
        val client = Stomp.over("ws://localhost:8080/ws")
        client.topic("").first()
        Unit
    }

    @Test
    fun `send suspends while disconnected`() = runBlocking {
        val client = Stomp.over("ws://localhost:8080/ws")
        val result = withTimeoutOrNull(100) {
            client.send("/app/chat", "hello")
            true
        }
        assertNull(result)
    }

    @Test
    fun `disconnect keeps connectionState false, clears topic ids, and emits CLOSED event`() = runBlocking {
        val client = Stomp.over("ws://localhost:8080/ws")
        client.disconnect()

        assertFalse(client.isConnected)
        assertEquals(false, client.connectionState.value)
        assertNull(client.getTopicId("/topic/chat"))

        val event = withTimeout(1000) { client.lifecycleFlow.first() }
        assertEquals(LifecycleEvent.Type.CLOSED, event.type)
    }
}
