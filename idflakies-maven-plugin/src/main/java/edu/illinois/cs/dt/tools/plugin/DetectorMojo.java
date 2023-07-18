package edu.illinois.cs.dt.tools.plugin;

import com.google.common.collect.Lists;
import com.opencsv.CSVReader;
import com.reedoei.eunomia.collections.ListEx;
import edu.illinois.cs.dt.tools.detection.MavenDetectorPathManager;
import edu.illinois.cs.dt.tools.detection.detectors.Detector;
import edu.illinois.cs.dt.tools.detection.detectors.DetectorFactory;
import edu.illinois.cs.dt.tools.runner.InstrumentingSmartRunner;
import edu.illinois.cs.dt.tools.utility.ErrorLogger;
import edu.illinois.cs.dt.tools.utility.GetMavenTestOrder;
import edu.illinois.cs.dt.tools.utility.Level;
import edu.illinois.cs.dt.tools.utility.Logger;
import edu.illinois.cs.dt.tools.utility.OperationTime;
import edu.illinois.cs.dt.tools.utility.PathManager;
import edu.illinois.cs.dt.tools.utility.TestClassData;
import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.data.framework.TestFramework;
import edu.illinois.cs.testrunner.runner.Runner;
import edu.illinois.cs.testrunner.runner.RunnerFactory;
import edu.illinois.cs.testrunner.testobjects.TestLocator;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import scala.collection.JavaConverters;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.RuntimeException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

@Mojo(name = "detect", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class DetectorMojo extends AbstractIDFlakiesMojo {

    protected Path outputPath;
    protected String coordinates;
    protected InstrumentingSmartRunner runner;
    private static Map<Integer, List<String>> locateTestList = new HashMap<>();
    // useful for modules with JUnit 4 tests but depend on something in JUnit 5
    private final boolean forceJUnit4 = Configuration.config().getProperty("dt.detector.forceJUnit4", false);

    // TODO: copy to eunomia
    private static ListEx<ListEx<String>> csv(final Path path) throws IOException {
        try (final FileInputStream fis = new FileInputStream(path.toAbsolutePath().toString());
             final InputStreamReader isr = new InputStreamReader(fis);
             final CSVReader reader = new CSVReader(isr)) {
            return new ListEx<>(reader.readAll()).map(ListEx::fromArray);
        }
    }

    private static <T> T until(final T t, final Function<T, T> f, final Predicate<T> pred) {
        if (pred.test(t)) {
            return t;
        } else {
            return until(f.apply(t), f, pred);
        }
    }

    private static <T> T untilNull(final T t, final Function<T, T> f) {
        return until(t, f, v -> f.apply(v) == null);
    }

    private static ListEx<ListEx<String>> transpose(final ListEx<ListEx<String>> rows) {
        final ListEx<ListEx<String>> result = new ListEx<>();

        final int len = rows.get(0).size();

        for (int i = 0; i < len; i++) {
            final int finalI = i;
            result.add(rows.map(s -> s.get(finalI)));
        }

        return result;
    }

    private long moduleTimeout(final MavenProject project) throws IOException {
        final MavenProject parent = MavenDetectorPathManager.getMavenProjectParent(project);

        final Path timeCsv = parent.getBasedir().toPath().resolve("module-test-time.csv");
        Files.copy(timeCsv, PathManager.detectionResults().resolve("module-test-time.csv"), StandardCopyOption.REPLACE_EXISTING);
        final ListEx<ListEx<String>> csv = csv(timeCsv);

        // Skip the header row, sum the second column to get the total time
        final double totalTime =
                csv.stream()
                        .mapToDouble(row -> Double.valueOf(row.get(1)))
                        .sum();

        // Lookup ourselves in the csv to see how long we took
        final Double moduleTime =
                csv.stream()
                        .filter(row -> row.get(0).equals(coordinates))
                        .findFirst()
                        .map(row -> Double.valueOf(row.get(1)))
                        .orElse(0.0);
        final double mainTimeout = Configuration.config().getProperty("detector.timeout", 6 * 3600.0); // 6 hours
        double timeout =
                Math.max(2.0, moduleTime * mainTimeout / totalTime);

        // Can only happen when the totalTime is 0. This means it will occur for all projects.
        // In this case, just allocate equal time to everyone.
        if (Double.isNaN(timeout)) {
            if (csv.size() > 0) {
                timeout = mainTimeout / csv.size();
            } else {
                // This makes no sense, because this means there are no modules
                throw new IllegalStateException("No modules/test times found in " + timeCsv);
            }
        }


        Logger.getGlobal().log(Level.INFO, "TIMEOUT_CALCULATED: Giving " + coordinates + " " + timeout + " seconds to run for " +
                                           DetectorFactory.detectorType());

        return (long) timeout; // Allocate time proportionally
    }

    private static double readRealTime(final Path path) throws IOException {
        for (final String line : Lists.reverse(Files.readAllLines(path))) {
            final String[] split = line.split(" ");

            if (split.length > 1 && split[0].equals("real")) {
                return Double.parseDouble(split[1]);
            }
        }

        return 0.0;
    }

    public static int moduleRounds(String coordinates) throws IOException {
        final boolean hasRounds = Configuration.config().properties().getProperty("dt.randomize.rounds") != null;
        final boolean hasTimeout = Configuration.config().properties().getProperty("detector.timeout") != null;

        final int roundNum = Configuration.config().getProperty("dt.randomize.rounds", 20);

        final int timeoutRounds;
        if (hasTimeout) {
            final Path timeCsv = PathManager.testTimeLog();

            if (Files.isReadable(timeCsv)) {
                final double totalTime = readRealTime(timeCsv);
                final double mainTimeout = Configuration.config().getProperty("detector.timeout", 6 * 3600.0); // 6 hours
                if (mainTimeout != 0) {

                    Logger.getGlobal().log(Level.INFO, "TIMEOUT_VALUE: Using a timeout of " +
                                                       mainTimeout + ", and that the total mvn test time is: " + totalTime);
                    timeoutRounds = (int) (mainTimeout / totalTime);
                } else {
                    timeoutRounds = roundNum;
                    Logger.getGlobal().log(Level.INFO, "TIMEOUT_VALUE specified as 0. " +
                                                       "Ignoring timeout and using number of rounds.");
                }
            } else {
                // Depending on the order in which the developers tell Maven to build modules, some projects like http-request
                // may not be able to parse the mvn-test-time.log at the base module if other submodules are built first
                timeoutRounds = roundNum;
                Logger.getGlobal().log(Level.INFO, "TIMEOUT_VALUE specified but cannot " +
                                       "read mvn-test-time.log at: " + timeCsv.toString());
                Logger.getGlobal().log(Level.INFO, "Ignoring timeout and using number of rounds.");
            }
        } else {
            Logger.getGlobal().log(Level.INFO, "No timeout specified. Using randomize.rounds: " + roundNum);
            timeoutRounds = roundNum;
        }

        final int rounds;
        if (!hasRounds) {
            rounds = timeoutRounds;
        } else {
            rounds = Math.min(timeoutRounds, roundNum);
        }

        Logger.getGlobal().log(Level.INFO, "ROUNDS_CALCULATED: Giving " + coordinates + " "
                + rounds + " rounds to run for " + DetectorFactory.detectorType());

        return rounds;
    }

    @Override
    public void execute() {
        superExecute();
        this.outputPath = PathManager.detectionResults();

        final ErrorLogger logger = new ErrorLogger();
        this.coordinates = mavenProject.getGroupId() + ":" + mavenProject.getArtifactId() + ":" + mavenProject.getVersion();

        long startTime = System.currentTimeMillis();
        try {
            defineSettings(logger, mavenProject);
            loadTestRunners(logger, mavenProject);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.runAndLogError(() -> detectorExecute(logger, mavenProject, moduleRounds(coordinates)));
        timing(startTime);
    }

    protected void superExecute() {
        super.execute();
    }

    protected void defineSettings(final ErrorLogger logger, final MavenProject mavenProject) throws IOException {
        Files.deleteIfExists(PathManager.errorPath());
        Files.deleteIfExists(PathManager.timePath());
        Files.createDirectories(PathManager.cachePath());
        Files.createDirectories(PathManager.detectionResults());
    }

    protected void loadTestRunners(final ErrorLogger logger, final MavenProject mavenProject) throws IOException {
        // Currently there could two runners, one for JUnit 4 and one for JUnit 5
        // If the maven project has both JUnit 4 and JUnit 5 tests, two runners will
        // be returned
        List<Runner> runners = RunnerFactory.allFrom(mavenProject);
        runners = removeZombieRunners(runners, mavenProject);

        if (runners.size() != 1) {
            if (forceJUnit4) {
                Runner nrunner = null;
                for (Runner runner : runners) {
                    if (runner.framework().toString() == "JUnit") {
                        nrunner = runner;
                        break;
                    }
                }
                if (nrunner != null) {
                    runners = new ArrayList<>(Arrays.asList(nrunner));
                } else {
                    String errorMsg;
                    if (runners.size() == 0) {
                        errorMsg =
                            "Module is not using a supported test framework (probably not JUnit), " +
                            "or there is no test.";
                    } else {
                        errorMsg = "dt.detector.forceJUnit4 is true but no JUnit 4 runners found. Perhaps the project only contains JUnit 5 tests.";
                    }
                    Logger.getGlobal().log(Level.INFO, errorMsg);
                    logger.writeError(errorMsg);
                    return;
                }
            } else {
                String errorMsg;
                if (runners.size() == 0) {
                    errorMsg =
                        "Module is not using a supported test framework (probably not JUnit), " +
                        "or there is no test.";
                } else {
                    // more than one runner, currently is not supported.
                    errorMsg =
                        "This project contains both JUnit 4 and JUnit 5 tests, which currently"
                        + " is not supported by iDFlakies";
                }
                Logger.getGlobal().log(Level.INFO, errorMsg);
                logger.writeError(errorMsg);
                return;
            }
        }

        if (this.runner == null) {
            this.runner = InstrumentingSmartRunner.fromRunner(runners.get(0), mavenProject.getBasedir());
        }
    }

    protected List<String> getTests(
            final MavenProject project,
            TestFramework testFramework) throws IOException {
        return getOriginalOrder(project, testFramework);
    }

    protected Void detectorExecute(final ErrorLogger logger, final MavenProject mavenProject, final int rounds) throws IOException {
        final List<String> tests = getTests(mavenProject, this.runner.framework());

        if (!tests.isEmpty()) {
            Files.createDirectories(outputPath);
            Files.write(PathManager.selectedTestPath(), String.join(System.lineSeparator(), tests).getBytes());
            final Detector detector = DetectorFactory.makeDetector(this.runner, mavenProject.getBasedir(), tests, rounds);
            Logger.getGlobal().log(Level.INFO, "Created dependent test detector (" + detector.getClass() + ").");
            detector.writeTo(outputPath);
        } else {
            String errorMsg = "Module has no tests, not running detector.";
            Logger.getGlobal().log(Level.INFO, errorMsg);
            logger.writeError(errorMsg);
        }

        return null;
    }

    private static List<String> locateTests(MavenProject project, TestFramework testFramework) {
        int id = Objects.hash(project, testFramework);
        if (!locateTestList.containsKey(id)) {
            Logger.getGlobal().log(Level.INFO, "Locating tests...");
            try {
                locateTestList.put(id, OperationTime.runOperation(() -> {
                    List<String> tests = new ArrayList<>(JavaConverters.bufferAsJavaList(TestLocator.tests(project, testFramework).toBuffer()));
                    Collections.sort(tests);
                    return tests;
                }, (tests, time) -> {
                    Logger.getGlobal().log(Level.INFO, "Located " + tests.size() + " tests. Time taken: " + time.elapsedSeconds() + " seconds");
                    return tests;
                }));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return locateTestList.get(id);
    }

    public static List<String> getOriginalOrder(
            final MavenProject project,
            TestFramework testFramework) throws IOException {
        return getOriginalOrder(project, testFramework, false);
    }

    public static List<String> getOriginalOrder(
            final MavenProject project,
            TestFramework testFramework,
            boolean ignoreExisting) throws IOException {
        if (!Files.exists(PathManager.originalOrderPath()) || ignoreExisting) {
            Logger.getGlobal().log(Level.INFO, "Getting original order by parsing logs. ignoreExisting set to: " + ignoreExisting);

            List<String> originalOrder = null;
            try {
                final Path surefireReportsPath = Paths.get(project.getBuild().getDirectory()).resolve("surefire-reports");
                final Path mvnTestLog = PathManager.testLog();
                if (Files.exists(mvnTestLog) && Files.exists(surefireReportsPath)) {
                    final List<TestClassData> testClassData = new GetMavenTestOrder(surefireReportsPath, mvnTestLog).testClassDataList();

                    final List<String> tests = new ArrayList<>();

                    String delimiter = testFramework.getDelimiter();

                    for (final TestClassData classData : testClassData) {
                        for (final String testName : classData.testNames) {
                            tests.add(classData.className + delimiter + testName);
                        }
                    }

                    originalOrder = tests;
                } else {
                    originalOrder = locateTests(project, testFramework);
                }
            } catch (Exception ignored) {}

            // If something went wrong, then configure originalOrder accordingly
            if (originalOrder == null) {
                originalOrder = locateTests(project, testFramework);
            }

            // Write the computed original order to file if did not exist or specified to ignore existing one
            Files.write(PathManager.originalOrderPath(), String.join(System.lineSeparator(), originalOrder).getBytes());
            return originalOrder;
        } else {
            return Files.readAllLines(PathManager.originalOrderPath());
        }
    }

    private static List<Runner> removeZombieRunners(
            List<Runner> runners, MavenProject project) throws IOException {
        // Some projects may include test frameworks without corresponding tests.
        // Filter out such zombie test frameworks (runners).
        // For example, a project have both JUnit 4 and 5 dependencies, but there is
        // no JUnit 4 tests. In such case, we need to remove the JUnit 4 runner.
        List<Runner> aliveRunners = new ArrayList<>();
        for (Runner runner : runners) {
            if (locateTests(project, runner.framework()).size() > 0) {
                aliveRunners.add(runner);
            }
        }
        return aliveRunners;
    }

    public static void timing(long startTime) {
        if (!Files.exists(PathManager.timePath())) {
            try {
                Files.createFile(PathManager.timePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        long endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;

        String time = duration + ",";
        try {
            Files.write(PathManager.timePath(), time.getBytes(),
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
