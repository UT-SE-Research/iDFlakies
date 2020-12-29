package com.fake.example.test;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

// NOTE: These dts are only to be used for testing by other classes, and should not be run directly.
// NOTE: It is very important that this class *NOT* be in a package containing "edu.illinois".
//       If it is, then it will be ignored by poldet.
@Ignore
public class TestPollutionDetection {
    private static int x = 0;

    @Test
    public void polluted() {
        assertEquals(0, x);
    }

    @Test
    public void polluter() {
        x = 4;
        assertEquals(4, x);
    }
}
