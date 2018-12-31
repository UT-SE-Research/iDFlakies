package edu.illinois.cs.dt.tools.runner;

import com.reedoei.testrunner.data.framework.TestFramework;
import com.reedoei.testrunner.data.results.TestRunResult;
import com.reedoei.testrunner.runner.Runner;
import com.reedoei.testrunner.runner.SmartRunner;
import com.reedoei.testrunner.runner.TestInfoStore;
import com.reedoei.testrunner.util.ExecutionInfo;
import com.reedoei.testrunner.util.ExecutionInfoBuilder;
import com.reedoei.testrunner.util.TempFiles;
import edu.illinois.cs.dt.tools.diagnosis.instrumentation.JavaAgent;
import edu.illinois.cs.dt.tools.diagnosis.instrumentation.StaticFieldPathManager;
import edu.illinois.cs.dt.tools.diagnosis.instrumentation.StaticTracer;
import edu.illinois.cs.dt.tools.diagnosis.instrumentation.TracerMode;
import org.apache.maven.project.MavenProject;
import scala.collection.immutable.Stream;
import scala.util.Failure;
import scala.util.Try;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class InstrumentingSmartRunner extends SmartRunner {
    private final String javaAgent;
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

        final URL url = JavaAgent.class.getProtectionDomain().getCodeSource().getLocation();
        this.javaAgent = url.getFile();

    }

    @Override
    public ExecutionInfo execution(final Stream<String> testOrder, final ExecutionInfoBuilder executionInfoBuilder) {
        final ExecutionInfoBuilder builder;
        if (outputPath != null) {
            builder = executionInfoBuilder.outputPath(outputPath);
        } else {
            builder = executionInfoBuilder;
        }

        if (!StaticTracer.mode().equals(TracerMode.NONE) && javaAgent != null) {
            return super.execution(testOrder,
                    builder
                            .addProperty("statictracer.tracer_path", String.valueOf(StaticFieldPathManager.modePath(StaticTracer.mode())))
                            .javaAgent(Paths.get(javaAgent)));
        } else {
            return super.execution(testOrder,
                    builder);
        }
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
