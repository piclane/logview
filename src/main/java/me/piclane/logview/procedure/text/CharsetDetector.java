package me.piclane.logview.procedure.text;

import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsPSMDetector;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

public class CharsetDetector {
    /** MS932 文字セット */
    private static final Charset CHARSET_MS932 = Charset.forName("windows-31j");

    /** 文字セット検出タイムアウト(ミリ秒) */
    private static final long CHARSET_DETECT_TIMEOUT_MILLIS = 1000L;

    /**
     * 文字セットを検出します
     *
     * @param path ファイル
     * @return 検出された文字セット
     * @throws IOException 入出力例外が発生した場合
     */
    public static Charset detect(Path path) throws IOException {
        try(BufferedByteReader reader = new BufferedByteReader(Files.newByteChannel(path, StandardOpenOption.READ))) {
            return detect(reader);
        }
    }

    /**
     * 文字セットを検出します
     *
     * @param reader {@link BufferedByteReader}
     * @return 検出された文字セット
     */
    public static Charset detect(BufferedByteReader reader) {
        try {
            UniversalDetector uDet = new UniversalDetector(null);
            nsDetector nDet = new nsDetector(nsPSMDetector.JAPANESE);
            byte[] buf = new byte[4096];
            int n;
            boolean isAscii = true, uDone = false, nDone = false;
            long timeout = System.currentTimeMillis() + CHARSET_DETECT_TIMEOUT_MILLIS;
            reader.position(0);
            while((n = reader.read(buf)) > 0 && !(nDone && uDone) && System.currentTimeMillis() < timeout) {
                if(isAscii) {
                    isAscii = nDet.isAscii(buf, n);
                }

                if(!isAscii) {
                    if(!nDone) {
                        nDone = nDet.DoIt(buf, n, false);
                    }
                    if(!uDone) {
                        uDet.handleData(buf, 0, n);
                        uDone = uDet.isDone();
                    }
                }
            }
            nDet.DataEnd();
            uDet.dataEnd();

            if(isAscii) {
                return StandardCharsets.UTF_8;
            }

            List<String> nDetected = Arrays.asList(nDet.getProbableCharsets());
            String uDetected = uDet.getDetectedCharset();
            if("nomatch".equals(nDetected.get(0))) {
                if(uDetected != null) {
                    return Charset.forName(uDetected);
                }
            }

            if(uDetected != null && nDetected.contains(uDetected)) {
                return Charset.forName(uDetected); // 二人の意見が一致
            } else if(nDetected.contains("Shift_JIS")) {
                return CHARSET_MS932;
            } else if(nDetected.contains("UTF-8")) {
                return StandardCharsets.UTF_8;
            } else if(nDetected.contains("UTF-16LE")) {
                return StandardCharsets.UTF_16LE;
            } else {
                return Charset.forName(nDetected.get(0));
            }
        } catch(Exception e) {
            return StandardCharsets.UTF_8;
        }
    }
}
