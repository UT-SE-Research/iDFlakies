package edu.illinois.cs.dt.tools.analysis;

import edu.illinois.cs.dt.tools.detection.DetectorPathManager;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public class ResultDirVisitor extends SimpleFileVisitor<Path> {
    private final List<Path> allResultsFolders;

    public ResultDirVisitor(final List<Path> allResultsFolders) {
        this.allResultsFolders = allResultsFolders;
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
        if (dir.getFileName().toString().equals("test-runs") || dir.getFileName().toString().equals("detection-results")) {
            return FileVisitResult.SKIP_SUBTREE;
        }

        if (containsResultsFolders(dir)) {
            allResultsFolders.add(dir);
        }

        return super.preVisitDirectory(dir, attrs);
    }

    private boolean containsResultsFolders(final Path p) {
        final Path detectionResults = p.resolve(DetectorPathManager.DETECTION_RESULTS);

        final boolean containsResults = Files.exists(detectionResults) || Files.exists(p.resolve("error"));

        if (!containsResults) {
            return false;
        }

        final Path parent = p.getParent();

        // TODO: This is probably no longer needed
        // We only want to run randomizeclasses, not both, because otherwise we'll try to insert some runs twice
        return !parent.getFileName().toString().equals("randomizemethods") ||
                !Files.exists(parent.resolveSibling("randomizeclasses"));
    }
}
