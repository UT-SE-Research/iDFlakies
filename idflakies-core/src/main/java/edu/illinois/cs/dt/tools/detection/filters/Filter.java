package edu.illinois.cs.dt.tools.detection.filters;

import edu.illinois.cs.dt.tools.runner.data.DependentTest;

public interface Filter {
    boolean keep(final DependentTest dependentTest, final int absoluteRound);
}
