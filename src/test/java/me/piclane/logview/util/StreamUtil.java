package me.piclane.logview.util;

import java.io.*;

/**
 * ストリームユーティリティー
 *
 * @author yohei_hina
 */
public class StreamUtil {
    /**
     * 入力ストリームから出力ストリームに吸い上げます
     *
     * @param in 入力ストリーム
     * @param out 出力ストリーム
     * @throws IOException 入出力例外が発生した場合
     */
    public static void pumpStream(InputStream in, OutputStream out) throws IOException {
        final int bufLen = 4096;
        byte[] buf = new byte[bufLen];
        int len;
        while((len = in.read(buf, 0, bufLen)) != -1) {
            out.write(buf, 0, len);
        }
    }

    /**
     * リーダーからライターに吸い上げます
     *
     * @param reader リーダー
     * @param writer ライター
     * @throws IOException 入出力例外が発生した場合
     */
    public static void pumpStream(Reader reader, Writer writer) throws IOException {
        final int bufLen = 4096;
        char[] buf = new char[bufLen];
        int len;
        while((len = reader.read(buf, 0, bufLen)) != -1) {
            writer.write(buf, 0, len);
        }
    }
}
