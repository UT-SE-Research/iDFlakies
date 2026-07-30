package edu.illinois.cs.dt.tools.minimizer.ranking;

public final class ScoredCandidate {
    private final String testName;
    private final double polluterScore;
    private final double nonPolluterScore;

    public ScoredCandidate(String testName, double polluterScore, double nonPolluterScore) {
        this.testName = testName;
        this.polluterScore = polluterScore;
        this.nonPolluterScore = nonPolluterScore;
    }

    public String getTestName()        { return testName; }
    public double getPolluterScore()   { return polluterScore; }
    public double getNonPolluterScore(){ return nonPolluterScore; }
}
