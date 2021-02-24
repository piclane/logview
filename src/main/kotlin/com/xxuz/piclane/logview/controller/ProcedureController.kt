package com.xxuz.piclane.logview.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.xxuz.piclane.logview.fs.Fs
import com.xxuz.piclane.logview.procedure.Param
import com.xxuz.piclane.logview.procedure.Status
import com.xxuz.piclane.logview.util.Json
import com.xxuz.piclane.logview.util.SessionTasks
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.io.IOException
import javax.websocket.CloseReason
import javax.websocket.CloseReason.CloseCodes
import javax.websocket.OnClose
import javax.websocket.OnMessage
import javax.websocket.Session
import javax.websocket.server.ServerEndpoint

/**
 * ファイル読み込みプロシージャ
 *
 * @author yohei_hina
 */
@CrossOrigin
class ProcedureController : TextWebSocketHandler() {
    @Autowired
    private lateinit var sessionTasks: SessionTasks

    @Autowired
    private lateinit var fs: Fs

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        sessionTasks.cancel(session)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            val param = Json.deserialize(message.payload, Param.ParamBuilder::class.java).build(fs)
            when (param.status) {
                Status.start -> sessionTasks.start(session, param.procedure.buildTask(session, param))
                Status.stop -> sessionTasks.cancel(session)
            }
        } catch (e: IllegalArgumentException) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason(e.message ?: ""))
        }
    }
}
