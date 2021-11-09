package edu.illinois.cs.dt.tools.plugin;

import com.google.gson.Gson;
import com.reedoei.eunomia.io.files.FileUtil;
import edu.illinois.cs.dt.tools.utility.Level;
import edu.illinois.cs.dt.tools.utility.Logger;
import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import edu.illinois.cs.testrunner.runner.Runner;
import edu.illinois.cs.testrunner.runner.RunnerFactory;
import scala.Option;
import scala.util.Try;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ReplayMojo extends AbstractIDFlakiesMojo {
    private Path replayPath;

    @Override
    public void execute() {
        final Option<Runner> runnerOption = RunnerFactory.from(mavenProject);

        if (runnerOption.isDefined()) {
            replayPath = Paths.get(System.getProperty("replay.path"));
          //  final Path outputPath = Paths.get(Configuration.config().properties().getProperty("replay.output_path"));
            final Path outputPath = Paths.get(System.getProperty("replay.output_path"));

            try {
                final Runner runner = runnerOption.get(); // safe because we checked above

                final Try<TestRunResult> testRunResultTry = runner.runList(testOrder());

                if (testRunResultTry.isSuccess()) {
                    Files.write(outputPath, new Gson().toJson(testRunResultTry.get()).getBytes());
                } else {
                    getLog().error(testRunResultTry.failed().get());
                }
            } catch (IOException e) {
                getLog().error(e);
            }
        } else {
            Logger.getGlobal().log(Level.INFO, "Module is not using a supported test framework (probably not JUnit).");
        }
    }

    private List<String> testOrder() throws IOException {
        try {
            return new Gson().fromJson(FileUtil.readFile(replayPath), TestRunResult.class).testOrder();
        } catch (Exception e) {
            return Files.readAllLines(replayPath);
        }
    }
}