package com.xxuz.piclane.logview.procedure

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.xxuz.piclane.logview.procedure.text.Direction
import com.xxuz.piclane.logview.procedure.text.OffsetStart
import com.xxuz.piclane.util.IntEqualTo
import com.xxuz.piclane.util.LongEqualTo
import com.xxuz.piclane.util.WebSocketSessionMock
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.streams.asSequence

@ExtendWith(MockitoExtension::class)
@Disabled
class PlainSearchTest {
    /** Test FileSystem  */
    private lateinit var testFs: FileSystem

    /** Test file path  */
    private lateinit var testPath: Path

    /** lines in test file  */
    private lateinit var expectLines: List<Line>
    
    @BeforeEach
    fun before() {
        val buf = StringBuilder()
        for (i in 0..299) {
            for (j in 0..9) {
                buf.append(i)
            }
            buf.append("\n")
        }
        val expectString = buf.toString()
        testFs = Jimfs.newFileSystem(Configuration.unix())
        testPath = testFs.getPath("/test.txt")
        Files.newBufferedWriter(testPath).use { out -> out.append(expectString) }
        updateExpectLines()
    }

    private fun updateExpectLines() {
        val pos = AtomicInteger()
        expectLines = Files.lines(testPath)
                .asSequence()
                .map { str -> Line(pos.getAndAdd(str.length + 1).toLong(), (str.length + 1).toLong(), str) }
                .filter { it.str.contains("1") }
                .toList()
    }

    @AfterEach
    fun after() {
        testFs.close()
    }

    @Test
    @Throws(Exception::class)
    fun testForward() {
        val session = WebSocketSessionMock()
        val lineNum = AtomicInteger()
        var t: Thread
        run {
            // Get the first 200 lines.
            val param = Param()
            param.path = this.testPath
            param.status = Status.start
            param.direction = Direction.forward
            param.lines = 100
            param.offsetBytes = 0
            param.offsetStart = OffsetStart.head
            param.skipLines = 0
            param.isFollow = true
            param.search = arrayOf("1")
            t = Thread(PlainSearch(session, param))
            t.start()

            // file length
            session.consume { m: Map<String, Any?> ->
                MatcherAssert.assertThat(m, Matchers.hasEntry("signal", "file_length"))
                MatcherAssert.assertThat(m, Matchers.hasKey("value"))
                MatcherAssert.assertThat(m, Matchers.hasEntry(Matchers.equalTo("value"), IntEqualTo(8200)))
                false
            }

            // begin of file signal
            session.consume { m: Map<String, Any> ->
                MatcherAssert.assertThat(m, Matchers.hasEntry("signal", "bof"))
                false
            }

            // lines
            session.consume { m: Map<String, Any?> ->
                val line = lineNum.getAndIncrement()
                val expectedLine = expectLines[line]
                MatcherAssert.assertThat(m, Matchers.hasEntry(Matchers.equalTo("pos"), LongEqualTo(expectedLine.pos)))
                MatcherAssert.assertThat(m, Matchers.hasEntry(Matchers.equalTo("len"), LongEqualTo(expectedLine.len)))
                MatcherAssert.assertThat(m, Matchers.hasEntry(Matchers.equalTo("str"), Matchers.equalTo(expectedLine.str)))
                100 != line
            }

            // end of requested signal
            session.consume { m: Map<String, Any> ->
                MatcherAssert.assertThat(m, Matchers.hasEntry("signal", "eor"))
                false
            }

            // Thread was dead
            Thread.sleep(100L)
            MatcherAssert.assertThat(t.isAlive, Matchers.`is`(Matchers.equalTo(false)))
        }
        run {
            // Get 100 lines of remains.
            val param = Param()
            param.path = this.testPath
            param.status = Status.start
            param.direction = Direction.forward
            param.lines = 200
            param.offsetBytes = expectLines[200].pos
            param.offsetStart = OffsetStart.head
            param.skipLines = 0
            param.isFollow = true
            t = Thread(PlainSearch(session, param))
            t.start()

            // file length
            session.consume { m: Map<String, Any?> ->
                MatcherAssert.assertThat(m, Matchers.hasEntry("signal", "file_length"))
                MatcherAssert.assertThat(m, Matchers.hasKey("value"))
                MatcherAssert.assertThat(m, Matchers.hasEntry(Matchers.equalTo("value"), IntEqualTo(8200)))
                false
            }

            // lines
            session.consume { m: Map<String, Any?> ->
                val line = lineNum.getAndIncrement()
                val expectedLine = expectLines[line]
                MatcherAssert.assertThat(m, Matchers.hasEntry(Matchers.equalTo("pos"), LongEqualTo(expectedLine.pos)))
                MatcherAssert.assertThat(m, Matchers.hasEntry(Matchers.equalTo("len"), LongEqualTo(expectedLine.len)))
                MatcherAssert.assertThat(m, Matchers.hasEntry(Matchers.equalTo("str"), Matchers.equalTo(expectedLine.str)))
                299 != line
            }

            // end of requested signal
            session.consume { m: Map<String, Any> ->
                MatcherAssert.assertThat(m, Matchers.hasEntry("signal", "eor"))
                false
            }

            // end of file signal
            session.consume { m: Map<String, Any> ->
                MatcherAssert.assertThat(m, Matchers.hasEntry("signal", "eof"))
                false
            }

            // Thread was not dead
            Thread.sleep(100L)
            MatcherAssert.assertThat(t.isAlive, Matchers.`is`(Matchers.equalTo(true)))
        }
        run {
            // Get the added rows
            // append 2 lines
            Files.write(testPath, Arrays.asList("aaaaaaaaaa", "bbbbbbbbbb"), StandardOpenOption.APPEND)
            Thread.sleep(200L)
            updateExpectLines()

            // test line aaaaaaaaaa
            session.consume { m: Map<String, Any?> ->
                val line = lineNum.getAndIncrement()
                val expectedLine = expectLines[line]
                MatcherAssert.assertThat(m, Matchers.hasEntry(Matchers.equalTo("pos"), LongEqualTo(expectedLine.pos)))
                MatcherAssert.assertThat(m, Matchers.hasEntry(Matchers.equalTo("len"), LongEqualTo(expectedLine.len)))
                MatcherAssert.assertThat(m, Matchers.hasEntry(Matchers.equalTo("str"), Matchers.equalTo(expectedLine.str)))
                301 != line
            }

            // end of file signal
            session.consume { m: Map<String, Any> ->
                MatcherAssert.assertThat(m, Matchers.hasEntry("signal", "eof"))
                false
            }
            t.interrupt()
            t.join()
        }
    }

    @Test
    @Throws(Exception::class)
    fun testBackward() {
        val session = WebSocketSessionMock()
        val lineNum = AtomicInteger(299)
        var t: Thread
        run {
            // Get the last 200 lines.
            val param = Param()
            param.path = this.testPath
            param.status = Status.start
            param.direction = Direction.backward
            param.lines = 200
            param.offsetBytes = 0
            param.offsetStart = OffsetStart.tail
            param.skipLines = 0
            param.isFollow = true
            t = Thread(PlainSearch(session, param))
            t.start()

            // file length
            session.consume { m: Map<String, Any?> ->
                MatcherAssert.assertThat(m, Matchers.hasEntry("signal", "file_length"))
                MatcherAssert.assertThat(m, Matchers.hasKey("value"))
                MatcherAssert.assertThat(m, Matchers.hasEntry(Matchers.equalTo("value"), IntEqualTo(8200)))
                false
            }

            // lines
            session.consume { m: Map<String, Any?> ->
                val line = lineNum.getAndDecrement()
                val expectedLine = expectLines[line]
                MatcherAssert.assertThat(m, Matchers.hasEntry(Matchers.equalTo("pos"), LongEqualTo(expectedLine.pos)))
                MatcherAssert.assertThat(m, Matchers.hasEntry(Matchers.equalTo("len"), LongEqualTo(expectedLine.len)))
                MatcherAssert.assertThat(m, Matchers.hasEntry(Matchers.equalTo("str"), Matchers.equalTo(expectedLine.str)))
                100 != line
            }

            // end of requested signal
            session.consume { m: Map<String, Any> ->
                MatcherAssert.assertThat(m, Matchers.hasEntry("signal", "eor"))
                false
            }

            // Thread was dead
            Thread.sleep(100L)
            MatcherAssert.assertThat(t.isAlive, Matchers.`is`(Matchers.equalTo(false)))
        }
        run {
            // Get 100 lines of remains.
            val param = Param()
            param.path = this.testPath
            param.status = Status.start
            param.direction = Direction.backward
            param.lines = 200
            param.offsetBytes = expectLines[100].pos
            param.offsetStart = OffsetStart.head
            param.skipLines = 0
            param.isFollow = true
            t = Thread(PlainSearch(session, param))
            t.start()

            // file length
            session.consume { m: Map<String, Any?> ->
                MatcherAssert.assertThat(m, Matchers.hasEntry("signal", "file_length"))
                MatcherAssert.assertThat(m, Matchers.hasKey("value"))
                MatcherAssert.assertThat(m, Matchers.hasEntry(Matchers.equalTo("value"), IntEqualTo(8200)))
                false
            }

            // lines
            session.consume { m: Map<String, Any?> ->
                val line = lineNum.getAndDecrement()
                val expectedLine = expectLines[line]
                MatcherAssert.assertThat(m, Matchers.hasEntry(Matchers.equalTo("pos"), LongEqualTo(expectedLine.pos)))
                MatcherAssert.assertThat(m, Matchers.hasEntry(Matchers.equalTo("len"), LongEqualTo(expectedLine.len)))
                MatcherAssert.assertThat(m, Matchers.hasEntry(Matchers.equalTo("str"), Matchers.equalTo(expectedLine.str)))
                0 != line
            }

            // end of requested signal
            session.consume { m: Map<String, Any> ->
                MatcherAssert.assertThat(m, Matchers.hasEntry("signal", "eor"))
                false
            }

            // begin of file signal
            session.consume { m: Map<String, Any> ->
                MatcherAssert.assertThat(m, Matchers.hasEntry("signal", "bof"))
                false
            }

            // Thread was dead
            Thread.sleep(100L)
            MatcherAssert.assertThat(t.isAlive, Matchers.`is`(Matchers.equalTo(false)))
        }
    }
}
