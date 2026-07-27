package edu.illinois.cs.dt.tools.minimizer;

import edu.illinois.cs.dt.tools.minimizer.ranking.HeuristicType;

import java.util.Arrays;

public enum MinimizerStrategy {
    RANKFO_PLUS_ONE,
    RANKFO_METHODS,
    RANKFO_DISTANCE_D,
    RANKFO_COMBINED_P1_D,
    RANKFO_COMBINED_M_D,
    DD_HALF_SPLIT,
    DD_HIERARCHICAL_SPLIT,
    DD_HISTORICAL_INFO_SPLIT,
    DD_NLP_SPLIT;

    public boolean isRankFO() {
        return name().startsWith("RANKFO_");
    }

    public HeuristicType heuristic() {
        switch (this) {
            case RANKFO_PLUS_ONE:       return HeuristicType.PLUS_ONE;
            case RANKFO_METHODS:        return HeuristicType.METHODS;
            case RANKFO_DISTANCE_D:     return HeuristicType.DISTANCE;
            case RANKFO_COMBINED_P1_D:  return HeuristicType.COMBINED_PLUS_ONE_DISTANCE;
            case RANKFO_COMBINED_M_D:   return HeuristicType.COMBINED_METHODS_DISTANCE;
            default:
                throw new IllegalStateException(
                    "heuristic() called on non-RankFO strategy: " + this);
        }
    }

    public static MinimizerStrategy fromProperty() {
        String val = System.getProperty("dt.minimizer.strategy", "DD_HALF_SPLIT");
        try {
            return valueOf(val.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Unknown dt.minimizer.strategy value: '" + val
                + "'. Valid values: " + Arrays.toString(values()), e);
        }
    }
}
