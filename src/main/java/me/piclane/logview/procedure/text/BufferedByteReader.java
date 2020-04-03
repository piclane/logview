package me.piclane.logview.procedure.text;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

/**
 * {@link SeekableByteChannel} から 1 バイトずつ読み込むクラス
 */
public class BufferedByteReader implements AutoCloseable {
    /** デフォルトバッファサイズ */
    public static final int DEFAULT_BUFFER_SIZE = 4096;

    public static final int UNDEFINED_BUFFER_SIZE = Integer.MIN_VALUE;

    private static final long UNDEFINED_POSITION = Long.MIN_VALUE;

    /** {@link SeekableByteChannel} */
    private final SeekableByteChannel channel;

    /** {@link ByteBuffer} */
    private final ByteBuffer buf;

    /** {@link ByteBuffer} に読み込まれたサイズ */
    private int bufferSize = UNDEFINED_BUFFER_SIZE;

    /** {@link ByteBuffer} の先頭の絶対位置 */
    private long position = UNDEFINED_POSITION;

    /**
     * コンストラクタ
     *
     * @param path 開くファイルへのパス
     * @param options ファイルを開く方法を指定するオプション
     * @throws IOException 入出力例外が発生した場合
     */
    public BufferedByteReader(Path path, OpenOption... options) throws IOException {
        this(Files.newByteChannel(path, options));
    }

    /**
     * コンストラクタ
     *
     * @param channel {@link SeekableByteChannel}
     */
    public BufferedByteReader(SeekableByteChannel channel) {
        this(channel, DEFAULT_BUFFER_SIZE);
    }

    /**
     * コンストラクタ
     *
     * @param channel {@link SeekableByteChannel}
     * @param bufferSize 一度に読み込むバッファのサイズ
     */
    public BufferedByteReader(SeekableByteChannel channel, int bufferSize) {
        this.channel = channel;
        this.buf = ByteBuffer.allocate(bufferSize);
    }

    /**
     * 現在の位置を取得します
     *
     * @return 現在の位置 (byte)
     * @throws IOException 入出力例外が発生した場合
     */
    public long position() throws IOException {
        if(this.position == UNDEFINED_POSITION) {
            this.position = this.channel.position();
        }
        if(this.bufferSize == UNDEFINED_BUFFER_SIZE) {
            return this.position;
        } else {
            return this.position + buf.position();
        }
    }

    /**
     * 現在の位置を設定します
     *
     * @param position 現在の位置 (byte)
     * @throws IOException 入出力例外が発生した場合
     */
    public void position(long position) throws IOException {
        if(this.bufferSize == UNDEFINED_BUFFER_SIZE) {
            // まだバッファが空の場合
            this.position = position;
            this.channel.position(position);
        } else if(position < this.position || (this.position + this.bufferSize) <= position) {
            // 現在保持しているバッファの範囲外の場合
            this.bufferSize = UNDEFINED_BUFFER_SIZE;
            this.position = position;
            this.channel.position(position);
        } else {
            // 現在保持しているバッファの範囲内の場合
            this.buf.position((int)(position - this.position));
        }
    }

    /**
     * 1 バイト読み込みます
     * 読み込んだバイトは 0 〜 255 の数値のため int で返されます
     *
     * @return 読み込まれた 1 バイト
     * @throws IOException 入出力例外が発生した場合
     */
    public int read() throws IOException {
        if(ensureBuffer()) {
            return -1;
        }
        return verifyBuffer(Byte.toUnsignedInt(buf.get()));
    }

    /**
     * 読み込んだデータをバイト配列 dst に格納します。
     * 実際に読み込まれたバイト数は整数として返されます。
     *
     * @param dst データの読込み先のバッファ
     * @return 実際に読み込まれたバイト数。ストリームの終わりに達してデータがない場合は-1
     * @throws IOException 入出力例外が発生した場合
     */
    public int read(byte[] dst) throws IOException {
        return read(dst, 0, dst.length);
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
    public int read(byte[] dst, int offset, int length) throws IOException {
        int read = 0;
        do {
            if (ensureBuffer()) {
                return read == 0 ? -1 : read;
            }
            int readLen = Math.min(length - read, buf.remaining());
            buf.get(dst, read + offset, readLen);
            read += verifyBuffer(readLen);
        } while(read < length);
        return read;
    }

    /**
     * 必要ならバッファを読み込みます
     *
     * @return EOF の場合 true そうでない場合 false
     * @throws IOException 入出力例外が発生した場合
     */
    private boolean ensureBuffer() throws IOException {
        if(this.bufferSize == UNDEFINED_BUFFER_SIZE) {
            buf.clear();
            this.position = channel.position();
            this.bufferSize = channel.read(buf);
            if (this.bufferSize == -1) {
                this.bufferSize = UNDEFINED_BUFFER_SIZE;
                return true;
            }
            buf.position(0);
        }
        return false;
    }

    /**
     * バッファの現在位置が変更された後に、残りバッファを検査します
     *
     * @param any 返値となる任意の値
     * @return 引数 any
     */
    private int verifyBuffer(int any) {
        if(this.bufferSize <= buf.position()) {
            this.position += this.bufferSize;
            this.bufferSize = UNDEFINED_BUFFER_SIZE;
        }
        return any;
    }

    /**
     * ファイルのサイズを取得します
     *
     * @return ファイルサイズ
     * @throws IOException 入出力例外が発生した場合
     */
    public long size() throws IOException {
        return channel.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        this.channel.close();
    }
}
