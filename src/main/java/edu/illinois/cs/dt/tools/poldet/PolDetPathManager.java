package edu.illinois.cs.dt.tools.poldet;

import edu.illinois.cs.dt.tools.utility.PathManager;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PolDetPathManager extends PathManager {
    public static final Path POLDET_RESULTS = Paths.get("poldet-results");

    public static Path poldetResults() {
        return path(POLDET_RESULTS);
    }
}
