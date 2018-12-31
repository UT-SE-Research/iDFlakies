package edu.illinois.cs.dt.tools.runner.data;

import com.reedoei.testrunner.configuration.Configuration;
import com.reedoei.testrunner.data.results.Result;
import com.reedoei.testrunner.data.results.TestRunResult;
import com.reedoei.testrunner.runner.Runner;
import edu.illinois.cs.dt.tools.detection.DetectorPathManager;
import edu.illinois.cs.dt.tools.minimizer.TestMinimizer;

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

    public boolean verify(final String name, final Runner runner, final Path path) {
        return verify(name, runner, null, path);
    }

    public boolean verify(final String dt, final Runner runner, final TestMinimizer minimizer, final Path path) {
        return IntStream.range(0, VERIFY_ROUNDS)
                .allMatch(i -> verifyRound(dt, runner, minimizer, path, i));
    }

    private boolean verifyRound(final String dt, final Runner runner, final TestMinimizer minimizer, final Path path, final int i) {
        System.out.printf("Verifying %s, status: expected %s", dt, this.result);
        Result result = null;
        try {
            final List<String> order = new ArrayList<>(this.order);
            if (!order.contains(dt)) {
                order.add(dt);
            }
            final TestRunResult results = runner.runList(order).get();

            result = results.results().get(dt).result();

            if (path != null) {
                final Path outputPath = DetectorPathManager.pathWithRound(path, dt + "-" + this.result, i);
                Files.createDirectories(outputPath.getParent());
                Files.write(outputPath, results.toString().getBytes());
            }
        } catch (Exception ignored) {}

        if (minimizer != null) {
            System.out.printf(", got %s, minimizer got %s\n", result, minimizer.expected());
            return this.result.equals(result) && this.result.equals(minimizer.expected());
        } else {
            System.out.printf(", got %s\n", result);
            return this.result.equals(result);
        }
    }
}
