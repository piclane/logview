package com.xxuz.piclane.logview.util

import com.xxuz.piclane.logview.util.Environment.expand
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Assume
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.nio.file.Files

class EnvironmentTest {
    @Test
    fun expandTest1() {
        val home = System.getenv("HOME")
        Assume.assumeThat(home, Matchers.not(Matchers.isEmptyOrNullString()))
        MatcherAssert.assertThat(expand("---\$HOME+++"), Matchers.`is`(Matchers.equalTo("---$home+++")))
        MatcherAssert.assertThat(expand("---\$HOME+++\$HOME***"), Matchers.`is`(Matchers.equalTo("---$home+++$home***")))
    }

    @Test
    fun expandTest2() {
        val home = System.getenv("HOME")
        Assume.assumeThat(home, Matchers.not(Matchers.isEmptyOrNullString()))
        MatcherAssert.assertThat(expand("---\${HOME}+++"), Matchers.`is`(Matchers.equalTo("---$home+++")))
        MatcherAssert.assertThat(expand("---\${HOME}+++\${HOME}***"), Matchers.`is`(Matchers.equalTo("---$home+++$home***")))
    }

    @Test
    fun expandTest3() {
        val home = System.getenv("HOME")
        Assume.assumeThat(home, Matchers.not(Matchers.isEmptyOrNullString()))
        val xyz = System.getenv("XXXYYYZZZ")
        Assume.assumeThat(xyz, Matchers.isEmptyOrNullString())
        MatcherAssert.assertThat(expand("---\${XXXYYYZZZ:-abc}+++"), Matchers.`is`(Matchers.equalTo("---abc+++")))
        MatcherAssert.assertThat(expand("---\${HOME:-abc}+++"), Matchers.`is`(Matchers.equalTo("---$home+++")))
    }

    @Test
    fun expandTest4() {
        val home = System.getenv("HOME")
        Assume.assumeThat(home, Matchers.not(Matchers.isEmptyOrNullString()))
        val xyz = System.getenv("XXXYYYZZZ")
        Assume.assumeThat(xyz, Matchers.isEmptyOrNullString())
        MatcherAssert.assertThat(expand("---\${XXXYYYZZZ:+abc}+++"), Matchers.`is`(Matchers.equalTo("---+++")))
        MatcherAssert.assertThat(expand("---\${HOME:+abc}+++"), Matchers.`is`(Matchers.equalTo("---abc+++")))
    }

    @Test
    @Throws(Exception::class)
    fun expandTest_File() {
        val temp = Files.createTempFile("env", ".txt")
        try {
            PrintWriter(Files.newBufferedWriter(temp)).use { pw -> pw.println("123456") }
            MatcherAssert.assertThat(expand("---\${@" + temp.toAbsolutePath() + "}+++"), Matchers.`is`(Matchers.equalTo("---123456+++")))
            MatcherAssert.assertThat(expand("@" + temp.toAbsolutePath()), Matchers.`is`(Matchers.equalTo("123456")))
        } finally {
            Files.delete(temp)
        }
    }

    @Test
    @Throws(Exception::class)
    fun expandTest_ExpandInFile() {
        val home = System.getenv("HOME")
        Assume.assumeThat(home, Matchers.not(Matchers.isEmptyOrNullString()))
        val temp = Files.createTempFile("env", ".txt")
        try {
            PrintWriter(Files.newBufferedWriter(temp)).use { pw -> pw.println("123\${HOME}456") }
            MatcherAssert.assertThat(expand("---\${@" + temp.toAbsolutePath() + "}+++"), Matchers.`is`(Matchers.equalTo("---123" + home + "456+++")))
        } finally {
            Files.delete(temp)
        }
    }
}
