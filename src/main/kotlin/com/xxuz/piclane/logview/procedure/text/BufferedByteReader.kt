package com.xxuz.piclane.logview.procedure.text

import kotlin.jvm.JvmOverloads
import java.nio.channels.SeekableByteChannel
import java.lang.AutoCloseable
import java.nio.file.OpenOption
import kotlin.Throws
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.min

/**
 * [SeekableByteChannel] から 1 バイトずつ読み込むクラス
 *
 * @author yohei_hina
 */
class BufferedByteReader @JvmOverloads constructor(
    /** [SeekableByteChannel]  */
    private val channel: SeekableByteChannel,

    /** 一度に読み込むバッファのサイズ */
    bufferSize: Int = DEFAULT_BUFFER_SIZE
) : AutoCloseable {
    /** [ByteBuffer]  */
    private val buf: ByteBuffer = ByteBuffer.allocate(bufferSize)

    /** [ByteBuffer] に読み込まれたサイズ  */
    private var bufferSize = UNDEFINED_BUFFER_SIZE

    /** [ByteBuffer] の先頭の絶対位置  */
    private var position = UNDEFINED_POSITION

    /**
     * コンストラクタ
     *
     * @param path 開くファイルへのパス
     * @param options ファイルを開く方法を指定するオプション
     * @throws IOException 入出力例外が発生した場合
     */
    constructor(path: Path, vararg options: OpenOption) : this(Files.newByteChannel(path, *options))

    /**
     * 現在の位置を取得します
     *
     * @return 現在の位置 (byte)
     * @throws IOException 入出力例外が発生した場合
     */
    @Throws(IOException::class)
    fun position(): Long {
        if (position == UNDEFINED_POSITION) {
            position = channel.position()
        }
        return if (bufferSize == UNDEFINED_BUFFER_SIZE) {
            position
        } else {
            position + buf.position()
        }
    }

    /**
     * 現在の位置を設定します
     *
     * @param position 新しい現在の位置 (byte)
     * @return 実際に移動された現在の位置 (byte)
     * @throws IOException 入出力例外が発生した場合
     */
    @Throws(IOException::class)
    fun position(position: Long): Long {
        if (bufferSize == UNDEFINED_BUFFER_SIZE) {
            // まだバッファが空の場合
            this.position = position
            channel.position(position)
            return this.position
        } else if (position < this.position || this.position + bufferSize <= position) {
            // 現在保持しているバッファの範囲外の場合
            bufferSize = UNDEFINED_BUFFER_SIZE
            this.position = position
            channel.position(position)
            return this.position
        } else {
            // 現在保持しているバッファの範囲内の場合
            buf.position((position - this.position).toInt())
            return position
        }
    }

    /**
     * 1 バイト読み込みます
     * 読み込んだバイトは 0 〜 255 の数値のため int で返されます
     *
     * @return 読み込まれた 1 バイト
     * @throws IOException 入出力例外が発生した場合
     */
    @Throws(IOException::class)
    fun read(): Int =
        if (ensureBuffer() == -1) {
            -1
        } else {
            verifyBuffer(java.lang.Byte.toUnsignedInt(buf.get()))
        }

    /**
     * 最大 length バイトのデータを、バイト配列に読み込みます。
     * length バイトまでの読込みが試行されますが、読み込まれるバイト数はもっと少ない場合もあります。
     * 実際に読み込まれたバイト数は整数として返されます。
     *
     * @param dst データの読込み先のバッファ
     * @param offset データが書き込まれる配列 dst の開始オフセット
     * @param length 読み込む最大バイト数
     * @return 実際に読み込まれたバイト数。ストリームの終わりに達してデータがない場合は-1
     * @throws IOException 入出力例外が発生した場合
     */
    @JvmOverloads
    @Throws(IOException::class)
    fun read(dst: ByteArray, offset: Int = 0, length: Int = dst.size): Int {
        var read = 0
        do {
            val actualReadLen = ensureBuffer()
            if (actualReadLen == -1) {
                return if (read == 0) -1 else read
            }
            val readLen = min(length - read, buf.remaining()).let {
                if(actualReadLen == 0) {
                    it
                } else {
                    min(it, actualReadLen)
                }
            }
            buf.get(dst, read + offset, readLen)
            read += verifyBuffer(readLen)
        } while (read < length)
        return read
    }

    /**
     * 必要ならバッファを読み込みます
     *
     * @return EOF の場合 -1 読み込む必要が無かった場合 0 実際に読み込んだ場合は読み込んだバイト数
     * @throws IOException 入出力例外が発生した場合
     */
    @Throws(IOException::class)
    private fun ensureBuffer(): Int {
        if (bufferSize == UNDEFINED_BUFFER_SIZE) {
            buf.clear()
            position = channel.position()
            bufferSize = channel.read(buf)
            if (bufferSize == -1) {
                bufferSize = UNDEFINED_BUFFER_SIZE
                return -1
            }
            buf.position(0)
            return bufferSize
        }
        return 0
    }

    /**
     * バッファの現在位置が変更された後に、残りバッファを検査します
     *
     * @param any 返値となる任意の値
     * @return 引数 any
     */
    private fun verifyBuffer(any: Int): Int {
        if (bufferSize <= buf.position()) {
            position += bufferSize.toLong()
            bufferSize = UNDEFINED_BUFFER_SIZE
        }
        return any
    }

    /**
     * ファイルのサイズを取得します
     *
     * @return ファイルサイズ
     * @throws IOException 入出力例外が発生した場合
     */
    @Throws(IOException::class)
    fun size(): Long {
        return channel.size()
    }

    /**
     * {@inheritDoc}
     */
    @Throws(IOException::class)
    override fun close() {
        channel.close()
    }

    /**
     * InputStream を生成します
     */
    fun inputStream(): InputStream = InputStreamImpl()

    companion object {
        /** デフォルトバッファサイズ  */
        const val DEFAULT_BUFFER_SIZE = 4096

        /** バッファサイズが未定  */
        private const val UNDEFINED_BUFFER_SIZE = Int.MIN_VALUE

        /** 位置が未定  */
        private const val UNDEFINED_POSITION = Long.MIN_VALUE
    }

    /**
     * BufferedByteReader による InputStream 実装
     */
    private inner class InputStreamImpl: InputStream() {
        /** マーカーのポジション */
        private var mark = position()

        override fun read(): Int =
            this@BufferedByteReader.read()

        override fun read(b: ByteArray, off: Int, len: Int): Int =
            this@BufferedByteReader.read(b, off, len)

        override fun skip(n: Long): Long {
            val currentPos = position()
            val nextPos = position(currentPos + n)
            return nextPos - currentPos
        }

        override fun available(): Int =
            (size() - position()).toInt()

        override fun mark(readlimit: Int) {
            mark = position()
        }

        override fun reset() {
            position(mark)
        }

        override fun markSupported(): Boolean =
            true
    }
}
