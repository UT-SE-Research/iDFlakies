package edu.illinois.cs.dt.tools.detection;

import edu.illinois.cs.dt.tools.utility.PathManager;

import org.apache.maven.project.MavenProject;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MavenDetectorPathManager extends DetectorPathManager {

    private MavenProject mavenProject;
    
    public MavenDetectorPathManager(MavenProject mavenProject){
        this.mavenProject = mavenProject;
    }
    
    @Override
    public Path detectionResultsInstance() {
        return path(mavenProject.getBasedir(), DETECTION_RESULTS);
    }

    @Override
    public Path detectionFileInstance(final File baseDir) {
        return detectionResults().resolve(FLAKY_LIST_PATH);
    }

    @Override
    public Path pathWithRoundInstance(final Path path, final String testName, final int round) {
        if (testName == null || testName.isEmpty()) {
            return path.resolve("round" + String.valueOf(round) + ".json");
        } else {
            return path.resolve(testName + "-round" + String.valueOf(round) + ".json");
        }
    }

    @Override
    public Path detectionRoundPathInstance(final File baseDir, final String name, final int round) {
        return pathWithRound(detectionResults().resolve(name), "", round);
    }

    @Override
    public Path filterPathInstance(final File baseDir, final String detectorType, final String filterType, final int absoluteRound) {
        return detectionRoundPath(baseDir, detectorType + "-" + filterType, absoluteRound);
    }

    @Override
    public Path originalOrderPathInstance(final File baseDir) {
        return path(baseDir, ORIGINAL_ORDER);
    }

    @Override
    public Path errorPathInstance(final File baseDir) {
        return path(baseDir, ERROR);
    }

    @Override
    public Path originalResultsLogInstance(final File baseDir) {
        return detectionResults().resolve(ORIGINAL_RESULTS_LOG);
    }

    @Override
    public Path mvnTestLogInstance(final MavenProject mavenProject) {
        return parentPath(mavenProject, MVN_TEST_LOG);
    }

    @Override
    public Path mvnTestTimeLogInstance(final MavenProject mavenProject) {
        return parentPath(mavenProject, MVN_TEST_TIME_LOG);
    }
}
