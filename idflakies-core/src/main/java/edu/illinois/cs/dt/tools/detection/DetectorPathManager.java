package edu.illinois.cs.dt.tools.detection;

import edu.illinois.cs.dt.tools.utility.PathManager;

import org.apache.maven.project.MavenProject;

import java.io.File;
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

    public static Path detectionResults(final File baseDir) {
        return path(baseDir, DETECTION_RESULTS);
    }

    public static Path detectionFile(final File baseDir) {
        return detectionResults(baseDir).resolve(FLAKY_LIST_PATH);
    }

    public static Path pathWithRound(final Path path, final String testName, final int round) {
        if (testName == null || testName.isEmpty()) {
            return path.resolve("round" + String.valueOf(round) + ".json");
        } else {
            return path.resolve(testName + "-round" + String.valueOf(round) + ".json");
        }
    }

    public static Path detectionRoundPath(final File baseDir, final String name, final int round) {
        return pathWithRound(detectionResults(baseDir).resolve(name), "", round);
    }

    public static Path filterPath(final File baseDir, final String detectorType, final String filterType, final int absoluteRound) {
        return detectionRoundPath(baseDir, detectorType + "-" + filterType, absoluteRound);
    }

    public static Path originalOrderPath(final File baseDir) {
        return path(baseDir, ORIGINAL_ORDER);
    }

    public static Path errorPath(final File baseDir) {
        return path(baseDir, ERROR);
    }

    public static Path originalResultsLog(final File baseDir) {
        return detectionResults(baseDir).resolve(ORIGINAL_RESULTS_LOG);
    }

    public static Path mvnTestLog(final MavenProject mavenProject) {
        return parentPath(mavenProject, MVN_TEST_LOG);
    }

    public static Path mvnTestTimeLog(final MavenProject mavenProject) {
        return parentPath(mavenProject, MVN_TEST_TIME_LOG);
    }
}
