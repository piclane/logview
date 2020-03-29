package me.piclane.logview.procedure.text;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

/**
 * {@link SeekableByteChannel} から 1 バイトずつ読み込むクラス
 * {@link AutoCloseable} ですがチャンネルを閉じたりはせず position を最後に読み込んだ位置に戻すだけです
 */
class ByteReader implements AutoCloseable {
    /** デフォルトバッファサイズ */
    public static final int DEFAULT_BUFFER_SIZE = 4096;

    /** {@link SeekableByteChannel} */
    private final SeekableByteChannel channel;

    /** {@link ByteBuffer} */
    private final ByteBuffer buf;

    /** {@link ByteBuffer} に読み込まれたサイズ */
    private int bufferSize = Integer.MIN_VALUE;

    /** {@link ByteBuffer} に読み込んだ時の position */
    private long position = -1;

    /**
     * コンストラクタ
     *
     * @param channel {@link SeekableByteChannel}
     */
    public ByteReader(SeekableByteChannel channel) {
        this(channel, DEFAULT_BUFFER_SIZE);
    }

    /**
     * コンストラクタ
     *
     * @param channel {@link SeekableByteChannel}
     * @param bufferSize 一度に読み込むバッファのサイズ
     */
    public ByteReader(SeekableByteChannel channel, int bufferSize) {
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
        if(this.position == -1) {
            this.position = this.channel.position();
        }
        return this.position + buf.position();
    }

    /**
     * 現在の位置を設定します
     *
     * @param position 現在の位置 (byte)
     * @throws IOException 入出力例外が発生した場合
     */
    public void position(long position) throws IOException {
        if(this.bufferSize == Integer.MIN_VALUE || this.bufferSize >= buf.position()) {
            this.position = position;
            this.channel.position(position);
        } else if(position < this.position || (this.position + this.bufferSize) < position) {
            this.bufferSize = -1;
            this.position = position;
            this.channel.position(position);
        } else {
            this.buf.position((int)(this.position - position));
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
        if(this.bufferSize == Integer.MIN_VALUE || this.bufferSize <= buf.position()) {
            this.position = channel.position();
            buf.clear();
            this.bufferSize = channel.read(buf);
            if (this.bufferSize == -1) {
                return -1;
            }
            buf.position(0);
        }
        return Byte.toUnsignedInt(buf.get());
    }

    /**
     * チャンネルは閉じません。
     * position を最後に読み込んだ位置に戻します。
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        this.channel.position(position());
    }
}
