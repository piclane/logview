package me.piclane.logview.procedure.text;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;

/**
 * 行を表現します
 *
 * @author yohei_hina
 */
public class Line {
    /**
     * consumer が必要とするまで Line を読み込みます
     *
     * @param channel {@link SeekableByteChannel}
     * @param charset 文字セット
     * @param consumer {@link LineConsumer}
     * @throws IOException 入出力例外が発生した場合
     */
    public static void readLine(SeekableByteChannel channel, Charset charset, LineConsumer consumer) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        boolean eof = false;
        int c;
        try(ByteReader reader = new ByteReader(channel)) {
            while(true) {
                boolean eol = false;
                long pos = reader.position();
                while (!eol) {
                    switch (c = reader.read()) {
                        case -1: // EOFに達した場合
                            eof = true;
                        case '\n':
                            eol = true;
                            break;
                        case '\r':
                            eol = true;
                            long cur = reader.position();
                            if ((reader.read()) != '\n') {
                                reader.position(cur);
                            }
                            break;
                        default:
                            baos.write(c);
                            break;
                    }
                }

                if (eof && baos.size() == 0) {
                    return;
                }
                if (!consumer.consume(new Line(
                        pos,
                        reader.position() - pos,
                        new String(baos.toByteArray(), charset)))) {
                    break;
                }
                baos.reset();
            }
        }
    }

    /**
     * 読み込まれた行を消費するためのインターフェイス
     */
    @FunctionalInterface
    public interface LineConsumer {
        /**
         * Line を消費します
         *
         * @param line 消費する Line
         * @return 次の Line を受け取る場合 true そうでない場合 false
         */
        boolean consume(Line line);
    }

    /**
     * 次の行頭までファイルポインタを移動します
     *
     * @param channel {@link SeekableByteChannel}
     * @throws IOException 入出力例外が発生した場合
     */
    public static long skipLine(SeekableByteChannel channel) throws IOException {
        try(ByteReader reader = new ByteReader(channel)) {
            while (true) {
                switch (reader.read()) {
                    case -1: // EOFに達した場合
                    case '\n':
                        return reader.position();
                    case '\r':
                        long cur = reader.position();
                        if (reader.read() != '\n') {
                            reader.position(cur);
                        } else {
                            cur++;
                        }
                        return cur;
                    default:
                        break;
                }
            }
        }
    }

    /** 行頭の位置 */
    public final long pos;

    /** バイト長 */
    public final long len;

    /** 一行の文字列 */
    public final String str;

    /**
     * コンストラクタ
     *
     * @param pos 行頭の位置
     * @param len バイト長
     * @param string 一行の文字列
     */
    private Line(long pos, long len, String string) {
        this.pos = pos;
        this.len = len;
        this.str = string;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return str;
    }
}
