package edu.illinois.cs.dt.tools.runner;

import com.google.gson.Gson;
import com.reedoei.eunomia.io.files.FileUtil;
import edu.illinois.cs.dt.tools.utility.PathManager;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class RunnerPathManager extends PathManager {
    public static final Path TEST_RUNS = Paths.get("test-runs");

    public static Path testRuns() {
        return path(TEST_RUNS);
    }

    public static Path outputPath() {
        return testRuns().resolve("output");
    }

    public static Path outputPath(final String id) {
        return outputPath().resolve(id);
    }

    public static Path outputPath(final TestRunResult run) {
        return outputPath(run.id());
    }

    public static Path resultsPath() {
        return testRuns().resolve("results");
    }

    public static Path resultsPath(final String id) {
        return resultsPath().resolve(id);
    }

    public static Path resultsPath(final TestRunResult run) {
        return resultsPath(run.id());
    }

    public static void outputResult(final Path tempOutput, final TestRunResult testRunResult) throws Exception {
        final Path outputPath = outputPath(testRunResult);
        final Path resultPath = resultsPath(testRunResult);

        Files.createDirectories(outputPath.getParent());
        Files.move(tempOutput, outputPath);

        Files.createDirectories(resultPath.getParent());
        Files.write(resultPath, testRunResult.toString().getBytes());
    }

    public static void clearTestRuns() throws IOException {
        FileUtils.deleteDirectory(testRuns().toFile());
    }

    public static Stream<TestRunResult> resultFor(final String trKey) {
        try {
            return Stream.of(new Gson().fromJson(FileUtil.readFile(resultsPath(trKey)), TestRunResult.class));
        } catch (IOException ignored) {}

        return Stream.empty();
    }
}
