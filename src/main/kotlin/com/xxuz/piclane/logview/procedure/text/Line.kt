package com.xxuz.piclane.logview.procedure.text

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

/**
 * 行を表現します
 *
 * @author yohei_hina
 */
class Line private constructor(
        /** 行頭の位置  */
        val pos: Long,

        /** バイト長  */
        val len: Long,

        /** 一行の文字列  */
        val str: String
) {
    /**
     * @see Object.toString
     */
    override fun toString(): String = str

    companion object {
        private const val INT_LF = '\n'.toInt()
        private const val INT_CR = '\r'.toInt()

        /**
         * consumer が必要とするまで Line を読み込みます
         *
         * @param reader [BufferedByteReader]
         * @param charset 文字セット
         * @param consumer {line} 消費する Line {return} 次の Line を受け取る場合 true そうでない場合 false
         */
        fun readLine(reader: BufferedByteReader, charset: Charset, consumer: (line: Line) -> Boolean) {
            val baos = ByteArrayOutputStream(4096)
            var eof = false
            var c: Int
            while (true) {
                var eol = false
                val pos = reader.position()
                while (!eol) {
                    when (reader.read().also { c = it }) {
                        -1 -> {
                            eof = true
                            eol = true
                        }
                        INT_LF -> eol = true
                        INT_CR -> {
                            eol = true
                            val cur = reader.position()
                            if (reader.read() != '\n'.toInt()) {
                                reader.position(cur)
                            }
                        }
                        else -> baos.write(c)
                    }
                }
                if (eof && baos.size() == 0) {
                    return
                }
                if (!consumer(Line(
                                pos,
                                reader.position() - pos,
                                String(baos.toByteArray(), charset)))) {
                    return
                }
                baos.reset()
            }
        }

        /**
         * 次の行頭までファイルポインタを移動します
         *
         * @param reader [BufferedByteReader]
         */
        fun skipLine(reader: BufferedByteReader): Long {
            while (true) {
                when (reader.read()) {
                    INT_LF, -1 -> return reader.position()
                    INT_CR -> {
                        var cur = reader.position()
                        if (reader.read() != '\n'.toInt()) {
                            reader.position(cur)
                        } else {
                            cur++
                        }
                        return cur
                    }
                    else -> {
                    }
                }
            }
        }
    }
}
