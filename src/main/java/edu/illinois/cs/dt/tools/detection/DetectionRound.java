package edu.illinois.cs.dt.tools.detection;

import com.google.gson.Gson;
import edu.illinois.cs.dt.tools.runner.data.DependentTest;
import edu.illinois.cs.dt.tools.runner.data.DependentTestList;

import java.util.List;

/**
 * Simple wrapper around the results of each round run by an ExecutingDetector
 */
public class DetectionRound {
    private final List<String> testRunIds;

    private final DependentTestList unfilteredTests;
    private final DependentTestList filteredTests;
    private final double roundTime;

    public DetectionRound(final List<String> testRunIds, final List<DependentTest> unfiltered, final List<DependentTest> filtered, final double roundTime) {
        this.testRunIds = testRunIds;
        this.unfilteredTests = new DependentTestList(unfiltered);
        this.filteredTests = new DependentTestList(filtered);
        this.roundTime = roundTime;
    }

    public List<String> testRunIds() {
        return testRunIds;
    }

    public double roundTime() {
        return roundTime;
    }

    public DependentTestList unfilteredTests() {
        return unfilteredTests;
    }

    public DependentTestList filteredTests() {
        return filteredTests;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
