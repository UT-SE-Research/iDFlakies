package edu.illinois.cs.dt.tools.poldet;

import java.util.List;

public class PolDetResult {

    private List<String> polluters;

    public PolDetResult(final List<String> polluters) {
        this.polluters = polluters;
    }

    public List<String> polluters() {
        return this.polluters;
    }
}
