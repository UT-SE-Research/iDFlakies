package edu.illinois.cs.dt.tools.detection;

import edu.illinois.cs.dt.tools.utility.PathManager;

import org.apache.maven.project.MavenProject;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class DetectorPathManager extends PathManager {
    private static DetectorPathManager instance;
    
    public static final Path DETECTION_RESULTS = Paths.get("detection-results");
    public static final Path FLAKY_LIST_PATH = Paths.get("flaky-lists.json");
    public static final Path ORIGINAL_ORDER = Paths.get("original-order");
    public static final Path ERROR = Paths.get("error");
    public static final Path ORIGINAL_RESULTS_LOG = Paths.get("original-results-ids");
    public static final Path MVN_TEST_LOG = Paths.get("mvn-test.log");
    public static final Path MVN_TEST_TIME_LOG = Paths.get("mvn-test-time.log");

    public static DetectorPathManager getInstance() { return instance; }
    
    public static void setInstance(DetectorPathManager dm) { instance = dm; }
    
    public static Path detectionResults() {
        return getInstance().detectionResultsInstance();
    }

    public static Path detectionFile() {
        return getInstance().detectionFileInstance();
    }

    public static Path pathWithRound(final Path path, final String testName, final int round) {
        return getInstance().pathWithRoundInstance(path, testName, round);
    }

    public static Path detectionRoundPath(final String name, final int round) {
        return getInstance().detectionRoundPathInstance(name, round);
    }

    public static Path filterPath(final String detectorType, final String filterType, final int absoluteRound) {
        return getInstance().filterPathInstance(detectorType, filterType, absoluteRound);
    }

    public static Path originalOrderPath() {
        return getInstance().originalOrderPathInstance();
    }

    public static Path errorPath() {
        return getInstance().errorPathInstance();
    }

    public static Path originalResultsLog() {
        return getInstance().originalResultsLogInstance();
    }

    public static Path testLog() {
        return getInstance().testLogInstance();
    }

    public static Path testTimeLog() {
        return getInstance().testTimeLogInstance();
    }
    
    public abstract Path detectionResultsInstance();

    public abstract Path detectionFileInstance();

    public abstract Path pathWithRoundInstance(final Path path, final String testName, final int round);

    public abstract Path detectionRoundPathInstance(final String name, final int round);

    public abstract Path filterPathInstance(final String detectorType, final String filterType, final int absoluteRound);

    public abstract Path originalOrderPathInstance();

    public abstract Path errorPathInstance();

    public abstract Path originalResultsLogInstance();

    public abstract Path testLogInstance();

    public abstract Path testTimeLogInstance();
}
