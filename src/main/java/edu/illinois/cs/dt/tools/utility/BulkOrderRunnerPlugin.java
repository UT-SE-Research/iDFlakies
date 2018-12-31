package edu.illinois.cs.dt.tools.utility;

import com.google.gson.Gson;
import com.reedoei.testrunner.configuration.Configuration;
import com.reedoei.testrunner.data.results.TestRunResult;
import com.reedoei.testrunner.mavenplugin.TestPlugin;
import com.reedoei.testrunner.mavenplugin.TestPluginPlugin;
import com.reedoei.testrunner.runner.Runner;
import com.reedoei.testrunner.runner.RunnerFactory;
import edu.illinois.cs.dt.tools.detection.DetectorPathManager;
import edu.illinois.cs.dt.tools.runner.InstrumentingSmartRunner;
import org.apache.maven.project.MavenProject;
import scala.Option;
import scala.util.Try;

import java.awt.geom.RectangularShape;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class BulkOrderRunnerPlugin extends TestPlugin {
    @Override
    public void execute(final MavenProject mavenProject) {
        final ErrorLogger errorLogger = new ErrorLogger(mavenProject);

        final Option<Runner> runnerOption = RunnerFactory.from(mavenProject);

        errorLogger.runAndLogError(() -> {
            Files.deleteIfExists(DetectorPathManager.errorPath());
            Files.createDirectories(DetectorPathManager.cachePath());
            Files.createDirectories(DetectorPathManager.detectionResults());

            if (runnerOption.isDefined()) {
                final InstrumentingSmartRunner runner = InstrumentingSmartRunner.fromRunner(runnerOption.get());

                final Path inputPath = Paths.get(Configuration.config().getProperty("bulk_runner.input_dir"));
                final Path outputPath = Paths.get(Configuration.config().getProperty("bulk_runner.output_dir"));

                run(runner, inputPath, outputPath);
            } else {
                final String errorMsg = "Module is not using a supported test framework (probably not JUnit).";
                TestPluginPlugin.info(errorMsg);
                errorLogger.writeError(errorMsg);
            }

            return null;
        });
    }

    private void run(final InstrumentingSmartRunner runner, final Path inputPath, final Path outputPath) throws IOException {
        final List<Path> collect = Files.list(inputPath).collect(Collectors.toList());
        for (int i = 0; i < collect.size(); i++) {
            final Path p = collect.get(i);
            TestPluginPlugin.info(String.format("Running (%d of %d): %s", i, collect.size(), p));
            runOrder(runner, p, outputPath);
        }
    }

    private void runOrder(final InstrumentingSmartRunner runner, final Path orderPath, final Path outputPath)
            throws IOException {
        final List<String> tests = Files.readAllLines(orderPath);
        final Try<TestRunResult> result = runner.runList(tests);

        if (result.isSuccess()) {
            Files.write(outputPath.resolve(orderPath.getFileName()),
                    new Gson().toJson(result.get()).getBytes());
        }
    }
}
