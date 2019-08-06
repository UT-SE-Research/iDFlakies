package edu.illinois.cs.dt.tools.poldet;

import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.data.results.Result;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import edu.illinois.cs.dt.tools.detection.DetectorPlugin;
import edu.illinois.cs.dt.tools.poldet.instrumentation.MainAgent;
import edu.illinois.cs.dt.tools.runner.InstrumentingSmartRunner;
import edu.illinois.cs.dt.tools.utility.ErrorLogger;
import edu.illinois.cs.dt.tools.utility.PathManager;
import edu.illinois.cs.testrunner.mavenplugin.TestPluginPlugin;
import edu.illinois.cs.testrunner.runner.Runner;
import edu.illinois.cs.testrunner.runner.RunnerFactory;
import org.apache.maven.project.MavenProject;
import scala.Option;
import scala.util.Try;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PolDetPlugin extends DetectorPlugin {

    private String coordinates;
    private InstrumentingSmartRunner runner;

    public PolDetPlugin() {
    }

    public PolDetPlugin(final InstrumentingSmartRunner runner) {
        this.runner = runner;
    }

    @Override
    public void execute(final MavenProject mavenProject) {
        final ErrorLogger logger = new ErrorLogger(mavenProject);
        this.coordinates = logger.coordinates();

        logger.runAndLogError(() -> poldetExecute(logger, mavenProject));
    }

    private Void poldetExecute(final ErrorLogger logger, final MavenProject mavenProject) throws IOException {
        final Option<Runner> runnerOption = RunnerFactory.from(mavenProject);

        // We need to do two checks to make sure that we can run this project
        // Firstly, we must be able to run it's tests (if we get a runner from the RunnerFactory, we're good)
        // Secondly, there must be some tests (see below)
        if (runnerOption.isDefined()) {
            if (this.runner == null) {
                this.runner = InstrumentingSmartRunner.fromRunner(runnerOption.get());
                runner.setAgent(MainAgent.class.getProtectionDomain().getCodeSource().getLocation().getFile());
            }

            final List<String> tests = DetectorPlugin.getOriginalOrder(mavenProject);

            // If there are no tests, we can't run PolDet
            if (!tests.isEmpty()) {
                // Set up the basic output directory structure that those fake tests rely on to start
                new File(PathManager.cachePath().toString()).mkdir();

                // Run all the tests once to get what are all the loaded classes, with the fake output test at the end
                List<String> testsToRun = new ArrayList<String>(tests);
                testsToRun.add("edu.illinois.cs.dt.tools.poldet.StateCaptureFakeTest.outputLoadedClasses");
                runner.runList(testsToRun);

                // In subsequent run, set up the listener so can start capturing, with fake test at the end that outputs results
                // Also try to "initialize" the run by loading in all the classes at the start
                Configuration.config().properties().setProperty("testrunner.testlistener_class", PolDetRunnerListener.class.getCanonicalName());
                testsToRun = new ArrayList<String>(tests);
                testsToRun.add(0, "edu.illinois.cs.dt.tools.poldet.StateCaptureFakeTest.initializeLoadedClasses");  // Potential thing to do...
                testsToRun.add("edu.illinois.cs.dt.tools.poldet.StateCaptureFakeTest.outputResults");
                runner.runList(testsToRun);
            } else {
                final String errorMsg = "Module has no tests, not running PolDet.";
                TestPluginPlugin.info(errorMsg);
                logger.writeError(errorMsg);
            }

        } else {
            final String errorMsg = "Module is not using a supported test framework (probably not JUnit).";
            TestPluginPlugin.info(errorMsg);
            logger.writeError(errorMsg);
        }

        return null;
    }
}
