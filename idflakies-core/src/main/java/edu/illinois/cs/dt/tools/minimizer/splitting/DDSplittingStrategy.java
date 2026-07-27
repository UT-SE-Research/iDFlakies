package edu.illinois.cs.dt.tools.minimizer.splitting;

import java.util.List;

public interface DDSplittingStrategy {
    /**
     * Partition candidates into at most n equal-ish sublists for one DD iteration.
     *
     * @param candidates non-empty list of candidate tests
     * @param n          requested partition count (usually 2)
     * @return non-empty list of non-overlapping sublists whose union equals candidates
     */
    List<List<String>> partition(List<String> candidates, int n);
}
