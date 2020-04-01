package me.piclane.logview.procedure.text;

import me.piclane.logview.procedure.Param;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.util.Deque;
import java.util.LinkedList;

/**
 * Line リーダー
 */
public class LineReader implements AutoCloseable {
    /** 行バッファ */
    private final Deque<Line> lines = new LinkedList<>();

    /** 行バッファのサイズ */
    private final int lineBufferSize;

    /** {@link SeekableByteChannel} */
    private final SeekableByteChannel channel;

    /** 文字セット */
    private final Charset charset;

    /** 走査方向 */
    private final Direction direction;

    /** オフセット */
    private Offset offset;

    /**
     * コンストラクタ
     *
     * @param channel {@link SeekableByteChannel}
     * @param param Param
     * @throws IOException 入出力例外が発生した場合
     */
    public LineReader(SeekableByteChannel channel, Param param) throws IOException {
        this(
            channel,
            param.getDirection(),
            Offset.of(channel, param.getOffsetBytes(), param.getOffsetStart(), param.getSkipLines()),
            param.getLines());
    }

    /**
     * コンストラクタ
     *
     * @param channel {@link SeekableByteChannel}
     * @param direction 走査方向
     * @param offset オフセット
     * @param lineBufferSize 行バッファのサイズ
     */
    public LineReader(SeekableByteChannel channel, Direction direction, Offset offset, int lineBufferSize) {
        this.channel = channel;
        this.direction = direction;
        this.offset = offset;
        this.lineBufferSize = lineBufferSize;
        this.charset = CharsetDetector.detect(channel);
    }

    /**
     * 現在のオフセットを取得します
     *
     * @return 現在のオフセット
     */
    public Offset getCurrentOffset() {
        return offset;
    }

    /**
     * ファイル長を最新に更新します
     *
     * @throws IOException 入出力例外が発生した場合
     */
    public void refresh() throws IOException {
        offset = offset.withLength(channel);
    }

    /**
     * {@link #readLine()} で次の行が取得できるかどうかを取得します
     *
     * @return {@link #readLine()} で次の行が取得できる場合 true そうでない場合 false
     */
    public boolean hasNextLine() {
        if(!lines.isEmpty()) {
            return true;
        }
        if(direction == Direction.forward) {
            return !offset.isEof();
        } else {
            return !offset.isBof();
        }
    }

    /**
     * 一行読み込みます
     *
     * @return 読み込んだ行
     * @throws IOException 入出力例外が発生した場合
     */
    public Line readLine() throws IOException {
        if(direction == Direction.forward) {
            if(!offset.isEof()) {
                readForward();
            }
            return lines.pollFirst();
        } else {
            if(!offset.isBof()) {
                readBackward();
            }
            return lines.pollLast();
        }
    }

    /**
     * 順方向に 1 行読み込みます
     *
     * @throws IOException 入出力例外が発生した場合
     */
    private void readForward() throws IOException {
        channel.position(offset.position);
        Line.readLine(channel, charset, line -> {
            lines.addLast(line);
            return lines.size() < lineBufferSize;
        });
        Line last = lines.getLast();
        if(last != null) {
            offset = offset.withPosition(last.pos + last.len);
        }
    }

    /**
     * 逆方向に 1 行読み込みます
     *
     * @throws IOException 入出力例外が発生した場合
     */
    private void readBackward() throws IOException {
        long end = offset.position;
        long start = Math.max(0, end - lineBufferSize * 100);
        Deque<Line> buf = new LinkedList<>();
        Line last;
        do {
            channel.position(start);
            long _start = start == 0 ? start : Line.skipLine(channel);
            long _end = end;
            Line.readLine(channel, charset, line -> {
                buf.addLast(line);
                return line.pos + line.len < _end;
            });
            end = _start;
            start = Math.max(0, _end - lineBufferSize * 100);
            while((last = buf.pollLast()) != null) {
                lines.addFirst(last);
            }
        } while(lines.size() < lineBufferSize && start > 0);

        Line first = lines.getFirst();
        if(first != null) {
            offset = offset.withPosition(first.pos);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        channel.close();
    }
}
