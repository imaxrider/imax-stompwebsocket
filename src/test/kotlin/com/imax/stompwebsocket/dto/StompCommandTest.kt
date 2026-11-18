package com.imax.stompwebsocket.dto

import org.junit.Assert.assertEquals
import org.junit.Test

class StompCommandTest {

    @Test
    fun `fromString parses all known commands`() {
        assertEquals(StompCommand.CONNECT, StompCommand.fromString("CONNECT"))
        assertEquals(StompCommand.CONNECTED, StompCommand.fromString("CONNECTED"))
        assertEquals(StompCommand.SEND, StompCommand.fromString("SEND"))
        assertEquals(StompCommand.MESSAGE, StompCommand.fromString("MESSAGE"))
        assertEquals(StompCommand.SUBSCRIBE, StompCommand.fromString("SUBSCRIBE"))
        assertEquals(StompCommand.UNSUBSCRIBE, StompCommand.fromString("UNSUBSCRIBE"))
        assertEquals(StompCommand.UNKNOWN, StompCommand.fromString("UNKNOWN"))
    }

    @Test
    fun `fromString is case insensitive`() {
        assertEquals(StompCommand.CONNECT, StompCommand.fromString("connect"))
        assertEquals(StompCommand.MESSAGE, StompCommand.fromString("Message"))
    }

    @Test
    fun `fromString returns UNKNOWN for null, blank, or invalid input`() {
        assertEquals(StompCommand.UNKNOWN, StompCommand.fromString(null))
        assertEquals(StompCommand.UNKNOWN, StompCommand.fromString(""))
        assertEquals(StompCommand.UNKNOWN, StompCommand.fromString("   "))
        assertEquals(StompCommand.UNKNOWN, StompCommand.fromString("INVALID_CMD"))
    }
}
