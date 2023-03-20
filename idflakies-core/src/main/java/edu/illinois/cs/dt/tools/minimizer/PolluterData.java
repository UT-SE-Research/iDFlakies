package edu.illinois.cs.dt.tools.minimizer;

import edu.illinois.cs.dt.tools.utility.OperationTime;

import java.util.ArrayList;
import java.util.List;

import edu.illinois.cs.dt.tools.minimizer.cleaner.CleanerData;

public class PolluterData {
    private final OperationTime time;
    private final int index;            // The index of when this polluter was found (0 is first)
    private final List<String> deps;
    private final CleanerData cleanerData;

    public PolluterData(final OperationTime time, final int index, final List<String> deps, final CleanerData cleanerData) {
        this.time = time;
        this.index = index;
        this.deps = deps;
        this.cleanerData = cleanerData;
    }

    public OperationTime time() {
        return time;
    }

    public int index() {
        return index;
    }

    public List<String> deps() {
        return deps;
    }

    public CleanerData cleanerData() {
        return cleanerData;
    }

    public List<String> withDeps(final String dependentTest) {
        final List<String> order = new ArrayList<>(deps);
        if (!order.contains(dependentTest)) {
            order.add(dependentTest);
        }
        return order;
    }
}
