package com.imax.stompwebsocket.pathmatcher

import com.imax.stompwebsocket.Stomp
import com.imax.stompwebsocket.dto.StompCommand
import com.imax.stompwebsocket.dto.StompHeader
import com.imax.stompwebsocket.dto.StompMessage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PathMatcherTest {

    private fun messageWithDestination(dest: String?): StompMessage {
        val headers = if (dest != null) {
            listOf(StompHeader(StompHeader.DESTINATION, dest))
        } else {
            emptyList()
        }
        return StompMessage(StompCommand.MESSAGE, headers, "body")
    }

    @Test
    fun `SimplePathMatcher matches exact destination only`() {
        val matcher = SimplePathMatcher()

        assertTrue(matcher.matches("/topic/chat", messageWithDestination("/topic/chat")))
        assertFalse(matcher.matches("/topic/chat", messageWithDestination("/topic/chat/1")))
        assertFalse(matcher.matches("/topic/chat", messageWithDestination(null)))
    }

    @Test
    fun `RabbitPathMatcher matches single and multi segment wildcards`() {
        val matcher = RabbitPathMatcher()

        assertTrue(matcher.matches("topic.news.sports", messageWithDestination("topic.news.sports")))
        assertTrue(matcher.matches("topic.*.sports", messageWithDestination("topic.news.sports")))
        assertFalse(matcher.matches("topic.*.sports", messageWithDestination("topic.news.world.sports")))
        assertTrue(matcher.matches("topic.#", messageWithDestination("topic.news.world.sports")))
        assertFalse(matcher.matches("topic.#", messageWithDestination(null)))
    }

    @Test
    fun `SubscriptionPathMatcher returns false when client is null or path not subscribed`() {
        val nullClientMatcher = SubscriptionPathMatcher(null)
        val msg = StompMessage(
            StompCommand.MESSAGE,
            listOf(StompHeader(StompHeader.SUBSCRIPTION, "sub-1")),
            "body"
        )
        assertFalse(nullClientMatcher.matches("/topic/chat", msg))

        val client = Stomp.over("ws://localhost:8080/ws")
        val clientMatcher = SubscriptionPathMatcher(client)
        assertFalse(clientMatcher.matches("/topic/chat", msg))
    }
}
