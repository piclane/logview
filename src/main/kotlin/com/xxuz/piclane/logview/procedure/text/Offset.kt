package com.xxuz.piclane.logview.procedure.text

import java.io.IOException
import java.nio.channels.SeekableByteChannel
import java.util.*

/**
 * オフセット
 *
 * @author yohei_hina
 */
class Offset private constructor(
        /** ファイル長  */
        val length: Long,

        /** 先頭からの位置 (Byte)  */
        val position: Long
) {
    /**
     * 先頭からの位置を指定して Offset の新しいインスタンスを生成します
     *
     * @param position 先頭からの位置 (Byte)
     * @return Offset の新しいインスタンス
     */
    fun withPosition(position: Long): Offset {
        return Offset(length, position.let { pos ->
            when {
                pos < 0 -> 0
                pos > length -> length
                else -> pos
            }
        })
    }

    /**
     * 最新のファイル長を指定して Offset の新しいインスタンスを生成します
     *
     * @param channel [SeekableByteChannel]
     * @return Offset の新しいインスタンス
     * @throws IOException 入出力例外が発生した場合
     */
    @Throws(IOException::class)
    fun withLength(channel: SeekableByteChannel): Offset {
        return Offset(channel.size(), position)
    }

    /**
     * 最新のファイル長を指定して Offset の新しいインスタンスを生成します
     *
     * @param reader [BufferedByteReader]
     * @return Offset の新しいインスタンス
     * @throws IOException 入出力例外が発生した場合
     */
    @Throws(IOException::class)
    fun withLength(reader: BufferedByteReader): Offset {
        return Offset(reader.size(), position)
    }

    /**
     * 位置がファイルの終端かどうかを取得します
     *
     * @return 位置がファイルの終端の場合 true そうでない場合 false
     */
    val isEof: Boolean
        get() = position == length

    /**
     * 位置がファイルの先頭かどうかを取得します
     *
     * @return 位置がファイルの先頭の場合 true そうでない場合 false
     */
    val isBof: Boolean
        get() = position == 0L

    companion object {
        /** ブロックサイズ  */
        private val BLOCK_SIZE: Long = BufferedByteReader.DEFAULT_BUFFER_SIZE.toLong()

        /**
         * Offset のインスタンスを生成します
         *
         * @param reader [BufferedByteReader]
         * @param offsetBytes 始点からの位置 (Byte)
         * @param offsetStart オフセットの開始位置
         * @param skipLines オフセット位置からスキップする行数
         * 正値が与えられた場合、末尾方向にスキップします
         * 負値が与えられた場合、先頭方向にスキップします
         * 0 の場合はスキップを行いません
         * @return 新しい Offset のインスタンス
         * @throws IOException 入出力例外が発生した場合
         */
        @Throws(IOException::class)
        fun of(reader: BufferedByteReader, offsetBytes: Long, offsetStart: OffsetStart, skipLines: Int): Offset {
            val length = reader.size()
            var position = if (offsetStart == OffsetStart.head) {
                Math.min(offsetBytes, length)
            } else {
                Math.max(0, length - offsetBytes)
            }

            return if (skipLines > 0) {
                reader.position(position)
                var i = 0
                while (i < skipLines && position < length) {
                    position = Line.skipLine(reader)
                    i++
                }
                Offset(length, position)
            } else if (skipLines < 0) {
                val skipLinesPositive = -skipLines
                var currentTailLineCount = 0
                var posBlockStart = Math.max(0, position - BLOCK_SIZE)
                var posBlockEnd = position
                val positions: MutableList<Long> = ArrayList()
                do {
                    // ブロックの先頭行に頭出し
                    reader.position(posBlockStart)

                    // ブロックの中の各行を読み取り
                    var currentPos: Long
                    positions.clear()
                    do {
                        positions.add(Line.skipLine(reader).also { currentPos = it })
                    } while (currentPos < posBlockEnd)

                    // 指定された行の開始位置を返して終了
                    if (currentTailLineCount + positions.size > skipLinesPositive) {
                        return Offset(length, positions[positions.size - skipLinesPositive + currentTailLineCount - 1])
                    }
                    currentTailLineCount += positions.size
                    posBlockEnd = posBlockStart - 1
                    posBlockStart = Math.max(0, posBlockEnd - BLOCK_SIZE)
                } while (posBlockStart > 0)
                Offset(length, 0)
            } else {
                Offset(length, position)
            }
        }
    }
}
