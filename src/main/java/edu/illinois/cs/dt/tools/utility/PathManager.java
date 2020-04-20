package edu.illinois.cs.dt.tools.utility;

import com.google.common.base.Preconditions;
import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.mavenplugin.TestPluginPlugin;
import org.apache.maven.project.MavenProject;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathManager {
    private static final String outputPath = Configuration.config().getProperty("dt.cache.absolute.path", "");
    public static Path modulePath() {
        return TestPluginPlugin.mavenProject().getBasedir().toPath();
    }

    private static MavenProject getMavenProjectParent(MavenProject mavenProject) {
        MavenProject parentProj = mavenProject;
        while (parentProj.getParent() != null && parentProj.getParent().getBasedir() != null) {
            parentProj = parentProj.getParent();
        }
        return parentProj;
    }

    public static Path parentPath() {
        return getMavenProjectParent(TestPluginPlugin.mavenProject()).getBasedir().toPath();
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
	    Path outputPathObj = Paths.get(outputPath);
	    try {
		Files.createDirectories(outputPathObj);
	    } catch (IOException e) {
		System.out.println(e.getMessage());
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
