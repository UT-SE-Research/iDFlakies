package edu.illinois.cs.dt.tools.minimizer;

import edu.illinois.cs.dt.tools.minimizer.ranking.HeuristicType;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MinimizerStrategyTest {

    @After
    public void clearProperty() {
        System.clearProperty("dt.minimizer.strategy");
    }

    @Test
    public void fromPropertyReturnsNullWhenNotSet() {
        System.clearProperty("dt.minimizer.strategy");
        assertNull(MinimizerStrategy.fromProperty());
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

}
