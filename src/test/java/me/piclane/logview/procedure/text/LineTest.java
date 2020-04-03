package me.piclane.logview.procedure.text;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class LineTest {
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
    public void testReadLine() throws IOException {
        try(BufferedByteReader reader = new BufferedByteReader(testPath)) {
            List<String> actualLines = new ArrayList<>();
            AtomicInteger lineNum = new AtomicInteger();
            Line.readLine(reader, StandardCharsets.UTF_8, line -> {
                actualLines.add(line.toString());
                assertThat(line.pos, is(equalTo(lineNum.getAndIncrement() * 11L)));
                assertThat(line.len, is(equalTo(11L)));
                return true;
            });
            assertThat(actualLines, is(contains(expectLines.toArray())));
        }
    }

    @Test
    public void testSkipLine() throws IOException {
        try(BufferedByteReader reader = new BufferedByteReader(testPath)) {
            for(int lineNum = 1; lineNum<=10; lineNum++) {
                long pos = Line.skipLine(reader);
                assertThat(pos, is(equalTo(lineNum * 11L)));
            }
        }
    }
}
