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
    public Path detectionFileInstance() {
        //delete baseDir here, remove baseDir from the overwritten method
        //look for all method references for both the static and instance versions of these,
        //ISOLATE FOR ALL DETECTORPATHMANAGER AND SUBCLASSES OF <-
        return detectionResultsInstance().resolve(FLAKY_LIST_PATH);
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
    public Path detectionRoundPathInstance(final String name, final int round) {
        return pathWithRoundInstance(detectionResultsInstance().resolve(name), "", round);
    }

    @Override
    public Path filterPathInstance(final String detectorType, final String filterType, final int absoluteRound) {
        return detectionRoundPathInstance(detectorType + "-" + filterType, absoluteRound);
    }

    @Override
    public Path originalOrderPathInstance() {
        return path(mavenProject.getBasedir(), ORIGINAL_ORDER);
    }

    @Override
    public Path errorPathInstance() {
        return path(mavenProject.getBasedir(), ERROR);
    }

    @Override
    public Path originalResultsLogInstance() {
        return detectionResultsInstance().resolve(ORIGINAL_RESULTS_LOG);
    }

    @Override
    public Path testLogInstance(final MavenProject mavenProject) {
        return parentPath(mavenProject, MVN_TEST_LOG);
    }

    @Override
    public Path testTimeLogInstance(final MavenProject mavenProject) {
        return parentPath(mavenProject, MVN_TEST_TIME_LOG);
    }
}
