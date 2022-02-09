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

    public static Path detectionFile(final File baseDir) {
        return getInstance().detectionFileInstance(baseDir);
    }

    public static Path pathWithRound(final Path path, final String testName, final int round) {
        return getInstance().pathWithRoundInstance(path, testName, round);
    }

    public static Path detectionRoundPath(final File baseDir, final String name, final int round) {
        return getInstance().detectionRoundPathInstance(baseDir, name, round);
    }

    public static Path filterPath(final File baseDir, final String detectorType, final String filterType, final int absoluteRound) {
        return getInstance().filterPathInstance(baseDir, detectorType, filterType, absoluteRound);
    }

    public static Path originalOrderPath(final File baseDir) {
        return getInstance().originalOrderPathInstance(baseDir);
    }

    public static Path errorPath(final File baseDir) {
        return getInstance().errorPathInstance(baseDir);
    }

    public static Path originalResultsLog(final File baseDir) {
        return getInstance().originalResultsLogInstance(baseDir);
    }

    public static Path mvnTestLog(final MavenProject mavenProject) {
        return getInstance().mvnTestLogInstance(mavenProject);
    }

    public static Path mvnTestTimeLog(final MavenProject mavenProject) {
        return getInstance().mvnTestTimeLogInstance(mavenProject);
    }
    
    public abstract Path detectionResultsInstance();

    public abstract Path detectionFileInstance(final File baseDir);

    public abstract Path pathWithRoundInstance(final Path path, final String testName, final int round);

    public abstract Path detectionRoundPathInstance(final File baseDir, final String name, final int round);

    public abstract Path filterPathInstance(final File baseDir, final String detectorType, final String filterType, final int absoluteRound);

    public abstract Path originalOrderPathInstance(final File baseDir);

    public abstract Path errorPathInstance(final File baseDir);

    public abstract Path originalResultsLogInstance(final File baseDir);

    public abstract Path mvnTestLogInstance(final MavenProject mavenProject);

    public abstract Path mvnTestTimeLogInstance(final MavenProject mavenProject);
}
