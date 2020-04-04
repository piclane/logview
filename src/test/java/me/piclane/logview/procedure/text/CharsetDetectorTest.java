package me.piclane.logview.procedure.text;

import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class CharsetDetectorTest {
    @Test
    public void test_utf8() throws Exception {
        Path path = Paths.get(CharsetDetectorTest.class.getResource("sample/text_utf8.txt").toURI());
        Charset actual = CharsetDetector.detect(path);
        assertThat(actual, is(equalTo(StandardCharsets.UTF_8)));
    }

    @Test
    public void test_utf16le() throws Exception {
        Path path = Paths.get(CharsetDetectorTest.class.getResource("sample/text_utf16le.txt").toURI());
        Charset actual = CharsetDetector.detect(path);
        assertThat(actual, is(equalTo(StandardCharsets.UTF_16LE)));
    }

    @Test
    public void test_utf16be() throws Exception {
        Path path = Paths.get(CharsetDetectorTest.class.getResource("sample/text_utf16be.txt").toURI());
        Charset actual = CharsetDetector.detect(path);
        assertThat(actual, is(equalTo(StandardCharsets.UTF_16BE)));
    }

    @Test
    public void test_sjis() throws Exception {
        Path path = Paths.get(CharsetDetectorTest.class.getResource("sample/text_sjis.txt").toURI());
        Charset actual = CharsetDetector.detect(path);
        assertThat(actual, is(equalTo(Charset.forName("windows-31j"))));
    }

    @Test
    public void test_eucjp() throws Exception {
        Path path = Paths.get(CharsetDetectorTest.class.getResource("sample/text_eucjp.txt").toURI());
        Charset actual = CharsetDetector.detect(path);
        assertThat(actual, is(equalTo(Charset.forName("EUC-JP"))));
    }
}
