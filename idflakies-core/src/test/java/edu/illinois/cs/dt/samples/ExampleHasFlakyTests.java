package edu.illinois.cs.dt.samples;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.fail;

// Do not run. Has intentionally flaky dts.
@Ignore
public class ExampleHasFlakyTests {
    private static int x = 0;

    @Test
    public void testFlakyFileDependent() throws Exception {
        final File testFile = new File("random-temp-file.txt");

        if (!testFile.exists()) {
            testFile.createNewFile();
            fail("File does not exist!");
        } else {
            testFile.delete();
        }
    }
}
