package edu.illinois.cs.dt.tools.utility;

import com.google.gson.Gson;
import com.reedoei.eunomia.io.files.FileUtil;
import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import edu.illinois.cs.testrunner.coreplugin.TestPlugin;
import edu.illinois.cs.testrunner.coreplugin.TestPluginUtil;
import edu.illinois.cs.testrunner.runner.Runner;
import edu.illinois.cs.testrunner.runner.RunnerFactory;
import edu.illinois.cs.testrunner.util.ProjectWrapper;
import scala.Option;
import scala.util.Try;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ReplayPlugin extends TestPlugin {
    private Path replayPath;

    @Override
    public void execute(final ProjectWrapper project) {
        final Option<Runner> runnerOption = RunnerFactory.from(project);

        if (runnerOption.isDefined()) {
            replayPath = Paths.get(Configuration.config().getProperty("replay.path"));
            final Path outputPath = Paths.get(Configuration.config().properties().getProperty("replay.output_path"));

            try {
                final Runner runner = runnerOption.get(); // safe because we checked above

                final Try<TestRunResult> testRunResultTry = runner.runList(testOrder());

                if (testRunResultTry.isSuccess()) {
                    Files.write(outputPath, new Gson().toJson(testRunResultTry.get()).getBytes());
                } else {
                    TestPluginUtil.project.error(testRunResultTry.failed().get());
                }
            } catch (IOException e) {
                TestPluginUtil.project.error(e);
            }
        } else {
            TestPluginUtil.project.info("Module is not using a supported test framework (probably not JUnit).");
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
