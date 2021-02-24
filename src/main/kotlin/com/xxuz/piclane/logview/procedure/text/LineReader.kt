package com.xxuz.piclane.logview.procedure.text

import com.xxuz.piclane.logview.procedure.Param
import java.io.IOException
import java.nio.charset.Charset
import java.util.*

/**
 * Line リーダー
 *
 * @author yohei_hina
 */
class LineReader(
        /** [BufferedByteReader] */
        private val reader: BufferedByteReader,

        /** 走査方向 */
        private val direction: Direction,

        /** オフセット */
        var currentOffset: Offset,

        /** 行バッファのサイズ */
        private val lineBufferSize: Int
) : AutoCloseable {
    /** 行バッファ  */
    private val lines: Deque<Line> = LinkedList()

    /** 文字セット  */
    private val charset: Charset = CharsetDetector.detect(reader)

    /**
     * ファイル長を最新に更新します
     */
    fun refresh() {
        currentOffset = currentOffset.withLength(reader)
    }

    /**
     * [.readLine] で次の行が取得できるかどうかを取得します
     *
     * @return [.readLine] で次の行が取得できる場合 true そうでない場合 false
     */
    fun hasNextLine(): Boolean {
        if (direction == Direction.forward) {
            if (!currentOffset.isEof && lines.isEmpty()) {
                readForward()
            }
        } else {
            if (!currentOffset.isBof && lines.isEmpty()) {
                readBackward()
            }
        }
        return !lines.isEmpty()
    }

    /**
     * 一行読み込みます
     *
     * @return 読み込んだ行
     * @throws IOException 入出力例外が発生した場合
     */
    fun readLine(): Line? {
        return if (direction == Direction.forward) {
            if (!currentOffset.isEof && lines.isEmpty()) {
                readForward()
            }
            lines.pollFirst()
        } else {
            if (!currentOffset.isBof && lines.isEmpty()) {
                readBackward()
            }
            lines.pollLast()
        }
    }

    /**
     * 順方向に 1 行読み込みます
     *
     * @throws IOException 入出力例外が発生した場合
     */
    private fun readForward() {
        reader.position(currentOffset.position)
        Line.readLine(reader, charset) { line ->
            lines.addLast(line)
            lines.size < lineBufferSize
        }
        val last = lines.peekLast()
        if (last != null) {
            currentOffset = currentOffset.withPosition(last.pos + last.len)
        }
    }

    /**
     * 逆方向に 1 行読み込みます
     *
     * @throws IOException 入出力例外が発生した場合
     */
    private fun readBackward() {
        var end = currentOffset.position
        var start = Math.max(0, end - lineBufferSize * 100)
        val buf: Deque<Line> = LinkedList()
        var last: Line
        do {
            reader.position(start)
            val _start = if (start == 0L) start else Line.skipLine(reader)
            val _end = end
            Line.readLine(reader, charset) { line ->
                buf.addLast(line)
                line.pos + line.len < _end
            }
            end = _start
            start = Math.max(0, _end - lineBufferSize * 100)
            while (true) {
                buf.pollLast()?.also {
                    last = it
                    lines.addFirst(last)
                } ?: break
            }
        } while (lines.size < lineBufferSize && start > 0)
        val first = lines.peekFirst()
        if (first != null) {
            currentOffset = currentOffset.withPosition(first.pos)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun close() {
        reader.close()
    }

    companion object {
        /**
         * [Param] から [LineReader] を生成します
         *
         * @param param Param
         * @throws IOException 入出力例外が発生した場合
         */
        @Throws(IOException::class)
        fun of(param: Param): LineReader {
            val reader = BufferedByteReader(param.path)
            return LineReader(
                    reader,
                    param.direction,
                    Offset.of(reader, param.offsetBytes, param.offsetStart, param.skipLines),
                    param.lines)
        }
    }
}
