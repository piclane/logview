package com.xxuz.piclane.logview.procedure.text

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path

@RunWith(SpringRunner::class)
@SpringBootTest
class BufferedByteReaderTest {
    /** Test FileSystem  */
    private lateinit var testFs: FileSystem

    /** Test file path  */
    private lateinit var testPath: Path

    /** Test file contents  */
    private lateinit var expectArray: ByteArray

    @BeforeEach
    fun before() {
        val buf = StringBuilder()
        for (i in 0..9) {
            buf.append("0123456789")
        }
        val expectString = buf.toString()
        expectArray = expectString.toByteArray(StandardCharsets.UTF_8)
        testFs = Jimfs.newFileSystem(Configuration.unix())
        testPath = testFs.getPath("/test.txt")
        Files.newBufferedWriter(testPath).use { out -> out.append(expectString) }
    }

    @AfterEach
    fun after() {
        testFs.close()
    }

    @Test
    fun testClose() {
        Files.newByteChannel(testPath).use { channel ->
            val reader = BufferedByteReader(channel)
            reader.close()
            Assert.assertFalse(channel.isOpen)
        }
    }

    @Test
    fun testSize() {
        BufferedByteReader(Files.newByteChannel(testPath), 10).use { reader -> MatcherAssert.assertThat(reader.size(), Matchers.`is`(Matchers.equalTo(100L))) }
    }

    @Test
    fun testRead() {
        BufferedByteReader(Files.newByteChannel(testPath), 10).use { reader ->
            var c: Int
            var index = 0
            while (reader.read().also { c = it } != -1) {
                MatcherAssert.assertThat(
                        "index $index is not equals.",
                        c,
                        Matchers.`is`(Matchers.equalTo(java.lang.Byte.toUnsignedInt(expectArray[index]))))
                index++
            }
        }
    }

    @Test
    fun testRead2() {
        BufferedByteReader(Files.newByteChannel(testPath), 1024).use { reader ->
            var c: Int
            var index = 0
            while (reader.read().also { c = it } != -1) {
                MatcherAssert.assertThat(
                        "index $index is not equals.",
                        c,
                        Matchers.`is`(Matchers.equalTo(java.lang.Byte.toUnsignedInt(expectArray[index]))))
                index++
            }
        }
    }

    @Test
    fun testReadToByteArray_LessThanBuffer() {
        BufferedByteReader(Files.newByteChannel(testPath), 20).use { reader ->
            for (i in 0..4) {
                reader.read()
            }
            val buf = ByteArray(20)
            val read = reader.read(buf, 5, 10)
            MatcherAssert.assertThat(read, Matchers.`is`(Matchers.equalTo(10)))
            val expect = "\u0000\u0000\u0000\u0000\u00005678901234\u0000\u0000\u0000\u0000\u0000".toByteArray(StandardCharsets.UTF_8)
            Assert.assertArrayEquals(expect, buf)
        }
    }

    @Test
    fun testReadToByteArray_MoreThanBuffer() {
        BufferedByteReader(Files.newByteChannel(testPath), 10).use { reader ->
            for (i in 0..4) {
                reader.read()
            }
            val buf = ByteArray(30)
            val read = reader.read(buf, 5, 20)
            MatcherAssert.assertThat(read, Matchers.`is`(Matchers.equalTo(20)))
            val expect = "\u0000\u0000\u0000\u0000\u000056789012345678901234\u0000\u0000\u0000\u0000\u0000".toByteArray(StandardCharsets.UTF_8)
            Assert.assertArrayEquals(expect, buf)
        }
    }

    @Test
    fun testReadToByteArray_MoreThanBuffer2() {
        BufferedByteReader(Files.newByteChannel(testPath), 7).use { reader ->
            for (i in 0..4) {
                reader.read()
            }
            val buf = ByteArray(30)
            val read = reader.read(buf, 5, 20)
            MatcherAssert.assertThat(read, Matchers.`is`(Matchers.equalTo(20)))
            val expect = "\u0000\u0000\u0000\u0000\u000056789012345678901234\u0000\u0000\u0000\u0000\u0000".toByteArray(StandardCharsets.UTF_8)
            Assert.assertArrayEquals(expect, buf)
        }
    }

    @Test
    fun testPositionRead() {
        BufferedByteReader(Files.newByteChannel(testPath), 10).use { reader ->
            for (i in 0..4) {
                reader.read()
            }
            MatcherAssert.assertThat(reader.position(), Matchers.`is`(Matchers.equalTo(5L)))
            val buf = ByteArray(15)
            val read = reader.read(buf)
            MatcherAssert.assertThat(read, Matchers.`is`(Matchers.equalTo(15)))
            MatcherAssert.assertThat(reader.position(), Matchers.`is`(Matchers.equalTo(20L)))
        }
    }

    @Test
    fun testPositionWrite() {
        BufferedByteReader(Files.newByteChannel(testPath), 10).use { reader ->
            reader.position(5L)
            MatcherAssert.assertThat(reader.position(), Matchers.`is`(Matchers.equalTo(5L)))
            MatcherAssert.assertThat(reader.read().toChar(), Matchers.`is`(Matchers.equalTo('5')))
            reader.position(8L)
            MatcherAssert.assertThat(reader.position(), Matchers.`is`(Matchers.equalTo(8L)))
            MatcherAssert.assertThat(reader.read().toChar(), Matchers.`is`(Matchers.equalTo('8')))
            reader.position(30L)
            MatcherAssert.assertThat(reader.position(), Matchers.`is`(Matchers.equalTo(30L)))
            MatcherAssert.assertThat(reader.read().toChar(), Matchers.`is`(Matchers.equalTo('0')))
        }
    }
}
