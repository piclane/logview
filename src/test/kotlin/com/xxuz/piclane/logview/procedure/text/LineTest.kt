package com.xxuz.piclane.logview.procedure.text

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class LineTest {
    /** Test FileSystem  */
    private lateinit var testFs: FileSystem

    /** Test file path  */
    private lateinit var testPath: Path

    /** lines in test file  */
    private lateinit var expectLines: List<String>
    
    @BeforeEach
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
    fun testReadLine() {
        BufferedByteReader(testPath).use { reader ->
            val actualLines: MutableList<String> = ArrayList()
            val lineNum = AtomicInteger()
            Line.readLine(reader, StandardCharsets.UTF_8) { line ->
                actualLines.add(line.toString())
                MatcherAssert.assertThat(line.pos, Matchers.`is`(Matchers.equalTo(lineNum.getAndIncrement() * 11L)))
                MatcherAssert.assertThat(line.len, Matchers.`is`(Matchers.equalTo(11L)))
                true
            }
            MatcherAssert.assertThat<List<String>>(actualLines, Matchers.`is`(Matchers.contains<Any>(*expectLines.toTypedArray())))
        }
    }

    @Test
    fun testSkipLine() {
        BufferedByteReader(testPath).use { reader ->
            for (lineNum in 1..10) {
                val pos = Line.skipLine(reader)
                MatcherAssert.assertThat(pos, Matchers.`is`(Matchers.equalTo(lineNum * 11L)))
            }
        }
    }
}
