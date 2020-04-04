package me.piclane.logview.procedure.text;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(Enclosed.class)
public class LineReaderTest {

    private abstract static class TestBase {
        /** Test FileSystem */
        private FileSystem testFs;

        /** Test file path */
        protected Path testPath;

        /** lines in test file */
        protected List<String> expectLines;

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
    }

    /**
     * 順方向テスト
     */
    public static class ForwardTest extends TestBase {
        @Test
        public void test() throws IOException {
            try(BufferedByteReader reader = new BufferedByteReader(testPath)) {
                Offset offset = Offset.of(reader, 0, OffsetStart.head, 0);
                LineReader lineReader = new LineReader(reader, Direction.forward, offset, 100);

                int lineNum = 0;
                for(Iterator<String> i=expectLines.iterator(); i.hasNext(); lineNum++) {
                    String expected = i.next();
                    Line line = lineReader.readLine();
                    assertThat(line, notNullValue());
                    assertThat(line.str, is(equalTo(expected)));
                    assertThat(line.pos, is(equalTo(lineNum * 11L)));
                    assertThat(line.len, is(equalTo(11L)));
                    assertThat(lineReader.hasNextLine(), is(equalTo(i.hasNext())));
                }
            }
        }
    }

    /**
     * 逆方向テスト
     */
    public static class BackwardTest extends TestBase {
        @Before
        public void before() throws IOException {
            super.before();
            Collections.reverse(expectLines);
        }

        @Test
        public void test() throws IOException {
            try(BufferedByteReader reader = new BufferedByteReader(testPath)) {
                Offset offset = Offset.of(reader, 0, OffsetStart.tail, 0);
                LineReader lineReader = new LineReader(reader, Direction.backward, offset, 100);

                int lineNum = 9;
                for(Iterator<String> i=expectLines.iterator(); i.hasNext(); lineNum--) {
                    String expected = i.next();
                    Line line = lineReader.readLine();
                    assertThat(line, notNullValue());
                    assertThat(line.str, is(equalTo(expected)));
                    assertThat(line.pos, is(equalTo(lineNum * 11L)));
                    assertThat(line.len, is(equalTo(11L)));
                    assertThat(lineReader.hasNextLine(), is(equalTo(i.hasNext())));
                }
            }
        }
    }
}
