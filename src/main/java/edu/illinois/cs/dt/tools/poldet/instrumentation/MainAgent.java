package edu.illinois.cs.dt.tools.poldet.instrumentation;

import java.lang.instrument.Instrumentation;

public class MainAgent {

    private static Instrumentation inst;

    public static void premain(String args, Instrumentation inst) throws Exception {
        MainAgent.inst = inst;
    }

    public static Instrumentation getInstrumentation() {
        return inst;
    }
}
