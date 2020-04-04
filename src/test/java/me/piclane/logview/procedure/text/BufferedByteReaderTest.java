package me.piclane.logview.procedure.text;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class BufferedByteReaderTest {
    /** Test FileSystem */
    private FileSystem testFs;

    /** Test file path */
    private Path testPath;

    /** Test file contents */
    private byte[] expectArray;

    @Before
    public void before() throws IOException {
        StringBuilder buf = new StringBuilder();
        for(int i=0; i<10; i++) {
            buf.append("0123456789");
        }
        String expectString = buf.toString();
        expectArray = expectString.getBytes(StandardCharsets.UTF_8);

        this.testFs = Jimfs.newFileSystem(Configuration.unix());
        this.testPath = testFs.getPath("/test.txt");
        try(BufferedWriter out = Files.newBufferedWriter(testPath)) {
            out.append(expectString);
        }
    }

    @After
    public void after() throws IOException {
        testFs.close();
    }

    @Test
    public void testClose() throws IOException {
        try(SeekableByteChannel channel = Files.newByteChannel(testPath)) {
            BufferedByteReader reader = new BufferedByteReader(channel);
            reader.close();
            Assert.assertFalse(channel.isOpen());
        }
    }

    @Test
    public void testSize() throws IOException {
        try(BufferedByteReader reader = new BufferedByteReader(Files.newByteChannel(testPath), 10)) {
            assertThat(reader.size(), is(equalTo(100L)));
        }
    }

    @Test
    public void testRead() throws IOException {
        try(BufferedByteReader reader = new BufferedByteReader(Files.newByteChannel(testPath), 10)) {
            int c;
            for(int index = 0; (c = reader.read()) != -1; index++) {
                assertThat(
                        "index " + index + " is not equals.",
                        c,
                        is(equalTo(Byte.toUnsignedInt(expectArray[index]))));
            }
        }
    }

    @Test
    public void testRead2() throws IOException {
        try(BufferedByteReader reader = new BufferedByteReader(Files.newByteChannel(testPath), 1024)) {
            int c;
            for(int index = 0; (c = reader.read()) != -1; index++) {
                assertThat(
                        "index " + index + " is not equals.",
                        c,
                        is(equalTo(Byte.toUnsignedInt(expectArray[index]))));
            }
        }
    }

    @Test
    public void testReadToByteArray_LessThanBuffer() throws IOException {
        try(BufferedByteReader reader = new BufferedByteReader(Files.newByteChannel(testPath), 20)) {
            for(int i=0; i<5; i++) {
                reader.read();
            }

            byte[] buf = new byte[20];
            int read = reader.read(buf, 5, 10);
            assertThat(read, is(equalTo(10)));
            byte[] expect = "\0\0\0\0\0005678901234\0\0\0\0\0".getBytes(StandardCharsets.UTF_8);
            Assert.assertArrayEquals(expect, buf);
        }
    }

    @Test
    public void testReadToByteArray_MoreThanBuffer() throws IOException {
        try(BufferedByteReader reader = new BufferedByteReader(Files.newByteChannel(testPath), 10)) {
            for(int i=0; i<5; i++) {
                reader.read();
            }

            byte[] buf = new byte[30];
            int read = reader.read(buf, 5, 20);
            assertThat(read, is(equalTo(20)));
            byte[] expect = "\0\0\0\0\00056789012345678901234\0\0\0\0\0".getBytes(StandardCharsets.UTF_8);
            Assert.assertArrayEquals(expect, buf);
        }
    }

    @Test
    public void testReadToByteArray_MoreThanBuffer2() throws IOException {
        try(BufferedByteReader reader = new BufferedByteReader(Files.newByteChannel(testPath), 7)) {
            for(int i=0; i<5; i++) {
                reader.read();
            }

            byte[] buf = new byte[30];
            int read = reader.read(buf, 5, 20);
            assertThat(read, is(equalTo(20)));
            byte[] expect = "\0\0\0\0\00056789012345678901234\0\0\0\0\0".getBytes(StandardCharsets.UTF_8);
            Assert.assertArrayEquals(expect, buf);
        }
    }

    @Test
    public void testPositionRead() throws IOException {
        try(BufferedByteReader reader = new BufferedByteReader(Files.newByteChannel(testPath), 10)) {
            for(int i=0; i<5; i++) {
                reader.read();
            }

            assertThat(reader.position(), is(equalTo(5L)));

            byte[] buf = new byte[15];
            int read = reader.read(buf);
            assertThat(read, is(equalTo(15)));

            assertThat(reader.position(), is(equalTo(20L)));
        }
    }

    @Test
    public void testPositionWrite() throws IOException {
        try(BufferedByteReader reader = new BufferedByteReader(Files.newByteChannel(testPath), 10)) {
            reader.position(5L);
            assertThat(reader.position(), is(equalTo(5L)));
            assertThat((char)reader.read(), is(equalTo('5')));

            reader.position(8L);
            assertThat(reader.position(), is(equalTo(8L)));
            assertThat((char)reader.read(), is(equalTo('8')));

            reader.position(30L);
            assertThat(reader.position(), is(equalTo(30L)));
            assertThat((char)reader.read(), is(equalTo('0')));
        }
    }
}
