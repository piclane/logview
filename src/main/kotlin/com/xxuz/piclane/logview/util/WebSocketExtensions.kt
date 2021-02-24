package com.xxuz.piclane.logview.util

import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.io.StringWriter
import java.io.Writer

/**
 * [Writer] を使用して書き出された文字列を送信します
 *
 * @param producer 文字列生成関数
 */
fun WebSocketSession.sendText(producer: (writer: Writer) -> Unit) {
    val writer = StringWriter()
    writer.use {
        producer(it)
    }
    this.sendMessage(TextMessage(writer.toString()))
}
