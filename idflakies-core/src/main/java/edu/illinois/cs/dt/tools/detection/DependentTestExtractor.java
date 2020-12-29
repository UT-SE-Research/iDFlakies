package edu.illinois.cs.dt.tools.detection;

import com.google.gson.Gson;
import com.reedoei.eunomia.collections.ListEx;
import com.reedoei.eunomia.io.files.FileUtil;
import com.reedoei.eunomia.util.StandardMain;
import edu.illinois.cs.dt.tools.analysis.ResultDirVisitor;
import edu.illinois.cs.dt.tools.detection.classifiers.DependentClassifier;
import edu.illinois.cs.dt.tools.detection.classifiers.NonorderClassifier;
import edu.illinois.cs.dt.tools.runner.data.DependentTest;
import edu.illinois.cs.dt.tools.runner.data.DependentTestList;
import edu.illinois.cs.dt.tools.runner.data.TestRun;
import edu.illinois.cs.dt.tools.utility.MD5;
import edu.illinois.cs.dt.tools.utility.TestRunParser;
import edu.illinois.cs.testrunner.data.results.Result;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import org.apache.commons.lang3.StringUtils;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class DependentTestExtractor extends StandardMain {
    private final Path results;

    private final Path outputPath;

    private DependentTestExtractor(final String[] args) {
        super(args);

        results = Paths.get(getArgRequired("results"));
        outputPath = Paths.get(getArg("output").orElse("output"));
    }

    public static void main(final String[] args) {
        try {
            new DependentTestExtractor(args).run();
        } catch (Exception e) {
            e.printStackTrace();

            System.exit(1);
        }

        System.exit(0);
    }

    @Override
    protected void run() throws Exception {
        final ListEx<Path> allResultsFolders = new ListEx<>();
        Files.walkFileTree(results, new ResultDirVisitor(allResultsFolders));

        for (int i = 0; i < allResultsFolders.size(); i++) {
            final Path resultsFolder = allResultsFolders.get(i);
            System.out.println("[INFO] Extracting results from " + resultsFolder + " (" + i + " of " + allResultsFolders.size() + ")");
            final Optional<String> subjectNameOpt = readProjectName(resultsFolder);

            final String subjectName;
            if (subjectNameOpt.isPresent()) {
                subjectName = subjectNameOpt.get();
            } else {
                System.out.println("[WARNING] No subject.properties in " + resultsFolder);
                subjectName = StringUtils.strip(resultsFolder.getFileName().toString().replace("/", "-"), "-");
            }

            if (!Files.exists(outputFilePath(subjectName))) {
                save(subjectName, extract(subjectName, resultsFolder));
            }

            System.out.println();
        }
    }

    private Path outputFilePath(final String subjectName) {
        return outputPath.resolve(subjectName + "-" + DetectorPathManager.FLAKY_LIST_PATH.getFileName());
    }

    private Optional<String> readProjectName(final Path resultsFolder) {
        final Path propertiesPath = resultsFolder.resolve("subject.properties");

        if (Files.exists(propertiesPath)) {
            final Properties properties = new Properties();
            try (final FileInputStream fis = new FileInputStream(propertiesPath.toFile())) {
                properties.load(fis);
            } catch (IOException e) {
                return Optional.empty();
            }

            return Optional.of(properties.getProperty("subject.name"));
        }

        return Optional.empty();
    }

    private boolean isNew(final DependentTestList dependentTestList, final DependentTest dependentTest) {
        final BiPredicate<TestRun, TestRun> pred =
                (a, b) -> MD5.hashOrder(a.order()).equals(MD5.hashOrder(b.order()));

        return dependentTestList.dts().stream()
                .anyMatch(dt -> !pred.test(dt.intended(), dependentTest.intended()) ||
                                !pred.test(dt.revealed(), dependentTest.revealed())) ||
               dependentTestList.dts().stream()
                .noneMatch(dt -> dt.name().equals(dependentTest.name()));
    }

    private void save(final String subjectName, final DependentTestList extracted) throws IOException {
        final Path outputFile = outputFilePath(subjectName);

        if (Files.exists(outputFile)) {
            try {
                final DependentTestList l = new Gson().fromJson(FileUtil.readFile(outputFile), DependentTestList.class);

                if (l != null) {
                    for (final DependentTest dependentTest : l.dts()) {
                        if (isNew(extracted, dependentTest)) {
                            extracted.dts().add(dependentTest);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        System.out.println("[INFO] Writing dt list to (" + extracted.size() + " tests) to: " + outputFile);

        try (final FileWriter writer = new FileWriter(outputFile.toFile())) {
            new Gson().toJson(extracted, writer);
        }
    }

    public DependentTestList extract(final String subjectName, final Path path) throws IOException {
        return dependentTests(subjectName, new TestRunParser(path).testRunResults().collect(Collectors.toList()));
    }

    private DependentTestList dependentTests(final String subjectName, final List<TestRunResult> results) {
        try (final NonorderClassifier nonorderClassifier = new NonorderClassifier();
             final DependentClassifier dependentClassifier = new DependentClassifier(false)) { // TODO: Create a setting to control this
            for (int i = 0; i < results.size(); i++) {
                final TestRunResult testRunResult = results.get(i);
                System.out.printf("\rUpdating classifiers with test run %s of %s (no: %d, od: %d): %s",
                        i + 1,
                        results.size(),
                        nonorderClassifier.nonorderTests().size(),
                        dependentClassifier.dependentTests(nonorderClassifier.nonorderTests()).size(),
                        testRunResult.id());
                nonorderClassifier.update(testRunResult);
                dependentClassifier.update(testRunResult);
            }

            if (!results.isEmpty()) {
                System.out.println();
            }
            System.out.println("Finished updating classifiers.");

            final Path outputFile = outputPath.resolve(subjectName + "-not.txt");
            Files.write(outputFile, nonorderClassifier.nonorderTests());

            return new DependentTestList(makeDependentTestList(nonorderClassifier, dependentClassifier));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return DependentTestList.empty();
    }

    private List<DependentTest> makeDependentTestList(final NonorderClassifier nonorderClassifier,
                                                      final DependentClassifier dependentClassifier) {
        System.out.println("Making dependent test list.");
        final Set<String> nonorderTests = nonorderClassifier.nonorderTests();

        final List<DependentTest> dependentTests = new ArrayList<>();

        dependentClassifier.dependentRuns().forEach((testName, testRuns) -> {
            if (nonorderTests.contains(testName)) {
                return;
            }

            testRuns.stream().filter(tr -> tr.result().equals(Result.PASS)).findFirst().ifPresent(passingRun -> {
                for (final TestRun testRun : testRuns) {
                    if (!testRun.result().equals(Result.PASS)) {
                        System.out.println("Creating dependent test entry (expected: " + testRun.result() + ")");
                        dependentTests.add(new DependentTest(testName, passingRun, testRun));
                        break;
                    }
                }
            });
        });

        return dependentTests;
    }
}
