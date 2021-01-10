package edu.illinois.cs.dt.tools.utility;

import com.google.common.base.Preconditions;
import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.coreplugin.TestPluginUtil;
import edu.illinois.cs.testrunner.util.ProjectWrapper;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathManager {
    private static final String outputPath = Configuration.config().getProperty("dt.cache.absolute.path", "");
    public static Path modulePath() {
        return TestPluginUtil.project.getBasedir().toPath();
    }

    private static ProjectWrapper getMavenProjectParent(ProjectWrapper project) {
        ProjectWrapper parentProj = project;
        while (parentProj.getParent() != null && parentProj.getParent().getBasedir() != null) {
            parentProj = parentProj.getParent();
        }
        return parentProj;
    }

    public static Path parentPath() {
        return getMavenProjectParent(TestPluginUtil.project).getBasedir().toPath();
    }

    public static Path parentPath(final Path relative) {
        Preconditions.checkState(!relative.isAbsolute(),
                "PathManager.parentPath(): Cache paths must be relative, not absolute (%s)", relative);

        return parentPath().resolve(relative);
    }

    public static Path cachePath() {
	if (outputPath == "") {
	    return modulePath().resolve(".dtfixingtools");
	} else {
	    TestPluginUtil.project.info("Accessing cachePath: " + outputPath);
	    Path outputPathObj = Paths.get(outputPath);
	    try {
		Files.createDirectories(outputPathObj);
	    } catch (IOException e) {
		TestPluginUtil.project.debug(e.getMessage());
	    }
	    return outputPathObj.resolve(modulePath().getFileName());
	}
    }

    public static Path path(final Path relative) {
        Preconditions.checkState(!relative.isAbsolute(),
                "PathManager.path(): Cache paths must be relative, not absolute (%s)", relative);

        return cachePath().resolve(relative);
    }
}
