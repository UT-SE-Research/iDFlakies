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
    private final Double time;
    private final String testRunId;

    public TestRun(final List<String> order, final Result result, final Double time, final String testRunId) {
        this.order = order;
        this.result = result;
        this.time = time;
        this.testRunId = testRunId;
    }

    public List<String> order() {
        return order;
    }

    public Result result() {
        return result;
    }

    public Double time() {
        return time;
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

    public boolean verifyTime(final String dt, final Runner runner, final Path path) {
        return IntStream.range(0, VERIFY_ROUNDS)
                .allMatch(i -> verifyRound(dt, runner, path, i));
    }

    private boolean verifyTimeRound(final String dt, final Runner runner, final Path path, final int i) {
        System.out.printf("Verifying time %s, status: expected %s", dt, this.result);
        Double time = -1.0;
        try {
            final List<String> order = new ArrayList<>(this.order);
            if (!order.contains(dt)) {
                order.add(dt);
            }
            final TestRunResult results = runner.runList(order).get();

            time = results.results().get(dt).time();

            if (path != null) {
                final Path outputPath = DetectorPathManager.pathWithRound(path, dt + "-" + this.result, i);
                Files.createDirectories(outputPath.getParent());
                Files.write(outputPath, results.toString().getBytes());
            }
        } catch (Exception ignored) {}

        System.out.printf(", got %d\n", time);
        return Math.abs(this.time - time) < (1 + 0.5 * this.time);  // Make sure that time does not fluctuate too much in verification
    }

    public boolean verifyTime(final String dt, final Runner runner, final Path path) {
        return IntStream.range(0, VERIFY_ROUNDS)
                .allMatch(i -> verifyRound(dt, runner, path, i));
    }

    private boolean verifyTimeRound(final String dt, final Runner runner, final Path path, final int i) {
        System.out.printf("Verifying time %s, status: expected %s", dt, this.result);
        Double time = -1.0;
        try {
            final List<String> order = new ArrayList<>(this.order);
            if (!order.contains(dt)) {
                order.add(dt);
            }
            final TestRunResult results = runner.runList(order).get();

            time = results.results().get(dt).time();

            if (path != null) {
                final Path outputPath = DetectorPathManager.pathWithRound(path, dt + "-" + this.result, i);
                Files.createDirectories(outputPath.getParent());
                Files.write(outputPath, results.toString().getBytes());
            }
        } catch (Exception ignored) {}

        System.out.printf(", got %d\n", time);
        return Math.abs(this.time - time) < (1 + 0.5 * this.time);  // Make sure that time does not fluctuate too much in verification
    }
}
