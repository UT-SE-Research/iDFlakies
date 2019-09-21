package edu.illinois.cs.dt.tools.poldet;

import edu.illinois.cs.dt.tools.detection.DetectorPlugin;
import edu.illinois.cs.dt.tools.poldet.instrumentation.MainAgent;
import edu.illinois.cs.dt.tools.runner.InstrumentingSmartRunner;
import edu.illinois.cs.dt.tools.utility.ErrorLogger;
import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.data.results.Result;
import edu.illinois.cs.testrunner.data.results.TestResult;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import edu.illinois.cs.testrunner.mavenplugin.TestPlugin;
import edu.illinois.cs.testrunner.mavenplugin.TestPluginPlugin;
import edu.illinois.cs.testrunner.runner.Runner;
import edu.illinois.cs.testrunner.runner.RunnerFactory;
import org.apache.maven.project.MavenProject;
import scala.Option;
import scala.util.Try;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PolDetCleanerFinderPlugin extends TestPlugin {

    private String coordinates;
    private InstrumentingSmartRunner runner;

    public PolDetCleanerFinderPlugin() {
    }

    public PolDetCleanerFinderPlugin(final InstrumentingSmartRunner runner) {
        this.runner = runner;
    }

    @Override
    public void execute(final MavenProject mavenProject) {
        final ErrorLogger logger = new ErrorLogger(mavenProject);
        this.coordinates = logger.coordinates();

        logger.runAndLogError(() -> cleanerFinderExecute(logger, mavenProject));
    }

    private Void cleanerFinderExecute(final ErrorLogger logger, final MavenProject mavenProject) throws IOException {
        final Option<Runner> runnerOption = RunnerFactory.from(mavenProject);

        // We must be able to run project's tests (if we get a runner from the RunnerFactory, we're good)
        if (runnerOption.isDefined()) {
            if (this.runner == null) {
                this.runner = InstrumentingSmartRunner.fromRunner(runnerOption.get());
                this.runner.setAgent(MainAgent.class.getProtectionDomain().getCodeSource().getLocation().getFile());
            }

            // There must be results from a prior PolDet run with all the polluter tests
            File poldetResultsFile = new File(PolDetPathManager.poldetResults().toString());
            if (poldetResultsFile.exists()) {
                List<String> polluters = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(new FileReader(poldetResultsFile))) {
                    String line = reader.readLine();
                    while (line != null) {
                        polluters.add(line);
                        line = reader.readLine();
                    }
                } catch (IOException ex) {
                }

                // For each polluter, first try to run in "isolation" to see if they actually pollute without other tests
                List<String> actualPolluters = new ArrayList<>();
                List<String> potentialCleaners = new ArrayList<>();
                for (String polluter : polluters) {
                    // If is still failing due to PolDet, then is polluter
                    if (!runIsolated(Collections.singletonList(polluter))) {
                        actualPolluters.add(polluter);
                    } else {    // Otherwise, while it was found "polluting" before, maybe it is a cleaner!
                        potentialCleaners.add(polluter);
                    }
                }

                // For all the actual polluters, try to run every other test after it to see if pollution is fixed
                List<String> tests = new ArrayList<>(DetectorPlugin.getOriginalOrder(mavenProject));
                tests.removeAll(potentialCleaners); // Potential cleaners are tried first
                for (String polluter : actualPolluters) {
                    // Try all the potential cleaners first
                    for (String candidate : potentialCleaners) {
                        List<String> testsToRun = new ArrayList<>();
                        testsToRun.add(polluter);
                        testsToRun.add(candidate);
                        if (runIsolated(testsToRun)) {
                            TestPluginPlugin.info("FOUND CLEANER FOR " + polluter + ": " + candidate);
                        }
                    }
                    // Try for all other tests next
                    for (String candidate : tests) {
                        List<String> testsToRun = new ArrayList<>();
                        testsToRun.add(polluter);
                        testsToRun.add(candidate);
                        if (runIsolated(testsToRun)) {
                            TestPluginPlugin.info("FOUND CLEANER FOR " + polluter + ": " + candidate);
                        }
                    }
                }
            } else {
                final String errorMsg = "Module needs to have results from prior run of PolDetPlugin to know what are the polluters.";
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

    // Returns PolDet result of running the tests passed in
    protected boolean runIsolated(List<String> tests) {
        // Run the test once to get what are all the loaded classes, with the fake output test at the end
        List<String> testsToRun = new ArrayList<>(tests);
        testsToRun.add(0, "edu.illinois.cs.dt.tools.poldet.StateCaptureFakeTest.outputLoadedClasses");
        this.runner.runList(testsToRun);

        // Now run the test to capture state after initializing
        testsToRun = new ArrayList<>(tests);
        testsToRun.add(0, "edu.illinois.cs.dt.tools.poldet.StateCaptureFakeTest.initializeLoadedClasses");  // Potential thing to do...
        testsToRun.add(1, "edu.illinois.cs.dt.tools.poldet.StateCaptureFakeTest.before");
        testsToRun.add("edu.illinois.cs.dt.tools.poldet.StateCaptureFakeTest.after");
        testsToRun.add("edu.illinois.cs.dt.tools.poldet.StateCaptureFakeTest.checkHasPolluters");
        testsToRun.add("edu.illinois.cs.dt.tools.poldet.StateCaptureFakeTest.outputDifferingRoots");

        // Check the result if something actually polluted
        Try<TestRunResult> testRunResultTry = runner.runList(testsToRun);
        if (testRunResultTry.isSuccess()) {
            TestResult res = testRunResultTry.get().results().get("edu.illinois.cs.dt.tools.poldet.StateCaptureFakeTest.checkHasPolluters");
            // If the final state check test is not passing, then there was pollution and this is a true polluter
            if (!res.result().equals(Result.PASS)) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }
}
