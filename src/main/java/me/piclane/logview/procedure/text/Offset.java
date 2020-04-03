package me.piclane.logview.procedure.text;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * オフセット
 */
public class Offset {
    /** ブロックサイズ */
    private static final long BLOCK_SIZE = BufferedByteReader.DEFAULT_BUFFER_SIZE;

    /** ファイル長 */
    public final long length;

    /** 先頭からの位置 (Byte) */
    public final long position;

    /**
     * Offset のインスタンスを生成します
     *
     * @param reader {@link BufferedByteReader}
     * @param offsetBytes 始点からの位置 (Byte)
     * @param offsetStart オフセットの開始位置
     * @param skipLines オフセット位置からスキップする行数
     *      正値が与えられた場合、末尾方向にスキップします
     *      負値が与えられた場合、先頭方向にスキップします
     *      0 の場合はスキップを行いません
     * @return 新しい Offset のインスタンス
     * @throws IOException 入出力例外が発生した場合
     */
    public static Offset of(BufferedByteReader reader, long offsetBytes, OffsetStart offsetStart, int skipLines) throws IOException {
        long length = reader.size();
        long position;
        if(offsetStart == OffsetStart.head) {
            position = Math.min(offsetBytes, length);
        } else {
            position = Math.max(0, length - offsetBytes);
        }
        if(skipLines > 0) {
            reader.position(position);
            for (int i = 0; i < skipLines && position < length; i++) {
                position = Line.skipLine(reader);
            }
            return new Offset(length, position);
        } else if(skipLines < 0) {
            skipLines = -skipLines;
            int currentTailLineCount = 0;
            long posBlockStart = Math.max(0, position - BLOCK_SIZE);
            long posBlockEnd = position;
            List<Long> positions = new ArrayList<>();
            do {
                // ブロックの先頭行に頭出し
                reader.position(posBlockStart);

                // ブロックの中の各行を読み取り
                long currentPos;
                positions.clear();
                do {
                    positions.add(currentPos = Line.skipLine(reader));
                } while (currentPos < posBlockEnd);

                // 指定された行の開始位置を返して終了
                if (currentTailLineCount + positions.size() >= skipLines) {
                    return new Offset(length, positions.get(positions.size() - skipLines + currentTailLineCount - 1));
                }

                currentTailLineCount += positions.size();
                posBlockEnd = posBlockStart - 1;
                posBlockStart = Math.max(0, posBlockEnd - BLOCK_SIZE);
            } while (posBlockStart > 0);
            return new Offset(length, 0);
        } else {
            return new Offset(length, position);
        }
    }

    /**
     * コンストラクタ
     *
     * @param length ファイル長
     * @param position 先頭からの位置 (Byte)
     */
    private Offset(long length, long position) {
        this.length = length;
        this.position = position;
    }

    /**
     * 先頭からの位置を指定して Offset の新しいインスタンスを生成します
     *
     * @param position 先頭からの位置 (Byte)
     * @return Offset の新しいインスタンス
     */
    public Offset withPosition(long position) {
        if(position < 0) {
            position = 0;
        } else if(position > length) {
            position = length;
        }
        return new Offset(this.length, position);
    }

    /**
     * 最新のファイル長を指定して Offset の新しいインスタンスを生成します
     *
     * @param channel {@link SeekableByteChannel}
     * @return Offset の新しいインスタンス
     * @throws IOException 入出力例外が発生した場合
     */
    public Offset withLength(SeekableByteChannel channel) throws IOException {
        return new Offset(channel.size(), position);
    }

    /**
     * 最新のファイル長を指定して Offset の新しいインスタンスを生成します
     *
     * @param reader {@link BufferedByteReader}
     * @return Offset の新しいインスタンス
     * @throws IOException 入出力例外が発生した場合
     */
    public Offset withLength(BufferedByteReader reader) throws IOException {
        return new Offset(reader.size(), position);
    }

    /**
     * 位置がファイルの終端かどうかを取得します
     *
     * @return 位置がファイルの終端の場合 true そうでない場合 false
     */
    public boolean isEof() {
        return position == length;
    }

    /**
     * 位置がファイルの先頭かどうかを取得します
     *
     * @return 位置がファイルの先頭の場合 true そうでない場合 false
     */
    public boolean isBof() {
        return position == 0;
    }
}
