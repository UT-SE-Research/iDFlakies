package edu.illinois.cs.dt.tools.analysis;

import com.google.gson.Gson;
import com.opencsv.CSVReader;
import com.reedoei.eunomia.collections.ListEx;
import com.reedoei.eunomia.io.files.FileUtil;
import com.reedoei.eunomia.util.StandardMain;
import edu.illinois.cs.dt.tools.detection.DetectionRound;
import edu.illinois.cs.dt.tools.detection.DetectorPathManager;
import edu.illinois.cs.dt.tools.detection.DetectorUtil;
import edu.illinois.cs.dt.tools.runner.RunnerPathManager;
import edu.illinois.cs.dt.tools.runner.data.DependentTest;
import edu.illinois.cs.dt.tools.runner.data.DependentTestList;
import edu.illinois.cs.dt.tools.utility.OperationTime;
import edu.illinois.cs.dt.tools.utility.TestRunParser;
import edu.illinois.cs.testrunner.data.results.Result;
import edu.illinois.cs.testrunner.data.results.TestResult;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import org.apache.commons.io.FilenameUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: would probably be better to have these insert methods in their respective classes with some
//       interface or something...
public class Analysis extends StandardMain {
    public static int roundNumber(final String filename) {
        // Files are named roundN.json, so strip extension and "round" and we'll have the number
        final String fileName = FilenameUtils.removeExtension(filename);
        return Integer.parseInt(fileName.substring("round".length()));
    }

    // List files and close the directory streams
    private static ListEx<Path> listFiles(final Path path) throws IOException {
        final ListEx<Path> result = new ListEx<>();

        try (final Stream<Path> stream = Files.list(path)) {
            result.addAll(stream.collect(Collectors.toList()));
        }

        return result;
    }

    private final Path results;
    private final SQLite sqlite;
    private int dtListIndex = 0;
    private final int maxTestRuns;
    private final Path subjectList;
    private final Path subjectListLOC;

    private Analysis(final String[] args) throws SQLException {
        super(args);

        this.results = Paths.get(getArgRequired("results")).toAbsolutePath();
        this.sqlite = new SQLite(Paths.get(getArgRequired("db")).toAbsolutePath());
        this.subjectList = Paths.get(getArgRequired("subjectList")).toAbsolutePath();
        this.subjectListLOC = Paths.get(getArgRequired("subjectListLoc")).toAbsolutePath();
        this.maxTestRuns = getArg("max-test-runs").map(Integer::parseInt).orElse(0);
    }

    public static void main(final String[] args) {
        try {
            new Analysis(args).run();

            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.exit(1);
    }

    public static ListEx<ListEx<String>> csv(final Path path) throws IOException {
        try (final FileInputStream fis = new FileInputStream(path.toAbsolutePath().toString());
             final InputStreamReader isr = new InputStreamReader(fis);
             final CSVReader reader = new CSVReader(isr)) {
            return new ListEx<>(reader.readAll()).map(ListEx::fromArray);
        }
    }

    @Override
    protected void run() throws Exception {
        createTables();

        insertFullSubjectList(subjectList);
        insertSubjectLOC(subjectListLOC);

        System.out.println();

        final List<Path> allResultsFolders = new ArrayList<>();
        Files.walkFileTree(results, new ResultDirVisitor(allResultsFolders));

        for (int i = 0; i < allResultsFolders.size(); i++) {
            final Path p = allResultsFolders.get(i);
            System.out.println("[INFO] Inserting results for module " + (i + 1) + " of " + allResultsFolders.size() + ": " + p);
            try {
                insertResults(p);
            } catch (IOException | SQLException e) {
                throw new RuntimeException(e);
            }
        }

        runPostSetup();

        sqlite.save();
    }

    private void insertSubjectLOC(final Path path) throws IOException, SQLException {
        System.out.println("[INFO] Inserting subject's LOC and TEST_LOC");

        final ListEx<ListEx<String>> csv = csv(path);

        for (final ListEx<String> rows : csv) {
            sqlite.statement(SQLStatements.UPDATE_SUBJECT_RAW_LOC)
                    .param(rows.get(2)) // loc
                    .param(rows.get(3)) // test_loc
                    .param(rows.get(0).toLowerCase()) // slug
                    .executeUpdate();
        }
    }

    private void insertFullSubjectList(final Path fullList) throws IOException, SQLException {
        if (!Files.exists(fullList)) {
            throw new FileNotFoundException(fullList.toAbsolutePath().toString());
        }

        System.out.println("[INFO] Inserting subject list from: " + fullList);

        try (final FileInputStream fis = new FileInputStream(fullList.toAbsolutePath().toString());
             final InputStreamReader isr = new InputStreamReader(fis);
             final CSVReader reader = new CSVReader(isr)) {

            String[] strings;
            while ((strings = reader.readNext()) != null) {
                if (strings.length >= 2) {
                    insertSubjectRaw(strings[0], strings[1]);
                }
            }
        }
    }

    private void insertSubjectRaw(final String url, final String sha) throws MalformedURLException, SQLException {
        // Get the slug. The path starts with a slash, so get rid of it via substring
        final String slug = new URL(url).getPath().substring(1);

        System.out.println("[INFO] Inserting " + url + " with slug " + slug + " and SHA " + sha);
        sqlite.statement(SQLStatements.INSERT_RAW_SUBJECT)
                .param(slug.toLowerCase())
                .param(url.toLowerCase())
                .param(sha.toLowerCase())
                .executeUpdate();
    }

    private void runPostSetup() throws IOException {
        System.out.println("[INFO] Running post setup queries");
        sqlite.executeFile(SQLStatements.POST_SETUP);
    }

    private void createTables() throws IOException {
        System.out.println("[INFO] Creating tables and views");
        sqlite.executeFile(SQLStatements.CREATE_TABLES);
        System.out.println();
    }

    private void insertResults(final Path path) throws IOException, SQLException {
        final String parent = findParent(path);

        if (parent == null) {
            return;
        }

        final String name = path.getFileName().toString();
        final String slug = parent.substring(0, parent.indexOf('_')).replace('.', '/');

        insertModuleTestTime(slug, path.resolve(DetectorPathManager.DETECTION_RESULTS).resolve("module-test-time.csv"));

        if (!sqlite.checkExists("subject", name)) {
            insertSubject(name, slug, path);
        }

        final Path originalOrderPath = path.resolve(DetectorPathManager.ORIGINAL_ORDER);
        if (!Files.exists(originalOrderPath)) {
            System.out.println("[WARNING] No original order found at " + path.resolve(DetectorPathManager.ORIGINAL_ORDER));
            return;
        }

        final List<String> originalOrder = Files.readAllLines(originalOrderPath);
        if (!sqlite.checkExists("original_order", "subject_name", name)) {
            System.out.println("[INFO] Inserting original order for " + name + " (" + originalOrder.size() + " tests)");

            final Procedure statement = sqlite.statement(SQLStatements.INSERT_ORIGINAL_ORDER);

            statement.beginTransaction();

            for (int i = 0; i < originalOrder.size(); i++) {
                statement
                        .param(name)
                        .param(originalOrder.get(i))
                        .param(i).addBatch();
            }

            statement.executeBatch();
            statement.commit();
            statement.endTransaction();
        }

        final Path results = path.resolve(RunnerPathManager.TEST_RUNS);

        if (!Files.isDirectory(results)) {
            System.out.println("[WARNING] No directory " + results + " for " + name);
            return;
        }

        final boolean foundPassing =
            new TestRunParser(results)
            .testRunResults()
            .anyMatch(trr -> {
                if (trr != null) {
                    if (trr.testOrder().equals(originalOrder)) {
                        System.out.println("[INFO] Found an original order run: " + trr.id());
                        if (DetectorUtil.allPass(trr)) {
                            System.out.println("[INFO] Found a passing order for " + name);
                            return true;
                        }
                    }
                }

                return false;
            });

        // If we got a no passing order exception, don't insert any of the other results
        if (!foundPassing) {
            System.out.println("[WARNING] SKIPPING: No passing order found for: " + name);
            for (final String detectorType : new String[] { "original", "random", "random-class", "reverse", "reverse-class"}) {
                if (Files.isDirectory(path.resolve(DetectorPathManager.DETECTION_RESULTS).resolve(detectorType))) {
                    System.out.println("[ERROR]: " + detectorType + " results for " + name + " at " + path.resolve(DetectorPathManager.DETECTION_RESULTS).resolve(detectorType));
                }
            }
            return;
        }

        System.out.println("[INFO] Found passing order for: " + name);

        insertTestRuns(name, path.resolve(RunnerPathManager.TEST_RUNS).resolve("results"));

        insertDetectionResults(name, "original", path.resolve(DetectorPathManager.DETECTION_RESULTS));
        insertDetectionResults(name, "random", path.resolve(DetectorPathManager.DETECTION_RESULTS));
        insertDetectionResults(name, "random-class", path.resolve(DetectorPathManager.DETECTION_RESULTS));
        insertDetectionResults(name, "reverse", path.resolve(DetectorPathManager.DETECTION_RESULTS));
        insertDetectionResults(name, "reverse-class", path.resolve(DetectorPathManager.DETECTION_RESULTS));

        insertVerificationResults(name, "random-verify", path.resolve(DetectorPathManager.DETECTION_RESULTS));
        insertVerificationResults(name, "random-class-verify", path.resolve(DetectorPathManager.DETECTION_RESULTS));
        insertVerificationResults(name, "reverse-verify", path.resolve(DetectorPathManager.DETECTION_RESULTS));
        insertVerificationResults(name, "reverse-class-verify", path.resolve(DetectorPathManager.DETECTION_RESULTS));
        insertVerificationResults(name, "random-confirmation-sampling", path.resolve(DetectorPathManager.DETECTION_RESULTS));
        insertVerificationResults(name, "random-class-confirmation-sampling", path.resolve(DetectorPathManager.DETECTION_RESULTS));
        insertVerificationResults(name, "reverse-confirmation-sampling", path.resolve(DetectorPathManager.DETECTION_RESULTS));
        insertVerificationResults(name, "reverse-class-confirmation-sampling", path.resolve(DetectorPathManager.DETECTION_RESULTS));

//        sqlite.save();

        System.out.println("[INFO] Finished " + name + " (" + slug + ")");
        System.out.println();
    }

    private int insertOperationTime(final OperationTime time) throws SQLException {
        return sqlite.statement(SQLStatements.INSERT_OPERATION_TIME)
                .param(time.startTime())
                .param(time.endTime())
                .param(time.elapsedSeconds())
                .insertSingleRow();
    }

    private void insertModuleTestTime(final String slug, final Path moduleTestTimePath) throws SQLException, IOException {
        if (!Files.exists(moduleTestTimePath)) {
            return;
        }

        System.out.println("[INFO] Inserting module test time for: " + slug);

        if (!sqlite.checkExists("subject", "slug", slug)) {
            final ListEx<ListEx<String>> rows = csv(moduleTestTimePath);

            for (ListEx<String> row : rows) {
                final String coordinates = row.get(0);
                final double time = Double.parseDouble(row.get(1));

                final String[] split = coordinates.split(":");

                sqlite.statement(SQLStatements.INSERT_MODULE_TEST_TIME)
                        .param(coordinates)
                        .param(split[0])
                        .param(split[1])
                        .param(split[2])
                        .param(time)
                        .executeUpdate();
            }
        }
    }

    private String findParent(final Path path) {
        if (path == null || path.getFileName() == null) {
            return null;
        }

        if (path.getFileName().toString().endsWith("_output")) {
            return path.getFileName().toString();
        } else {
            return findParent(path.getParent());
        }
    }

    private void insertSubject(final String name, final String slug, final Path path) throws SQLException, IOException {
        System.out.println("[INFO] Inserting results for " + name + " (" + slug + ")");

        // If the subject does not already exist, insert it
        if (!sqlite.checkExists("subject", name)) {
            sqlite.statement(SQLStatements.INSERT_SUBJECT).param(name).param(slug).executeUpdate();
        }
    }

    private void insertTestRuns(final String name, final Path testRunResults) throws IOException, SQLException {
        if (!Files.isDirectory(testRunResults)) {
            return;
        }

        final ListEx<Path> paths = listFiles(testRunResults);

        final int limit = Math.min(maxTestRuns, paths.size());
        System.out.println("[INFO] Inserting test runs for " + name + " (" + paths.size() + " runs, saving " + limit + ")");

        for (int i = 0; i < limit; i++) {
            final Path p = paths.get(i);
            System.out.print("\r[INFO] Inserting run " + (i + 1) + " of " + paths.size());
            insertTestRunResult(name, new Gson().fromJson(FileUtil.readFile(p), TestRunResult.class));
        }

        System.out.println();
    }

    private void insertTestRunResult(final String name, final TestRunResult testRunResult) throws SQLException {
        if (testRunResult == null) {
            return;
        }

        if (sqlite.checkExists("test_run_result", testRunResult.id())) {
            return;
        }

        sqlite.statement(SQLStatements.INSERT_TEST_RUN_RESULT)
                .param(name)
                .param(testRunResult.id())
                .param(0)
                .executeUpdate();

        final Procedure statement = sqlite.statement(SQLStatements.INSERT_TEST_RESULT);

        statement.beginTransaction();

        final AtomicInteger count = new AtomicInteger();
        testRunResult.results().forEach((testName, testResult) -> {
            try {
                statement.param(testRunResult.id())
                        .param(testRunResult.testOrder().indexOf(testResult.name()))
                        .param(testResult.name())
                        .param((float) testResult.time())
                        .param(String.valueOf(testResult.result()))
                        .addBatch();
                count.incrementAndGet();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        statement.executeBatch();

        statement.commit();
        statement.endTransaction();

        sqlite.statement(SQLStatements.UPDATE_TEST_RUN_RESULT_COUNT)
                .param(count.get())
                .param(testRunResult.id())
                .executeUpdate();
    }

    private void insertDetectionResults(final String name, final String roundType, final Path path) throws IOException {
        final Path detectionResults = path.resolve(roundType);

        int i = 0;

        final Set<String> knownFlakyTests = new HashSet<>();
        final Set<String> addedRounds = new HashSet<>();

        if (Files.exists(detectionResults)) {
            final ListEx<Path> paths = listFiles(detectionResults);
            System.out.println("[INFO] Inserting " + roundType + " detection results for " + name
                    + " (" + paths.size() + " results)");

            for (i = 0; Files.exists(detectionResults.resolve("round" + i + ".json")); i++) {
                final Path p = detectionResults.resolve("round" + i  + ".json");
                final int roundNumber = roundNumber(p.getFileName().toString());

                try {
                    final DetectionRound round = new Gson().fromJson(FileUtil.readFile(p), DetectionRound.class);

                    if (round != null && round.unfilteredTests() != null && round.unfilteredTests().names() != null) {
                        knownFlakyTests.addAll(round.unfilteredTests().names());
                        addedRounds.addAll(round.testRunIds());
                    }

                    insertDetectionRound(name, roundType, roundNumber, round);
                } catch (IOException | SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (roundType.equals("original")) {
            insertOriginalResults(name, i, addedRounds, knownFlakyTests, path.getParent());
        }
    }

    private void insertOriginalResults(final String subjectName, final int prevRoundNum,
                                       final Set<String> addedRounds,
                                       final Set<String> knownFlakyTests, final Path path) throws IOException {
        final Path results = path.resolve(RunnerPathManager.TEST_RUNS);
        final Path originalOrderPath = path.resolve(DetectorPathManager.ORIGINAL_ORDER);

        if (!Files.exists(originalOrderPath)) {
            System.out.println("[WARNING] No original order for " + subjectName + " at " + originalOrderPath);
            return;
        }

        if (!Files.isDirectory(results)) {
            System.out.println("[WARNING] No directory " + results + " for " + subjectName);
            return;
        }

        System.out.println("[INFO] Trying to insert all original order runs from: " + path);

        final List<String> originalOrder = Files.readAllLines(originalOrderPath);

        final AtomicInteger counter = new AtomicInteger(prevRoundNum);

        final TestRunResult passing = passingRun(originalOrder);

        new TestRunParser(results).testRunResults()
            .forEach(trr -> {
                if (trr != null) {
                    if (trr.testOrder().equals(originalOrder) && !addedRounds.contains(trr.id())) {
                        System.out.println("Found an original order to try to insert: " + trr.id());
                        final List<DependentTest> result = DetectorUtil.flakyTests(passing, trr, true);
                        final DetectionRound dr = new DetectionRound(Collections.singletonList(trr.id()), result,
                                result.stream().filter(t -> !knownFlakyTests.contains(t.name())).collect(Collectors.toList()),
                                -1);
                        try {
                            insertDetectionRound(subjectName, "original", counter.incrementAndGet(), dr);
                        } catch (IOException | SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
    }

    // This method exists only for the purpose of creating an all passing run that can be passed
    // into the method to compare/generate the flaky tests
    private TestRunResult passingRun(final List<String> originalOrder) {
        final Map<String, TestResult> testOutcomes = new HashMap<>();

        for (final String testName : originalOrder) {
            testOutcomes.put(testName, new TestResult(testName, Result.PASS, -1, new StackTraceElement[0]));
        }

        return new TestRunResult("the id is unimportant and should never be referenced",
                originalOrder,
                testOutcomes);
    }

    private void insertDetectionRound(final String name, final String roundType,
                                       final int roundNumber, final DetectionRound round)
            throws IOException, SQLException {
        if (round == null) {
            return;
        }

        final int unfilteredId = insertDependentTestList(round.unfilteredTests());
        final int filteredId = insertDependentTestList(round.filteredTests());

        final int detectionRoundId =
                sqlite.statement(SQLStatements.INSERT_DETECTION_ROUND)
                .param(name)
                .param(unfilteredId)
                .param(filteredId)
                .param(roundType)
                .param(roundNumber)
                .param((float) round.roundTime())
                .insertSingleRow();

        // Might occur when using old results
        if (round.testRunIds() != null) {
            for (final String testRunId : round.testRunIds()) {
                sqlite.statement(SQLStatements.INSERT_DETECTION_ROUND_TEST_RUN)
                        .param(detectionRoundId)
                        .param(testRunId)
                        .executeUpdate();
            }
        }
    }

    private int insertDependentTestList(final DependentTestList dependentTestList) throws IOException, SQLException {
        final int index = dtListIndex;
        dtListIndex++;

        for (DependentTest dependentTest : dependentTestList.dts()) {
            final int dependentTestId = insertDependentTest(dependentTest);

            sqlite.statement(SQLStatements.INSERT_FLAKY_TEST_LIST)
                    .param(index)
                    .param(dependentTestId)
                    .executeUpdate();
        }

        return index;
    }

    private int insertDependentTest(final DependentTest dependentTest) throws SQLException {
        return sqlite.statement(SQLStatements.INSERT_FLAKY_TEST)
                .param(dependentTest.name())
                .param(dependentTest.intended().testRunId())
                .param(dependentTest.revealed().testRunId())
                .insertSingleRow();
    }

    private void insertVerificationResults(final String name, final String roundType, final Path basePath) throws IOException {
        final Path verificationResults = basePath.resolve(roundType);

        if (!Files.isDirectory(verificationResults)) {
            return;
        }

        final ListEx<Path> paths = listFiles(verificationResults);

        System.out.println("[INFO] Inserting " + roundType + " verification results for " + name + " (" + paths.size() + " rounds)");

        paths.forEach(p -> {
            try {
                insertVerificationRound(name, roundType, roundNumber(p.getFileName().toString()), p);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void insertVerificationRound(final String name, final String roundType, final int roundNumber, final Path p) throws IOException {
        listFiles(p).forEach(verificationStep -> {
            final String filename = verificationStep.getFileName().toString();
            final String[] split = filename.split("-");

            final String testName = split[0];
            final Result result = Result.valueOf(split[1]);
            final int verificationRoundNumber = roundNumber(split[2]);

            try {
                final TestRunResult testRunResult = new Gson().fromJson(FileUtil.readFile(verificationStep), TestRunResult.class);

                sqlite.statement(SQLStatements.INSERT_VERIFICATION_ROUND)
                        .param(name)
                        .param(roundNumber)
                        .param(testRunResult.id())
                        .param(roundType)
                        .param(verificationRoundNumber)
                        .param(testName)
                        .param(String.valueOf(result))
                        .param(String.valueOf(testRunResult.results().get(testName).result()))
                        .executeUpdate();

                insertTestRunResult(name, testRunResult);
            } catch (IOException | SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
