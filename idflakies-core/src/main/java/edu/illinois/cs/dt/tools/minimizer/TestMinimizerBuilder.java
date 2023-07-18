package edu.illinois.cs.dt.tools.minimizer;

import edu.illinois.cs.dt.tools.runner.InstrumentingSmartRunner;

import java.util.ArrayList;
import java.util.List;

public class TestMinimizerBuilder {
    private final List<String> testOrder;
    private final String dependentTest;
    private InstrumentingSmartRunner runner;

    public TestMinimizerBuilder(final InstrumentingSmartRunner runner) {
        this.runner = runner;

        testOrder = new ArrayList<>();
        dependentTest = "";
    }

    public TestMinimizerBuilder(final List<String> testOrder, final String dependentTest,
                                final InstrumentingSmartRunner runner) {
        this.testOrder = testOrder;
        this.dependentTest = dependentTest;
        this.runner = runner;
    }

    public TestMinimizerBuilder testOrder(final List<String> testOrder) {
        return new TestMinimizerBuilder(testOrder, this.dependentTest, this.runner);
    }

    public TestMinimizerBuilder dependentTest(final String dependentTest) {
        return new TestMinimizerBuilder(this.testOrder, dependentTest, this.runner);
    }

    public TestMinimizerBuilder runner(final InstrumentingSmartRunner runner) {
        return new TestMinimizerBuilder(this.testOrder, this.dependentTest, runner);
    }

    public TestMinimizer build() {
        return new TestMinimizer(this.testOrder, this.runner, this.dependentTest);
    }

    public TestMinimizer buildNOD() {
        return new NODTestMinimizer(this.testOrder, this.runner, this.dependentTest);
    }
}
