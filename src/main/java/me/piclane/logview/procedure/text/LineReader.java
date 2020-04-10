package me.piclane.logview.procedure.text;

import me.piclane.logview.procedure.Param;

import java.io.IOException;
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

    /** {@link BufferedByteReader} */
    private final BufferedByteReader reader;

    /** 文字セット */
    private final Charset charset;

    /** 走査方向 */
    private final Direction direction;

    /** オフセット */
    private Offset offset;

    /**
     * {@link Param} から {@link LineReader} を生成します
     *
     * @param param Param
     * @throws IOException 入出力例外が発生した場合
     */
    public static LineReader of(Param param) throws IOException {
        BufferedByteReader reader = new BufferedByteReader(param.getPath());
        return new LineReader(
            reader,
            param.getDirection(),
            Offset.of(reader, param.getOffsetBytes(), param.getOffsetStart(), param.getSkipLines()),
            param.getLines());
    }

    /**
     * コンストラクタ
     *
     * @param reader {@link BufferedByteReader}
     * @param direction 走査方向
     * @param offset オフセット
     * @param lineBufferSize 行バッファのサイズ
     */
    public LineReader(BufferedByteReader reader, Direction direction, Offset offset, int lineBufferSize) {
        this.reader = reader;
        this.direction = direction;
        this.offset = offset;
        this.lineBufferSize = lineBufferSize;
        this.charset = CharsetDetector.detect(reader);
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
        offset = offset.withLength(reader);
    }

    /**
     * {@link #readLine()} で次の行が取得できるかどうかを取得します
     *
     * @return {@link #readLine()} で次の行が取得できる場合 true そうでない場合 false
     * @throws IOException 入出力例外が発生した場合
     */
    public boolean hasNextLine() throws IOException {
        if(direction == Direction.forward) {
            if(!offset.isEof() && lines.isEmpty()) {
                readForward();
            }
        } else {
            if(!offset.isBof() && lines.isEmpty()) {
                readBackward();
            }
        }
        return !lines.isEmpty();
    }

    /**
     * 一行読み込みます
     *
     * @return 読み込んだ行
     * @throws IOException 入出力例外が発生した場合
     */
    public Line readLine() throws IOException {
        if(direction == Direction.forward) {
            if(!offset.isEof() && lines.isEmpty()) {
                readForward();
            }
            return lines.pollFirst();
        } else {
            if(!offset.isBof() && lines.isEmpty()) {
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
        reader.position(offset.position);
        Line.readLine(reader, charset, line -> {
            lines.addLast(line);
            return lines.size() < lineBufferSize;
        });
        Line last = lines.peekLast();
        if (last != null) {
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
            reader.position(start);
            long _start = start == 0 ? start : Line.skipLine(reader);
            long _end = end;
            Line.readLine(reader, charset, line -> {
                buf.addLast(line);
                return line.pos + line.len < _end;
            });
            end = _start;
            start = Math.max(0, _end - lineBufferSize * 100);
            while((last = buf.pollLast()) != null) {
                lines.addFirst(last);
            }
        } while(lines.size() < lineBufferSize && start > 0);

        Line first = lines.peekFirst();
        if(first != null) {
            offset = offset.withPosition(first.pos);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        reader.close();
    }
}
