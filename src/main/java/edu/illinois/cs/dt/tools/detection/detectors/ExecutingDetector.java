package edu.illinois.cs.dt.tools.detection.detectors;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Streams;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.reedoei.eunomia.io.VerbosePrinter;
import com.reedoei.eunomia.io.files.FileUtil;
import com.reedoei.eunomia.string.StringUtil;
import edu.illinois.cs.dt.tools.detection.DetectionRound;
import edu.illinois.cs.dt.tools.detection.DetectorPathManager;
import edu.illinois.cs.dt.tools.detection.DetectorUtil;
import edu.illinois.cs.dt.tools.detection.filters.Filter;
import edu.illinois.cs.dt.tools.runner.data.DependentTest;
import edu.illinois.cs.dt.tools.runner.data.DependentTestList;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import edu.illinois.cs.testrunner.runner.Runner;
import edu.illinois.cs.testrunner.configuration.Configuration;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ExecutingDetector implements Detector, VerbosePrinter {
    protected Runner runner;
    private boolean countOnlyFirstFailure = Boolean.parseBoolean(Configuration.config().getProperty("dt.detector.count.only.first.failure", "false"));

    protected int rounds;
    private List<Filter> filters = new ArrayList<>();
    protected final String name;
    protected final AtomicInteger absoluteRound = new AtomicInteger(0);

    private final Stopwatch stopwatch = Stopwatch.createUnstarted();

    public ExecutingDetector(final Runner runner, final int rounds, final String name) {
        this.runner = runner;
        this.rounds = rounds;
        this.name = name;
    }

    public abstract DetectionRound results() throws Exception;

    protected TestRunResult runList(final List<String> tests) {
        return runner.runList(tests).get();
    }

    public DetectionRound makeDts(final TestRunResult intended, final TestRunResult revealed) {
        final List<DependentTest> result = DetectorUtil.flakyTests(intended, revealed, countOnlyFirstFailure);

        return new DetectionRound(Collections.singletonList(revealed.id()),
                result,
                filter(result, absoluteRound.get()).collect(Collectors.toList()),
                stopwatch.elapsed(TimeUnit.NANOSECONDS) / 1E9);
    }

    public ExecutingDetector addFilter(final Filter filter) {
        filters.add(filter);

        return this;
    }

    @Override
    public Stream<DependentTest> detect() {
        return Streams.stream(new RunnerIterator());
    }

    private Stream<DependentTest> filter(List<DependentTest> dts, final int absoluteRound) {
        if (!dts.isEmpty()) {
            for (final Filter filter : filters) {
                dts = dts.stream().filter(t -> filter.keep(t, absoluteRound)).collect(Collectors.toList());
            }
        }

        return dts.stream();
    }

    @Override
    public void writeTo(final Path dir) throws IOException {
        FileUtil.makeDirectoryDestructive(dir);

        final Path listPath = dir.resolve("list.txt");
        final Path dtListPath = dir.resolve(DetectorPathManager.FLAKY_LIST_PATH);

        final DependentTestList dtList = new DependentTestList(detect());
        System.out.println(); // End the progress line.

        print(String.format("[INFO] Found %d tests, writing list to %s and dt lists to %s\n", dtList.size(), listPath, dtListPath));

        // *** keeping track of previous tests for next run ***
        System.out.println(DetectorPathManager.ORIGINAL_ORDER.toString());
        Scanner read = new Scanner(new File(".dtfixingtools/original-order"));
        read.useDelimiter("\n");
        List<String> tests = new ArrayList<>();
        while (read.hasNext())
        {
            tests.add(read.nextLine());
        }
        read.close();
        // write new test order to file
        Gson gson = new Gson();
        Type gsonType = new TypeToken<List>(){}.getType();
        String gsonString = gson.toJson(tests,gsonType);
        Files.write(DetectorPathManager.PREVIOUS_TESTS, gsonString.getBytes());

        Files.write(dtListPath, dtList.toString().getBytes());
        Files.write(listPath, StringUtil.unlines(dtList.names()).getBytes());
    }

    private class RunnerIterator implements Iterator<DependentTest> {
        private final long origStartTimeMs = System.currentTimeMillis();
        private long startTimeMs = System.currentTimeMillis();
        private long previousStopTimeMs = System.currentTimeMillis();
	private boolean roundsAreTotal = Boolean.parseBoolean(Configuration.config().getProperty("dt.detector.roundsemantics.total", "false"));

        private int i = 0;

        private final List<DependentTest> result = new ArrayList<>();

        @Override
        public boolean hasNext() {
            while (i < rounds && result.isEmpty()) {
                generate();
            }

            return !result.isEmpty();
        }

        private DetectionRound generateDetectionRound() {
            final Path path = DetectorPathManager.detectionRoundPath(name, absoluteRound.get());

            // Load it if possible
//            try {
//                if (Files.exists(path)) {
//                    return new Gson().fromJson(FileUtil.readFile(path), DetectionRound.class);
//                }
//            } catch (IOException ignored) {}

            // Otherwise run the detection round
            final long stopTime = System.currentTimeMillis();

            try {
                stopwatch.reset().start();
                final DetectionRound result = results();
                stopwatch.stop();

                Files.createDirectories(path.getParent());
                Files.write(path, result.toString().getBytes());

                previousStopTimeMs = stopTime;

                return result;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void generate() {
            final DetectionRound round = generateDetectionRound();

            final double elapsed = previousStopTimeMs - startTimeMs;
            final double totalElapsed = (System.currentTimeMillis() - origStartTimeMs) / 1000.0;
            final double estimate = elapsed / (i + 1) * (rounds - i - 1) / 1000;

            if (!round.filteredTests().dts().isEmpty()) {
                System.out.print(String.format("\r[INFO] Found %d tests in round %d of %d (%.1f seconds elapsed (%.1f total), %.1f seconds remaining).\n",
                        round.filteredTests().size(), ++i, rounds, elapsed / 1000, totalElapsed, estimate));
                result.addAll(round.filteredTests().dts());
		if (!roundsAreTotal){
		    i = 0;
		}
                startTimeMs = System.currentTimeMillis();
            } else {
		if (roundsAreTotal)
		    System.out.print(String.format("\r[INFO] Found %d tests in round %d of %d (%.1f seconds elapsed (%.1f total), %.1f seconds remaining).\n",
						   round.filteredTests().size(), ++i, rounds, elapsed / 1000, totalElapsed, estimate));
		else
		    System.out.print(String.format("\r[INFO] Found %d tests in round %d of %d (%.1f seconds elapsed (%.1f total), %.1f seconds remaining).",
						   round.filteredTests().size(), ++i, rounds, elapsed / 1000, totalElapsed, estimate));
            }

            absoluteRound.incrementAndGet();
        }

        @Override
        public DependentTest next() {
            if (hasNext()) {
                return result.remove(0);
            } else {
                return null;
            }
        }
    }
}
