package edu.illinois.cs.dt.tools.runner.data;

import com.google.gson.Gson;
import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.runner.Runner;

import java.nio.file.Path;

public class DependentTest {
    private static final boolean VERIFY_DTS = Configuration.config().getProperty("dt.verify", true);

    private final String name;

    private final TestRun intended;
    private final TestRun revealed;

    public DependentTest(final String name, final TestRun intended, final TestRun revealed) {
        this.name = name;
        this.intended = intended;
        this.revealed = revealed;
    }

    public String name() {
        return name;
    }

    public TestRun intended() {
        return intended;
    }

    public TestRun revealed() {
        return revealed;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public boolean verify(final Runner runner, final Path path) {
        return intended.verify(name, runner, path) && revealed.verify(name, runner, path);
    }
}
