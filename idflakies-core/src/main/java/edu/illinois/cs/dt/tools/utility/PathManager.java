package edu.illinois.cs.dt.tools.utility;

import com.google.common.base.Preconditions;

import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.data.results.Result;
import edu.illinois.cs.dt.tools.utility.Level;
import edu.illinois.cs.dt.tools.utility.Logger;

import org.apache.commons.io.FilenameUtils;
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
    public static final Path MINIMIZED = Paths.get("minimized");
    public static final Path FIXER = Paths.get("fixer");
    public static final Path TIME = Paths.get("time");
    public static final Path ERROR = Paths.get("error");
    public static final Path ORIGINAL_RESULTS_LOG = Paths.get("original-results-ids");
    public static final Path MVN_TEST_LOG = Paths.get("mvn-test.log");
    public static final Path MVN_TEST_TIME_LOG = Paths.get("mvn-test-time.log");

    public static final String BACKUP_EXTENSION = ".orig";
    public static final String PATCH_EXTENSION = ".patch";

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

    public static Path minimizedPath() {
        return getInstance().minimizedPathInstance();
    }

    public static Path minimizedPath(final Path relative) {
        return getInstance().minimizedPathInstance(relative);
    }

    public static Path minimizedPath(final String dependentTest, final String hash, final Result expected) {
        return getInstance().minimizedPathInstance(dependentTest, hash, expected);
    }

    public static Path fixerPath() {
        return getInstance().fixerPathInstance();
    }

    public static Path fixerPath(final Path relative) {
        return getInstance().fixerPathInstance(relative);
    }

    public static Path fixerPath(final String dependentTest) {
        return getInstance().fixerPathInstance(dependentTest);
    }

    public static Path compiledPath(final Path sourcePath) {
        return getInstance().compiledPathInstance(sourcePath);
    }

    public static Path backupPath(final Path path) {
        if (path.getParent() == null) {
            return Paths.get(path.getFileName().toString() + BACKUP_EXTENSION);
        } else {
            return path.getParent().resolve(path.getFileName().toString() + BACKUP_EXTENSION);
        }
    }

    public static Path changeExtension(final Path path, final String newExtension) {
        final String extToAdd = newExtension.startsWith(".") ? newExtension : "." + newExtension;

        return path.toAbsolutePath().getParent().resolve(FilenameUtils.removeExtension(path.getFileName().toString()) + extToAdd);
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

    protected abstract Path minimizedPathInstance();

    protected abstract Path minimizedPathInstance(final Path relative);

    protected abstract Path minimizedPathInstance(final String dependentTest, final String hash, final Result expected);

    protected abstract Path fixerPathInstance();

    protected abstract Path fixerPathInstance(final Path relative);

    protected abstract Path fixerPathInstance(final String dependentTest);

    protected abstract Path compiledPathInstance(final Path sourcePath);
}
