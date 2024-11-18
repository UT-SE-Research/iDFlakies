package edu.illinois.cs.testrunner.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Path;

public class StreamGobbler extends Thread {
    private InputStream is;
    private PrintStream ps;

    public StreamGobbler(final InputStream is, final PrintStream ps) {
        this.is = is;
        this.ps = ps;
    }

    @Override
    public void run() {
        try {
            final BufferedReader br = new BufferedReader(new InputStreamReader(is));

            while (true) {
                final String line = br.readLine();
                if (line == null) {
                    break;
                } else {
                    ps.println(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
