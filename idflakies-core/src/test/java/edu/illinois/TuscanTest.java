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
    private static int[][] matrix;

    @DataPoints
    public static int[] integers() {
        int[] range = IntStream.rangeClosed(2, 20).toArray();
        return range;
    }

    @Theory
    public void test(int n) throws Exception {
        int count;
        List<Pair<Integer>> s = new ArrayList<Pair<Integer>>();
        matrix = Tuscan.generateTuscanPermutations(n);
        // System.out.println("Tuscan " + n + ":");
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length - 2; j++) {
                Pair<Integer> newPair = new Pair<Integer>(matrix[i][j], matrix[i][j + 1]);
                if (n != 3 && n != 5 && s.contains(newPair)) {
                    throw new Exception("Not unique");
                }
                s.add(newPair);
            }
        }
        count = s.size();
        if (n == 5) {
            Assert.assertEquals(count, 21);
        } else if (n == 3) {
            Assert.assertEquals(count, 8);
        } else {
            Assert.assertEquals(n * (n - 1), count);
        }
    }
}