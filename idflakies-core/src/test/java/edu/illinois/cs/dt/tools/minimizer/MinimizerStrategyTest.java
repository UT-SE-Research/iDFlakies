package edu.illinois.cs.dt.tools.minimizer;

import edu.illinois.cs.dt.tools.minimizer.ranking.HeuristicType;
import edu.illinois.cs.dt.tools.minimizer.splitting.HalfSplitStrategy;
import edu.illinois.cs.dt.tools.minimizer.splitting.HierarchicalSplitStrategy;
import edu.illinois.cs.dt.tools.minimizer.splitting.HistoricalRankFSplitStrategy;
import edu.illinois.cs.dt.tools.minimizer.splitting.TFIDFSplitStrategy;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MinimizerStrategyTest {

    @After
    public void clearProperty() {
        System.clearProperty("dt.minimizer.strategy");
    }

    @Test
    public void defaultStrategyIsHalfSplit() {
        System.clearProperty("dt.minimizer.strategy");
        assertEquals(MinimizerStrategy.DD_HALF_SPLIT, MinimizerStrategy.fromProperty());
    }

    @Test
    public void rankfoStrategiesReportIsRankFOTrue() {
        assertTrue(MinimizerStrategy.RANKFO_PLUS_ONE.isRankFO());
        assertTrue(MinimizerStrategy.RANKFO_METHODS.isRankFO());
        assertTrue(MinimizerStrategy.RANKFO_DISTANCE_D.isRankFO());
        assertTrue(MinimizerStrategy.RANKFO_COMBINED_P1_D.isRankFO());
        assertTrue(MinimizerStrategy.RANKFO_COMBINED_M_D.isRankFO());
    }

    @Test
    public void ddStrategiesReportIsRankFOFalse() {
        assertFalse(MinimizerStrategy.DD_HALF_SPLIT.isRankFO());
        assertFalse(MinimizerStrategy.DD_HIERARCHICAL_SPLIT.isRankFO());
        assertFalse(MinimizerStrategy.DD_HISTORICAL_INFO_SPLIT.isRankFO());
        assertFalse(MinimizerStrategy.DD_NLP_SPLIT.isRankFO());
    }

    @Test
    public void heuristicMapsCorrectlyForAllRankFOStrategies() {
        assertEquals(HeuristicType.PLUS_ONE,
                     MinimizerStrategy.RANKFO_PLUS_ONE.heuristic());
        assertEquals(HeuristicType.METHODS,
                     MinimizerStrategy.RANKFO_METHODS.heuristic());
        assertEquals(HeuristicType.DISTANCE,
                     MinimizerStrategy.RANKFO_DISTANCE_D.heuristic());
        assertEquals(HeuristicType.COMBINED_PLUS_ONE_DISTANCE,
                     MinimizerStrategy.RANKFO_COMBINED_P1_D.heuristic());
        assertEquals(HeuristicType.COMBINED_METHODS_DISTANCE,
                     MinimizerStrategy.RANKFO_COMBINED_M_D.heuristic());
    }

    @Test(expected = IllegalStateException.class)
    public void heuristicThrowsForDdStrategy() {
        MinimizerStrategy.DD_HALF_SPLIT.heuristic();
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromPropertyThrowsForUnknownValue() {
        System.setProperty("dt.minimizer.strategy", "INVALID_STRATEGY");
        MinimizerStrategy.fromProperty();
    }

    @Test
    public void fromPropertyParsesAllValidStrategyNames() {
        for (MinimizerStrategy expected : MinimizerStrategy.values()) {
            System.setProperty("dt.minimizer.strategy", expected.name());
            assertEquals(expected, MinimizerStrategy.fromProperty());
        }
    }

    // ── DDSplittingStrategy implementations ─────────────────────────────────

    @Test
    public void halfSplitPartitionsEvenly() {
        HalfSplitStrategy s = new HalfSplitStrategy();
        List<List<String>> parts = s.partition(Arrays.asList("A", "B", "C", "D"), 2);
        assertEquals(2, parts.size());
        assertEquals(Arrays.asList("A", "B"), parts.get(0));
        assertEquals(Arrays.asList("C", "D"), parts.get(1));
    }

    @Test
    public void halfSplitSingleElementReturnsOneChunk() {
        HalfSplitStrategy s = new HalfSplitStrategy();
        List<List<String>> parts = s.partition(Arrays.asList("A"), 2);
        assertEquals(1, parts.size());
        assertEquals(Arrays.asList("A"), parts.get(0));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void hierarchicalStubThrows() {
        new HierarchicalSplitStrategy().partition(Arrays.asList("A"), 2);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void historicalRankFStubThrows() {
        new HistoricalRankFSplitStrategy().partition(Arrays.asList("A"), 2);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void tfidfStubThrows() {
        new TFIDFSplitStrategy().partition(Arrays.asList("A"), 2);
    }
}
