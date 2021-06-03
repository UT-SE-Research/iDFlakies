package edu.illinois.cs.dt.tools.utility;

import com.google.common.base.Preconditions;
import edu.illinois.cs.dt.tools.utility.Level;
import edu.illinois.cs.dt.tools.utility.Logger;
import edu.illinois.cs.testrunner.configuration.Configuration;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathManager {
    private static final String outputPath = System.getProperty("dt.cache.absolute.path", "");

    public static Path modulePath(final File baseDir) {
        return baseDir.toPath();
    }

    private static MavenProject getMavenProjectParent(final MavenProject mavenProject) {
        MavenProject parentProj = mavenProject;
        while (parentProj.getParent() != null && parentProj.getParent().getBasedir() != null) {
            parentProj = parentProj.getParent();
        }
        return parentProj;
    }

    public static Path parentPath(final MavenProject mavenProject) {
        return getMavenProjectParent(mavenProject).getBasedir().toPath();
    }

    public static Path parentPath(final MavenProject mavenProject, final Path relative) {
        Preconditions.checkState(!relative.isAbsolute(),
                "PathManager.parentPath(): Cache paths must be relative, not absolute (%s)", relative);

        return parentPath(mavenProject).resolve(relative);
    }

    public static Path cachePath(final File baseDir) {
        Logger.getGlobal().log(Level.INFO, "Accessing cachePath: " + outputPath);
        if (outputPath == "") {
            return modulePath(baseDir).resolve(".dtfixingtools");
        } else {
            Path outputPathObj = Paths.get(outputPath);
            try {
                Files.createDirectories(outputPathObj);
            } catch (IOException e) {
                Logger.getGlobal().log(Level.FINE, e.getMessage());
            }
            return outputPathObj.resolve(modulePath(baseDir).getFileName());
        }
    }

    public static Path path(final File baseDir, final Path relative) {
        Preconditions.checkState(!relative.isAbsolute(),
                "PathManager.path(): Cache paths must be relative, not absolute (%s)", relative);

        return cachePath(baseDir).resolve(relative);
    }
}
