package com.xxuz.piclane.logview.procedure.text

import org.mozilla.universalchardet.UniversalDetector
import org.mozilla.intl.chardet.nsDetector
import org.mozilla.intl.chardet.nsPSMDetector
import java.lang.Exception
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Class to detect the charset
 *
 * @author yohei_hina
 */
object CharsetDetector {
    /** MS932 文字セット  */
    private val CHARSET_MS932 = Charset.forName("windows-31j")

    /** 文字セット検出タイムアウト(ミリ秒)  */
    private const val CHARSET_DETECT_TIMEOUT_MILLIS = 1000L

    /**
     * 文字セットを検出します
     *
     * @param path ファイル
     * @return 検出された文字セット
     */
    fun detect(path: Path): Charset =
        BufferedByteReader(Files.newByteChannel(path, StandardOpenOption.READ)).use {
            detect(it)
        }

    /**
     * 文字セットを検出します
     *
     * @param reader [BufferedByteReader]
     * @return 検出された文字セット
     */
    fun detect(reader: BufferedByteReader): Charset {
        val pos = reader.position()
        try {
            val uDet = UniversalDetector(null)
            val nDet = nsDetector(nsPSMDetector.JAPANESE)
            val buf = ByteArray(4096)
            var n: Int
            var isAscii = true
            var uDone = false
            var nDone = false
            val timeout = System.currentTimeMillis() + CHARSET_DETECT_TIMEOUT_MILLIS
            reader.position(0L)
            while (reader.read(buf).also { n = it } > 0 && !(nDone && uDone) && System.currentTimeMillis() < timeout) {
                if (isAscii) {
                    isAscii = nDet.isAscii(buf, n)
                }
                if (!isAscii) {
                    if (!nDone) {
                        nDone = nDet.DoIt(buf, n, false)
                    }
                    if (!uDone) {
                        uDet.handleData(buf, 0, n)
                        uDone = uDet.isDone
                    }
                }
            }
            nDet.DataEnd()
            uDet.dataEnd()
            if (isAscii) {
                return StandardCharsets.UTF_8
            }
            val nDetected = listOf(*nDet.probableCharsets)
            val uDetected = uDet.detectedCharset
            if ("nomatch" == nDetected[0]) {
                if (uDetected != null) {
                    return Charset.forName(uDetected)
                }
            }
            return when {
                uDetected != null && nDetected.contains(uDetected) -> Charset.forName(uDetected) // 二人の意見が一致
                nDetected.contains("Shift_JIS") -> CHARSET_MS932
                nDetected.contains("UTF-8") -> StandardCharsets.UTF_8
                nDetected.contains("UTF-16LE") -> StandardCharsets.UTF_16LE
                else -> Charset.forName(nDetected[0])
            }
        } catch (e: Exception) {
            return StandardCharsets.UTF_8
        } finally {
            reader.position(pos)
        }
    }
}
