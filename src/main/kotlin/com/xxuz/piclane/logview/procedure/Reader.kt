package com.xxuz.piclane.logview.procedure

import com.xxuz.piclane.logview.procedure.text.*
import com.xxuz.piclane.logview.util.Json
import com.xxuz.piclane.logview.util.sendText
import org.slf4j.LoggerFactory
import org.springframework.web.socket.WebSocketSession
import java.io.IOException
import java.util.*

/**
 * ふつうの読込
 *
 * @author yohei_hina
 */
internal class Reader(
        /** [WebSocketSession]  */
        private val session: WebSocketSession,
        /** パラメーター  */
        private val param: Param
) : Runnable {

    /**
     * @see Runnable.run
     */
    override fun run() {
        val path = param.path
        val currentThread = Thread.currentThread()
        val oldName = currentThread.name
        currentThread.name = "${javaClass.simpleName}-${path.fileName}"

        val lines = arrayOfNulls<Line>(LINES_TO_SEND_AT_ONCE)
        try {
            LineReader.of(param).use { reader ->
                session.sendText { writer ->
                    val signal: MutableList<Any> = ArrayList()
                    val offset: Offset = reader.currentOffset
                    signal.add(Signal.FILE_LENGTH(offset.length))
                    if (offset.isBof) {
                        signal.add(Signal.BOF)
                    }
                    Json.serialize(writer, signal)
                }
                var lineCount: Int
                var lineCountAll = 0
                var hasNextLine = true
                while (session.isOpen && hasNextLine && lineCountAll < param.lines) {
                    // 割込チェック
                    if (Thread.interrupted()) {
                        throw InterruptedException()
                    }

                    // 最大 lineCountMax 行読み込む
                    lineCount = 0
                    while (lineCount < LINES_TO_SEND_AT_ONCE && lineCountAll < param.lines) {
                        val line = reader.readLine()
                        if (line == null) {
                            hasNextLine = false
                            break
                        }
                        lines[lineCount] = line
                        lineCount++
                        lineCountAll++
                    }

                    // lineCount行まとめて出力
                    if (lineCount > 0) {
                        session.sendText { writer -> Json.serialize(writer, lines.copyOf(lineCount)) }
                    }
                }
                session.sendText { writer -> Json.serialize(writer, arrayOf(Signal.EOR)) }

                // 最終行まで検索が終了したことを伝える
                if (param.direction == Direction.forward && !reader.hasNextLine()) {
                    session.sendText { writer -> Json.serialize(writer, arrayOf(Signal.EOF)) }
                } else if (param.direction == Direction.backward && !reader.hasNextLine()) {
                    session.sendText { writer -> Json.serialize(writer, arrayOf(Signal.BOF)) }
                } else {
                    return  // 最終行まで読み込んでいない場合は続きを監視しない
                }

                // 続きを監視しない場合は終わり
                if (!param.isFollow) {
                    return
                }

                // 追加行を送信
                while (session.isOpen) {
                    // 割込チェック
                    if (Thread.interrupted()) {
                        throw InterruptedException()
                    }

                    // ファイル長更新
                    reader.refresh()

                    // 最大 lineCountMax 行読み込む
                    hasNextLine = true
                    while (hasNextLine) {
                        lineCount = 0
                        while (lineCount < LINES_TO_SEND_AT_ONCE) {
                            val line = reader.readLine()
                            if (line == null) {
                                hasNextLine = false
                                break
                            }
                            lines[lineCount++] = line
                        }

                        // lineCount行まとめて出力
                        if (lineCount > 0) {
                            session.sendText { writer ->
                                val result = Arrays.copyOf(lines, lineCount + 1, Array<Any>::class.java)
                                result[lineCount] = Signal.EOF
                                Json.serialize(writer, result)
                            }
                        }
                    }

                    // 次の行が出力されるまで待機
                    Thread.sleep(200L)
                }
            }
        } catch (e: IOException) {
            // return
        } catch (e: InterruptedException) {
        } catch (e: Exception) {
            logger.error("An error has occurred.", e)
        } finally {
            currentThread.name = oldName
        }
    }

    companion object {
        /** logger  */
        private val logger = LoggerFactory.getLogger(Reader::class.java)

        /** 一度に送信する行数  */
        const val LINES_TO_SEND_AT_ONCE = 100
    }
}
