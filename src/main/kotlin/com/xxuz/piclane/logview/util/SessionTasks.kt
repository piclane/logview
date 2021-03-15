package com.xxuz.piclane.logview.util

import com.xxuz.piclane.logview.procedure.Signal
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.io.IOException
import java.util.concurrent.*
import javax.annotation.PreDestroy

/**
 * セッションと、セッションに紐付くタスクを管理します
 *
 * @author yohei_hina
 */
@Component
class SessionTasks {
    /** スレッドキャッシュ  */
    private val es = Executors.newCachedThreadPool()

    /** セッションID と [TaskRunner] のマップ  */
    private val futures: MutableMap<String, TaskRunner> = ConcurrentHashMap()

    /**
     * 指定されたセッションに紐付くタスクを開始します
     *
     * @param session [WebSocketSession]
     * @param task タスク
     */
    fun start(session: WebSocketSession, task: () -> Unit) {
        startImpl(session, task)
    }

    /**
     * 指定されたセッションに紐付いた処理中のタスクを中断します
     *
     * @param session [WebSocketSession]
     */
    fun cancel(session: WebSocketSession) {
        cancelImpl(session) {
            sendStoppedSignal(it)
        }
    }

    /**
     * 停止シグナルを送出します
     *
     * @param session [WebSocketSession]
     */
    private fun sendStoppedSignal(session: WebSocketSession) {
        if (session.isOpen) {
            try {
                session.sendMessage(TextMessage(Json.serialize(arrayOf<Any>(Signal.STOPPED))))
            } catch (e: IOException) {
                logger.warn("Failed to send STOPPED signal.", e)
            }
        }
    }

    /**
     * 指定されたセッションに紐付くタスクを開始します
     *
     * @param session [WebSocketSession]
     * @param task タスク
     */
    private fun startImpl(session: WebSocketSession, task: () -> Unit) {
        cancelImpl(session) {
            val id = session.id
            val tr = TaskRunner(id, task)
            tr.future = es.submit(tr)
            futures[id] = tr
        }
    }

    /**
     * 指定されたセッションに紐付いた処理中のタスクを中断します
     *
     * @param session [WebSocketSession]
     * @param done セッションが完全に終了した後に呼び出されます
     */
    private fun cancelImpl(session: WebSocketSession, done: (session: WebSocketSession) -> Unit) {
        val id = session.id
        val tr = futures[id]
        if (tr != null) {
            tr.future?.cancel(true)
            es.submit {
                try {
                    tr.latch.await()
                } catch (e: InterruptedException) {
                    // nop
                }
                done(session)
            }
        } else {
            done(session)
        }
    }

    /**
     * インスタンスを破棄します
     */
    @PreDestroy
    fun destroy() {
        try {
            es.shutdownNow()
            es.awaitTermination(5L, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            // nop
        }
    }

    /**
     * タスクの実行と、実行状態を保持します
     */
    private inner class TaskRunner(
            /** セッションID  */
            val sessionId: String,
            /** タスク  */
            val task: () -> Unit) : Callable<Void>
    {
        /** タスクが完了したらラッチが外れる CountDownLatch  */
        val latch = CountDownLatch(1)

        /** タスクの非同期計算の結果  */
        var future: Future<*>? = null

        /**
         * {@inheritDoc}
         */
        override fun call(): Void? {
            try {
                task()
            } finally {
                latch.countDown()
                futures.remove(sessionId)
            }
            return null
        }
    }

    companion object {
        /** Logger  */
        private val logger = LoggerFactory.getLogger(SessionTasks::class.java)
    }
}
