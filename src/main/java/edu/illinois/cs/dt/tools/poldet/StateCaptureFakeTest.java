package edu.illinois.cs.dt.tools.poldet;

import edu.illinois.cs.dt.tools.poldet.instrumentation.MainAgent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StateCaptureFakeTest {

    @Test
    public void outputLoadedClasses() {
        if (MainAgent.getInstrumentation() == null) {
            System.out.println("NO INSTRUMENTATION");
            return;
        }

        // Use instrumentation to get what are all the loaded classes and report them
        List<String> classes = new ArrayList<>();
        Class[] loadedClasses = MainAgent.getInstrumentation().getAllLoadedClasses();
        for (Class clz : loadedClasses) {
            // Skip if one of our classes or top-level stuff
            String className = clz.getName();
            if (StateCapture.ignoreClass(className)) {
                continue;
            }
            classes.add(className);
        }

        // Write it out
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(".dtfixingtools/loadedclasses"))) {
            for (String clz : classes) {
                writer.write(clz);
                writer.write("\n");
            }
        } catch (IOException ex) {
        }
    }

    @Test
    public void initializeLoadedClasses() {
        // Read in all the loaded classes from prior run
        List<String> classes = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(".dtfixingtools/loadedclasses"))) {
            String line = reader.readLine();
            while (line != null) {
                classes.add(line);
                line = reader.readLine();
            }
        } catch (IOException ex) {
        }

        // Try to load all the classes
        for (String clz : classes) {
            try {
                Class.forName(clz);
            } catch (Exception ex) {
                // If cannot load, just move on
            } catch (NoClassDefFoundError err) {
                // If cannot load, just move on
            }
        }
    }

    @Test
    public void before() {
        StateCapture.captureBefore("candidate");
    }

    @Test
    public void after() {
        StateCapture.captureAfter("candidate");
    }

    @Test
    public void checkHasPolluters() {
        assertEquals(0, StateCapture.getPolluters().size());
    }

    @Test
    public void outputResults() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(".dtfixingtools/poldet-results"));
            for (String polluter : StateCapture.getPolluters()) {
                writer.write(polluter);
                writer.write("\n");
            }
            writer.close();
        } catch (IOException ex) {
        }
        assertEquals(0, StateCapture.getPolluters().size());
    }

    @Test
    public void outputDifferingRoots() {
        for (String root : StateCapture.getRoots()) {
            System.out.println("ROOT: " + root);
        }
    }
}
