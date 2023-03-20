package edu.illinois.cs.dt.tools.minimizer;

import edu.illinois.cs.dt.tools.utility.deltadebug.DeltaDebugger;
import edu.illinois.cs.testrunner.data.results.Result;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import edu.illinois.cs.testrunner.runner.SmartRunner;

import java.util.ArrayList;
import java.util.List;

public class TestMinimizerDeltaDebugger extends DeltaDebugger<String> {

    private final SmartRunner runner;
    private final String dependentTest;
    private final Result expected;

    public TestMinimizerDeltaDebugger(SmartRunner runner, String dependentTest, Result expected) {
        this.runner = runner;
        this.dependentTest = dependentTest;
        this.expected = expected;
    }

    @Override
    public boolean checkValid(List<String> tests) {
        return this.expected == result(tests);
    }

    private Result result(final List<String> tests) {
        try {
            return runResult(tests).results().get(this.dependentTest).result();
        } catch (java.lang.IllegalThreadStateException e) {
             // indicates timeout
            return Result.SKIPPED;
        }
    }

    private TestRunResult runResult(final List<String> tests) {
        final List<String> actualOrder = new ArrayList<>(tests);

        if (!actualOrder.contains(this.dependentTest)) {
            actualOrder.add(this.dependentTest);
        }

        return this.runner.runList(actualOrder).get();
    }

}
