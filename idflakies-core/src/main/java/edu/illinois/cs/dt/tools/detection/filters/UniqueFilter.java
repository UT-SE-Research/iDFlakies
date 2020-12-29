package edu.illinois.cs.dt.tools.detection.filters;

import edu.illinois.cs.dt.tools.runner.data.DependentTest;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class UniqueFilter implements Filter {
    private final Set<String> prevTests = new HashSet<>();

    public Set<String> prevTests() {
        return prevTests;
    }

    @Override
    public boolean keep(final DependentTest dependentTest, final int absoluteRound) {
        final boolean found = prevTests.contains(dependentTest.name());
        prevTests.add(dependentTest.name());
        return !found;
    }
}
