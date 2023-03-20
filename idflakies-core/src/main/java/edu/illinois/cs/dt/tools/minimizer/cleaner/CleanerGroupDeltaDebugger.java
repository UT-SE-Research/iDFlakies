package edu.illinois.cs.dt.tools.minimizer.cleaner;

import edu.illinois.cs.dt.tools.utility.deltadebug.DeltaDebugger;
import edu.illinois.cs.testrunner.data.results.Result;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import edu.illinois.cs.testrunner.runner.SmartRunner;

import java.util.ArrayList;
import java.util.List;
import scala.util.Try;

public class CleanerGroupDeltaDebugger extends DeltaDebugger<String> {

    private final SmartRunner runner;
    private final String dependentTest;
    private final List<String> deps;
    private final Result isolationResult;

    public CleanerGroupDeltaDebugger(SmartRunner runner, String dependentTest, List<String> deps, Result isolationResult) {
        this.runner = runner;
        this.dependentTest = dependentTest;
        this.deps = deps;
        this.isolationResult = isolationResult;
    }

    @Override
    public boolean checkValid(List<String> cleanerCandidate) {
        final List<String> tests = new ArrayList<>(this.deps);
        tests.addAll(cleanerCandidate);
        tests.add(this.dependentTest);

        final Try<TestRunResult> testRunResultTry = this.runner.runList(tests);

        return testRunResultTry.isSuccess() &&
               testRunResultTry.get().results().get(this.dependentTest).result().equals(this.isolationResult);
    }
}
