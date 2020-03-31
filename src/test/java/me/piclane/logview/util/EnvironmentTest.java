package me.piclane.logview.util;

import org.junit.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.junit.Assume.*;

public class EnvironmentTest {
    @Test
    public void expandTest1() {
        String home = System.getenv("HOME");
        assumeThat(home, not(isEmptyOrNullString()));

        assertThat(Environment.expand("---$HOME+++"), is(equalTo("---" + home + "+++")));
        assertThat(Environment.expand("---$HOME+++$HOME***"), is(equalTo("---" + home + "+++" + home + "***")));
    }

    @Test
    public void expandTest2() {
        String home = System.getenv("HOME");
        assumeThat(home, not(isEmptyOrNullString()));

        assertThat(Environment.expand("---${HOME}+++"), is(equalTo("---" + home + "+++")));
        assertThat(Environment.expand("---${HOME}+++${HOME}***"), is(equalTo("---" + home + "+++" + home + "***")));
    }

    @Test
    public void expandTest3() {
        String home = System.getenv("HOME");
        assumeThat(home, not(isEmptyOrNullString()));
        String xyz = System.getenv("XXXYYYZZZ");
        assumeThat(xyz, isEmptyOrNullString());

        assertThat(Environment.expand("---${XXXYYYZZZ:-abc}+++"), is(equalTo("---abc+++")));
        assertThat(Environment.expand("---${HOME:-abc}+++"), is(equalTo("---" + home + "+++")));
    }

    @Test
    public void expandTest4() {
        String home = System.getenv("HOME");
        assumeThat(home, not(isEmptyOrNullString()));
        String xyz = System.getenv("XXXYYYZZZ");
        assumeThat(xyz, isEmptyOrNullString());

        assertThat(Environment.expand("---${XXXYYYZZZ:+abc}+++"), is(equalTo("---+++")));
        assertThat(Environment.expand("---${HOME:+abc}+++"), is(equalTo("---abc+++")));
    }

    @Test
    public void expandTest_File() throws Exception {
        Path temp = Files.createTempFile("env", ".txt");
        try {
            try(PrintWriter pw = new PrintWriter(Files.newBufferedWriter(temp))) {
                pw.println("123456");
            }

            assertThat(Environment.expand("---${@" + temp.toAbsolutePath() + "}+++"), is(equalTo("---123456+++")));
            assertThat(Environment.expand("@" + temp.toAbsolutePath()), is(equalTo("123456")));
        } finally {
            Files.delete(temp);
        }
    }

    @Test
    public void expandTest_ExpandInFile() throws Exception {
        String home = System.getenv("HOME");
        assumeThat(home, not(isEmptyOrNullString()));

        Path temp = Files.createTempFile("env", ".txt");
        try {
            try(PrintWriter pw = new PrintWriter(Files.newBufferedWriter(temp))) {
                pw.println("123${HOME}456");
            }

            assertThat(Environment.expand("---${@" + temp.toAbsolutePath() + "}+++"), is(equalTo("---123" + home + "456+++")));
        } finally {
            Files.delete(temp);
        }
    }
}
