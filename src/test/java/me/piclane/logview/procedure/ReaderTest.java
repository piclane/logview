package me.piclane.logview.procedure;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import me.piclane.logview.procedure.text.Direction;
import me.piclane.logview.procedure.text.OffsetStart;
import me.piclane.util.IntEqualTo;
import me.piclane.util.LongEqualTo;
import me.piclane.util.MessageConsumableWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.powermock.api.mockito.PowerMockito.*;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Session.class, RemoteEndpoint.Basic.class})
public class ReaderTest {
    /** Test FileSystem */
    private FileSystem testFs;

    /** Test file path */
    private Path testPath;

    /** lines in test file */
    private List<Line> expectLines;

    @Before
    public void before() throws IOException {
        StringBuilder buf = new StringBuilder();
        for(int i=0; i<300; i++) {
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

        updateExpectLines();
    }

    private void updateExpectLines() throws IOException {
        AtomicInteger pos = new AtomicInteger();
        expectLines = Files.lines(testPath)
                .map(str ->  new Line(pos.getAndAdd(str.length() + 1), str.length() + 1, str))
                .collect(Collectors.toList());
    }

    @After
    public void after() throws IOException {
        testFs.close();
    }

    @Test
    public void testForward() throws Exception {
        MessageConsumableWriter writer = new MessageConsumableWriter();

        RemoteEndpoint.Basic endpoint = mock(RemoteEndpoint.Basic.class);
        doReturn(writer).when(endpoint).getSendWriter();

        Session session = mock(Session.class);
        doReturn(true).when(session).isOpen();
        doReturn(endpoint).when(session).getBasicRemote();

        AtomicInteger lineNum = new AtomicInteger();
        Thread t;

        { // Get the first 200 lines.
            Param param = new Param();
            param.setPath(this.testPath);
            param.setStatus(Status.start);
            param.setDirection(Direction.forward);
            param.setLines(200);
            param.setOffsetBytes(0);
            param.setOffsetStart(OffsetStart.head);
            param.setSkipLines(0);
            param.setFollow(true);

            t = new Thread(new Reader(session, param));
            t.start();

            // file length
            writer.consume(m -> {
                assertThat(m, hasEntry("signal", "file_length"));
                assertThat(m, hasKey("value"));
                assertThat(m, hasEntry(equalTo("value"), new IntEqualTo(8200)));
                return false;
            });

            // begin of file signal
            writer.consume(m -> {
                assertThat(m, hasEntry("signal", "bof"));
                return false;
            });

            // lines
            writer.consume(m -> {
                int line = lineNum.getAndIncrement();
                Line expectedLine = expectLines.get(line);
                assertThat(m, hasEntry(equalTo("pos"), new LongEqualTo(expectedLine.pos)));
                assertThat(m, hasEntry(equalTo("len"), new LongEqualTo(expectedLine.len)));
                assertThat(m, hasEntry(equalTo("str"), equalTo(expectedLine.str)));
                return 199 != line;
            });

            // end of requested signal
            writer.consume(m -> {
                assertThat(m, hasEntry("signal", "eor"));
                return false;
            });

            // Thread was dead
            Thread.sleep(100L);
            assertThat(t.isAlive(), is(equalTo(false)));
        }

        { // Get 100 lines of remains.
            Param param = new Param();
            param.setPath(this.testPath);
            param.setStatus(Status.start);
            param.setDirection(Direction.forward);
            param.setLines(200);
            param.setOffsetBytes(expectLines.get(200).pos);
            param.setOffsetStart(OffsetStart.head);
            param.setSkipLines(0);
            param.setFollow(true);

            t = new Thread(new Reader(session, param));
            t.start();

            // file length
            writer.consume(m -> {
                assertThat(m, hasEntry("signal", "file_length"));
                assertThat(m, hasKey("value"));
                assertThat(m, hasEntry(equalTo("value"), new IntEqualTo(8200)));
                return false;
            });

            // lines
            writer.consume(m -> {
                int line = lineNum.getAndIncrement();
                Line expectedLine = expectLines.get(line);
                assertThat(m, hasEntry(equalTo("pos"), new LongEqualTo(expectedLine.pos)));
                assertThat(m, hasEntry(equalTo("len"), new LongEqualTo(expectedLine.len)));
                assertThat(m, hasEntry(equalTo("str"), equalTo(expectedLine.str)));
                return 299 != line;
            });

            // end of requested signal
            writer.consume(m -> {
                assertThat(m, hasEntry("signal", "eor"));
                return false;
            });

            // end of file signal
            writer.consume(m -> {
                assertThat(m, hasEntry("signal", "eof"));
                return false;
            });

            // Thread was not dead
            Thread.sleep(100L);
            assertThat(t.isAlive(), is(equalTo(true)));
        }

        { // Get the added rows
            // append 2 lines
            Files.write(testPath, Arrays.asList("aaaaaaaaaa", "bbbbbbbbbb"), StandardOpenOption.APPEND);
            Thread.sleep(200L);

            updateExpectLines();

            // test line aaaaaaaaaa
            writer.consume(m -> {
                int line = lineNum.getAndIncrement();
                Line expectedLine = expectLines.get(line);
                assertThat(m, hasEntry(equalTo("pos"), new LongEqualTo(expectedLine.pos)));
                assertThat(m, hasEntry(equalTo("len"), new LongEqualTo(expectedLine.len)));
                assertThat(m, hasEntry(equalTo("str"), equalTo(expectedLine.str)));
                return 301 != line;
            });

            // end of file signal
            writer.consume(m -> {
                assertThat(m, hasEntry("signal", "eof"));
                return false;
            });

            t.interrupt();
            t.join();
        }
    }

    @Test
    public void testBackward() throws Exception {
        MessageConsumableWriter writer = new MessageConsumableWriter();

        RemoteEndpoint.Basic endpoint = mock(RemoteEndpoint.Basic.class);
        doReturn(writer).when(endpoint).getSendWriter();

        Session session = mock(Session.class);
        doReturn(true).when(session).isOpen();
        doReturn(endpoint).when(session).getBasicRemote();

        AtomicInteger lineNum = new AtomicInteger(299);
        Thread t;

        { // Get the last 200 lines.
            Param param = new Param();
            param.setPath(this.testPath);
            param.setStatus(Status.start);
            param.setDirection(Direction.backward);
            param.setLines(200);
            param.setOffsetBytes(0);
            param.setOffsetStart(OffsetStart.tail);
            param.setSkipLines(0);
            param.setFollow(true);

            t = new Thread(new Reader(session, param));
            t.start();

            // file length
            writer.consume(m -> {
                assertThat(m, hasEntry("signal", "file_length"));
                assertThat(m, hasKey("value"));
                assertThat(m, hasEntry(equalTo("value"), new IntEqualTo(8200)));
                return false;
            });

            // lines
            writer.consume(m -> {
                int line = lineNum.getAndDecrement();
                Line expectedLine = expectLines.get(line);
                assertThat(m, hasEntry(equalTo("pos"), new LongEqualTo(expectedLine.pos)));
                assertThat(m, hasEntry(equalTo("len"), new LongEqualTo(expectedLine.len)));
                assertThat(m, hasEntry(equalTo("str"), equalTo(expectedLine.str)));
                return 100 != line;
            });

            // end of requested signal
            writer.consume(m -> {
                assertThat(m, hasEntry("signal", "eor"));
                return false;
            });

            // Thread was dead
            Thread.sleep(100L);
            assertThat(t.isAlive(), is(equalTo(false)));
        }

        { // Get 100 lines of remains.
            Param param = new Param();
            param.setPath(this.testPath);
            param.setStatus(Status.start);
            param.setDirection(Direction.backward);
            param.setLines(200);
            param.setOffsetBytes(expectLines.get(100).pos);
            param.setOffsetStart(OffsetStart.head);
            param.setSkipLines(0);
            param.setFollow(true);

            t = new Thread(new Reader(session, param));
            t.start();

            // file length
            writer.consume(m -> {
                assertThat(m, hasEntry("signal", "file_length"));
                assertThat(m, hasKey("value"));
                assertThat(m, hasEntry(equalTo("value"), new IntEqualTo(8200)));
                return false;
            });

            // lines
            writer.consume(m -> {
                int line = lineNum.getAndDecrement();
                Line expectedLine = expectLines.get(line);
                assertThat(m, hasEntry(equalTo("pos"), new LongEqualTo(expectedLine.pos)));
                assertThat(m, hasEntry(equalTo("len"), new LongEqualTo(expectedLine.len)));
                assertThat(m, hasEntry(equalTo("str"), equalTo(expectedLine.str)));
                return 0 != line;
            });

            // end of requested signal
            writer.consume(m -> {
                assertThat(m, hasEntry("signal", "eor"));
                return false;
            });

            // begin of file signal
            writer.consume(m -> {
                assertThat(m, hasEntry("signal", "bof"));
                return false;
            });

            // Thread was dead
            Thread.sleep(100L);
            assertThat(t.isAlive(), is(equalTo(false)));
        }
    }

}
