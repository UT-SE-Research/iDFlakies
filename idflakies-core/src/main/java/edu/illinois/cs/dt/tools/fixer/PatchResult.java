package edu.illinois.cs.dt.tools.fixer;

import edu.illinois.cs.dt.tools.utility.OperationTime;

import java.nio.file.Path;

public class PatchResult {

    private OperationTime time;
    private FixStatus status;
    private String dependentTest;
    private String polluter;
    private String cleaner;
    private int iterations;
    private String patchLocation;

    public PatchResult(final OperationTime time, final FixStatus status,
                       final String dependentTest, final String polluter, final String cleaner,
                       final int iterations, final String patchLocation) {
        this.time = time;
        this.status = status;
        this.dependentTest = dependentTest;
        this.polluter = polluter;
        this.cleaner = cleaner;
        this.iterations = iterations;
        this.patchLocation = patchLocation;
    }

    public OperationTime time() {
        return this.time;
    }

    public FixStatus status() {
        return this.status;
    }

    public String dependentTest() {
        return this.dependentTest;
    }

    public String polluter() {
        return this.polluter == null ? "N/A" : this.polluter;
    }

    public int iterations() {
        return this.iterations;
    }

    public String patchLocation() {
        return this.patchLocation;
    }
}
