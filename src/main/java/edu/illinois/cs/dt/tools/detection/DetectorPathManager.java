package edu.illinois.cs.dt.tools.detection;

import edu.illinois.cs.dt.tools.utility.PathManager;

import java.nio.file.Path;
import java.nio.file.Paths;

public class DetectorPathManager extends PathManager {
    public static final Path DETECTION_RESULTS = Paths.get("detection-results");
    public static final Path FLAKY_LIST_PATH = Paths.get("flaky-lists.json");
    public static final Path ORIGINAL_ORDER = Paths.get("original-order");
    public static final Path ERROR = Paths.get("error");
    public static final Path ORIGINAL_RESULTS_LOG = Paths.get("original-results-ids");
    public static final Path MVN_TEST_LOG = Paths.get("mvn-test.log");
    public static final Path MVN_TEST_TIME_LOG = Paths.get("mvn-test-time.log");

    public static Path detectionResults() {
        return path(DETECTION_RESULTS);
    }

    public static Path detectionFile() {
        return detectionResults().resolve(FLAKY_LIST_PATH);
    }

    public static Path pathWithRound(final Path path, final String testName, final int round) {
        if (testName == null || testName.isEmpty()) {
            return path.resolve("round" + String.valueOf(round) + ".json");
        } else {
            return path.resolve(testName + "-round" + String.valueOf(round) + ".json");
        }
    }

    public static Path detectionRoundPath(final String name, final int round) {
        return pathWithRound(detectionResults().resolve(name), "", round);
    }

    public static Path filterPath(final String detectorType, final String filterType, final int absoluteRound) {
        return detectionRoundPath(detectorType + "-" + filterType, absoluteRound);
    }

    public static Path originalOrderPath() {
        return path(ORIGINAL_ORDER);
    }

    public static Path errorPath() {
        return path(ERROR);
    }

    public static Path originalResultsLog() {
        return detectionResults().resolve(ORIGINAL_RESULTS_LOG);
    }

    public static Path mvnTestLog() {
        return parentPath(MVN_TEST_LOG);
    }

    public static Path mvnTestTimeLog() {
        return parentPath(MVN_TEST_TIME_LOG);
    }
}
