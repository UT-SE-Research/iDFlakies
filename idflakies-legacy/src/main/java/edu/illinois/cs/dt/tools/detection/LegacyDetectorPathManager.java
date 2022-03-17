package edu.illinois.cs.dt.tools.detection;

import com.google.common.base.Preconditions;

import edu.illinois.cs.dt.tools.utility.Level;
import edu.illinois.cs.dt.tools.utility.Logger;
import edu.illinois.cs.dt.tools.utility.PathManager;

import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.coreplugin.TestPluginUtil;
import edu.illinois.cs.testrunner.util.ProjectWrapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LegacyDetectorPathManager extends PathManager {
    
    @Override
    public Path detectionResultsInstance() {
        return path(DETECTION_RESULTS);
    }

    @Override
    public Path detectionFileInstance() {
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
        return path(ORIGINAL_ORDER);
    }

    @Override
    public Path errorPathInstance() {
        return path(ERROR);
    }

    @Override
    public Path originalResultsLogInstance() {
        return detectionResultsInstance().resolve(ORIGINAL_RESULTS_LOG);
    }

    @Override
    public Path testLogInstance() {
        return parentPath(MVN_TEST_LOG);
    }

    @Override
    public Path testTimeLogInstance() {
        return parentPath(MVN_TEST_TIME_LOG);
    }

    @Override
    public Path cachePathInstance() {
        String outputPath = Configuration.config().properties().getProperty("dt.cache.absolute.path", "");
        Logger.getGlobal().log(Level.INFO, "Accessing cachePath: " + outputPath);
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
    public Path modulePathInstance() {
        return TestPluginUtil.project.getBasedir().toPath();
    }

    @Override
    protected Path parentPath() {   //get rid of mvnproj argument here use it according to plugin
        return getProjectParent(TestPluginUtil.project).getBasedir().toPath();
    }

    @Override
    protected Path parentPath(final Path relative) {
        Preconditions.checkState(!relative.isAbsolute(),
                "PathManager.parentPath(): Cache paths must be relative, not absolute (%s)", relative);

        return parentPath().resolve(relative);
    }

    @Override
    public Path pathInstance(final Path relative) {
        Preconditions.checkState(!relative.isAbsolute(),
                "PathManager.path(): Cache paths must be relative, not absolute (%s)", relative);

        return cachePath().resolve(relative);
    }

    public static ProjectWrapper getProjectParent(ProjectWrapper project) {
        ProjectWrapper parentProj = project;
        while (parentProj.getParent() != null && parentProj.getParent().getBasedir() != null) {
            parentProj = parentProj.getParent();
        }
        return parentProj;
    }    

    
}
