package me.piclane.logview.procedure;

/**
 * ログの行とは別に送信したい信号を表現します
 *
 * @author yohei_hina
 */
public class Signal {
    /** ファイルの先頭 */
    public static final Signal BOF = new Signal("bof");

    /** ファイルの終端 */
    public static final Signal EOF = new Signal("eof");

    /** ファイルの長さ */
    public static Signal FILE_LENGTH(long length) {
        return new Signal("file_length", length);
    }

    /** メッセージ */
    public final String signal;

    /** 値 */
    public final Object value;

    /**
     * コンストラクタ
     *
     * @param signal シグナル
     */
    private Signal(String signal) {
        this.signal = signal;
        this.value = null;
    }

    /**
     * コンストラクタ
     *
     * @param signal シグナル
     * @param value 値
     */
    private Signal(String signal, Object value) {
        this.signal = signal;
        this.value = value;
    }
}
