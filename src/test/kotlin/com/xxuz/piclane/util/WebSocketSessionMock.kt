package com.xxuz.piclane.util

import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.springframework.http.HttpHeaders
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketExtension
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.net.InetSocketAddress
import java.net.URI
import java.security.Principal
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.collections.HashMap

class WebSocketSessionMock : WebSocketSession {
    /** LinkedBlockingQueue to store strings sent in different threads  */
    private val queue: BlockingQueue<String> = LinkedBlockingQueue()

    /** gson  */
    private val mapper = ObjectMapper()

    /** [Deque<Map<String, Any>>] 型 */
    private val dequeMapType = mapper.typeFactory.let { tf ->
        tf.constructCollectionType(
                LinkedList::class.java,
                tf.constructMapType(HashMap::class.java, String::class.java, Any::class.java))
    }

    /** Last received messages  */
    private var messages: Deque<Map<String, Any>>? = null

    private var isOpen = true

    /**
     * Consume the messages one by one.
     *
     * @param consumer {message} message {return} true if the next message is requested, otherwise false
     * @throws InterruptedException If interrupted while taking a message from the queue
     */
    fun consume(consumer: (message: Map<String, Any>) -> Boolean) {
        do {
            if (messages == null || messages!!.isEmpty()) {
                val json = queue.take()
                MatcherAssert.assertThat(json, Matchers.`is`(Matchers.allOf(Matchers.startsWith("["), Matchers.endsWith("]"))))
                messages = mapper.readValue(json, dequeMapType)
//                print(messages?.size);
//                print(" ");
//                println(json);
            }
        } while (consumer(messages!!.pollFirst()))
    }

    override fun sendMessage(message: WebSocketMessage<*>) {
        queue.add(message.payload.toString())
    }

    override fun close() {
        isOpen = false
    }

    override fun close(status: CloseStatus) {
        isOpen = false
    }

    override fun isOpen(): Boolean = isOpen

    override fun getId(): String {
        TODO("Not yet implemented")
    }

    override fun getUri(): URI? {
        TODO("Not yet implemented")
    }

    override fun getHandshakeHeaders(): HttpHeaders {
        TODO("Not yet implemented")
    }

    override fun getAttributes(): MutableMap<String, Any> {
        TODO("Not yet implemented")
    }

    override fun getPrincipal(): Principal? {
        TODO("Not yet implemented")
    }

    override fun getLocalAddress(): InetSocketAddress? {
        TODO("Not yet implemented")
    }

    override fun getRemoteAddress(): InetSocketAddress? {
        TODO("Not yet implemented")
    }

    override fun getAcceptedProtocol(): String? {
        TODO("Not yet implemented")
    }

    override fun setTextMessageSizeLimit(messageSizeLimit: Int) {
        TODO("Not yet implemented")
    }

    override fun getTextMessageSizeLimit(): Int {
        TODO("Not yet implemented")
    }

    override fun setBinaryMessageSizeLimit(messageSizeLimit: Int) {
        TODO("Not yet implemented")
    }

    override fun getBinaryMessageSizeLimit(): Int {
        TODO("Not yet implemented")
    }

    override fun getExtensions(): MutableList<WebSocketExtension> {
        TODO("Not yet implemented")
    }
}
