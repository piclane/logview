package me.piclane.logview.procedure;

import me.piclane.logview.procedure.text.*;
import me.piclane.logview.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * ふつうの検索
 *
 * @author yohei_hina
 */
class PlainSearch implements Runnable {
    /** logger */
    private static final Logger logger = LoggerFactory.getLogger(PlainSearch.class);

    /** {@link Session} */
    private final Session session;

    /** パラメーター */
    private final Param param;

    /** 最後にバッファをフラッシュした時刻(ミリ秒) */
    private long lastFlushMillis = System.currentTimeMillis();

    /** バッファ */
    private final LinkedList<Line> lineBuffer = new LinkedList<>();

    /**
     * コンストラクタ
     *
     * @param session {@link Session}
     * @param param パラメーター
     */
    public PlainSearch(Session session, Param param) {
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

        try(LineReader reader = LineReader.of(param)) {
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

            // 検索結果を送信
            while(session.isOpen()) {
                // バッファをフラッシュ
                flushBuffer(false, false);

                // 割込チェック
                if(Thread.interrupted()) {
                    throw new InterruptedException();
                }

                // 一行読み込み
                Line line = reader.readLine();
                if(line == null) {
                    break;
                }

                // 書き出し
                bufferLine(line);
            }

            // 割込チェック
            if(Thread.interrupted()) {
                throw new InterruptedException();
            }

            // バッファをフラッシュ
            flushBuffer(true, true);

            // 最終行まで検索が終了したことを伝える
            if(param.getDirection() == Direction.forward && !reader.hasNextLine()) {
                try (Writer writer = session.getBasicRemote().getSendWriter()) {
                    Json.serialize(new Object[]{Signal.EOF}, writer);
                }
            } else if(param.getDirection() == Direction.backward && !reader.hasNextLine()) {
                try (Writer writer = session.getBasicRemote().getSendWriter()) {
                    Json.serialize(new Object[]{Signal.BOF}, writer);
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
                // バッファをフラッシュ
                flushBuffer(false, true);

                // 割込チェック
                if(Thread.interrupted()) {
                    throw new InterruptedException();
                }

                // ファイル長更新
                reader.refresh();

                Line line = reader.readLine();
                if(line == null) {
                    Thread.sleep(200L);
                    continue;
                }

                // 書き出し
                bufferLine(line);
            }
        } catch (IOException | InterruptedException e) {
            // return
        } catch (Exception e) {
            logger.error("An error has occurred.", e);
        } finally {
            currentThread.setName(oldName);
        }
    }

    /**
     * 指定されたクエリ文字列が、指定された行のいずれかに含まれている場合、全ての行をバッファリングします。
     *
     * @param line 出力対象となる行
     */
    private void bufferLine(Line line) {
        if(!session.isOpen()) {
            return;
        }

        // 対象語が含まれていたらグループをまとめて送信
        if(containsQuery(line)) {
            lineBuffer.add(line);
        }
    }

    /**
     * バッファをフラッシュします
     *
     * @param force 強制的にフラッシュする場合
     * @param eof ファイルの終端シグナルを送出する場合
     * @throws IOException 入出力例外が発生した場合
     */
    private void flushBuffer(boolean force, boolean eof) throws IOException {
        long now = System.currentTimeMillis();
        if(!force && now - lastFlushMillis <= 200L && lineBuffer.size() < 100) {
            return;
        }
        if(lineBuffer.isEmpty()) {
            lastFlushMillis = now;
            return;
        }
        try(Writer writer = session.getBasicRemote().getSendWriter()) {
            Object[] buf;
            if(eof) {
                buf = lineBuffer.toArray(new Object[lineBuffer.size() + 1]);
                buf[buf.length - 1] = Signal.EOF;
            } else {
                buf = lineBuffer.toArray();
            }
            Json.serialize(buf, writer);
        }
        lineBuffer.clear();
        lastFlushMillis = now;
    }

    /**
     * 指定された全ての行を対象に、全てのクエリ文字列が含まれているかどうかを返します
     *
     * @param line 検索対象となるの行
     * @return 指定された全ての行を対象に、全てのクエリ文字列が含まれている場合 true そうでない場合 false
     */
    private boolean containsQuery(Line line) {
        String[] qs = param.getSearch();
        if(qs == null || qs.length == 0) {
            return true;
        }

        int qsl = qs.length;
        boolean[] matches = new boolean[qsl];
        int matchCount = 0;
        for(int i=0; i<qsl; i++) {
            if(matches[i]) {
                continue;
            }
            if(line.str.contains(qs[i])) {
                matches[i] = true;
                matchCount++;
            }
            if(matchCount == qsl) {
                break;
            }
        }
        return matchCount == qsl;
    }
}
