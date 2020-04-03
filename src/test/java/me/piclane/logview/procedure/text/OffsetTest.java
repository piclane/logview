package me.piclane.logview.procedure.text;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class OffsetTest {
    /** Test FileSystem */
    private FileSystem testFs;

    /** Test file path */
    private Path testPath;

    /** lines in test file */
    private List<String> expectLines;

    @Before
    public void before() throws IOException {
        StringBuilder buf = new StringBuilder();
        for(int i=0; i<10; i++) {
            for(int j=0; j<10; j++) {
                buf.append(i);
            }
            buf.append("\n");
        }
        String expectString = buf.toString();

        this.testFs = Jimfs.newFileSystem(Configuration.unix());
        this.testPath = testFs.getPath("/test.txt");
        try(BufferedWriter out = Files.newBufferedWriter(testPath)) {
            out.append(expectString);
        }
        expectLines = Files.readAllLines(testPath);
    }

    @After
    public void after() throws IOException {
        testFs.close();
    }

    @Test
    public void test_of_OffsetStart_Head() throws IOException {
        Offset offset;
        try(BufferedByteReader reader = new BufferedByteReader(testPath)) {
            offset = Offset.of(reader, 0, OffsetStart.head, 0);
            assertThat(offset.length, is(equalTo(110L)));
            assertThat(offset.position, is(equalTo(0L)));

            offset = Offset.of(reader, 200, OffsetStart.head, 0);
            assertThat(offset.length, is(equalTo(110L)));
            assertThat(offset.position, is(equalTo(110L)));
        }
    }

    @Test
    public void test_of_OffsetStart_Tail() throws IOException {
        Offset offset;
        try(BufferedByteReader reader = new BufferedByteReader(testPath)) {
            offset = Offset.of(reader, 0, OffsetStart.tail, 0);
            assertThat(offset.length, is(equalTo(110L)));
            assertThat(offset.position, is(equalTo(110L)));

            offset = Offset.of(reader, 10, OffsetStart.tail, 0);
            assertThat(offset.length, is(equalTo(110L)));
            assertThat(offset.position, is(equalTo(100L)));
        }
    }

    @Test
    public void test_of_SkipLines_Positive() throws IOException {
        Offset offset;
        try(BufferedByteReader reader = new BufferedByteReader(testPath)) {
            offset = Offset.of(reader, 0, OffsetStart.head, 3);
            assertThat(offset.length, is(equalTo(110L)));
            assertThat(offset.position, is(equalTo(33L)));

            offset = Offset.of(reader, 0, OffsetStart.tail, 3);
            assertThat(offset.length, is(equalTo(110L)));
            assertThat(offset.position, is(equalTo(110L)));
        }
    }

    @Test
    public void test_of_SkipLines_Negative() throws IOException {
        Offset offset;
        try(BufferedByteReader reader = new BufferedByteReader(testPath)) {
            offset = Offset.of(reader, 0, OffsetStart.tail, -3);
            assertThat(offset.length, is(equalTo(110L)));
            assertThat(offset.position, is(equalTo(77L)));

            offset = Offset.of(reader, 0, OffsetStart.head, -3);
            assertThat(offset.length, is(equalTo(110L)));
            assertThat(offset.position, is(equalTo(0L)));
        }
    }

    @Test
    public void test_withPosition() throws IOException {
        Offset offset;
        try(BufferedByteReader reader = new BufferedByteReader(testPath)) {
            offset = Offset.of(reader, 0, OffsetStart.head, 0);
            assertThat(offset.length, is(equalTo(110L)));
            assertThat(offset.position, is(equalTo(0L)));

            offset = offset.withPosition(60L);
            assertThat(offset.length, is(equalTo(110L)));
            assertThat(offset.position, is(equalTo(60L)));

            offset = offset.withPosition(-10L);
            assertThat(offset.length, is(equalTo(110L)));
            assertThat(offset.position, is(equalTo(0L)));

            offset = offset.withPosition(10000L);
            assertThat(offset.length, is(equalTo(110L)));
            assertThat(offset.position, is(equalTo(110L)));
        }
    }

    @Test
    public void test_withLength() throws IOException {
        Offset offset;
        try(BufferedByteReader reader = new BufferedByteReader(testPath)) {
            offset = Offset.of(reader, 0, OffsetStart.head, 0);
            assertThat(offset.length, is(equalTo(110L)));
            assertThat(offset.position, is(equalTo(0L)));

            try(BufferedWriter out = Files.newBufferedWriter(testPath, StandardOpenOption.APPEND)) {
                out.append("aaaaaaaaaa\n");
            }

            offset = offset.withLength(reader);
            assertThat(offset.length, is(equalTo(121L)));
            assertThat(offset.position, is(equalTo(0L)));
        }
    }

    @Test
    public void test_isEof() throws IOException {
        Offset offset;
        try(BufferedByteReader reader = new BufferedByteReader(testPath)) {
            offset = Offset.of(reader, 0, OffsetStart.head, 0);
            assertThat(offset.length, is(equalTo(110L)));
            assertThat(offset.position, is(equalTo(0L)));
            assertThat(offset.isEof(), is(false));

            offset = Offset.of(reader, 0, OffsetStart.tail, 0);
            assertThat(offset.length, is(equalTo(110L)));
            assertThat(offset.position, is(equalTo(110L)));
            assertThat(offset.isEof(), is(true));
        }
    }

    @Test
    public void test_isBof() throws IOException {
        Offset offset;
        try(BufferedByteReader reader = new BufferedByteReader(testPath)) {
            offset = Offset.of(reader, 0, OffsetStart.head, 0);
            assertThat(offset.length, is(equalTo(110L)));
            assertThat(offset.position, is(equalTo(0L)));
            assertThat(offset.isBof(), is(true));

            offset = Offset.of(reader, 0, OffsetStart.tail, 0);
            assertThat(offset.length, is(equalTo(110L)));
            assertThat(offset.position, is(equalTo(110L)));
            assertThat(offset.isBof(), is(false));
        }
    }
}
