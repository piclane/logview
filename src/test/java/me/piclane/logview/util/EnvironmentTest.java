package me.piclane.logview.util;

import org.junit.Test;

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
}
