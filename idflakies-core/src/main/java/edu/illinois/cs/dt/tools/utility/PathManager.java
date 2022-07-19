package edu.illinois.cs.dt.tools.utility;

import com.google.common.base.Preconditions;
import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.dt.tools.utility.Level;
import edu.illinois.cs.dt.tools.utility.Logger;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class PathManager {

    private static PathManager instance;
    
    public static final Path DETECTION_RESULTS = Paths.get("detection-results");
    public static final Path FLAKY_LIST_PATH = Paths.get("flaky-lists.json");
    public static final Path ORIGINAL_ORDER = Paths.get("original-order");
    public static final Path SELECTED_TESTS = Paths.get("selected-tests");
    public static final Path TIME = Paths.get("time");
    public static final Path ERROR = Paths.get("error");
    public static final Path ORIGINAL_RESULTS_LOG = Paths.get("original-results-ids");
    public static final Path MVN_TEST_LOG = Paths.get("mvn-test.log");
    public static final Path MVN_TEST_TIME_LOG = Paths.get("mvn-test-time.log");

    public static PathManager getInstance() { return instance; }
    
    public static void setInstance(PathManager dm) { instance = dm; }
    
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

    public static Path selectedTestPath() { return getInstance().selectedTestPathInstance(); }

    public static Path timePath() { return getInstance().timePathInstance(); }

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
    
    public static Path modulePath() {
	    return getInstance().modulePathInstance();
    }

    public static Path cachePath() {
        return getInstance().cachePathInstance();
    }

    public static Path startsPath() {
        return getInstance().startsPathInstance();
    }

    public static Path ekstaziPath() {
        return getInstance().ekstaziPathInstance();
    }

    public static Path path(final Path relative) {
        return getInstance().pathInstance(relative);
    }

    protected abstract Path detectionResultsInstance();

    protected abstract Path detectionFileInstance();

    protected abstract Path pathWithRoundInstance(final Path path, final String testName, final int round);

    protected abstract Path detectionRoundPathInstance(final String name, final int round);

    protected abstract Path filterPathInstance(final String detectorType, final String filterType, final int absoluteRound);

    protected abstract Path originalOrderPathInstance();

    protected abstract Path selectedTestPathInstance();

    protected abstract Path timePathInstance();

    protected abstract Path errorPathInstance();

    protected abstract Path originalResultsLogInstance();

    protected abstract Path testLogInstance();

    protected abstract Path testTimeLogInstance();

    protected abstract Path parentPath();
    
    protected abstract Path parentPath(final Path relative);
    
    protected abstract Path cachePathInstance();

    protected abstract Path startsPathInstance();

    protected abstract Path ekstaziPathInstance();
    
    protected abstract Path pathInstance(final Path relative);
    
    protected abstract Path modulePathInstance();
    
}
