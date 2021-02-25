package com.xxuz.piclane.logview.util

import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Pattern

/**
 * 環境変数展開用ユーティリティー
 *
 * @author yohei_hina
 */
object Environment {
    /** logger  */
    private val logger = LoggerFactory.getLogger(Environment::class.java)

    /** 展開対象のパターン  */
    private val PATTERN_PARAM = Pattern.compile("""\$\{(?<name1>[^}]+)}|\$(?<name2>[a-zA-Z0-9_]+)|(?:^(?<name3>@.*)$)""")

    /** 展開マクロ  */
    private val PATTERN_MACRO = Pattern.compile("""^(?<file>@?)(?<name>[^:]+)(:(?<sign>[-+])(?<alt>.*))?$""")

    /**
     * 文字列内の環境変数を展開します
     *
     * @param string 文字列
     * @return 環境変数が展開された文字列
     */
    fun expand(string: Any?): String? {
        if (string == null) {
            return null
        }
        val src = string.toString()
        val m = PATTERN_PARAM.matcher(src)
        val buf = StringBuilder()
        var pos = 0
        while (m.find()) {
            val start = m.start()
            val end = m.end()
            var value: String?
            var name = m.group("name1")
            if (name == null) {
                name = m.group("name2")
            }
            if (name == null) {
                name = m.group("name3")
            }
            val m2 = PATTERN_MACRO.matcher(name)
            if (m2.matches()) {
                val isFile = "@" == m2.group("file")
                name = m2.group("name")
                value = if (isFile) expand(readFile(name)) else System.getenv(name)
                val validVal = value != null && value.length > 0
                val sign = m2.group("sign")
                val altval = m2.group("alt")
                if ("-" == sign && !validVal || "+" == sign && validVal) {
                    value = altval
                }
            } else {
                value = System.getenv(name)
            }
            buf.append(src, pos, start)
            if (value != null) {
                buf.append(value)
            }
            pos = end
        }
        buf.append(src, pos, src.length)
        return buf.toString()
    }

    /**
     * ファイルを読み込みます
     *
     * @param path 読み込むファイルのパス
     * @return ファイルの内容
     */
    private fun readFile(path: String): String? {
        val target = Paths.get(path)
        return if (!Files.exists(target)) {
            null
        } else try {
            Files.readAllLines(target, StandardCharsets.UTF_8).joinToString("\n")
        } catch (e: IOException) {
            logger.error("Failed to read file: $path", e)
            null
        }
    }
}
