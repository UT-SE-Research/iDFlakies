package edu.illinois.cs.dt.tools.runner;

import edu.illinois.cs.testrunner.data.framework.TestFramework;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import edu.illinois.cs.testrunner.runner.Runner;
import edu.illinois.cs.testrunner.runner.SmartRunner;
import edu.illinois.cs.testrunner.runner.TestInfoStore;
import edu.illinois.cs.testrunner.util.ExecutionInfo;
import edu.illinois.cs.testrunner.util.ExecutionInfoBuilder;
import edu.illinois.cs.testrunner.util.TempFiles;
import scala.collection.immutable.Stream;
import scala.util.Failure;
import scala.util.Try;

import java.nio.file.Path;
import java.util.Map;

public class InstrumentingSmartRunner extends SmartRunner {
    private Path outputPath;

    public static InstrumentingSmartRunner fromRunner(final Runner runner) {
        if (runner instanceof SmartRunner) {
            return new InstrumentingSmartRunner(runner.framework(), ((SmartRunner) runner).info(),
                                                runner.classpath(), runner.environment(), runner.outputPath());
        } else {
            return new InstrumentingSmartRunner(runner.framework(), new TestInfoStore(),
                                                runner.classpath(), runner.environment(), runner.outputPath());
        }
    }

    private InstrumentingSmartRunner(final TestFramework testFramework, final TestInfoStore infoStore,
                                     final String cp, final Map<String, String> env, final Path outputPath) {
        super(testFramework, infoStore, cp, env, outputPath);

    }

    @Override
    public ExecutionInfo execution(final Stream<String> testOrder, final ExecutionInfoBuilder executionInfoBuilder) {
        final ExecutionInfoBuilder builder;
        if (outputPath != null) {
            builder = executionInfoBuilder.outputPath(outputPath);
        } else {
            builder = executionInfoBuilder;
        }

        return super.execution(testOrder, builder);
    }

    @Override
    public Try<TestRunResult> runWithCp(final String cp, final Stream<String> testOrder) {
        // Save stdout,stderr, and run result to a file
        final Try<Try<TestRunResult>> result = TempFiles.withTempFile(outputPath -> {
            try {
                writeTo(outputPath);

                final Try<TestRunResult> testRunResultTry = super.runWithCp(cp, testOrder);

                if (testRunResultTry.isSuccess()) {
                    RunnerPathManager.outputResult(outputPath, testRunResultTry.get());
                }

                return testRunResultTry;
            } catch (Exception e){
                return new Failure<>(e);
            }
        });

        return result.get();
    }

    private void writeTo(final Path outputPath) {
        this.outputPath = outputPath;
    }
}
