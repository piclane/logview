package com.xxuz.piclane.logview.procedure

import com.xxuz.piclane.logview.procedure.text.Direction
import com.xxuz.piclane.logview.procedure.text.Line
import com.xxuz.piclane.logview.procedure.text.LineReader
import com.xxuz.piclane.logview.procedure.text.Offset
import com.xxuz.piclane.logview.util.Json
import com.xxuz.piclane.logview.util.sendText
import org.slf4j.LoggerFactory
import org.springframework.web.socket.WebSocketSession
import java.io.IOException
import java.util.*
import java.util.regex.Pattern
import javax.websocket.Session

/**
 * かしこい検索
 *
 * @author yohei_hina
 */
internal class SmartSearch(
        /** [WebSocketSession]  */
        private val session: WebSocketSession,
        /** パラメーター  */
        private val param: Param
) : Runnable {
    /** 最後にバッファをフラッシュした時刻(ミリ秒)  */
    private var lastFlushMillis = System.currentTimeMillis()

    /** バッファ  */
    private val lineBuffer = LinkedList<Line>()

    /**
     * @see Runnable.run
     */
    override fun run() {
        val path = param.path
        val currentThread = Thread.currentThread()
        val oldName = currentThread.name
        currentThread.name = "${javaClass.simpleName}-${path.fileName}"
        try {
            LineReader.of(param).use { reader ->
                session.sendText { writer ->
                    val signal = mutableListOf<Any>()
                    val offset: Offset = reader.currentOffset
                    signal.add(Signal.FILE_LENGTH(offset.length))
                    if (offset.isBof) {
                        signal.add(Signal.BOF)
                    }
                    Json.serialize(writer, signal)
                }

                // 検索結果を送信
                val groupLines = LinkedList<Line>()
                while (session.isOpen) {
                    // バッファをフラッシュ
                    flushBuffer(false, false)

                    // 割込チェック
                    if (Thread.interrupted()) {
                        throw InterruptedException()
                    }

                    // 一行読み込み
                    val line: Line = reader.readLine() ?: break

                    // グループ先頭行かどうか
                    val m = GROUP_START_PATTERN.matcher(line.str)

                    // (Direction.forward) この行がグループ開始行だった場合、前の行までを出力する
                    if (param.direction == Direction.forward && m.matches()) {
                        bufferLines(groupLines)
                    }

                    // この行をグループに追加
                    groupLines.addLast(line)
                    if (groupLines.size > 10000) {
                        groupLines.removeFirst()
                    }

                    // (Direction.backward) この行がグループ開始行だった場合、この行までを出力する
                    if (param.direction == Direction.backward && m.matches()) {
                        bufferLines(groupLines)
                    }
                }

                // 割込チェック
                if (Thread.interrupted()) {
                    throw InterruptedException()
                }

                // 最後のグループを送信
                bufferLines(groupLines)

                // バッファをフラッシュ
                flushBuffer(true, true)

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
                var lastLineFetch = System.currentTimeMillis()
                while (session.isOpen) {
                    // バッファをフラッシュ
                    flushBuffer(false, true)

                    // 割込チェック
                    if (Thread.interrupted()) {
                        throw InterruptedException()
                    }

                    // ファイル長更新
                    reader.refresh()

                    // 最後の行を読み込んでから、しばらくファイルに変化が見られない場合、行を出力してみる
                    val now = System.currentTimeMillis()
                    if (now - lastLineFetch > 600L) {
                        bufferLines(groupLines)
                    }
                    val line = reader.readLine()
                    if (line == null) {
                        Thread.sleep(200L)
                        continue
                    } else {
                        lastLineFetch = now
                    }

                    // グループ先頭行かどうか
                    val m = GROUP_START_PATTERN.matcher(line.str)

                    // (Direction.forward) この行がグループ開始行だった場合、前の行までを出力する
                    if (param.direction == Direction.forward && m.matches()) {
                        bufferLines(groupLines)
                    }

                    // この行をグループに追加
                    groupLines.addLast(line)
                    if (groupLines.size > 10000) {
                        groupLines.removeFirst()
                    }

                    // (Direction.backward) この行がグループ開始行だった場合、この行までを出力する
                    if (param.direction == Direction.backward && m.matches()) {
                        bufferLines(groupLines)
                    }
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

    /**
     * 指定されたクエリ文字列が、指定された行のいずれかに含まれている場合、全ての行をバッファリングします。
     *
     * @param lines 出力対象となる全ての行
     */
    private fun bufferLines(lines: MutableList<Line>) {
        if (lines.isEmpty() || !session.isOpen) {
            return
        }

        // 対象語が含まれていたらグループをまとめて送信
        if (containsQuery(lines)) {
            lineBuffer.addAll(lines)
        }
        lines.clear()
    }

    /**
     * バッファをフラッシュします
     *
     * @param force 強制的にフラッシュする場合
     * @param eof ファイルの終端シグナルを送出する場合
     * @throws IOException 入出力例外が発生した場合
     */
    @Throws(IOException::class)
    private fun flushBuffer(force: Boolean, eof: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastFlushMillis <= 200L && lineBuffer.size < 100) {
            return
        }
        if (lineBuffer.isEmpty()) {
            lastFlushMillis = now
            return
        }
        session.sendText { writer ->
            val buf: Array<Any>
            if (eof) {
                buf = lineBuffer.toTypedArray()
                buf[buf.size - 1] = Signal.EOF
            } else {
                buf = lineBuffer.toTypedArray()
            }
            Json.serialize(writer, buf)
        }
        lineBuffer.clear()
        lastFlushMillis = now
    }

    /**
     * 指定された全ての行を対象に、全てのクエリ文字列が含まれているかどうかを返します
     *
     * @param lines 検索対象となる全ての行
     * @return 指定された全ての行を対象に、全てのクエリ文字列が含まれている場合 true そうでない場合 false
     */
    private fun containsQuery(lines: List<Line>?): Boolean {
        if (lines == null || lines.isEmpty()) {
            return false
        }
        val qs = param.search
        if (qs.isEmpty()) {
            return true
        }
        val qsl = qs.size
        val matches = BooleanArray(qsl)
        var matchCount = 0
        for (line in lines) {
            for (i in 0 until qsl) {
                if (matches[i]) {
                    continue
                }
                if (line.str.contains(qs[i])) {
                    matches[i] = true
                    matchCount++
                }
                if (matchCount == qsl) {
                    break
                }
            }
        }
        return matchCount == qsl
    }

    companion object {
        /** logger  */
        private val logger = LoggerFactory.getLogger(SmartSearch::class.java)

        /** グループの先頭行パターン  */
        private val GROUP_START_PATTERN = Pattern.compile("^(" +
                // よくある「ログレベル+日付」形式
                "(\\[[^\\]]+\\] )?([A-Z]+:\\s*)?\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}" +
                "|" +  // 「日付+ログレベル」形式
                "\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(\\.\\d{3})?([+-]\\d{4})?\\s*(\\[[a-zA-Z ]+\\])?" +
                "|" +  // RabbitMQ 形式
                "=[A-Z]+ REPORT====" +
                "|" +  // Apache アクセスログ形式
                "\\d+\\.\\d+\\.\\d+\\.\\d+ (-|[^ ]+) (-|[^ ]+) \\[[0-9A-Za-z/:+ ]+\\]" +
                "|" +  // Apache エラーログ形式
                "\\[[0-9A-Za-z/:+ ]+\\] \\[error\\] \\[client \\d+\\.\\d+\\.\\d+\\.\\d+\\] " +
                ").*")
    }
}
