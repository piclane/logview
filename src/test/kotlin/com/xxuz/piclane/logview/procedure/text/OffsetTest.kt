package com.xxuz.piclane.logview.procedure.text

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class OffsetTest {
    /** Test FileSystem  */
    private lateinit var testFs: FileSystem

    /** Test file path  */
    private lateinit var testPath: Path

    /** lines in test file  */
    private lateinit var expectLines: List<String>
    
    @BeforeEach
    @Throws(IOException::class)
    fun before() {
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

    @Test
    fun test_of_OffsetStart_Head() {
        var offset: Offset
        BufferedByteReader(testPath).use { reader ->
            offset = Offset.of(reader, 0, OffsetStart.head, 0)
            MatcherAssert.assertThat(offset.length, Matchers.`is`(Matchers.equalTo(110L)))
            MatcherAssert.assertThat(offset.position, Matchers.`is`(Matchers.equalTo(0L)))
            offset = Offset.of(reader, 200, OffsetStart.head, 0)
            MatcherAssert.assertThat(offset.length, Matchers.`is`(Matchers.equalTo(110L)))
            MatcherAssert.assertThat(offset.position, Matchers.`is`(Matchers.equalTo(110L)))
        }
    }

    @Test
    fun test_of_OffsetStart_Tail() {
        var offset: Offset
        BufferedByteReader(testPath).use { reader ->
            offset = Offset.of(reader, 0, OffsetStart.tail, 0)
            MatcherAssert.assertThat(offset.length, Matchers.`is`(Matchers.equalTo(110L)))
            MatcherAssert.assertThat(offset.position, Matchers.`is`(Matchers.equalTo(110L)))
            offset = Offset.of(reader, 10, OffsetStart.tail, 0)
            MatcherAssert.assertThat(offset.length, Matchers.`is`(Matchers.equalTo(110L)))
            MatcherAssert.assertThat(offset.position, Matchers.`is`(Matchers.equalTo(100L)))
        }
    }

    @Test
    fun test_of_SkipLines_Positive() {
        var offset: Offset
        BufferedByteReader(testPath).use { reader ->
            offset = Offset.of(reader, 0, OffsetStart.head, 3)
            MatcherAssert.assertThat(offset.length, Matchers.`is`(Matchers.equalTo(110L)))
            MatcherAssert.assertThat(offset.position, Matchers.`is`(Matchers.equalTo(33L)))
            offset = Offset.of(reader, 0, OffsetStart.tail, 3)
            MatcherAssert.assertThat(offset.length, Matchers.`is`(Matchers.equalTo(110L)))
            MatcherAssert.assertThat(offset.position, Matchers.`is`(Matchers.equalTo(110L)))
        }
    }

    @Test
    fun test_of_SkipLines_Negative() {
        var offset: Offset
        BufferedByteReader(testPath).use { reader ->
            offset = Offset.of(reader, 0, OffsetStart.tail, -3)
            MatcherAssert.assertThat(offset.length, Matchers.`is`(Matchers.equalTo(110L)))
            MatcherAssert.assertThat(offset.position, Matchers.`is`(Matchers.equalTo(77L)))
            offset = Offset.of(reader, 0, OffsetStart.head, -3)
            MatcherAssert.assertThat(offset.length, Matchers.`is`(Matchers.equalTo(110L)))
            MatcherAssert.assertThat(offset.position, Matchers.`is`(Matchers.equalTo(0L)))
        }
    }

    @Test
    fun test_withPosition() {
        var offset: Offset
        BufferedByteReader(testPath).use { reader ->
            offset = Offset.of(reader, 0, OffsetStart.head, 0)
            MatcherAssert.assertThat(offset.length, Matchers.`is`(Matchers.equalTo(110L)))
            MatcherAssert.assertThat(offset.position, Matchers.`is`(Matchers.equalTo(0L)))
            offset = offset.withPosition(60L)
            MatcherAssert.assertThat(offset.length, Matchers.`is`(Matchers.equalTo(110L)))
            MatcherAssert.assertThat(offset.position, Matchers.`is`(Matchers.equalTo(60L)))
            offset = offset.withPosition(-10L)
            MatcherAssert.assertThat(offset.length, Matchers.`is`(Matchers.equalTo(110L)))
            MatcherAssert.assertThat(offset.position, Matchers.`is`(Matchers.equalTo(0L)))
            offset = offset.withPosition(10000L)
            MatcherAssert.assertThat(offset.length, Matchers.`is`(Matchers.equalTo(110L)))
            MatcherAssert.assertThat(offset.position, Matchers.`is`(Matchers.equalTo(110L)))
        }
    }

    @Test
    fun test_withLength() {
        var offset: Offset
        BufferedByteReader(testPath).use { reader ->
            offset = Offset.of(reader, 0, OffsetStart.head, 0)
            MatcherAssert.assertThat(offset.length, Matchers.`is`(Matchers.equalTo(110L)))
            MatcherAssert.assertThat(offset.position, Matchers.`is`(Matchers.equalTo(0L)))
            Files.newBufferedWriter(testPath, StandardOpenOption.APPEND).use { out -> out.append("aaaaaaaaaa\n") }
            offset = offset.withLength(reader)
            MatcherAssert.assertThat(offset.length, Matchers.`is`(Matchers.equalTo(121L)))
            MatcherAssert.assertThat(offset.position, Matchers.`is`(Matchers.equalTo(0L)))
        }
    }

    @Test
    fun test_isEof() {
        var offset: Offset
        BufferedByteReader(testPath).use { reader ->
            offset = Offset.of(reader, 0, OffsetStart.head, 0)
            MatcherAssert.assertThat(offset.length, Matchers.`is`(Matchers.equalTo(110L)))
            MatcherAssert.assertThat(offset.position, Matchers.`is`(Matchers.equalTo(0L)))
            MatcherAssert.assertThat(offset.isEof, Matchers.`is`(false))
            offset = Offset.of(reader, 0, OffsetStart.tail, 0)
            MatcherAssert.assertThat(offset.length, Matchers.`is`(Matchers.equalTo(110L)))
            MatcherAssert.assertThat(offset.position, Matchers.`is`(Matchers.equalTo(110L)))
            MatcherAssert.assertThat(offset.isEof, Matchers.`is`(true))
        }
    }

    @Test
    fun test_isBof() {
        var offset: Offset
        BufferedByteReader(testPath).use { reader ->
            offset = Offset.of(reader, 0, OffsetStart.head, 0)
            MatcherAssert.assertThat(offset.length, Matchers.`is`(Matchers.equalTo(110L)))
            MatcherAssert.assertThat(offset.position, Matchers.`is`(Matchers.equalTo(0L)))
            MatcherAssert.assertThat(offset.isBof, Matchers.`is`(true))
            offset = Offset.of(reader, 0, OffsetStart.tail, 0)
            MatcherAssert.assertThat(offset.length, Matchers.`is`(Matchers.equalTo(110L)))
            MatcherAssert.assertThat(offset.position, Matchers.`is`(Matchers.equalTo(110L)))
            MatcherAssert.assertThat(offset.isBof, Matchers.`is`(false))
        }
    }
}
