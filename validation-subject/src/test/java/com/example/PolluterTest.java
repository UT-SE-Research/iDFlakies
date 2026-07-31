package com.example;

import org.junit.Test;

public class PolluterTest {
    @Test
    public void pollute() {
        SharedState.polluted = true;
    }
}
