package edu.illinois.cs.dt.tools.plugin;

import com.google.gson.Gson;
import edu.illinois.cs.dt.tools.runner.InstrumentingSmartRunner;
import edu.illinois.cs.dt.tools.utility.ErrorLogger;
import edu.illinois.cs.dt.tools.utility.Level;
import edu.illinois.cs.dt.tools.utility.Logger;
import edu.illinois.cs.dt.tools.utility.PathManager;
import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import edu.illinois.cs.testrunner.runner.Runner;
import edu.illinois.cs.testrunner.runner.RunnerFactory;
import scala.Option;
import scala.util.Try;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.awt.geom.RectangularShape;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Mojo(name = "bulk", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class BulkOrderRunnerMojo extends AbstractIDFlakiesMojo {

    @Override
    public void execute() {
        super.execute();
        final ErrorLogger errorLogger = new ErrorLogger();
        final Option<Runner> runnerOption = RunnerFactory.from(mavenProject);
        errorLogger.runAndLogError(() -> {
            Files.deleteIfExists(PathManager.errorPath());
            Files.createDirectories(PathManager.cachePath());
            Files.createDirectories(PathManager.detectionResults());

            if (runnerOption.isDefined()) {
                final InstrumentingSmartRunner runner = InstrumentingSmartRunner.fromRunner(runnerOption.get(), mavenProject.getBasedir());
                final Path inputPath = Paths.get(Configuration.config().getProperty("bulk_runner.input_dir"));
                final Path outputPath = Paths.get(Configuration.config().getProperty("bulk_runner.output_dir"));
                run(runner, inputPath, outputPath);
            } else {
                final String errorMsg = "Module is not using a supported test framework (probably not JUnit).";
                Logger.getGlobal().log(Level.INFO, errorMsg);
                errorLogger.writeError(errorMsg);
            }

            return null;
        });
    }

    private void run(final InstrumentingSmartRunner runner, final Path inputPath, final Path outputPath) throws IOException {
        final List<Path> collect = Files.list(inputPath).collect(Collectors.toList());
        for (int i = 0; i < collect.size(); i++) {
            final Path p = collect.get(i);
            Logger.getGlobal().log(Level.INFO, String.format("Running (%d of %d): %s", i, collect.size(), p));
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
