package edu.illinois.cs.dt.tools.minimizer;

import edu.illinois.cs.dt.tools.runner.InstrumentingSmartRunner;
import edu.illinois.cs.dt.tools.utility.OperationTime;

import java.util.ArrayList;
import java.util.List;

// A sort of dummy class to represent trying to minimize a test found to be NOD, simply returns the MinimizedTestsResult that is NOD
// Should behave just like TestMinimizer, just does not do any actual work
public class NODTestMinimizer extends TestMinimizer {

    public NODTestMinimizer(final List<String> testOrder, final InstrumentingSmartRunner runner, final String dependentTest) {
        super(testOrder, runner, dependentTest);
    }

    @Override
    public MinimizeTestsResult run() throws Exception {
        // Dummy call, does not do any work and returns a MinimizeTestsResult marked with NOD
        return new MinimizeTestsResult(OperationTime.instantaneous(), expectedRun, expected, dependentTest, new ArrayList<PolluterData>(), FlakyClass.NOD);
    }

}
