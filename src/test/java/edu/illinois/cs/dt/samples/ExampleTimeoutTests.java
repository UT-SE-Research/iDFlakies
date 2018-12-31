package edu.illinois.cs.dt.samples;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExampleTimeoutTests {
    public static int x = 0;

    @BeforeClass
    public static void beforeClass() {
        x = 0;
    }

    @AfterClass
    public static void afterClass() {
        x = 0;
    }

    @Test
    public void test1() {
        x = 5;
        assertEquals(5, x);
    }

    @Test
    public void test2() throws Exception {
        if (x == 5) {
            assertEquals(5, x);
        } else {
            Thread.sleep(1000000);
        }
    }
}
