package com.xxuz.piclane.logview.procedure.text

import com.xxuz.piclane.logview.procedure.text.CharsetDetector.detect
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

class CharsetDetectorTest {
    @Test
    fun test_utf8() {
        val path = Paths.get(CharsetDetectorTest::class.java.getResource("/CharsetDetectorTest/text_utf8.txt").toURI())
        val actual = detect(path)
        MatcherAssert.assertThat(actual, Matchers.`is`(Matchers.equalTo(StandardCharsets.UTF_8)))
    }

    @Test
    fun test_utf16le() {
        val path = Paths.get(CharsetDetectorTest::class.java.getResource("/CharsetDetectorTest/text_utf16le.txt").toURI())
        val actual = detect(path)
        MatcherAssert.assertThat(actual, Matchers.`is`(Matchers.equalTo(StandardCharsets.UTF_16LE)))
    }

    @Test
    fun test_utf16be() {
        val path = Paths.get(CharsetDetectorTest::class.java.getResource("/CharsetDetectorTest/text_utf16be.txt").toURI())
        val actual = detect(path)
        MatcherAssert.assertThat(actual, Matchers.`is`(Matchers.equalTo(StandardCharsets.UTF_16BE)))
    }

    @Test
    fun test_sjis() {
        val path = Paths.get(CharsetDetectorTest::class.java.getResource("/CharsetDetectorTest/text_sjis.txt").toURI())
        val actual = detect(path)
        MatcherAssert.assertThat(actual, Matchers.`is`(Matchers.equalTo(Charset.forName("windows-31j"))))
    }

    @Test
    fun test_eucjp() {
        val path = Paths.get(CharsetDetectorTest::class.java.getResource("/CharsetDetectorTest/text_eucjp.txt").toURI())
        val actual = detect(path)
        MatcherAssert.assertThat(actual, Matchers.`is`(Matchers.equalTo(Charset.forName("EUC-JP"))))
    }
}
