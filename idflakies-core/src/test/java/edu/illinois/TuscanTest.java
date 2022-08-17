package edu.illinois;

import java.util.*;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import edu.illinois.cs.dt.tools.utility.Tuscan;

@RunWith(Theories.class)
public class TuscanTest {

    @DataPoints
    public static int[] integers() {
        int[] range = IntStream.rangeClosed(1, 20).toArray();
        return range;
    }

    @Theory
    public void test(int n) throws Exception {
        int[][] matrix = Tuscan.generateTuscanPermutations(n);
        Set<List<Integer>> finalPairs = new LinkedHashSet<List<Integer>>();
        Set<List<Integer>> visitedPairs = new LinkedHashSet<List<Integer>>();
        for (int i = 0; i < matrix.length; i++) {
            List<List<Integer>> allPairs = generateAllPairs(matrix[i]);
            for (int j = 0; j < matrix[i].length - 2; j++) {
                // All rows will have an extra 0 at the end hence we have -2
                List<Integer> newPair = new ArrayList<Integer>();
                newPair.add(matrix[i][j]);
                newPair.add(matrix[i][j + 1]);
                if (!allPairs.contains(newPair)) {
                    throw new Exception("Does not contain pair");
                }
                visitedPairs.add(newPair);
            }
            finalPairs.addAll(allPairs);
        }
        Assert.assertEquals(finalPairs.size(), visitedPairs.size());
    }
    
    private static List<List<Integer>> generateAllPairs (int[] row) {
        List<List<Integer>> allPairs = new ArrayList<>();
        for (int i = 0; i < row.length - 1; i++) {
            for (int j = 0; j < row.length - 1; j++) {
                if (row[i] != row[j]) {
                    List<Integer> newPair = new ArrayList<Integer>();
                    newPair.add(row[i]);
                    newPair.add(row[j]);
                    allPairs.add(newPair);
                }
            }
        }
        return allPairs;
    }
}
