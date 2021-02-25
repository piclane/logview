package com.xxuz.piclane.logview.procedure.text

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import java.io.IOException
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path

@RunWith(Enclosed::class)
class LineReaderTest {
    abstract class TestBase {
        /** Test FileSystem  */
        private lateinit var testFs: FileSystem

        /** Test file path  */
        protected lateinit var testPath: Path

        /** lines in test file  */
        protected lateinit var expectLines: List<String>

        @BeforeEach
        open fun before() {
            val buf = StringBuilder()
            for (i in 0..9) {
                for (j in 0..9) {
                    buf.append(i)
                }
                buf.append("\n")
            }
            val expectString = buf.toString()
            testFs = Jimfs.newFileSystem(Configuration.unix())
            testPath = testFs.getPath("/test.txt")
            Files.newBufferedWriter(testPath).use { out -> out.append(expectString) }
            expectLines = Files.readAllLines(testPath)
        }

        @AfterEach
        fun after() {
            testFs.close()
        }
    }

    /**
     * 順方向テスト
     */
    class ForwardTest : TestBase() {
        @Test
        fun test() {
            BufferedByteReader(testPath).use { reader ->
                val offset = Offset.of(reader, 0, OffsetStart.head, 0)
                val lineReader = LineReader(reader, Direction.forward, offset, 100)
                var lineNum = 0
                val i = expectLines.iterator()
                while (i.hasNext()) {
                    val expected = i.next()
                    val line = lineReader.readLine()
                    MatcherAssert.assertThat(line, Matchers.notNullValue())
                    MatcherAssert.assertThat(line?.str, Matchers.`is`(Matchers.equalTo(expected)))
                    MatcherAssert.assertThat(line?.pos, Matchers.`is`(Matchers.equalTo(lineNum * 11L)))
                    MatcherAssert.assertThat(line?.len, Matchers.`is`(Matchers.equalTo(11L)))
                    MatcherAssert.assertThat(lineReader.hasNextLine(), Matchers.`is`(Matchers.equalTo(i.hasNext())))
                    lineNum++
                }
            }
        }
    }

    /**
     * 逆方向テスト
     */
    class BackwardTest : TestBase() {
        @BeforeEach
        override fun before() {
            super.before()
            expectLines = expectLines.asReversed()
        }

        @Test
        fun test() {
            BufferedByteReader(testPath).use { reader ->
                val offset = Offset.of(reader, 0, OffsetStart.tail, 0)
                val lineReader = LineReader(reader, Direction.backward, offset, 100)
                var lineNum = 9
                val i = expectLines.iterator()
                while (i.hasNext()) {
                    val expected = i.next()
                    val line = lineReader.readLine()
                    MatcherAssert.assertThat(line, Matchers.notNullValue())
                    MatcherAssert.assertThat(line?.str, Matchers.`is`(Matchers.equalTo(expected)))
                    MatcherAssert.assertThat(line?.pos, Matchers.`is`(Matchers.equalTo(lineNum * 11L)))
                    MatcherAssert.assertThat(line?.len, Matchers.`is`(Matchers.equalTo(11L)))
                    MatcherAssert.assertThat(lineReader.hasNextLine(), Matchers.`is`(Matchers.equalTo(i.hasNext())))
                    lineNum--
                }
            }
        }
    }
}
