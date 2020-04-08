package me.piclane.logview.procedure;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import me.piclane.logview.procedure.text.Direction;
import me.piclane.logview.procedure.text.OffsetStart;
import me.piclane.util.IntEqualTo;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
    private List<String> expectLines;

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
        expectLines = Files.readAllLines(testPath);
    }

    @After
    public void after() throws IOException {
        testFs.close();
    }

    @Test
    public void test() throws Exception {
        MessageConsumableWriter writer = new MessageConsumableWriter();

        RemoteEndpoint.Basic endpoint = mock(RemoteEndpoint.Basic.class);
        doReturn(writer).when(endpoint).getSendWriter();

        Session session = mock(Session.class);
        doReturn(true).when(session).isOpen();
        doReturn(endpoint).when(session).getBasicRemote();

        Param param = new Param();
        param.setPath(this.testPath);
        param.setStatus(Status.start);
        param.setDirection(Direction.forward);
        param.setLines(200);
        param.setOffsetBytes(0);
        param.setOffsetStart(OffsetStart.head);
        param.setSkipLines(0);
        param.setFollow(true);

        Thread t = new Thread(new Reader(session, param));
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
        AtomicInteger lineNum = new AtomicInteger();
        int[] pos = new int[] { 0 };
        writer.consume(m -> {
            int line = lineNum.getAndIncrement();
            String expectedStr = expectLines.get(line);
            int expectedLen = expectedStr.length() + 1;

            assertThat(m, hasEntry(equalTo("pos"), new IntEqualTo(pos[0])));
            assertThat(m, hasEntry(equalTo("len"), new IntEqualTo(expectedLen)));
            assertThat(m, hasEntry(equalTo("str"), equalTo(expectedStr)));
            pos[0] += expectedLen;
            return param.getLines() - 1 != line;
        });

        // end of requested signal
        writer.consume(m -> {
            assertThat(m, hasEntry("signal", "eor"));
            return false;
        });

        t.interrupt();
        t.join();
    }

}
