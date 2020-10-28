package edu.illinois.cs.dt.tools.runner.data;

import edu.illinois.cs.dt.tools.detection.DetectorPathManager;
import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.data.results.Result;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import edu.illinois.cs.testrunner.runner.Runner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class TestRun {
    private static final int VERIFY_ROUNDS = Configuration.config().getProperty("dt.verify.rounds", 1);

    private final List<String> order;
    private final Result result;
    private final String testRunId;

    public TestRun(final List<String> order, final Result result, final String testRunId) {
        this.order = order;
        this.result = result;
        this.testRunId = testRunId;
    }

    public List<String> order() {
        return order;
    }

    public Result result() {
        return result;
    }

    public String testRunId() {
        return testRunId;
    }

    public boolean verify(final String dt, final Runner runner, final Path path) {
        return IntStream.range(0, VERIFY_ROUNDS)
                .allMatch(i -> verifyRound(dt, runner, path, i));
    }

    private boolean verifyRound(final String dt, final Runner runner, final Path path, final int i) {
        Result newResult = null;
        try {
            final List<String> order = new ArrayList<>(this.order);
            if (!order.contains(dt)) {
                order.add(dt);
            }
            final TestRunResult results = runner.runList(order).get();

            newResult = results.results().get(dt).result();

            if (path != null) {
                final Path outputPath = DetectorPathManager.pathWithRound(path, dt + "-" + this.result, i);
                Files.createDirectories(outputPath.getParent());
                Files.write(outputPath, results.toString().getBytes());
            }
        } catch (Exception ignored) {}

        System.out.printf("Verified %s, status: expected %s, got %s\n",
                          dt, this.result, newResult);
        return this.result.equals(newResult);
    }
}
