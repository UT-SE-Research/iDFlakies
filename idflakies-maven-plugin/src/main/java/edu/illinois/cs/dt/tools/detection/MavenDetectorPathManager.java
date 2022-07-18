package edu.illinois.cs.dt.tools.detection;

import com.google.common.base.Preconditions;

import edu.illinois.cs.dt.tools.utility.Level;
import edu.illinois.cs.dt.tools.utility.Logger;
import edu.illinois.cs.dt.tools.utility.PathManager;
import edu.illinois.cs.testrunner.configuration.Configuration;

import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MavenDetectorPathManager extends PathManager {

    private MavenProject mavenProject;
    
    public MavenDetectorPathManager(MavenProject mavenProject){
        this.mavenProject = mavenProject;
    }
    
    @Override
    public Path detectionResultsInstance() {
        return pathInstance(PathManager.DETECTION_RESULTS);
    }

    @Override
    public Path detectionFileInstance() {
        return detectionResultsInstance().resolve(PathManager.FLAKY_LIST_PATH);
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
	String originalOrderPath = Configuration.config().properties().getProperty("dt.original.order", "");
	Path originalOrderPathObj;
        if (originalOrderPath == "") {
	    originalOrderPathObj = pathInstance(PathManager.ORIGINAL_ORDER);
        } else {
            originalOrderPathObj = Paths.get(originalOrderPath);
        }
	Logger.getGlobal().log(Level.INFO, "Using original order in path: " + originalOrderPathObj);
	return originalOrderPathObj;
    }

    @Override
    public Path selectedTestPathInstance() { return pathInstance(PathManager.SELECTED_TESTS); }

    @Override
    public Path timePathInstance() { return pathInstance(PathManager.TIME); }

    @Override
    public Path errorPathInstance() {
        return pathInstance(PathManager.ERROR);
    }

    @Override
    public Path originalResultsLogInstance() {
        return detectionResultsInstance().resolve(PathManager.ORIGINAL_RESULTS_LOG);
    }

    @Override
    public Path testLogInstance() {
        return parentPath(PathManager.MVN_TEST_LOG);
    }

    @Override
    public Path testTimeLogInstance() {
        return parentPath(PathManager.MVN_TEST_TIME_LOG);
    }

    @Override
    public Path cachePathInstance() {
        String outputPath = Configuration.config().properties().getProperty("dt.cache.absolute.path", "");
        Logger.getGlobal().log(Level.FINE, "Accessing cachePath: " + outputPath);
        if (outputPath == "") {
            return modulePath().resolve(".dtfixingtools");
        } else {
            Path outputPathObj = Paths.get(outputPath);
            try {
                Files.createDirectories(outputPathObj);
            } catch (IOException e) {
                Logger.getGlobal().log(Level.FINE, e.getMessage());
            }
            return outputPathObj.resolve(modulePath().getFileName());
        }
    }

    @Override
    public Path startsPathInstance() {
        String outputPath = Configuration.config().properties().getProperty("dt.incdetector.startspath", "");
        Logger.getGlobal().log(Level.FINE, "Accessing relative startspath: " + outputPath);
        if (outputPath == "") {
            return modulePath().resolve(".starts");
        } else {
            // access relative paths; in case of multi-module projects
            return modulePath().resolve(outputPath);
        }
    }

    @Override
    public Path ekstaziPathInstance() {
        String outputPath = Configuration.config().properties().getProperty("dt.incdetector.ekstazipath", "");
        Logger.getGlobal().log(Level.FINE, "Accessing relative ekstazipath: " + outputPath);
        if (outputPath == "") {
            return modulePath().resolve(".ekstazi");
        } else {
            // access relative paths; in case of multi-module projects
            return modulePath().resolve(outputPath);
        }
    }

    @Override
    public Path pathInstance(final Path relative) {
        Preconditions.checkState(!relative.isAbsolute(),
                "PathManager.path(): Cache paths must be relative, not absolute (%s)", relative);

        return cachePathInstance().resolve(relative);
    }

    @Override
    public Path modulePathInstance() {
        return mavenProject.getBasedir().toPath();
    }

    @Override
    protected Path parentPath() {
        return getMavenProjectParent(mavenProject).getBasedir().toPath();
    }

    @Override
    protected Path parentPath(final Path relative) {
        Preconditions.checkState(!relative.isAbsolute(),
                "PathManager.parentPath(): Cache paths must be relative, not absolute (%s)", relative);

        return parentPath().resolve(relative);
    }

    public static MavenProject getMavenProjectParent(MavenProject project) {
        MavenProject parentProj = project;
        while (parentProj.getParent() != null && parentProj.getParent().getBasedir() != null) {
            parentProj = parentProj.getParent();
        }
        return parentProj;
    }

}
