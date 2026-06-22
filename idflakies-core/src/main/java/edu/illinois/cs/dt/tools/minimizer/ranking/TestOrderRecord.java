package edu.illinois.cs.dt.tools.minimizer.ranking;

import edu.illinois.cs.testrunner.data.results.Result;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TestOrderRecord {
    private final List<String> testOrder;
    private final Map<String, Result> testResults;

    public TestOrderRecord(List<String> testOrder, Map<String, Result> testResults) {
        this.testOrder = Collections.unmodifiableList(new ArrayList<>(testOrder));
        this.testResults = Collections.unmodifiableMap(new HashMap<>(testResults));
    }

    /** Returns tests appearing strictly before targetTest. Empty if targetTest not in order. */
    public List<String> testsBeforeTarget(String targetTest) {
        int idx = testOrder.indexOf(targetTest);
        if (idx < 0) return Collections.emptyList();
        return Collections.unmodifiableList(testOrder.subList(0, idx));
    }

    /** Returns null if testName was not run in this round. */
    public Result getResult(String testName) {
        return testResults.get(testName);
    }

    public List<String> getTestOrder() {
        return testOrder;
    }
}
