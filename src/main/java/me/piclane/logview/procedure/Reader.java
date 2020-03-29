package me.piclane.logview.procedure;

import me.piclane.logview.procedure.text.Line;
import me.piclane.logview.procedure.text.LineReader;
import me.piclane.logview.procedure.text.Offset;
import me.piclane.logview.util.Json;

import javax.websocket.Session;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ふつうの読込
 */
class Reader implements Runnable {
    /** {@link Session} */
    private final Session session;

    /** パラメーター */
    private final Param param;

    /**
     * コンストラクタ
     *
     * @param session {@link Session}
     * @param param パラメーター
     */
    public Reader(Session session, Param param) {
        this.session = session;
        this.param = param;
    }

    /**
     * @see Runnable#run()
     */
    public void run() {
        Path path = param.getPath();
        Thread currentThread = Thread.currentThread();
        String oldName = currentThread.getName();
        currentThread.setName(getClass().getSimpleName() + "-" + path.getFileName());

        // 一度に送信する行数
        int lineCountMax = 100;
        Line[] lines = new Line[lineCountMax];

        try(LineReader reader = new LineReader(Files.newByteChannel(path), param)) {
            // ファイル長を送信
            try(Writer writer = session.getBasicRemote().getSendWriter()) {
                List<Object> signal = new ArrayList<>();
                Offset offset = reader.getCurrentOffset();
                signal.add(Signal.FILE_LENGTH(offset.length));
                if(offset.isBof()) {
                    signal.add(Signal.BOF);
                }
                Json.serialize(signal, writer);
            }

            int lineCount, lineCountAll = 0;
            boolean hasNextLine = true;
            while(session.isOpen() && hasNextLine && lineCountAll < param.getLines()) {
                // 割込チェック
                if(Thread.interrupted()) {
                    throw new InterruptedException();
                }

                // 最大 lineCountMax 行読み込む
                for(lineCount=0; lineCount<lineCountMax && lineCountAll<param.getLines(); lineCount++, lineCountAll++) {
                    Line line = reader.readLine();
                    if(line == null) {
                        hasNextLine = false;
                        break;
                    }
                    lines[lineCount] = line;
                }

                // lineCount行まとめて出力
                if(lineCount > 0) {
                    try(Writer writer = session.getBasicRemote().getSendWriter()) {
                        Json.serialize(Arrays.copyOf(lines, lineCount), writer);
                    }
                }
            }

            // 最終行まで検索が終了したことを伝える
            if(reader.getCurrentOffset().isEof()) {
                try (Writer writer = session.getBasicRemote().getSendWriter()) {
                    Json.serialize(new Object[]{Signal.EOF}, writer);
                }
            } else {
                return; // 最終行まで読み込んでいない場合は続きを監視しない
            }

            // 続きを監視しない場合は終わり
            if(!param.isFollow()) {
                return;
            }

            // 追加行を送信
            while(session.isOpen()) {
                // 割込チェック
                if(Thread.interrupted()) {
                    throw new InterruptedException();
                }

                // ファイル長更新
                reader.refresh();

                // 最大 lineCountMax 行読み込む
                hasNextLine = true;
                while(hasNextLine) {
                    for (lineCount = 0; lineCount < lineCountMax; ) {
                        Line line = reader.readLine();
                        if (line == null) {
                            hasNextLine = false;
                            break;
                        }
                        lines[lineCount++] = line;
                    }

                    // lineCount行まとめて出力
                    if (lineCount > 0) {
                        try (Writer writer = session.getBasicRemote().getSendWriter()) {
                            Object[] result = Arrays.copyOf(lines, lineCount + 1, Object[].class);
                            result[lineCount] = Signal.EOF;
                            Json.serialize(result, writer);
                        }
                    }
                }

                // 次の行が出力されるまで待機
                Thread.sleep(200L);
            }
        } catch (IOException | InterruptedException e) {
            // return
        } finally {
            currentThread.setName(oldName);
        }
    }
}
