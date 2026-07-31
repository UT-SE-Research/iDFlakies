package com.example;

import org.junit.Test;
import static org.junit.Assert.assertFalse;

public class VictimTest {
    @Test
    public void victim() {
        assertFalse("SharedState was polluted by another test", SharedState.polluted);
    }
}
