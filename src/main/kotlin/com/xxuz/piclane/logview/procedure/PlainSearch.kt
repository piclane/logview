package com.xxuz.piclane.logview.procedure

import com.xxuz.piclane.logview.procedure.text.*
import com.xxuz.piclane.logview.util.Json
import com.xxuz.piclane.logview.util.sendText
import org.slf4j.LoggerFactory
import org.springframework.web.socket.WebSocketSession
import java.io.IOException
import java.util.*

/**
 * ふつうの検索
 *
 * @author yohei_hina
 */
internal class PlainSearch (
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
                while (session.isOpen) {
                    // バッファをフラッシュ
                    flushBuffer(false, false)

                    // 割込チェック
                    if (Thread.interrupted()) {
                        throw InterruptedException()
                    }

                    // 一行読み込み
                    val line: Line = reader.readLine() ?: break

                    // 書き出し
                    bufferLine(line)
                }

                // 割込チェック
                if (Thread.interrupted()) {
                    throw InterruptedException()
                }

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
                while (session.isOpen) {
                    // バッファをフラッシュ
                    flushBuffer(false, true)

                    // 割込チェック
                    if (Thread.interrupted()) {
                        throw InterruptedException()
                    }

                    // ファイル長更新
                    reader.refresh()
                    val line = reader.readLine()
                    if (line == null) {
                        Thread.sleep(200L)
                        continue
                    }

                    // 書き出し
                    bufferLine(line)
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
     * @param line 出力対象となる行
     */
    private fun bufferLine(line: Line) {
        if (!session.isOpen) {
            return
        }

        // 対象語が含まれていたらグループをまとめて送信
        if (containsQuery(line)) {
            lineBuffer.add(line)
        }
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
     * @param line 検索対象となるの行
     * @return 指定された全ての行を対象に、全てのクエリ文字列が含まれている場合 true そうでない場合 false
     */
    private fun containsQuery(line: Line): Boolean {
        val qs = param.search
        if (qs.isEmpty()) {
            return true
        }
        val qsl = qs.size
        val matches = BooleanArray(qsl)
        var matchCount = 0
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
        return matchCount == qsl
    }

    companion object {
        /** logger  */
        private val logger = LoggerFactory.getLogger(PlainSearch::class.java)
    }
}
