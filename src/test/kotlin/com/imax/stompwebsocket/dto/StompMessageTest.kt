package com.imax.stompwebsocket.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StompMessageTest {

    @Test
    fun `findHeader returns matching header value or null`() {
        val message = StompMessage(
            command = StompCommand.MESSAGE,
            headers = listOf(
                StompHeader(StompHeader.DESTINATION, "/topic/chat"),
                StompHeader(StompHeader.CONTENT_TYPE, "application/json")
            ),
            payload = "{}"
        )

        assertEquals("/topic/chat", message.findHeader(StompHeader.DESTINATION))
        assertEquals("application/json", message.findHeader(StompHeader.CONTENT_TYPE))
        assertNull(message.findHeader(StompHeader.MESSAGE_ID))
    }

    @Test
    fun `compile formats frame with headers, payload, and null terminator`() {
        val message = StompMessage(
            command = StompCommand.SEND,
            headers = listOf(StompHeader(StompHeader.DESTINATION, "/app/chat")),
            payload = "Hello STOMP"
        )

        val expected = "SEND\ndestination:/app/chat\n\nHello STOMP\u0000"
        assertEquals(expected, message.compile())
    }

    @Test
    fun `compile with legacyWhitespace appends double newline after payload`() {
        val message = StompMessage(
            command = StompCommand.SEND,
            headers = listOf(StompHeader(StompHeader.DESTINATION, "/app/chat")),
            payload = "Hello"
        )

        val expected = "SEND\ndestination:/app/chat\n\nHello\n\n\u0000"
        assertEquals(expected, message.compile(legacyWhitespace = true))
    }

    @Test
    fun `compile with null payload omits body`() {
        val message = StompMessage(
            command = StompCommand.SUBSCRIBE,
            headers = listOf(
                StompHeader(StompHeader.ID, "sub-1"),
                StompHeader(StompHeader.DESTINATION, "/topic/news")
            ),
            payload = null
        )

        val expected = "SUBSCRIBE\nid:sub-1\ndestination:/topic/news\n\n\u0000"
        assertEquals(expected, message.compile())
    }

    @Test
    fun `from parses MESSAGE frame with colons in header value and multiline body`() {
        val raw = "MESSAGE\n" +
            "destination:/topic/chat\n" +
            "timestamp:2026-11-18T12:00:00Z\n" +
            "\n" +
            "line 1\n" +
            "line 2\u0000"

        val parsed = StompMessage.from(raw)

        assertEquals(StompCommand.MESSAGE, parsed.command)
        assertEquals("/topic/chat", parsed.findHeader(StompHeader.DESTINATION))
        assertEquals("2026-11-18T12:00:00Z", parsed.findHeader("timestamp"))
        assertEquals("line 1\nline 2", parsed.payload)
    }

    @Test
    fun `from handles CRLF line endings`() {
        val raw = "CONNECTED\r\nversion:1.2\r\nheart-beat:10000,10000\r\n\r\n\u0000"

        val parsed = StompMessage.from(raw)

        assertEquals(StompCommand.CONNECTED, parsed.command)
        assertEquals("1.2", parsed.findHeader("version"))
        assertEquals("10000,10000", parsed.findHeader(StompHeader.HEART_BEAT))
        assertEquals("", parsed.payload)
    }

    @Test
    fun `from returns UNKNOWN for null or heartbeat newline`() {
        val nullMsg = StompMessage.from(null)
        assertEquals(StompCommand.UNKNOWN, nullMsg.command)
        assertNull(nullMsg.payload)

        val pongMsg = StompMessage.from("\n")
        assertEquals(StompCommand.UNKNOWN, pongMsg.command)
        assertEquals("\n", pongMsg.payload)
    }

    @Test
    fun `compile and from round trip preserves message`() {
        val original = StompMessage(
            command = StompCommand.SEND,
            headers = listOf(
                StompHeader(StompHeader.DESTINATION, "/app/echo"),
                StompHeader(StompHeader.CONTENT_TYPE, "text/plain")
            ),
            payload = "{\"msg\":\"ok\"}"
        )

        val reparsed = StompMessage.from(original.compile())

        assertEquals(original.command, reparsed.command)
        assertEquals(original.headers, reparsed.headers)
        assertEquals(original.payload, reparsed.payload)
    }
}
