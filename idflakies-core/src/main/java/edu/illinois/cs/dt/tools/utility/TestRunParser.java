package edu.illinois.cs.dt.tools.utility;

import com.google.gson.Gson;
import com.reedoei.eunomia.collections.ListEx;
import com.reedoei.eunomia.collections.PairStream;
import com.reedoei.eunomia.io.files.FileUtil;
import edu.illinois.cs.dt.tools.runner.RunnerPathManager;
import edu.illinois.cs.testrunner.data.results.TestRunResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestRunParser {
    // List files and close the directory streams
    private static ListEx<Path> listFiles(final Path path) throws IOException {
        final ListEx<Path> result = new ListEx<>();

        try (final Stream<Path> stream = Files.list(path)) {
            result.addAll(stream.collect(Collectors.toList()));
        }

        return result;
    }

    private final Path basePath;

    public TestRunParser(final Path basePath) {
        this.basePath = basePath;
    }

    public PairStream<String, TestRunResult> testResults() throws IOException {
        return testResults(Files.walk(basePath));
    }

    public Stream<TestRunResult> testRunResults() throws IOException {
        return Files.walk(basePath).filter(this::isTestRun).flatMap(this::testRunResult);
    }

    public PairStream<String, TestRunResult> testResults(final Stream<Path> paths) {
        return PairStream.fromStream(paths.filter(this::isTestRun), this::testRunOutput, this::testRunResult)
                .flatMap((outputs, trrs) -> PairStream.zip(outputs.iterator(), trrs.iterator()));
    }

    private Stream<TestRunResult> testRunResult(final Path path) {
        try {
            return listFiles(path.resolve("results")).stream()
                    .flatMap(FileUtil::safeReadFile)
                    .flatMap(s -> {
                        try {
                            return Stream.of(new Gson().fromJson(s, TestRunResult.class));
                        } catch (Exception e) {
                            return Stream.empty();
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Stream<String> testRunOutput(final Path path) {
        try {
            return listFiles(path.resolve("output")).stream()
                    .flatMap(FileUtil::safeReadFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isTestRun(final Path path) {
        final Path output = path.resolve("output");
        final Path results = path.resolve("results");

        return path.getFileName().toString().equals(RunnerPathManager.TEST_RUNS.getFileName().toString()) &&
               Files.isDirectory(output) &&
               Files.isDirectory(results);
    }
}
