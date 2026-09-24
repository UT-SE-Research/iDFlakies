package com.example;

import org.junit.Test;

public class CleanerTest {
    @Test
    public void clean() {
        SharedState.polluted = false;
    }
}
