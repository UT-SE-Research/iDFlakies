package edu.illinois.cs.dt.tools.minimizer.splitting;

import java.util.ArrayList;
import java.util.List;

public class HalfSplitStrategy implements DDSplittingStrategy {

    @Override
    public List<List<String>> partition(List<String> candidates, int n) {
        List<List<String>> result = new ArrayList<>();
        int size = Math.max(1, candidates.size() / n);
        for (int i = 0; i < candidates.size(); i += size) {
            result.add(new ArrayList<>(
                candidates.subList(i, Math.min(i + size, candidates.size()))));
        }
        return result;
    }
}
