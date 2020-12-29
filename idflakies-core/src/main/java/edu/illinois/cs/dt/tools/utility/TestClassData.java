package edu.illinois.cs.dt.tools.utility;

import java.util.List;

public class TestClassData {
    public String className;
    public List<String> testNames;
    public double classTime;

    public TestClassData(String className, List<String> testNames, double classTime) {
        this.className = className;
        this.testNames = testNames;
        this.classTime = classTime;
    }

    public double classTime() {
        return classTime;
    }

    public String className() {
        return className;
    }

    public List<String> testNames() {
        return testNames;
    }
}
