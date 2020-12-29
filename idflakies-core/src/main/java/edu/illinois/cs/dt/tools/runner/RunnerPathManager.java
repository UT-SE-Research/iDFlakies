package edu.illinois.cs.dt.tools.runner;

import com.google.gson.Gson;
import com.reedoei.eunomia.io.files.FileUtil;
import edu.illinois.cs.dt.tools.utility.PathManager;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class RunnerPathManager extends PathManager {
    public static final Path TEST_RUNS = Paths.get("test-runs");

    public static Path testRuns(final File baseDir) {
        return path(baseDir, TEST_RUNS);
    }

    public static Path outputPath(final File baseDir) {
        return testRuns(baseDir).resolve("output");
    }

    public static Path outputPath(final File baseDir, final String id) {
        return outputPath(baseDir).resolve(id);
    }

    public static Path outputPath(final File baseDir, final TestRunResult run) {
        return outputPath(baseDir, run.id());
    }

    public static Path resultsPath(final File baseDir) {
        return testRuns(baseDir).resolve("results");
    }

    public static Path resultsPath(final File baseDir, final String id) {
        return resultsPath(baseDir).resolve(id);
    }

    public static Path resultsPath(final File baseDir, final TestRunResult run) {
        return resultsPath(baseDir, run.id());
    }

    public static void outputResult(final Path tempOutput, final File baseDir, final TestRunResult testRunResult) throws Exception {
        final Path outputPath = outputPath(baseDir, testRunResult);
        final Path resultPath = resultsPath(baseDir, testRunResult);

        Files.createDirectories(outputPath.getParent());
        Files.move(tempOutput, outputPath);

        Files.createDirectories(resultPath.getParent());
        Files.write(resultPath, testRunResult.toString().getBytes());
    }

    public static void clearTestRuns(final File baseDir) throws IOException {
        FileUtils.deleteDirectory(testRuns(baseDir).toFile());
    }

    public static Stream<TestRunResult> resultFor(final File baseDir, final String trKey) {
        try {
            return Stream.of(new Gson().fromJson(FileUtil.readFile(resultsPath(baseDir, trKey)), TestRunResult.class));
        } catch (IOException ignored) {}

        return Stream.empty();
    }
}
